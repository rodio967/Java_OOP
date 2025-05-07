package factory.ThreadPool;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class FixedThreadPool implements ThreadPool {
    private final BlockingQueue<Task> taskQueue;
    private final List<WorkerThread> threads;
    private volatile boolean isShutdown;

    public FixedThreadPool(int threadCount) {
        this.taskQueue = new LinkedBlockingQueue<>();
        this.threads = new ArrayList<>(threadCount);
        this.isShutdown = false;


        for (int i = 0; i < threadCount; i++) {
            WorkerThread worker = new WorkerThread("PoolThread-" + i);
            worker.start();
            threads.add(worker);
        }
    }

    @Override
    public void execute(Task task) {
        if (isShutdown) {
            throw new IllegalStateException("ThreadPool is shutdown");
        }
        try {
            taskQueue.put(task);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void shutdown() {
        isShutdown = true;
        threads.forEach(WorkerThread::interrupt);
    }


    private class WorkerThread extends Thread {
        public WorkerThread(String name) {
            super(name);
        }

        @Override
        public void run() {
            while (!isShutdown || !taskQueue.isEmpty()) {
                try {
                    Task task = taskQueue.take();
                    task.execute();
                } catch (InterruptedException e) {
                    if (isShutdown) {break;}
                } catch (Exception e) {
                    System.err.println("Ошибка при выполнении задачи " + e.getMessage());
                }
            }
        }
    }
}
