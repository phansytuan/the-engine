package com.taskflow;

import com.taskflow.model.enums.TaskStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TaskStatusTest {

    @Test
    @DisplayName("TODO can transition to IN_PROGRESS and CANCELLED only")
    void todoTransitions() {
        assertThat(TaskStatus.TODO.canTransitionTo(TaskStatus.IN_PROGRESS)).isTrue();
        assertThat(TaskStatus.TODO.canTransitionTo(TaskStatus.CANCELLED)).isTrue();
        assertThat(TaskStatus.TODO.canTransitionTo(TaskStatus.IN_REVIEW)).isFalse();
        assertThat(TaskStatus.TODO.canTransitionTo(TaskStatus.COMPLETED)).isFalse();
    }

    @Test
    @DisplayName("IN_PROGRESS can transition to IN_REVIEW and CANCELLED only")
    void inProgressTransitions() {
        assertThat(TaskStatus.IN_PROGRESS.canTransitionTo(TaskStatus.IN_REVIEW)).isTrue();
        assertThat(TaskStatus.IN_PROGRESS.canTransitionTo(TaskStatus.CANCELLED)).isTrue();
        assertThat(TaskStatus.IN_PROGRESS.canTransitionTo(TaskStatus.COMPLETED)).isFalse();
        assertThat(TaskStatus.IN_PROGRESS.canTransitionTo(TaskStatus.TODO)).isFalse();
    }

    @Test
    @DisplayName("COMPLETED and CANCELLED cannot transition to anything")
    void terminalStatesCannotTransition() {
        for (TaskStatus target : TaskStatus.values()) {
            assertThat(TaskStatus.COMPLETED.canTransitionTo(target)).isFalse();
            assertThat(TaskStatus.CANCELLED.canTransitionTo(target)).isFalse();
        }
    }

    @Test
    @DisplayName("isCompleted returns true for COMPLETED and CANCELLED")
    void isCompleted() {
        assertThat(TaskStatus.COMPLETED.isCompleted()).isTrue();
        assertThat(TaskStatus.CANCELLED.isCompleted()).isTrue();
        assertThat(TaskStatus.TODO.isCompleted()).isFalse();
        assertThat(TaskStatus.IN_PROGRESS.isCompleted()).isFalse();
    }

    @Test
    @DisplayName("displayName is human-readable")
    void displayName() {
        assertThat(TaskStatus.IN_PROGRESS.getDisplayName()).isEqualTo("In Progress");
        assertThat(TaskStatus.TODO.getDisplayName()).isEqualTo("To Do");
    }
}
