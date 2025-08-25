package factory.ThreadPool;

public interface ThreadPool {
    void execute(Task task);
    void shutdown();
}
