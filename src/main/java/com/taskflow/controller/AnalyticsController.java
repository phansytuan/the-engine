package com.taskflow.controller;

import com.taskflow.model.entity.Task;
import com.taskflow.model.enums.TaskPriority;
import com.taskflow.model.enums.TaskStatus;
import com.taskflow.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    /** GET /api/analytics/dashboard */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboard() {
        return ResponseEntity.ok(analyticsService.getDashboardSummary());
    }

    /** GET /api/analytics/by-status */
    @GetMapping("/by-status")
    public ResponseEntity<Map<TaskStatus, Long>> getByStatus() {
        return ResponseEntity.ok(analyticsService.getTaskCountByStatus());
    }

    /** GET /api/analytics/by-priority */
    @GetMapping("/by-priority")
    public ResponseEntity<Map<TaskPriority, Long>> getByPriority() {
        return ResponseEntity.ok(analyticsService.getTaskCountByPriority());
    }

    /** GET /api/analytics/overdue */
    @GetMapping("/overdue")
    public ResponseEntity<List<Task>> getOverdue() {
        return ResponseEntity.ok(analyticsService.getOverdueTasks());
    }

    /** GET /api/analytics/completion-rate */
    @GetMapping("/completion-rate")
    public ResponseEntity<Map<String, Object>> getCompletionRate() {
        double rate = analyticsService.getCompletionRate();
        return ResponseEntity.ok(Map.of(
            "completionRate", rate,
            "formatted", String.format("%.1f%%", rate)
        ));
    }

    /** GET /api/analytics/top-assignees?limit=5 */
    @GetMapping("/top-assignees")
    public ResponseEntity<List<Map<String, Object>>> getTopAssignees(
            @RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(analyticsService.getTopAssignees(limit));
    }
}
