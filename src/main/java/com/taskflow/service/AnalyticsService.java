package com.taskflow.service;

import com.taskflow.model.entity.Task;
import com.taskflow.model.enums.TaskPriority;
import com.taskflow.model.enums.TaskStatus;
import com.taskflow.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Demonstrates Java Stream API for analytics.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AnalyticsService {

    private final TaskRepository taskRepository;

    /** Count tasks grouped by status. */
    public Map<TaskStatus, Long> getTaskCountByStatus() {
        return taskRepository.findAll().stream()
            .collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));
    }

    /** Count tasks grouped by priority. */
    public Map<TaskPriority, Long> getTaskCountByPriority() {
        return taskRepository.findAll().stream()
            .collect(Collectors.groupingBy(Task::getPriority, Collectors.counting()));
    }

    /** All overdue tasks, sorted by priority descending. */
    public List<Task> getOverdueTasks() {
        return taskRepository.findAll().stream()
            .filter(Task::isOverdue)
            .sorted(Comparator.comparingInt(t -> -t.getPriority().getLevel()))
            .collect(Collectors.toList());
    }

    /** Completion rate as a percentage (0-100). */
    public double getCompletionRate() {
        List<Task> all = taskRepository.findAll();
        if (all.isEmpty()) return 0.0;
        long completed = all.stream()
            .filter(t -> t.getStatus() == TaskStatus.COMPLETED)
            .count();
        return (completed * 100.0) / all.size();
    }

    /** Top N assignees ranked by number of tasks assigned. */
    public List<Map<String, Object>> getTopAssignees(int limit) {
        return taskRepository.findAll().stream()
            .filter(t -> t.getAssignee() != null)
            .collect(Collectors.groupingBy(
                t -> t.getAssignee().getName(),
                Collectors.counting()
            ))
            .entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
            .limit(limit)
            .map(e -> {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("assignee", e.getKey());
                row.put("taskCount", e.getValue());
                return row;
            })
            .collect(Collectors.toList());
    }

    /** Full summary dashboard. */
    public Map<String, Object> getDashboardSummary() {
        List<Task> all = taskRepository.findAll();

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalTasks",       all.size());
        summary.put("completionRate",   String.format("%.1f%%", getCompletionRate()));
        summary.put("overdueTasks",     all.stream().filter(Task::isOverdue).count());
        summary.put("byStatus",         getTaskCountByStatus());
        summary.put("byPriority",       getTaskCountByPriority());
        summary.put("topAssignees",     getTopAssignees(5));
        summary.put("generatedAt",      LocalDateTime.now().toString());
        return summary;
    }
}
