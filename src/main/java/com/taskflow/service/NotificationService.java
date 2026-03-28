package com.taskflow.service;

import com.taskflow.model.entity.Task;
import com.taskflow.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * Async notification service — demonstrates @Async and CompletableFuture.
 * In production this would integrate with email/Slack/push.
 */
@Slf4j
@Service
public class NotificationService {

    @Async
    public CompletableFuture<Void> notifyTaskAssigned(Task task, User assignee) {
        log.info("[ASYNC] Notifying {} that task '{}' was assigned to them",
            assignee.getEmail(), task.getTitle());
        simulateDelay(200);
        log.info("[ASYNC] Notification sent to {}", assignee.getEmail());
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> notifyTaskCompleted(Task task) {
        log.info("[ASYNC] Notifying creator {} that task '{}' is complete",
            task.getCreator().getEmail(), task.getTitle());
        simulateDelay(150);
        log.info("[ASYNC] Completion notification sent");
        return CompletableFuture.completedFuture(null);
    }

    @Async
    public CompletableFuture<Void> notifyTaskOverdue(Task task) {
        log.warn("[ASYNC] OVERDUE alert for task '{}' (assigned to {})",
            task.getTitle(),
            task.getAssignee() != null ? task.getAssignee().getEmail() : "unassigned");
        simulateDelay(100);
        return CompletableFuture.completedFuture(null);
    }

    private void simulateDelay(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }
}
