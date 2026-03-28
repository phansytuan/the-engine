package com.taskflow;

import com.taskflow.model.entity.Task;
import com.taskflow.model.entity.User;
import com.taskflow.model.enums.TaskPriority;
import com.taskflow.model.enums.TaskStatus;
import com.taskflow.model.enums.UserRole;
import com.taskflow.repository.TaskRepository;
import com.taskflow.service.AnalyticsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock private TaskRepository taskRepository;
    @InjectMocks private AnalyticsService analyticsService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
            .email("u@t.com").name("Alice").password("pw")
            .role(UserRole.DEVELOPER).active(true).build();
    }

    private Task makeTask(TaskStatus status, TaskPriority priority) {
        return Task.builder()
            .title("T").status(status).priority(priority)
            .creator(user).build();
    }

    @Test
    @DisplayName("getCompletionRate returns 0 for empty list")
    void completionRate_empty() {
        when(taskRepository.findAll()).thenReturn(List.of());
        assertThat(analyticsService.getCompletionRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("getCompletionRate calculates correctly")
    void completionRate_mixed() {
        when(taskRepository.findAll()).thenReturn(List.of(
            makeTask(TaskStatus.COMPLETED, TaskPriority.LOW),
            makeTask(TaskStatus.TODO, TaskPriority.MEDIUM),
            makeTask(TaskStatus.COMPLETED, TaskPriority.HIGH),
            makeTask(TaskStatus.IN_PROGRESS, TaskPriority.URGENT)
        ));
        assertThat(analyticsService.getCompletionRate()).isEqualTo(50.0);
    }

    @Test
    @DisplayName("getTaskCountByStatus groups correctly")
    void taskCountByStatus() {
        when(taskRepository.findAll()).thenReturn(List.of(
            makeTask(TaskStatus.TODO, TaskPriority.LOW),
            makeTask(TaskStatus.TODO, TaskPriority.MEDIUM),
            makeTask(TaskStatus.COMPLETED, TaskPriority.HIGH)
        ));
        Map<TaskStatus, Long> result = analyticsService.getTaskCountByStatus();
        assertThat(result.get(TaskStatus.TODO)).isEqualTo(2L);
        assertThat(result.get(TaskStatus.COMPLETED)).isEqualTo(1L);
    }

    @Test
    @DisplayName("getDashboardSummary contains expected keys")
    void dashboardSummary_hasKeys() {
        when(taskRepository.findAll()).thenReturn(List.of(
            makeTask(TaskStatus.TODO, TaskPriority.MEDIUM)
        ));
        Map<String, Object> summary = analyticsService.getDashboardSummary();
        assertThat(summary).containsKeys("totalTasks", "completionRate", "overdueTasks",
            "byStatus", "byPriority", "topAssignees", "generatedAt");
        assertThat(summary.get("totalTasks")).isEqualTo(1);
    }
}
