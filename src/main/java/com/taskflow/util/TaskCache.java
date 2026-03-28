package com.taskflow.util;

import com.taskflow.model.entity.Task;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple in-memory cache using ConcurrentHashMap.
 * Demonstrates thread-safe collections (Concurrency concept).
 */
@Slf4j
@Component
public class TaskCache {

    /**
     * ConcurrentHashMap: thread-safe map for concurrent access.
     * Unlike synchronized HashMap, it uses lock striping for better throughput.
     */
    private final ConcurrentHashMap<Long, Task> cache = new ConcurrentHashMap<>();

    public void put(Task task) {
        cache.put(task.getId(), task);
        log.debug("Cached task {}", task.getId());
    }

    public Optional<Task> get(Long id) {
        Task t = cache.get(id);
        if (t != null) log.debug("Cache HIT for task {}", id);
        return Optional.ofNullable(t);
    }

    public void evict(Long id) {
        cache.remove(id);
        log.debug("Evicted task {} from cache", id);
    }

    public void clear() {
        cache.clear();
        log.debug("Cache cleared");
    }

    public int size() {
        return cache.size();
    }

    /**
     * computeIfAbsent is atomic — safe under concurrent access.
     */
    public Task getOrLoad(Long id, java.util.function.Function<Long, Task> loader) {
        return cache.computeIfAbsent(id, loader);
    }
}
