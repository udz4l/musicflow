package com.musicflow;

import com.musicflow.model.Track;
import com.musicflow.source.HttpCatalogAdapter;
import com.musicflow.source.HttpCatalogClient;
import com.musicflow.source.MusicSource;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class HttpCatalogDemo {

    public static void main(String[] args) throws IOException {
        byte[] catalog = Files.readAllBytes(
                Path.of("demo-audio", "catalog.txt")
        );

        HttpServer server = HttpServer.create(
                new InetSocketAddress("127.0.0.1", 0),
                0
        );

        server.createContext("/catalog.txt", exchange -> {
            try {
                exchange.getResponseHeaders().set(
                        "Content-Type",
                        "text/plain; charset=UTF-8"
                );

                exchange.sendResponseHeaders(200, catalog.length);
                exchange.getResponseBody().write(catalog);
            } finally {
                exchange.close();
            }
        });

        server.start();

        try {
            int port = server.getAddress().getPort();

            URI catalogUri = URI.create(
                    "http://127.0.0.1:" + port + "/catalog.txt"
            );

            MusicSource source = new HttpCatalogAdapter(
                    new HttpCatalogClient(),
                    catalogUri
            );

            List<Track> tracks = source.loadTracks();

            System.out.println("Source: " + catalogUri);
            System.out.println("Loaded tracks: " + tracks.size());

            for (Track track : tracks) {
                System.out.println(
                        track.artist() + " - " + track.title()
                );
                System.out.println("Audio: " + track.audioUri());
            }
        } finally {
            server.stop(0);
        }

        System.out.println("HTTP catalog demo completed.");
    }
}