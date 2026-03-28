package com.taskflow.model.entity;

import com.taskflow.model.enums.TaskPriority;
import com.taskflow.model.enums.TaskStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Inheritance(strategy = InheritanceType.JOINED)
public class Task extends BaseEntity {

    @NotBlank
    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaskStatus status = TaskStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaskPriority priority = TaskPriority.MEDIUM;

    private LocalDateTime dueDate;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    public void complete() {
        if (!status.canTransitionTo(TaskStatus.COMPLETED)) {
            throw new IllegalStateException(
                "Cannot complete task in " + status + " status. " +
                "Task must be IN_REVIEW to be completed."
            );
        }
        this.status = TaskStatus.COMPLETED;
    }

    public void cancel() {
        if (status.isCompleted()) {
            throw new IllegalStateException("Cannot cancel a completed/cancelled task");
        }
        this.status = TaskStatus.CANCELLED;
    }

    public void updateStatus(TaskStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                "Cannot transition from " + this.status + " to " + newStatus
            );
        }
        this.status = newStatus;
    }

    public boolean isOverdue() {
        return dueDate != null &&
               LocalDateTime.now().isAfter(dueDate) &&
               !status.isCompleted();
    }

    public void addComment(Comment comment) {
        comments.add(comment);
        comment.setTask(this);
    }
}
