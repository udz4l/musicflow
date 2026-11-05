package com.musicflow.source;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class LocalCatalogReader {

    public record Entry(
            String code,
            String songName,
            String performer,
            String fileName
    ) {
    }

    public List<Entry> readEntries(Path catalogFile) throws IOException {
        List<String> lines = Files.readAllLines(
                catalogFile,
                StandardCharsets.UTF_8
        );

        List<Entry> entries = new ArrayList<>();

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

            entries.add(new Entry(
                    fields[0],
                    fields[1],
                    fields[2],
                    fields[3]
            ));
        }

        return List.copyOf(entries);
    }
}