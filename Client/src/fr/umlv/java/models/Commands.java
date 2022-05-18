package fr.umlv.java.models;

import java.util.ArrayDeque;

public class Commands {
    private final int CAPACITY = 10;
    private final static Object lock = new Object();
    private final ArrayDeque<Message> queue = new ArrayDeque<>(CAPACITY);
    private final Context context;

    public Commands(Context context) {
        this.context = context;
    }
    public void transferMessage(Message msg) throws InterruptedException {
        synchronized (lock) {
            while (queue.size() == CAPACITY)
                lock.wait();
            queue.add(msg);
            lock.notify();
        }
    }

    public void retrieveCommand() {
        synchronized (lock) {
            while(!queue.isEmpty()) {
                context.queueMessage(queue.pop());
            }
            lock.notify();
        }
    }
}
