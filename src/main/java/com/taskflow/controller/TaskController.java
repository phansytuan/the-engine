package com.taskflow.controller;

import com.taskflow.model.entity.Comment;
import com.taskflow.model.entity.Task;
import com.taskflow.model.enums.TaskStatus;
import com.taskflow.service.NotificationService;
import com.taskflow.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;
    private final NotificationService notificationService;

    /** GET /api/tasks?status=TODO */
    @GetMapping
    public ResponseEntity<List<Task>> getAllTasks(
            @RequestParam(required = false) TaskStatus status) {
        List<Task> tasks = status != null
            ? taskService.getTasksByStatus(status)
            : taskService.getAllTasks();
        return ResponseEntity.ok(tasks);
    }

    /** GET /api/tasks/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<Task> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    /** GET /api/tasks/assignee/{userId} */
    @GetMapping("/assignee/{userId}")
    public ResponseEntity<List<Task>> getTasksByAssignee(@PathVariable Long userId) {
        return ResponseEntity.ok(taskService.getTasksByAssignee(userId));
    }

    /** GET /api/tasks/overdue */
    @GetMapping("/overdue")
    public ResponseEntity<List<Task>> getOverdueTasks() {
        return ResponseEntity.ok(taskService.getOverdueTasks());
    }

    /** POST /api/tasks  (X-User-Id header = creator) */
    @PostMapping
    public ResponseEntity<Task> createTask(
            @Valid @RequestBody Task task,
            @RequestHeader("X-User-Id") Long userId) {
        Task created = taskService.createTask(task, userId);
        if (created.getAssignee() != null) {
            notificationService.notifyTaskAssigned(created, created.getAssignee());
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** PUT /api/tasks/{id} */
    @PutMapping("/{id}")
    public ResponseEntity<Task> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody Task task,
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(taskService.updateTask(id, task, userId));
    }

    /** PATCH /api/tasks/{id}/status?status=IN_PROGRESS */
    @PatchMapping("/{id}/status")
    public ResponseEntity<Task> updateStatus(
            @PathVariable Long id,
            @RequestParam TaskStatus status,
            @RequestHeader("X-User-Id") Long userId) {
        Task updated = taskService.updateTaskStatus(id, status, userId);
        if (status == TaskStatus.COMPLETED) {
            notificationService.notifyTaskCompleted(updated);
        }
        return ResponseEntity.ok(updated);
    }

    /** PATCH /api/tasks/{id}/assign?assigneeId=2 */
    @PatchMapping("/{id}/assign")
    public ResponseEntity<Task> assignTask(
            @PathVariable Long id,
            @RequestParam Long assigneeId,
            @RequestHeader("X-User-Id") Long userId) {
        Task updated = taskService.assignTask(id, assigneeId, userId);
        notificationService.notifyTaskAssigned(updated, updated.getAssignee());
        return ResponseEntity.ok(updated);
    }

    /** POST /api/tasks/{id}/comments */
    @PostMapping("/{id}/comments")
    public ResponseEntity<Comment> addComment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @RequestHeader("X-User-Id") Long userId) {
        String content = body.get("content");
        if (content == null || content.isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        Comment comment = taskService.addComment(id, content, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    /** DELETE /api/tasks/{id} */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        taskService.deleteTask(id, userId);
        return ResponseEntity.noContent().build();
    }
}
