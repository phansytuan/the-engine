package com.taskflow.exception;

public class TaskNotFoundException extends RuntimeException {
    private final Long taskId;

    public TaskNotFoundException(Long taskId) {
        super("Task not found with id: " + taskId);
        this.taskId = taskId;
    }

    public Long getTaskId() {
        return taskId;
    }
}
