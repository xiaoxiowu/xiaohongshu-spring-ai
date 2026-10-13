package com.example.xiaohongshuai.service.impl;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.xiaohongshuai.config.McpRegistry;
import com.example.xiaohongshuai.entity.ImageAsset;
import com.example.xiaohongshuai.mapper.ImageAssetMapper;
import com.example.xiaohongshuai.service.ImageAssetService;
import com.example.xiaohongshuai.service.McpClientService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImageAssetServiceImpl implements ImageAssetService {

    private static final Pattern HTTP_IMAGE_PATTERN = Pattern.compile("https?://[^\\s\"'\\\\]+");
    private static final Pattern DATA_IMAGE_PATTERN = Pattern.compile("data:image/([a-zA-Z0-9.+-]+);base64,([A-Za-z0-9+/=\\r\\n]+)");
    private static final String NO_TEXT_PROMPT = "画面中不要出现任何文字、中文、英文字母、数字、logo、水印、商标、标题、标语、段落、UI 文案或排版文字。";

    private final ImageAssetMapper imageAssetMapper;
    private final McpClientService mcpClientService;
    private final ObjectMapper objectMapper;
    private final Path imageDir;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    public ImageAssetServiceImpl(ImageAssetMapper imageAssetMapper, McpClientService mcpClientService,
            ObjectMapper objectMapper, @Value("${xhs.assets.image-dir:./data/assets/images}") String imageDir) {
        this.imageAssetMapper = imageAssetMapper;
        this.mcpClientService = mcpClientService;
        this.objectMapper = objectMapper;
        this.imageDir = Paths.get(imageDir).toAbsolutePath().normalize();
    }

    @Override
    @Transactional
    public ImageAsset generate(GenerateImageRequest request) {
        if (request == null || request.prompt() == null || request.prompt().isBlank()) {
            throw new IllegalArgumentException("prompt is required");
        }
        if (!mcpClientService.hasTool(McpRegistry.Tools.JIMENG_GENERATE_IMAGE)) {
            throw new IllegalStateException("MCP tool not found: " + McpRegistry.Tools.JIMENG_GENERATE_IMAGE);
        }

        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("prompt", buildImagePrompt(request.prompt()));
        if (!isBlank(request.aspectRatio())) {
            arguments.put("aspect_ratio", request.aspectRatio());
        }

        McpSchema.CallToolResult result = mcpClientService.callTool(McpRegistry.Tools.JIMENG_GENERATE_IMAGE, arguments);
        String rawResponse = mcpClientService.toJson(result);
        StoredImage storedImage = storeFromResponse(rawResponse);
        return createAsset(
                firstText(request.name(), "即梦图片"),
                request.prompt(),
                request.tags(),
                request.usageScene(),
                request.aspectRatio(),
                McpRegistry.AssetSources.JIMENG,
                storedImage.localPath(),
                storedImage.originalUrl(),
                rawResponse,
                request.remark());
    }

    @Override
    @Transactional
    public ImageAsset importUrl(ImportImageUrlRequest request) {
        if (request == null || isBlank(request.url())) {
            throw new IllegalArgumentException("url is required");
        }
        StoredImage storedImage = downloadImage(request.url());
        return createAsset(
                firstText(request.name(), "导入图片"),
                request.prompt(),
                request.tags(),
                request.usageScene(),
                request.aspectRatio(),
                McpRegistry.AssetSources.URL,
                storedImage.localPath(),
                storedImage.originalUrl(),
                null,
                request.remark());
    }

    @Override
    public List<ImageAsset> list(String keyword, String tag, String source, String status) {
        QueryWrapper<ImageAsset> query = new QueryWrapper<>();
        if (!isBlank(keyword)) {
            query.and(wrapper -> wrapper.like("name", keyword)
                    .or()
                    .like("prompt", keyword)
                    .or()
                    .like("remark", keyword));
        }
        if (!isBlank(tag)) {
            query.like("tags", tag);
        }
        if (!isBlank(source)) {
            query.eq("source", source);
        }
        if (!isBlank(status)) {
            query.eq("status", status);
        }
        query.orderByDesc("created_at");
        return imageAssetMapper.selectList(query);
    }

    @Override
    public ImageAsset getRequired(Long id) {
        ImageAsset asset = imageAssetMapper.selectById(id);
        if (asset == null) {
            throw new IllegalArgumentException("Image asset not found: " + id);
        }
        return asset;
    }

    @Override
    public List<ImageAsset> listByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return imageAssetMapper.selectBatchIds(ids);
    }

    @Override
    @Transactional
    public ImageAsset update(Long id, UpdateImageAssetRequest request) {
        ImageAsset asset = getRequired(id);
        if (request.name() != null) {
            asset.setName(request.name());
        }
        if (request.tags() != null) {
            asset.setTags(request.tags());
        }
        if (request.usageScene() != null) {
            asset.setUsageScene(request.usageScene());
        }
        if (request.aspectRatio() != null) {
            asset.setAspectRatio(request.aspectRatio());
        }
        if (request.status() != null) {
            asset.setStatus(request.status());
        }
        if (request.remark() != null) {
            asset.setRemark(request.remark());
        }
        asset.setUpdatedAt(LocalDateTime.now());
        imageAssetMapper.updateById(asset);
        return asset;
    }

    @Override
    public Resource file(Long id) {
        ImageAsset asset = getRequired(id);
        Path file = Paths.get(asset.getLocalPath()).toAbsolutePath().normalize();
        if (!file.startsWith(imageDir) || !Files.exists(file)) {
            throw new IllegalArgumentException("Image file not found: " + id);
        }
        return new FileSystemResource(file);
    }

    private ImageAsset createAsset(String name, String prompt, String tags, String usageScene,
            String aspectRatio, String source, String localPath, String originalUrl,
            String mcpResponse, String remark) {
        LocalDateTime now = LocalDateTime.now();
        ImageAsset asset = new ImageAsset();
        asset.setName(name);
        asset.setPrompt(prompt);
        asset.setTags(tags);
        asset.setUsageScene(usageScene);
        asset.setAspectRatio(aspectRatio);
        asset.setSource(source);
        asset.setStatus("ACTIVE");
        asset.setLocalPath(localPath);
        asset.setOriginalUrl(originalUrl);
        asset.setMcpResponse(mcpResponse);
        asset.setRemark(remark);
        asset.setCreatedAt(now);
        asset.setUpdatedAt(now);
        imageAssetMapper.insert(asset);
        return asset;
    }

    private StoredImage storeFromResponse(String rawResponse) {
        String dataImage = findDataImage(rawResponse);
        if (dataImage != null) {
            return saveDataImage(dataImage);
        }
        String url = findImageUrl(rawResponse);
        if (url != null) {
            return downloadImage(url);
        }
        String base64 = findBase64(rawResponse);
        if (base64 != null) {
            return saveBase64(base64, "png", null);
        }
        throw new IllegalStateException("No image data found in jimeng MCP response: " + rawResponse);
    }

    private StoredImage downloadImage(String url) {
        try {
            url = trimUrl(url);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Download image failed, status: " + response.statusCode());
            }
            String contentType = response.headers().firstValue("content-type").orElse("");
            String extension = extensionFromContentType(contentType);
            Path file = writeFile(response.body(), extension);
            return new StoredImage(file.toString(), url);
        }
        catch (IOException ex) {
            throw new IllegalStateException("Download image failed: " + ex.getMessage(), ex);
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Download image interrupted", ex);
        }
    }

    private StoredImage saveDataImage(String dataImage) {
        Matcher matcher = DATA_IMAGE_PATTERN.matcher(dataImage);
        if (!matcher.find()) {
            throw new IllegalStateException("Invalid data image");
        }
        return saveBase64(matcher.group(2), matcher.group(1), null);
    }

    private StoredImage saveBase64(String value, String extension, String originalUrl) {
        try {
            byte[] bytes = Base64.getDecoder().decode(value.replaceAll("\\s+", ""));
            Path file = writeFile(bytes, normalizeExtension(extension));
            return new StoredImage(file.toString(), originalUrl);
        }
        catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Decode base64 image failed", ex);
        }
    }

    private Path writeFile(byte[] bytes, String extension) {
        try {
            Files.createDirectories(imageDir);
            Path file = imageDir.resolve(UUID.randomUUID() + "." + normalizeExtension(extension)).normalize();
            Files.write(file, bytes);
            return file;
        }
        catch (IOException ex) {
            throw new IllegalStateException("Save image failed: " + ex.getMessage(), ex);
        }
    }

    private String findDataImage(String text) {
        Matcher matcher = DATA_IMAGE_PATTERN.matcher(text);
        return matcher.find() ? matcher.group(0) : null;
    }

    private String findImageUrl(String text) {
        try {
            JsonNode root = objectMapper.readTree(text);
            String value = findImageUrl(root);
            if (value != null) {
                return value;
            }
        }
        catch (Exception ignored) {
            // Fallback to regex scan below.
        }
        Matcher matcher = HTTP_IMAGE_PATTERN.matcher(text);
        while (matcher.find()) {
            String url = matcher.group();
            if (looksLikeImageUrl(url)) {
                return trimUrl(url);
            }
        }
        return null;
    }

    private String findImageUrl(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isTextual() && node.asText().startsWith("http") && looksLikeImageUrl(node.asText())) {
            return trimUrl(node.asText());
        }
        for (JsonNode child : node) {
            String value = findImageUrl(child);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String findBase64(String text) {
        try {
            JsonNode root = objectMapper.readTree(text);
            return findBase64(root);
        }
        catch (Exception ex) {
            return null;
        }
    }

    private String findBase64(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isTextual()) {
            String value = node.asText();
            if (!value.startsWith("http") && value.length() > 200 && value.matches("[A-Za-z0-9+/=\\r\\n]+")) {
                return value;
            }
        }
        for (JsonNode child : node) {
            String value = findBase64(child);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private boolean looksLikeImageUrl(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.contains(".png") || lower.contains(".jpg") || lower.contains(".jpeg")
                || lower.contains(".webp") || lower.contains(".gif") || lower.contains("image");
    }

    private String trimUrl(String url) {
        return url.strip().replaceAll("[\\\\),.;\\u00a0]+$", "");
    }

    private String extensionFromContentType(String contentType) {
        String lower = contentType.toLowerCase(Locale.ROOT);
        if (lower.contains("jpeg") || lower.contains("jpg")) {
            return "jpg";
        }
        if (lower.contains("webp")) {
            return "webp";
        }
        if (lower.contains("gif")) {
            return "gif";
        }
        return "png";
    }

    private String normalizeExtension(String extension) {
        if (isBlank(extension)) {
            return "png";
        }
        String normalized = extension.toLowerCase(Locale.ROOT).replace(".", "");
        return "jpeg".equals(normalized) ? "jpg" : normalized;
    }

    private String firstText(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private String buildImagePrompt(String prompt) {
        return prompt.strip() + "\n" + NO_TEXT_PROMPT;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record StoredImage(String localPath, String originalUrl) {
    }
}
