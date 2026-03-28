package com.taskflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class TaskFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskFlowApplication.class, args);
        System.out.println("🚀 TaskFlow Application Started!");
        System.out.println("📋 API: http://localhost:8080/api/tasks");
        System.out.println("🗄️  H2 Console: http://localhost:8080/h2-console");
    }
}
