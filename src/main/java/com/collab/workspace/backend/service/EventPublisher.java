package com.collab.workspace.backend.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

@Component
public class EventPublisher {

    private final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();

    public void register(Consumer<String> listener) {
        listeners.add(listener);
    }

    public void unregister(Consumer<String> listener) {
        listeners.remove(listener);
    }

    public void publish(String type, String message) {
        String payload = Instant.now() + "|" + type + "|" + message;
        listeners.forEach(listener -> listener.accept(payload));
    }
}
