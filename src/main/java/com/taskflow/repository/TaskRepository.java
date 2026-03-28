package com.taskflow.repository;

import com.taskflow.model.entity.Task;
import com.taskflow.model.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByStatus(TaskStatus status);

    List<Task> findByAssigneeId(Long assigneeId);

    List<Task> findByCreatorId(Long creatorId);

    @Query("SELECT t FROM Task t WHERE t.dueDate < :now AND t.status NOT IN :completedStatuses")
    List<Task> findOverdueTasks(
        @Param("now") LocalDateTime now,
        @Param("completedStatuses") List<TaskStatus> completedStatuses
    );
}
