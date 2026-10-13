package com.example.xiaohongshuai.service;

import java.time.LocalDateTime;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

import org.springframework.stereotype.Component;

@Component
public class XhsBrowserTaskLock {

    private final Semaphore semaphore = new Semaphore(1, true);

    private volatile String currentTaskName;

    private volatile LocalDateTime startedAt;

    public <T> T runExclusive(String taskName, Supplier<T> action) {
        try (Lease ignored = acquire(taskName)) {
            return action.get();
        }
    }

    public Lease acquire(String taskName) {
        if (!semaphore.tryAcquire()) {
            String runningTask = currentTaskName == null ? "未知任务" : currentTaskName;
            throw new IllegalStateException("小红书浏览器正在执行：" + runningTask + "，请稍后再试");
        }
        currentTaskName = taskName;
        startedAt = LocalDateTime.now();
        return new Lease(this);
    }

    public Status status() {
        return new Status(semaphore.availablePermits() == 0, currentTaskName, startedAt);
    }

    private void release() {
        currentTaskName = null;
        startedAt = null;
        semaphore.release();
    }

    public static final class Lease implements AutoCloseable {

        private final XhsBrowserTaskLock owner;

        private boolean closed;

        private Lease(XhsBrowserTaskLock owner) {
            this.owner = owner;
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                owner.release();
            }
        }
    }

    public record Status(boolean busy, String taskName, LocalDateTime startedAt) {
    }
}
