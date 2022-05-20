package fr.umlv.java.models;

import fr.umlv.java.models.context.ContextClient;

import java.util.ArrayDeque;

public class Commands {
    private final int CAPACITY = 10;
    private final static Object lock = new Object();
    private final ArrayDeque<Message> queue = new ArrayDeque<>(CAPACITY);
    private final ContextClient contextClient;

    public Commands(ContextClient contextClient) {
        this.contextClient = contextClient;
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
                contextClient.queueMessage(queue.pop());
            }
            lock.notify();
        }
    }
}
