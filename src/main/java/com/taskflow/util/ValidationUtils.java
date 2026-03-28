package com.taskflow.util;

import com.taskflow.model.entity.Task;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class ValidationUtils {

    private ValidationUtils() {}

    public static List<String> validateTask(Task task) {
        List<String> errors = new ArrayList<>();

        if (task.getTitle() == null || task.getTitle().isBlank()) {
            errors.add("Title must not be blank");
        } else if (task.getTitle().length() > 200) {
            errors.add("Title must not exceed 200 characters");
        }

        if (task.getDueDate() != null && task.getDueDate().isBefore(LocalDateTime.now())) {
            errors.add("Due date must be in the future");
        }

        return errors;
    }
}
