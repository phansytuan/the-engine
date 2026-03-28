# TaskFlow — Java Concepts Reference

A deep-dive into the Java concepts applied throughout the TaskFlow enterprise task-management API.  
Each section maps a concept directly to the source files where it lives, so you can read the code and the explanation side-by-side.

---

## Table of Contents

1. [Object-Oriented Programming (OOP)](#1-object-oriented-programming-oop)
   - [Encapsulation](#11-encapsulation)
   - [Inheritance](#12-inheritance)
   - [Polymorphism](#13-polymorphism)
   - [Abstraction](#14-abstraction)
2. [Java Collections Framework](#2-java-collections-framework)
3. [Memory Management](#3-memory-management)
4. [Concurrency & Multithreading](#4-concurrency--multithreading)
5. [Java 8+ Features](#5-java-8-features)
6. [Exception Handling](#6-exception-handling)

---

## 1. Object-Oriented Programming (OOP)

### 1.1 Encapsulation

**What it is:** Bundling data and the methods that operate on it inside a single class, and restricting direct access to the internal state from outside that class.

**Where it appears in TaskFlow:**

`Task.java` is the clearest example. The `status` field is `private` (enforced by Lombok's `@Data`). It cannot be set freely from outside — callers must go through dedicated methods that enforce business rules:

```java
// Task.java
public void updateStatus(TaskStatus newStatus) {
    if (!this.status.canTransitionTo(newStatus)) {        // guard
        throw new IllegalStateException(
            "Cannot transition from " + this.status + " to " + newStatus
        );
    }
    this.status = newStatus;                              // mutation only happens here
}

public void complete() {
    if (!status.canTransitionTo(TaskStatus.COMPLETED)) {
        throw new IllegalStateException("Task must be IN_REVIEW to be completed.");
    }
    this.status = TaskStatus.COMPLETED;
}
```

If `status` were a public field, any caller could write `task.status = COMPLETED` and skip the state-machine checks entirely. Encapsulation prevents that.

`User.java` shows a second pattern — selectively hiding fields from the JSON serialiser while still allowing the field to be written on creation:

```java
// User.java
@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
private String password;
```

The password is accepted in `POST /api/users` request bodies but never serialised into any response — a common security requirement enforced at the data model level.

`ValidationUtils.java` encapsulates validation logic inside a utility class with a private constructor so it can never be instantiated:

```java
public final class ValidationUtils {
    private ValidationUtils() {}   // prevents instantiation
    public static List<String> validateTask(Task task) { ... }
}
```

---

### 1.2 Inheritance

**What it is:** A class (subclass) extends another class (superclass), inheriting its fields and methods and optionally overriding or extending them.

**Where it appears in TaskFlow:**

`BaseEntity` is the root of the entity hierarchy. It holds the three fields every entity shares — `id`, `createdAt`, and `updatedAt` — along with the JPA lifecycle hooks that maintain them automatically:

```java
// BaseEntity.java
@MappedSuperclass          // tells JPA to include these columns in child tables
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }
}
```

`Task`, `User`, and `Comment` all extend `BaseEntity`. They inherit `id` and the timestamp columns without repeating a single line of that boilerplate.

`SubTask` carries the hierarchy one level deeper:

```java
// SubTask.java
@Inheritance(strategy = InheritanceType.JOINED)  // on Task
public class SubTask extends Task {

    @ManyToOne
    private Task parentTask;

    @Override
    public void complete() {
        super.complete();          // reuse parent logic
        // SubTask-specific logic added here
    }
}
```

`@Inheritance(strategy = InheritanceType.JOINED)` on `Task` tells JPA to store the base columns in the `tasks` table and the `SubTask`-specific column (`parent_task_id`) in a separate `subtasks` table, joining them on `id`. This maps the Java inheritance tree directly to the relational schema.

---

### 1.3 Polymorphism

**What it is:** One interface, many implementations. Code written against a type works correctly regardless of the concrete subtype at runtime.

**Where it appears in TaskFlow:**

**Method overriding** — `SubTask.complete()` overrides `Task.complete()`. If you hold a reference of type `Task` that actually points to a `SubTask`, calling `.complete()` dispatches to the overridden version:

```java
Task t = new SubTask();   // polymorphic reference
t.complete();             // dispatches to SubTask.complete() at runtime
```

**Interface polymorphism** — every Spring service and repository is injected as an interface. `TaskService` holds a `TaskRepository` reference, not a `SimpleJpaRepository` reference:

```java
// TaskService.java
private final TaskRepository taskRepository;   // interface type
```

In production, Spring injects a JPA-backed proxy. In tests, it injects a `Mockito` mock. The service code doesn't change — it is genuinely polymorphic over the implementation.

**Enum polymorphism via the switch expression** — `TaskStatus.canTransitionTo()` uses Java 14+ switch expressions, where each enum constant behaves differently:

```java
// TaskStatus.java
public boolean canTransitionTo(TaskStatus newStatus) {
    return switch (this) {
        case TODO        -> newStatus == IN_PROGRESS || newStatus == CANCELLED;
        case IN_PROGRESS -> newStatus == IN_REVIEW   || newStatus == CANCELLED;
        case IN_REVIEW   -> newStatus == COMPLETED   || newStatus == IN_PROGRESS;
        case COMPLETED, CANCELLED -> false;
    };
}
```

Asking `TODO.canTransitionTo(COMPLETED)` returns `false`; asking `IN_REVIEW.canTransitionTo(COMPLETED)` returns `true` — same method call, different behaviour depending on the receiver.

---

### 1.4 Abstraction

**What it is:** Exposing only what a caller needs to know, hiding the implementation details behind a well-defined interface or abstract class.

**Where it appears in TaskFlow:**

`BaseEntity` is declared `abstract` — it models the concept of "a persistable entity" without being directly instantiatable:

```java
public abstract class BaseEntity { ... }
```

The Spring Repository interfaces are the most prominent abstraction. `TaskRepository` declares five methods; the JPA infrastructure generates all the SQL at runtime:

```java
// TaskRepository.java
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByStatus(TaskStatus status);
    List<Task> findByAssigneeId(Long assigneeId);
    List<Task> findOverdueTasks(@Param("now") LocalDateTime now, ...);
}
```

`TaskController` only calls `TaskService`; it never touches a repository or writes SQL. `TaskService` only calls repositories; it never constructs HTTP responses. Each layer is abstracted from the layers above and below it.

`TaskStatus` abstracts the transition rules from the callers. `TaskService.updateTaskStatus()` simply calls `task.updateStatus(newStatus)` — it doesn't know which transitions are legal. That logic lives inside the enum where it belongs.

---

## 2. Java Collections Framework

### Overview of Collections Used

| Collection | Location | Why that type |
|---|---|---|
| `List<Task>` | `TaskService`, `AnalyticsService`, `TaskRepository` | Ordered, allows duplicates; natural fit for query results |
| `List<Comment>` | `Task.java` (`comments` field) | Preserves insertion order, supports `@OrderBy` |
| `Set<Task>` | `User.java` (`assignedTasks` field) | Prevents duplicate task references per user |
| `Map<TaskStatus, Long>` | `AnalyticsService` | Status → count grouping from Stream collectors |
| `Map<String, Object>` | `AnalyticsService`, `AnalyticsController` | Flexible dashboard payload |
| `LinkedHashMap` | `AnalyticsService` | `Map` with predictable insertion-order iteration |
| `HashMap` | `GlobalExceptionHandler` | Field → error message, order irrelevant |
| `ArrayList` | `ValidationUtils`, `Task.java` default | Resizable array; fast random access |
| `ConcurrentHashMap` | `TaskCache.java` | Thread-safe map, no global lock |

---

### `List` — `ArrayList` and result lists

`ArrayList` is used wherever an ordered, resizable sequence of items is needed:

```java
// ValidationUtils.java
List<String> errors = new ArrayList<>();
errors.add("Title must not be blank");
return errors;
```

`ArrayList` gives O(1) amortised add and O(1) get-by-index. Because validation errors are produced sequentially and consumed once, it is the right default.

Repository return types are `List<Task>` throughout:

```java
// TaskRepository.java
List<Task> findByStatus(TaskStatus status);
List<Task> findByAssigneeId(Long assigneeId);
```

JPA materialises query results into a `java.util.ArrayList` backed list. `List` (not `ArrayList`) is declared in the interface because callers only care about the sequence abstraction, not the backing implementation.

---

### `Set` — `HashSet` for unique task references

```java
// User.java
@OneToMany(mappedBy = "assignee", cascade = CascadeType.ALL)
@Builder.Default
@JsonIgnore
private Set<Task> assignedTasks = new HashSet<>();
```

A user can be assigned to many tasks, but the same task cannot appear twice in the collection. `Set` enforces this uniqueness by contract. `HashSet` is chosen because there is no need for ordering — lookups and inserts are O(1) on average.

---

### `Map` — `HashMap` and `LinkedHashMap`

`HashMap` in `GlobalExceptionHandler` collects validation field errors where order doesn't matter:

```java
// GlobalExceptionHandler.java
Map<String, String> errors = new HashMap<>();
ex.getBindingResult().getFieldErrors()
    .forEach(error -> errors.put(error.getField(), error.getDefaultMessage()));
```

`LinkedHashMap` in `AnalyticsService` preserves the insertion order of the dashboard summary keys, so the JSON response always lists them in a predictable sequence (`totalTasks`, `completionRate`, `overdueTasks`, …):

```java
// AnalyticsService.java
Map<String, Object> summary = new LinkedHashMap<>();
summary.put("totalTasks",     all.size());
summary.put("completionRate", String.format("%.1f%%", getCompletionRate()));
summary.put("overdueTasks",   all.stream().filter(Task::isOverdue).count());
```

The Stream-based analytics produce typed maps from `Collectors.groupingBy`:

```java
// AnalyticsService.java
Map<TaskStatus, Long> result = taskRepository.findAll().stream()
    .collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));
```

This collapses a `List<Task>` into a `Map<TaskStatus, Long>` in one pass.

---

### `ConcurrentHashMap` — `TaskCache`

```java
// TaskCache.java
private final ConcurrentHashMap<Long, Task> cache = new ConcurrentHashMap<>();

public Task getOrLoad(Long id, Function<Long, Task> loader) {
    return cache.computeIfAbsent(id, loader);  // atomic — safe under concurrent access
}
```

`ConcurrentHashMap` allows multiple threads to read and write simultaneously using segment-level locking (lock striping), instead of the single global lock that a `Collections.synchronizedMap()` wrapper would impose. `computeIfAbsent` is an atomic compare-and-insert — it guarantees the loader runs at most once per key even if two threads race on the same id.

---

## 3. Memory Management

### JVM Architecture

The JVM divides memory into several regions:

```
┌─────────────────────────────────────────────┐
│                    JVM Memory               │
│                                             │
│  ┌──────────────┐   ┌─────────────────────┐ │
│  │  Method Area │   │        Heap         │ │
│  │  (Metaspace) │   │  ┌───────────────┐  │ │
│  │              │   │  │  Young Gen    │  │ │
│  │  Classes     │   │  │  Eden | S0 S1 │  │ │
│  │  Static vars │   │  ├───────────────┤  │ │
│  │  Constants   │   │  │   Old Gen     │  │ │
│  └──────────────┘   │  └───────────────┘  │ │
│                     └─────────────────────┘ │
│  ┌────────────────────────────────────────┐ │
│  │     Thread Stacks (one per thread)     │ │
│  │  Frame │ Frame │ Frame                 │ │
│  └────────────────────────────────────────┘ │
└─────────────────────────────────────────────┘
```

- **Metaspace** holds class definitions. `Task.class`, `TaskService.class`, and all other compiled classes are loaded here at startup and stay for the process lifetime.
- **Heap** holds all object instances. Every `Task`, `User`, and `Comment` object created during request handling lives here.
- **Thread Stack** holds stack frames — local variables and method call chains — one stack per thread.

---

### Stack vs. Heap in TaskFlow

**Stack allocation** — every method invocation pushes a frame. Primitives and object references in a method are stack-local:

```java
// TaskService.java
public Task getTaskById(Long id) {               // 'id' reference on stack
    return taskRepository.findById(id)
        .orElseThrow(() -> new TaskNotFoundException(id));
}
```

The `id` reference sits on the current thread's stack. When the method returns, the frame is popped and `id` is gone — zero GC involvement.

**Heap allocation** — all objects live on the heap:

```java
// TaskService.java
Comment comment = Comment.builder()             // new Comment object → heap
    .content(content)
    .task(task)
    .author(author)
    .build();
return commentRepository.save(comment);
```

The `Comment` object is allocated on the heap. The variable `comment` is a reference on the stack that points to it. Once `commentRepository.save(comment)` returns and `comment` goes out of scope, the heap object becomes unreachable.

**Lambda closures** — lambdas that capture local variables also allocate on the heap, because the captured variable must outlive the method frame:

```java
// TaskService.java
.orElseThrow(() -> new TaskNotFoundException(id))  // captures 'id' → heap closure
```

---

### Garbage Collection

The G1 GC (default since Java 9) runs in the background. In TaskFlow:

- **Short-lived objects**: `Task` lists returned per request are allocated in Young Gen (Eden space). Most are unreachable after the response is sent and are collected cheaply in a Minor GC without touching Old Gen.
- **Long-lived objects**: The `ConcurrentHashMap` inside `TaskCache` holds `Task` references indefinitely. Those entries are promoted to Old Gen and collected only during a Major (Full) GC — or when explicitly evicted via `cache.evict(id)`.
- **Finalisers are not used.** No class in TaskFlow overrides `finalize()`, which is correct — finalisation is unpredictable and has been deprecated since Java 9.

Practical GC awareness in the code:

```java
// TaskCache.java
public void evict(Long id) {
    cache.remove(id);          // removes the strong reference — object becomes GC-eligible
}

public void clear() {
    cache.clear();             // drops all references — entire cache becomes GC-eligible
}
```

The cache explicitly removes references rather than relying on GC to somehow "know" an entry is stale. This is the correct pattern for manual caches.

---

## 4. Concurrency & Multithreading

### Thread Pool — `AsyncConfig` and `ExecutorService`

Spring Boot runs each HTTP request on a Tomcat worker thread. Long-running work (sending notifications, writing audit logs) should not block that thread. TaskFlow delegates such work to a separate thread pool configured in `AsyncConfig`:

```java
// AsyncConfig.java
@Bean(name = "taskExecutor")
public Executor taskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(corePoolSize);       // always-alive threads (default 5)
    executor.setMaxPoolSize(maxPoolSize);          // burst ceiling (default 10)
    executor.setQueueCapacity(queueCapacity);      // work queue before rejection (default 100)
    executor.setThreadNamePrefix("TaskFlow-Async-");
    executor.initialize();
    return executor;
}
```

This wraps a `java.util.concurrent.ThreadPoolExecutor` — the core JDK `ExecutorService` — with Spring's lifecycle management. The thread pool reuses threads rather than creating a new `Thread` per task, which avoids the expensive OS-level thread creation overhead.

---

### `@Async` and `CompletableFuture`

`NotificationService` offloads all notification work to the thread pool:

```java
// NotificationService.java
@Async                                              // runs on TaskFlow-Async-N thread
public CompletableFuture<Void> notifyTaskAssigned(Task task, User assignee) {
    log.info("[ASYNC] Notifying {} ...", assignee.getEmail());
    simulateDelay(200);                             // simulate email / Slack API call
    return CompletableFuture.completedFuture(null);
}
```

`@Async` causes Spring to submit the method body to the `taskExecutor` bean. The calling Tomcat thread returns immediately — it does not wait 200 ms for the notification. `CompletableFuture<Void>` lets the caller optionally chain follow-up work or check for completion, but `TaskController` discards the future in this case (fire-and-forget).

The call site in `TaskController` shows the non-blocking pattern clearly:

```java
// TaskController.java
Task created = taskService.createTask(task, userId);
if (created.getAssignee() != null) {
    notificationService.notifyTaskAssigned(created, created.getAssignee()); // returns immediately
}
return ResponseEntity.status(HttpStatus.CREATED).body(created);            // response sent
```

---

### Synchronization — `ConcurrentHashMap` in `TaskCache`

`TaskCache` is a shared singleton accessed by all request-handling threads simultaneously. The `ConcurrentHashMap` handles the synchronisation:

```java
// TaskCache.java
private final ConcurrentHashMap<Long, Task> cache = new ConcurrentHashMap<>();

public Task getOrLoad(Long id, Function<Long, Task> loader) {
    return cache.computeIfAbsent(id, loader);   // atomic: read + conditional write
}
```

`computeIfAbsent` is an atomic operation on `ConcurrentHashMap`. If two threads race to load the same task id, only one will execute `loader` — the other will wait and receive the result of the first execution. No `synchronized` keyword or explicit `Lock` is needed.

---

### Handling `InterruptedException`

```java
// NotificationService.java
private void simulateDelay(long ms) {
    try {
        Thread.sleep(ms);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();   // restore interrupt flag — critical
    }
}
```

Calling `Thread.currentThread().interrupt()` after catching `InterruptedException` is the correct pattern. `Thread.sleep` clears the thread's interrupt flag when it throws. Re-setting the flag allows callers further up the stack to detect that an interrupt occurred, rather than silently swallowing it.

---

## 5. Java 8+ Features

### Stream API

`AnalyticsService` is written almost entirely with the Stream API. Streams allow data pipelines to be expressed as a chain of declarative operations rather than for-loops with mutable state.

**Filtering and collecting:**

```java
// AnalyticsService.java
public List<Task> getOverdueTasks() {
    return taskRepository.findAll().stream()
        .filter(Task::isOverdue)                                    // keep only overdue
        .sorted(Comparator.comparingInt(t -> -t.getPriority().getLevel()))  // sort descending
        .collect(Collectors.toList());
}
```

**Grouping and counting:**

```java
// AnalyticsService.java
public Map<TaskStatus, Long> getTaskCountByStatus() {
    return taskRepository.findAll().stream()
        .collect(Collectors.groupingBy(Task::getStatus, Collectors.counting()));
}
```

`Collectors.groupingBy` partitions the stream by the key selector (`getStatus`) and applies a downstream collector (`counting()`) to each group.

**Multi-stage pipeline — top assignees:**

```java
// AnalyticsService.java
return taskRepository.findAll().stream()
    .filter(t -> t.getAssignee() != null)
    .collect(Collectors.groupingBy(t -> t.getAssignee().getName(), Collectors.counting()))
    .entrySet().stream()
    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
    .limit(limit)
    .map(e -> {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("assignee",  e.getKey());
        row.put("taskCount", e.getValue());
        return row;
    })
    .collect(Collectors.toList());
```

This pipeline filters → groups → re-streams → sorts → limits → transforms → collects without a single mutable accumulator variable.

---

### Lambda Expressions

Lambdas appear throughout the codebase wherever a functional interface is required.

**Method references (compact lambdas):**

```java
Task::getStatus    // equivalent to task -> task.getStatus()
Task::isOverdue    // equivalent to task -> task.isOverdue()
```

**Inline lambdas in service logic:**

```java
// TaskService.java
return taskRepository.findById(id)
    .orElseThrow(() -> new TaskNotFoundException(id));
//               ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//               Supplier<Throwable> lambda — evaluated only if empty
```

**Lambdas as callbacks:**

```java
// TaskCache.java
public Task getOrLoad(Long id, Function<Long, Task> loader) {
    return cache.computeIfAbsent(id, loader);
}

// caller passes a lambda:
cache.getOrLoad(taskId, id -> taskRepository.findById(id).orElseThrow());
```

The `Function<Long, Task>` parameter accepts any lambda (or method reference) that maps a `Long` to a `Task`.

---

### `Optional`

`UserRepository.findByEmail` returns `Optional<User>` — signalling to callers that no user may exist for a given email, without resorting to `null`:

```java
// UserRepository.java
Optional<User> findByEmail(String email);

// UserService.java
public User getUserByEmail(String email) {
    return userRepository.findByEmail(email)
        .orElseThrow(() -> new UserNotFoundException(email));
}
```

`Optional.orElseThrow` is a clean, readable way to say "unwrap the value or throw an exception". There is no null check, no NPE risk. The same pattern is used in `TaskService.getTaskById`:

```java
return taskRepository.findById(id)
    .orElseThrow(() -> new TaskNotFoundException(id));
```

---

## 6. Exception Handling

### Checked vs. Unchecked Exceptions

All custom exceptions in TaskFlow extend `RuntimeException` — they are **unchecked**:

```java
// TaskNotFoundException.java
public class TaskNotFoundException extends RuntimeException {
    private final Long taskId;
    public TaskNotFoundException(Long taskId) {
        super("Task not found with id: " + taskId);
        this.taskId = taskId;
    }
    public Long getTaskId() { return taskId; }
}
```

| Exception | Extends | HTTP Status |
|---|---|---|
| `TaskNotFoundException` | `RuntimeException` | 404 Not Found |
| `UserNotFoundException` | `RuntimeException` | 404 Not Found |
| `InvalidTaskStateException` | `RuntimeException` | 400 Bad Request |
| `UnauthorizedAccessException` | `RuntimeException` | 403 Forbidden |

**Why unchecked?** Checked exceptions (`extends Exception`) must be declared in every method signature and either caught or re-thrown by every caller. In a layered Spring application this produces verbose, brittle code. Unchecked exceptions bubble up the call stack until caught by the global handler — no boilerplate required in each service method.

`UserNotFoundException` demonstrates overloaded constructors for two lookup modes:

```java
// UserNotFoundException.java
public UserNotFoundException(Long id)     { super("User not found with id: "    + id);    }
public UserNotFoundException(String email){ super("User not found with email: " + email); }
```

---

### Global Error Handling Strategy

`GlobalExceptionHandler` is annotated with `@RestControllerAdvice`, which means it intercepts exceptions thrown from any `@RestController` in the application — a single place for all error-to-HTTP-response mapping:

```java
// GlobalExceptionHandler.java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTaskNotFound(TaskNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(404, ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedAccessException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
            .body(new ErrorResponse(403, ex.getMessage(), LocalDateTime.now()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
            .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errors);
    }

    @ExceptionHandler(Exception.class)          // catch-all — must be last
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(500, "An unexpected error occurred: " + ex.getMessage(),
                LocalDateTime.now()));
    }

    @Data @AllArgsConstructor
    public static class ErrorResponse {
        private int status;
        private String message;
        private LocalDateTime timestamp;
    }
}
```

The handlers are resolved most-specific-first. `TaskNotFoundException` is caught before the catch-all `Exception` handler, ensuring each error type returns the correct HTTP status.

Every error response shares the same `ErrorResponse` shape — `{ status, message, timestamp }` — so API clients only need to parse one structure for all error cases.

**How it flows end-to-end:**

```
TaskController.getTaskById(99)
  └─► TaskService.getTaskById(99)
        └─► taskRepository.findById(99) → Optional.empty()
              └─► orElseThrow(() -> new TaskNotFoundException(99))
                    └─► [bubbles up uncaught through all layers]
                          └─► GlobalExceptionHandler.handleTaskNotFound()
                                └─► HTTP 404 { status: 404, message: "Task not found with id: 99", ... }
```

No try-catch in `TaskController` or `TaskService`. No checked exception declarations. One handler class manages all of it.

---

## Quick Reference Map

| Java Concept | File(s) |
|---|---|
| Encapsulation | `Task.java`, `User.java`, `ValidationUtils.java` |
| Inheritance | `BaseEntity.java`, `Task.java`, `SubTask.java`, `Comment.java`, `User.java` |
| Polymorphism | `SubTask.java`, `TaskStatus.java`, all Repository interfaces |
| Abstraction | `BaseEntity.java`, `TaskRepository.java`, `TaskService.java` |
| `ArrayList` / `List` | `ValidationUtils.java`, `TaskService.java`, all Repository return types |
| `HashSet` / `Set` | `User.java` (`assignedTasks`) |
| `HashMap` | `GlobalExceptionHandler.java` |
| `LinkedHashMap` | `AnalyticsService.java` |
| `ConcurrentHashMap` | `TaskCache.java` |
| JVM / GC | `TaskCache.java` (`evict`, `clear`), `Task.java` (short-lived request objects) |
| Thread pool / `ExecutorService` | `AsyncConfig.java` |
| `@Async` / `CompletableFuture` | `NotificationService.java` |
| `InterruptedException` handling | `NotificationService.java` |
| Stream API | `AnalyticsService.java` |
| Lambda / method references | `TaskService.java`, `AnalyticsService.java`, `TaskCache.java` |
| `Optional` | `UserRepository.java`, `TaskService.java`, `UserService.java` |
| Unchecked exceptions | `exception/` package (all four custom exceptions) |
| Global exception handler | `GlobalExceptionHandler.java` |
