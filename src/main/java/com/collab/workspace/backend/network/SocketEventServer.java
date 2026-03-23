package com.collab.workspace.backend.network;

import com.collab.workspace.backend.service.EventPublisher;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

@Component
public class SocketEventServer {

    private final EventPublisher eventPublisher;
    private final boolean enabled;
    private final int port;
    private final Set<PrintWriter> clients = ConcurrentHashMap.newKeySet();
    private final Consumer<String> broadcaster = this::broadcast;
    private ServerSocket serverSocket;
    private Thread acceptThread;

    public SocketEventServer(
        EventPublisher eventPublisher,
        @Value("${backend.events.enabled:true}") boolean enabled,
        @Value("${backend.events.port:9091}") int port
    ) {
        this.eventPublisher = eventPublisher;
        this.enabled = enabled;
        this.port = port;
    }

    @PostConstruct
    public void start() {
        if (!enabled) {
            return;
        }

        eventPublisher.register(broadcaster);
        acceptThread = new Thread(this::runServer, "java-workspace-event-server");
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    private void runServer() {
        try (ServerSocket socket = new ServerSocket(port)) {
            this.serverSocket = socket;
            while (!Thread.currentThread().isInterrupted()) {
                Socket client = socket.accept();
                PrintWriter writer = new PrintWriter(client.getOutputStream(), true);
                clients.add(writer);
                writer.println("CONNECTED|java-workspace-events");
            }
        } catch (IOException ignored) {
        }
    }

    private void broadcast(String payload) {
        clients.removeIf(PrintWriter::checkError);
        clients.forEach(writer -> writer.println(payload));
    }

    @PreDestroy
    public void stop() {
        eventPublisher.unregister(broadcaster);
        if (acceptThread != null) {
            acceptThread.interrupt();
        }
        if (serverSocket != null) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
        clients.forEach(PrintWriter::close);
        clients.clear();
    }
}
