package com.musicflow.source;

import com.musicflow.model.Track;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class HttpCatalogAdapter implements MusicSource {

    private final HttpCatalogClient client;
    private final URI catalogUri;

    public HttpCatalogAdapter(
            HttpCatalogClient client,
            URI catalogUri
    ) {
        this.client = Objects.requireNonNull(client, "Client is required");
        this.catalogUri = Objects.requireNonNull(
                catalogUri,
                "Catalog URI is required"
        );

        String scheme = catalogUri.getScheme();

        if (!("http".equalsIgnoreCase(scheme)
                || "https".equalsIgnoreCase(scheme))
                || catalogUri.getHost() == null) {
            throw new IllegalArgumentException(
                    "Catalog URI must be an absolute HTTP or HTTPS address"
            );
        }
    }

    @Override
    public List<Track> loadTracks() throws IOException {
        String catalog = client.fetchCatalog(catalogUri);

        List<Track> tracks = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        List<String> lines = catalog.lines().toList();

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).strip();

            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            String[] fields = line.split(";", -1);

            if (fields.length != 4) {
                throw new IOException(
                        "Catalog line " + (i + 1)
                                + " must contain four fields"
                );
            }

            for (int j = 0; j < fields.length; j++) {
                fields[j] = fields[j].strip();

                if (fields[j].isEmpty()) {
                    throw new IOException(
                            "Empty field at catalog line " + (i + 1)
                    );
                }
            }

            if (!ids.add(fields[0])) {
                throw new IOException(
                        "Duplicate track ID: " + fields[0]
                );
            }

            try {
                URI audioUri = catalogUri.resolve(fields[3]);
                String scheme = audioUri.getScheme();

                if (!("http".equalsIgnoreCase(scheme)
                        || "https".equalsIgnoreCase(scheme))
                        || audioUri.getHost() == null) {
                    throw new IllegalArgumentException(
                            "Remote audio must use HTTP or HTTPS"
                    );
                }

                tracks.add(new Track(
                        fields[0],
                        fields[1],
                        fields[2],
                        audioUri
                ));
            } catch (IllegalArgumentException exception) {
                throw new IOException(
                        "Invalid remote track at catalog line " + (i + 1),
                        exception
                );
            }
        }

        return List.copyOf(tracks);
    }
}