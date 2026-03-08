package com.minsang.notionlite.lab.persistence;

import com.minsang.notionlite.lab.storage.SlottedPage;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Persists page list into one binary file.
 */
public class PageFileStore {
    private static final int MAGIC = 0xDB5A1234;

    public void save(Path path, List<SlottedPage> pages) {
        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(path)))) {
                out.writeInt(MAGIC);
                out.writeInt(pages.size());
                for (SlottedPage page : pages) {
                    byte[] payload = page.toBytes();
                    out.writeInt(payload.length);
                    out.write(payload);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public List<SlottedPage> load(Path path) {
        if (!Files.exists(path)) {
            return List.of();
        }

        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(path)))) {
            int magic = in.readInt();
            if (magic != MAGIC) {
                throw new IllegalStateException("Invalid snapshot file magic");
            }

            int pageCount = in.readInt();
            List<SlottedPage> pages = new ArrayList<>(pageCount);
            for (int i = 0; i < pageCount; i++) {
                int length = in.readInt();
                byte[] payload = in.readNBytes(length);
                pages.add(SlottedPage.fromBytes(payload));
            }
            return pages;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
