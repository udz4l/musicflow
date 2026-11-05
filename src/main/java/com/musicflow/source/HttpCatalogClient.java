package com.musicflow.source;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class HttpCatalogClient {

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public String fetchCatalog(URI catalogUri) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(catalogUri)
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        try {
            HttpResponse<String> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(
                            StandardCharsets.UTF_8
                    )
            );

            if (response.statusCode() != 200) {
                throw new IOException(
                        "Catalog request failed: HTTP "
                                + response.statusCode()
                );
            }

            return response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            throw new IOException(
                    "Catalog request was interrupted",
                    exception
            );
        }
    }
}