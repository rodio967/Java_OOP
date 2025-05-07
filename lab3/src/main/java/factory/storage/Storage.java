package factory.storage;

import java.util.LinkedList;
import java.util.Queue;

public class Storage<T> {
    private final int capacity;
    private final Queue<T> items = new LinkedList<>();
    private final String name;
    private int producedCount = 0;

    public Storage(int capacity, String name) {
        this.capacity = capacity;
        this.name = name;
    }

    public synchronized void add(T item) throws InterruptedException {
        while (items.size() >= capacity) {
            wait();
        }
        items.add(item);
        producedCount++;
        notifyAll();
    }

    public synchronized T get() throws InterruptedException {
        while (items.isEmpty()) {
            wait();
        }
        T item = items.poll();
        notifyAll();
        return item;
    }

    public synchronized int getSize() {
        return items.size();
    }

    public synchronized int getProducedCount() {
        return producedCount;
    }

    public String getName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }
}
