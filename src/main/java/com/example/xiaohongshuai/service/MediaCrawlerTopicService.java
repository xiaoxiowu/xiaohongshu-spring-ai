package com.example.xiaohongshuai.service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.example.xiaohongshuai.vo.TopicAnalysisVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MediaCrawlerTopicService {

    private final ObjectMapper objectMapper;
    private final Path workDir;
    private final String command;
    private final boolean getComments;
    private final int maxCommentsPerNote;

    public MediaCrawlerTopicService(ObjectMapper objectMapper,
            @Value("${xhs.mediacrawler.work-dir:E:/workspace/MediaCrawler-main}") String workDir,
            @Value("${xhs.mediacrawler.command:uv}") String command,
            @Value("${xhs.mediacrawler.get-comments:false}") boolean getComments,
            @Value("${xhs.mediacrawler.max-comments-per-note:5}") int maxCommentsPerNote) {
        this.objectMapper = objectMapper;
        this.workDir = Path.of(workDir);
        this.command = command;
        this.getComments = getComments;
        this.maxCommentsPerNote = maxCommentsPerNote;
    }

    public MediaCrawlerResult search(String keyword, int limit, String sortBy, String noteType, String publishTime) {
        return search(keyword, limit, sortBy, noteType, publishTime, ignored -> {
        });
    }

    public MediaCrawlerResult search(String keyword, int limit, String sortBy, String noteType, String publishTime,
            Consumer<String> progress) {
        return search(keyword, limit, sortBy, noteType, publishTime, progress, ignored -> {
        });
    }

    public MediaCrawlerResult search(String keyword, int limit, String sortBy, String noteType, String publishTime,
            Consumer<String> progress, Consumer<List<TopicAnalysisVO.FeedSample>> sampleProgress) {
        long startedAt = System.currentTimeMillis();
        runCrawler(keyword, limit, sortBy, noteType, publishTime, startedAt, progress, sampleProgress);
        progress.accept("MediaCrawler 执行完成，开始读取 JSONL 结果");
        SampleSnapshot snapshot = collectSamples(keyword, limit, sortBy, noteType, publishTime, startedAt);
        List<TopicAnalysisVO.FeedSample> samples = snapshot.samples();
        sampleProgress.accept(samples);

        progress.accept("JSONL 解析完成，匹配到 " + samples.size() + " 条笔记");
        return new MediaCrawlerResult(samples, toJson(snapshot.contentNodes()));
    }

    private void runCrawler(String keyword, int limit, String sortBy, String noteType, String publishTime,
            long startedAt, Consumer<String> progress, Consumer<List<TopicAnalysisVO.FeedSample>> sampleProgress) {
        if (!Files.isDirectory(workDir)) {
            throw new IllegalStateException("MediaCrawler 目录不存在：" + workDir);
        }

        List<String> commandLine = new ArrayList<>();
        commandLine.add(resolveCommand());
        commandLine.add("run");
        commandLine.add("main.py");
        commandLine.add("--platform");
        commandLine.add("xhs");
        commandLine.add("--lt");
        commandLine.add("qrcode");
        commandLine.add("--type");
        commandLine.add("search");
        commandLine.add("--keywords");
        commandLine.add(keyword);
        commandLine.add("--save_data_option");
        commandLine.add("jsonl");
        commandLine.add("--get_comment");
        commandLine.add(getComments ? "yes" : "no");
        commandLine.add("--get_sub_comment");
        commandLine.add("no");
        commandLine.add("--get_media");
        commandLine.add("no");
        commandLine.add("--crawler_max_notes_count");
        commandLine.add(String.valueOf(limit));
        commandLine.add("--max_comments_count_singlenotes");
        commandLine.add(String.valueOf(maxCommentsPerNote));
        commandLine.add("--max_concurrency_num");
        commandLine.add("1");
        progress.accept("启动命令：" + String.join(" ", commandLine));

        ProcessBuilder builder = new ProcessBuilder(commandLine)
                .directory(workDir.toFile())
                .redirectErrorStream(true);
        try {
            Process process = builder.start();
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    progress.accept(cleanProgressLine(line));
                    if (line.contains("content_item:") || line.contains("xhs note comment:")) {
                        SampleSnapshot snapshot = collectSamples(keyword, limit, sortBy, noteType, publishTime,
                                startedAt);
                        sampleProgress.accept(snapshot.samples());
                    }
                    if (output.length() < 6000) {
                        output.append(line).append('\n');
                    }
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IllegalStateException("MediaCrawler 执行失败，退出码：" + exitCode + "\n" + output);
            }
            progress.accept("MediaCrawler 进程已正常结束");
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("MediaCrawler 执行被中断", ex);
        }
        catch (Exception ex) {
            throw new IllegalStateException("MediaCrawler 执行失败：" + ex.getMessage(), ex);
        }
    }

    private SampleSnapshot collectSamples(String keyword, int limit, String sortBy, String noteType, String publishTime,
            long startedAt) {
        List<JsonNode> contentNodes = readJsonl("search_contents_*.jsonl", keyword, startedAt);
        Map<String, List<String>> commentsByNoteId = getComments
                ? collectComments(readJsonl("search_comments_*.jsonl", null, startedAt)) : Map.of();
        List<TopicAnalysisVO.FeedSample> samples = contentNodes.stream()
                .map(node -> toSample(node, commentsByNoteId.getOrDefault(text(node, "note_id"), List.of())))
                .filter(sample -> matchesNoteType(sample, noteType))
                .filter(sample -> matchesPublishTime(sample.rawDetailResponse(), publishTime))
                .sorted(comparator(sortBy))
                .limit(limit)
                .toList();
        return new SampleSnapshot(contentNodes, samples);
    }

    private String cleanProgressLine(String line) {
        if (line == null) {
            return "";
        }
        String trimmed = line.strip();
        int contentIndex = trimmed.indexOf("content_item:");
        if (contentIndex >= 0) {
            return "采集到笔记：" + trimmed.substring(contentIndex + "content_item:".length()).strip();
        }
        int commentIndex = trimmed.indexOf("xhs note comment:");
        if (commentIndex >= 0) {
            return "采集到评论：" + trimmed.substring(commentIndex + "xhs note comment:".length()).strip();
        }
        return trimmed;
    }

    private String resolveCommand() {
        Path configuredPath = Path.of(command);
        if (configuredPath.isAbsolute()) {
            return command;
        }
        Path localUv = Path.of(System.getProperty("user.home"), ".local", "bin", "uv.exe");
        if ("uv".equalsIgnoreCase(command) && Files.isRegularFile(localUv)) {
            return localUv.toString();
        }
        return command;
    }

    private List<JsonNode> readJsonl(String glob, String keyword, long startedAt) {
        Path jsonlDir = workDir.resolve("data").resolve("xhs").resolve("jsonl");
        if (!Files.isDirectory(jsonlDir)) {
            return List.of();
        }
        long fromTs = startedAt - 5000;
        List<JsonNode> result = new ArrayList<>();
        try (Stream<Path> files = Files.list(jsonlDir)) {
            List<Path> paths = files
                    .filter(path -> path.getFileName().toString().matches(glob.replace("*", ".*")))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
            for (Path path : paths) {
                for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
                    JsonNode node = parse(line);
                    if (node == null) {
                        continue;
                    }
                    if (keyword != null && !keyword.equals(text(node, "source_keyword"))) {
                        continue;
                    }
                    Long lastModifyTs = longValue(node, "last_modify_ts");
                    if (lastModifyTs != null && lastModifyTs < fromTs) {
                        continue;
                    }
                    result.add(node);
                }
            }
        }
        catch (Exception ex) {
            throw new IllegalStateException("读取 MediaCrawler 结果失败：" + ex.getMessage(), ex);
        }
        return result;
    }

    private Map<String, List<String>> collectComments(List<JsonNode> comments) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (JsonNode comment : comments) {
            String noteId = text(comment, "note_id");
            String content = text(comment, "content");
            if (isBlank(noteId) || isBlank(content)) {
                continue;
            }
            result.computeIfAbsent(noteId, ignored -> new ArrayList<>());
            List<String> noteComments = result.get(noteId);
            if (noteComments.size() < 5 && !noteComments.contains(content)) {
                noteComments.add(content);
            }
        }
        return result;
    }

    private TopicAnalysisVO.FeedSample toSample(JsonNode node, List<String> comments) {
        String noteId = text(node, "note_id");
        return new TopicAnalysisVO.FeedSample(
                noteId,
                text(node, "xsec_token"),
                text(node, "title"),
                text(node, "desc"),
                text(node, "nickname"),
                firstText(text(node, "note_url"), isBlank(noteId) ? null : "https://www.xiaohongshu.com/explore/" + noteId),
                intValue(node, "liked_count"),
                intValue(node, "collected_count"),
                intValue(node, "comment_count"),
                intValue(node, "share_count"),
                comments,
                node.toString());
    }

    private boolean matchesNoteType(TopicAnalysisVO.FeedSample sample, String noteType) {
        if (isBlank(noteType) || "不限".equals(noteType) || isBlank(sample.rawDetailResponse())) {
            return true;
        }
        JsonNode node = parse(sample.rawDetailResponse());
        String type = text(node, "type");
        if ("视频".equals(noteType)) {
            return "video".equalsIgnoreCase(type);
        }
        if ("图文".equals(noteType)) {
            return !"video".equalsIgnoreCase(type);
        }
        return true;
    }

    private boolean matchesPublishTime(String raw, String publishTime) {
        if (isBlank(publishTime) || "不限".equals(publishTime) || isBlank(raw)) {
            return true;
        }
        JsonNode node = parse(raw);
        Long time = longValue(node, "time");
        if (time == null) {
            return true;
        }
        long now = System.currentTimeMillis();
        return switch (publishTime) {
            case "一天内" -> time >= now - 24L * 60 * 60 * 1000;
            case "一周内" -> time >= now - 7L * 24 * 60 * 60 * 1000;
            case "半年内" -> time >= now - 183L * 24 * 60 * 60 * 1000;
            default -> true;
        };
    }

    private Comparator<TopicAnalysisVO.FeedSample> comparator(String sortBy) {
        Comparator<TopicAnalysisVO.FeedSample> byLikes = Comparator.comparing(
                sample -> value(sample.likes()), Comparator.reverseOrder());
        if ("最多评论".equals(sortBy)) {
            return Comparator.comparing((TopicAnalysisVO.FeedSample sample) -> value(sample.comments()),
                    Comparator.reverseOrder());
        }
        if ("最多收藏".equals(sortBy)) {
            return Comparator.comparing((TopicAnalysisVO.FeedSample sample) -> value(sample.collects()),
                    Comparator.reverseOrder());
        }
        if ("最新".equals(sortBy)) {
            return Comparator.comparing((TopicAnalysisVO.FeedSample sample) -> publishTime(sample.rawDetailResponse()),
                    Comparator.reverseOrder());
        }
        return byLikes;
    }

    private long publishTime(String raw) {
        Long value = longValue(parse(raw), "time");
        return value == null ? 0 : value;
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }

    private JsonNode parse(String value) {
        if (isBlank(value)) {
            return null;
        }
        try {
            return objectMapper.readTree(value);
        }
        catch (Exception ex) {
            return null;
        }
    }

    private String text(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return null;
        }
        String value = node.get(field).asText();
        return value.isBlank() ? null : value;
    }

    private Integer intValue(JsonNode node, String field) {
        Long value = longValue(node, field);
        if (value == null) {
            return null;
        }
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : value.intValue();
    }

    private Long longValue(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("[^0-9]", "");
        if (normalized.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(normalized);
        }
        catch (NumberFormatException ex) {
            return null;
        }
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (!isBlank(value)) {
                return value;
            }
        }
        return null;
    }

    private String toJson(List<JsonNode> nodes) {
        Set<String> unique = new LinkedHashSet<>();
        nodes.forEach(node -> unique.add(node.toString()));
        return "[" + String.join(",", unique) + "]";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record MediaCrawlerResult(
            List<TopicAnalysisVO.FeedSample> samples,
            String rawSearchResponse) {
    }

    private record SampleSnapshot(
            List<JsonNode> contentNodes,
            List<TopicAnalysisVO.FeedSample> samples) {
    }
}
