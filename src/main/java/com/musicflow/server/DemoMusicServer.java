package com.musicflow.server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DemoMusicServer implements AutoCloseable {

    private final Path directory;
    private final HttpServer server;

    private final ExecutorService executor =
            Executors.newFixedThreadPool(4);

    public DemoMusicServer(Path directory) throws IOException {
        this.directory = directory.toRealPath();

        if (!Files.isDirectory(this.directory)) {
            throw new IOException(
                    "Music directory does not exist: " + directory
            );
        }

        server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );

        server.createContext("/", this::handleRequest);
        server.setExecutor(executor);
    }

    public void start() {
        server.start();
    }

    public URI catalogUri() {
        return URI.create(
                "http://127.0.0.1:"
                        + server.getAddress().getPort()
                        + "/catalog.txt"
        );
    }

    private void handleRequest(HttpExchange exchange)
            throws IOException {

        try {
            if (!"GET".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Allow", "GET");
                exchange.sendResponseHeaders(405, -1);
                return;
            }

            String requestPath = exchange.getRequestURI().getPath();

            if (requestPath == null || !requestPath.startsWith("/")) {
                exchange.sendResponseHeaders(400, -1);
                return;
            }

            String name = requestPath.substring(1);

            // This demo serves only a flat catalog and WAV files.
            if (name.isBlank()
                    || name.contains("/")
                    || name.contains("\\")
                    || !(name.equals("catalog.txt")
                    || name.endsWith(".wav"))) {

                exchange.sendResponseHeaders(404, -1);
                return;
            }

            Path requested = directory.resolve(name).normalize();

            if (!requested.startsWith(directory)
                    || !Files.isRegularFile(requested)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            Path realFile = requested.toRealPath();

            if (!realFile.startsWith(directory)) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }

            String contentType = name.endsWith(".wav")
                    ? "audio/wav"
                    : "text/plain; charset=UTF-8";

            try (InputStream input = Files.newInputStream(realFile)) {
                exchange.getResponseHeaders().set(
                        "Content-Type",
                        contentType
                );

                exchange.sendResponseHeaders(
                        200,
                        Files.size(realFile)
                );

                input.transferTo(exchange.getResponseBody());
            }
        } finally {
            exchange.close();
        }
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }
}