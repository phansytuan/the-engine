package com.taskflow.service;

import com.taskflow.exception.TaskNotFoundException;
import com.taskflow.exception.UnauthorizedAccessException;
import com.taskflow.model.entity.Comment;
import com.taskflow.model.entity.Task;
import com.taskflow.model.entity.User;
import com.taskflow.model.enums.TaskStatus;
import com.taskflow.repository.CommentRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service layer: all business logic lives here.
 * Controllers stay thin; repositories stay dumb.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final CommentRepository commentRepository;

    // ── Queries ──────────────────────────────────────────────────────────────

    public List<Task> getAllTasks() {
        return taskRepository.findAll();
    }

    public List<Task> getTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status);
    }

    public List<Task> getTasksByAssignee(Long userId) {
        return taskRepository.findByAssigneeId(userId);
    }

    public List<Task> getOverdueTasks() {
        return taskRepository.findOverdueTasks(
            LocalDateTime.now(),
            List.of(TaskStatus.COMPLETED, TaskStatus.CANCELLED)
        );
    }

    public Task getTaskById(Long id) {
        return taskRepository.findById(id)
            .orElseThrow(() -> new TaskNotFoundException(id));
    }

    // ── Commands ─────────────────────────────────────────────────────────────

    @Transactional
    public Task createTask(Task task, Long creatorId) {
        User creator = findUser(creatorId, "Creator");
        task.setCreator(creator);

        if (task.getAssignee() != null && task.getAssignee().getId() != null) {
            User assignee = findUser(task.getAssignee().getId(), "Assignee");
            task.setAssignee(assignee);
        }

        return taskRepository.save(task);
    }

    @Transactional
    public Task updateTask(Long id, Task updatedTask, Long userId) {
        Task existing = getTaskById(id);
        requireCanModify(existing, userId);

        existing.setTitle(updatedTask.getTitle());
        existing.setDescription(updatedTask.getDescription());
        existing.setPriority(updatedTask.getPriority());
        existing.setDueDate(updatedTask.getDueDate());

        return taskRepository.save(existing);
    }

    @Transactional
    public Task updateTaskStatus(Long taskId, TaskStatus newStatus, Long userId) {
        Task task = getTaskById(taskId);
        requireCanModify(task, userId);
        task.updateStatus(newStatus);
        return taskRepository.save(task);
    }

    @Transactional
    public Task assignTask(Long taskId, Long assigneeId, Long requesterId) {
        Task task = getTaskById(taskId);
        User requester = findUser(requesterId, "Requester");

        if (!requester.canManageTasks()) {
            throw new UnauthorizedAccessException("Only managers and admins can assign tasks");
        }

        User assignee = findUser(assigneeId, "Assignee");
        task.setAssignee(assignee);
        return taskRepository.save(task);
    }

    @Transactional
    public Comment addComment(Long taskId, String content, Long authorId) {
        Task task = getTaskById(taskId);
        User author = findUser(authorId, "Author");

        Comment comment = Comment.builder()
            .content(content)
            .task(task)
            .author(author)
            .build();

        return commentRepository.save(comment);
    }

    @Transactional
    public void deleteTask(Long id, Long userId) {
        Task task = getTaskById(id);
        User user = findUser(userId, "User");

        boolean isCreator = task.getCreator().getId().equals(userId);
        if (!isCreator && !user.canManageTasks()) {
            throw new UnauthorizedAccessException("Only the creator or admins can delete this task");
        }

        taskRepository.delete(task);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User findUser(Long id, String role) {
        return userRepository.findById(id)
            .orElseThrow(() -> new RuntimeException(role + " not found with id: " + id));
    }

    private void requireCanModify(Task task, Long userId) {
        boolean isCreator  = task.getCreator().getId().equals(userId);
        boolean isAssignee = task.getAssignee() != null && task.getAssignee().getId().equals(userId);
        if (!isCreator && !isAssignee) {
            throw new UnauthorizedAccessException("User " + userId + " cannot modify this task");
        }
    }
}
