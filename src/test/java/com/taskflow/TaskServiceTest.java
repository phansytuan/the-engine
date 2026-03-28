package com.taskflow;

import com.taskflow.exception.TaskNotFoundException;
import com.taskflow.exception.UnauthorizedAccessException;
import com.taskflow.model.entity.Task;
import com.taskflow.model.entity.User;
import com.taskflow.model.enums.TaskPriority;
import com.taskflow.model.enums.TaskStatus;
import com.taskflow.model.enums.UserRole;
import com.taskflow.repository.CommentRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock private TaskRepository taskRepository;
    @Mock private UserRepository userRepository;
    @Mock private CommentRepository commentRepository;

    @InjectMocks private TaskService taskService;

    private User admin;
    private User developer;
    private Task task;

    @BeforeEach
    void setUp() {
        admin = User.builder()
            .email("admin@test.com").name("Admin").password("pw")
            .role(UserRole.ADMIN).active(true).build();
        setId(admin, 1L);

        developer = User.builder()
            .email("dev@test.com").name("Dev").password("pw")
            .role(UserRole.DEVELOPER).active(true).build();
        setId(developer, 2L);

        task = Task.builder()
            .title("Test Task").description("desc")
            .status(TaskStatus.TODO).priority(TaskPriority.HIGH)
            .creator(admin).assignee(developer).build();
        setId(task, 10L);
    }

    // ── getTaskById ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("getTaskById returns task when found")
    void getTaskById_found() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        Task result = taskService.getTaskById(10L);
        assertThat(result.getTitle()).isEqualTo("Test Task");
    }

    @Test
    @DisplayName("getTaskById throws TaskNotFoundException when missing")
    void getTaskById_notFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> taskService.getTaskById(99L))
            .isInstanceOf(TaskNotFoundException.class)
            .hasMessageContaining("99");
    }

    // ── createTask ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("createTask sets creator and saves")
    void createTask_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Task newTask = Task.builder().title("New").build();
        Task result = taskService.createTask(newTask, 1L);

        assertThat(result.getCreator()).isEqualTo(admin);
        verify(taskRepository, times(1)).save(any());
    }

    // ── updateTaskStatus ──────────────────────────────────────────────────────

    @Test
    @DisplayName("updateTaskStatus transitions TODO → IN_PROGRESS successfully")
    void updateStatus_validTransition() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.updateTaskStatus(10L, TaskStatus.IN_PROGRESS, 1L);
        assertThat(result.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("updateTaskStatus rejects invalid transition TODO → COMPLETED")
    void updateStatus_invalidTransition() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() ->
            taskService.updateTaskStatus(10L, TaskStatus.COMPLETED, 1L))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("updateTaskStatus throws when user is not creator or assignee")
    void updateStatus_unauthorized() {
        User stranger = User.builder().email("x@x.com").name("X")
            .password("pw").role(UserRole.DEVELOPER).active(true).build();
        setId(stranger, 99L);

        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() ->
            taskService.updateTaskStatus(10L, TaskStatus.IN_PROGRESS, 99L))
            .isInstanceOf(UnauthorizedAccessException.class);
    }

    // ── assignTask ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("assignTask succeeds when requester is a manager/admin")
    void assignTask_success() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(userRepository.findById(2L)).thenReturn(Optional.of(developer));
        when(taskRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Task result = taskService.assignTask(10L, 2L, 1L);
        assertThat(result.getAssignee()).isEqualTo(developer);
    }

    @Test
    @DisplayName("assignTask throws when requester is a plain developer")
    void assignTask_unauthorized() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findById(2L)).thenReturn(Optional.of(developer));

        assertThatThrownBy(() -> taskService.assignTask(10L, 2L, 2L))
            .isInstanceOf(UnauthorizedAccessException.class)
            .hasMessageContaining("managers and admins");
    }

    // ── deleteTask ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteTask succeeds when requester is creator")
    void deleteTask_byCreator() {
        when(taskRepository.findById(10L)).thenReturn(Optional.of(task));
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        taskService.deleteTask(10L, 1L);
        verify(taskRepository).delete(task);
    }

    @Test
    @DisplayName("getAllTasks returns all tasks from repository")
    void getAllTasks_returnsList() {
        when(taskRepository.findAll()).thenReturn(List.of(task));
        List<Task> result = taskService.getAllTasks();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Test Task");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /** Reflectively set the id field (inherited from BaseEntity) */
    private void setId(Object entity, Long id) {
        try {
            var field = com.taskflow.model.entity.BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
