/*
Functional requirement

Functional requirements
1. Schedule a task for a specific time.
2. Support HIGH, MEDIUM, and LOW priorities.
3. Support one-time and recurring tasks.
4. Cancel a task before execution.
5. Run tasks concurrently using a thread pool.
6. Track task status.


Non-functional requirements
1. Thread-safe scheduling and cancellation.
2. A task must not be dispatched twice.
3. One task's failure must not stop other tasks.
4. Clean separation of responsibilities.
*

Entities

1. ScheduledTask
2. TaskSchedular
3. Dispatcher
4. ExecutorService
* */

import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

enum Priority {
    HIGH, MEDIUM, LOW
}

enum TaskType {
    ONE_TIME, RECURRING
}

enum TaskStatus {
    SCHEDULED, RUNNING, COMPLETED, FAILED, CANCELLED
}

interface Task {
    void execute();
}

class ScheduledTask implements Comparable<ScheduledTask> {
    final String id;
    final Task task;
    volatile long executionTime;
    final Priority priority;
    final TaskType taskType;
    final long interval;
    volatile TaskStatus status;

    public ScheduledTask(Task task, long executionTime, Priority priority, TaskType taskType, long interval) {
        this.id = UUID.randomUUID().toString();
        this.task = task;
        this.executionTime = executionTime;
        this.priority = priority;
        this.taskType = taskType;
        this.interval = interval;
        this.status = TaskStatus.SCHEDULED;
    }

    @Override
    public int compareTo(ScheduledTask other) {
        int result = Long.compare(this.executionTime, other.executionTime);
        if (result != 0) {
            return result;
        }
        // Earlier priority ordinal (HIGH=0) wins when execution times tie.
        return Integer.compare(this.priority.ordinal(), other.priority.ordinal());
    }
}

class TaskScheduler {
    final PriorityQueue<ScheduledTask> queue = new PriorityQueue<>();

    public void schedule(ScheduledTask task) {
        synchronized (queue) {
            task.status = TaskStatus.SCHEDULED;
            queue.offer(task);
            queue.notifyAll();
        }
    }

    public boolean cancelTask(String taskId) {
        synchronized (queue) {
            ScheduledTask target = null;
            for (ScheduledTask task : queue) {
                if (task.id.equals(taskId)) {
                    target = task;
                    break;
                }
            }
            if (target != null) {
                target.status = TaskStatus.CANCELLED;
                queue.remove(target);
                queue.notifyAll();
                return true;
            }
            return false;
        }
    }
}

class TaskDispatcher implements Runnable {
    private final TaskScheduler scheduler;
    private final ExecutorService workerPool;
    private volatile boolean running;
    private Thread dispatcherThread;

    public TaskDispatcher(TaskScheduler scheduler, ExecutorService workerPool) {
        this.scheduler = scheduler;
        this.workerPool = workerPool;
    }

    public void start() {
        running = true;
        dispatcherThread = new Thread(this, "task-dispatcher");
        dispatcherThread.start();
    }

    public void stop() {
        running = false;
        synchronized (scheduler.queue) {
            // Wake the dispatcher so it can observe running == false and exit.
            scheduler.queue.notifyAll();
        }
        if (dispatcherThread != null) {
            dispatcherThread.interrupt();
        }
    }

    @Override
    public void run() {
        while (running) {
            ScheduledTask task;
            synchronized (scheduler.queue) {
                // 1. Wait while there is nothing to do.
                while (scheduler.queue.isEmpty() && running) {
                    try {
                        scheduler.queue.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
                if (!running) {
                    return;
                }

                // 2. Look at the earliest task without removing it yet.
                task = scheduler.queue.peek();
                long waitTime = task.executionTime - System.currentTimeMillis();

                // 3. If it is not due yet, wait until its time (or until the queue changes).
                if (waitTime > 0) {
                    try {
                        scheduler.queue.wait(waitTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    continue;
                }

                // 4. Claim the due task. Removing it here guarantees it is dispatched once.
                task = scheduler.queue.poll();
            }

            // 5. Hand off execution to the worker pool so the dispatcher stays free.
            final ScheduledTask readyTask = task;
            workerPool.submit(() -> runTask(readyTask));
        }
    }

    private void runTask(ScheduledTask readyTask) {
        // Cancelled between scheduling and dispatch? Skip it.
        if (readyTask.status == TaskStatus.CANCELLED) {
            return;
        }
        readyTask.status = TaskStatus.RUNNING;
        try {
            readyTask.task.execute();
            readyTask.status = TaskStatus.COMPLETED;
        } catch (Exception e) {
            // One task's failure must not stop others — isolate and record it.
            readyTask.status = TaskStatus.FAILED;
            System.err.println("Task " + readyTask.id + " failed: " + e.getMessage());
        }

        // Recurring tasks are re-scheduled for their next interval.
        if (readyTask.taskType == TaskType.RECURRING && running) {
            readyTask.executionTime = System.currentTimeMillis() + readyTask.interval;
            scheduler.schedule(readyTask);
        }
    }
}

public class TaskSchedulingSystem {
    public static void main(String[] args) throws InterruptedException {
        TaskScheduler scheduler = new TaskScheduler();
        ExecutorService workerPool = Executors.newFixedThreadPool(4);
        TaskDispatcher dispatcher = new TaskDispatcher(scheduler, workerPool);
        dispatcher.start();

        long now = System.currentTimeMillis();

        // One-time HIGH priority task, due in ~500ms.
        ScheduledTask t1 = new ScheduledTask(
                () -> System.out.println("[t1] one-time HIGH ran at " + System.currentTimeMillis()),
                now + 500, Priority.HIGH, TaskType.ONE_TIME, 0);

        // One-time task that fails — must not affect others.
        ScheduledTask t2 = new ScheduledTask(
                () -> { throw new RuntimeException("boom"); },
                now + 500, Priority.MEDIUM, TaskType.ONE_TIME, 0);

        // Recurring LOW priority task every 1s.
        ScheduledTask t3 = new ScheduledTask(
                () -> System.out.println("[t3] recurring tick at " + System.currentTimeMillis()),
                now + 1000, Priority.LOW, TaskType.RECURRING, 1000);

        // A task we will cancel before it runs.
        ScheduledTask t4 = new ScheduledTask(
                () -> System.out.println("[t4] SHOULD NOT RUN"),
                now + 2000, Priority.HIGH, TaskType.ONE_TIME, 0);

        scheduler.schedule(t1);
        scheduler.schedule(t2);
        scheduler.schedule(t3);
        scheduler.schedule(t4);

        // Cancel t4 before its execution time.
        Thread.sleep(800);
        System.out.println("Cancelling t4: " + scheduler.cancelTask(t4.id) + " (status=" + t4.status + ")");

        // Let the recurring task tick a few times.
        Thread.sleep(3500);

        System.out.println("Statuses -> t1:" + t1.status + " t2:" + t2.status + " t3:" + t3.status + " t4:" + t4.status);

        // Graceful shutdown.
        dispatcher.stop();
        workerPool.shutdown();
        if (!workerPool.awaitTermination(5, TimeUnit.SECONDS)) {
            workerPool.shutdownNow();
        }
        System.out.println("Scheduler stopped.");
    }
}
