package com.example.xiaohongshuai.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import com.example.xiaohongshuai.dto.TopicAnalysisSearchDTO;
import com.example.xiaohongshuai.vo.TopicAnalysisJobVO;
import com.example.xiaohongshuai.vo.TopicAnalysisVO;
import org.springframework.stereotype.Service;

@Service
public class TopicAnalysisJobService {

    private static final int MAX_LOGS = 200;

    private final TopicAnalysisService topicAnalysisService;
    private final XhsBrowserTaskLock xhsBrowserTaskLock;
    private final Map<String, JobState> jobs = new ConcurrentHashMap<>();

    public TopicAnalysisJobService(TopicAnalysisService topicAnalysisService, XhsBrowserTaskLock xhsBrowserTaskLock) {
        this.topicAnalysisService = topicAnalysisService;
        this.xhsBrowserTaskLock = xhsBrowserTaskLock;
    }

    public TopicAnalysisJobVO start(TopicAnalysisSearchDTO request) {
        XhsBrowserTaskLock.Lease lease = xhsBrowserTaskLock.acquire("热门选题采集");
        JobState job = new JobState(UUID.randomUUID().toString(), request.keyword());
        jobs.put(job.id, job);
        job.addLog("任务已创建，准备启动 MediaCrawler");

        CompletableFuture.runAsync(() -> {
            try {
                job.addLog("开始采集关键词：" + request.keyword());
                TopicAnalysisVO result = topicAnalysisService.search(request.keyword(), request.limit(),
                        request.sortBy(), request.noteType(), request.publishTime(), job::addLog, job::updateSamples);
                job.complete(result);
            }
            catch (Exception ex) {
                job.fail(ex.getMessage());
            }
            finally {
                lease.close();
            }
        });

        return job.toVO();
    }

    public TopicAnalysisJobVO getRequired(String id) {
        JobState job = jobs.get(id);
        if (job == null) {
            throw new IllegalArgumentException("Topic analysis job not found: " + id);
        }
        return job.toVO();
    }

    private static final class JobState {

        private final String id;
        private final String keyword;
        private final LocalDateTime createdAt = LocalDateTime.now();
        private final List<String> logs = new ArrayList<>();
        private List<TopicAnalysisVO.FeedSample> samples = List.of();

        private String status = "RUNNING";
        private TopicAnalysisVO result;
        private String errorMessage;
        private LocalDateTime finishedAt;

        private JobState(String id, String keyword) {
            this.id = id;
            this.keyword = keyword;
        }

        private synchronized void addLog(String line) {
            if (line == null || line.isBlank()) {
                return;
            }
            logs.add(line.length() > 500 ? line.substring(0, 500) : line);
            if (logs.size() > MAX_LOGS) {
                logs.remove(0);
            }
        }

        private synchronized void complete(TopicAnalysisVO result) {
            this.result = result;
            this.samples = result.samples();
            this.status = "SUCCESS";
            this.finishedAt = LocalDateTime.now();
            addLog("采集完成，已生成 AI 分析结果");
        }

        private synchronized void fail(String errorMessage) {
            this.status = "FAILED";
            this.errorMessage = errorMessage;
            this.finishedAt = LocalDateTime.now();
            addLog("任务失败：" + errorMessage);
        }

        private synchronized TopicAnalysisJobVO toVO() {
            return new TopicAnalysisJobVO(id, status, keyword, List.copyOf(logs), samples, result, errorMessage,
                    createdAt, finishedAt);
        }

        private synchronized void updateSamples(List<TopicAnalysisVO.FeedSample> samples) {
            this.samples = samples == null ? List.of() : samples;
        }
    }
}
