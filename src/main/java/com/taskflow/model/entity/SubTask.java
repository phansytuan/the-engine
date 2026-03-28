package com.taskflow.model.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subtasks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class SubTask extends Task {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_task_id", nullable = false)
    private Task parentTask;

    @Override
    public void complete() {
        super.complete();
        // SubTask-specific logic can be added here
    }
}
