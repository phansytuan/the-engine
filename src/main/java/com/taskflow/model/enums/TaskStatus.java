package com.taskflow.model.enums;

public enum TaskStatus {
    TODO("To Do"),
    IN_PROGRESS("In Progress"),
    IN_REVIEW("In Review"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    private final String displayName;

    TaskStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isCompleted() {
        return this == COMPLETED || this == CANCELLED;
    }

    public boolean canTransitionTo(TaskStatus newStatus) {
        return switch (this) {
            case TODO -> newStatus == IN_PROGRESS || newStatus == CANCELLED;
            case IN_PROGRESS -> newStatus == IN_REVIEW || newStatus == CANCELLED;
            case IN_REVIEW -> newStatus == COMPLETED || newStatus == IN_PROGRESS;
            case COMPLETED, CANCELLED -> false;
        };
    }
}
