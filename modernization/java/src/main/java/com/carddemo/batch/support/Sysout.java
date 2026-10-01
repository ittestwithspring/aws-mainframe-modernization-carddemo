package com.carddemo.batch.support;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Job output equivalent to the COBOL program's DISPLAY lines (SYSOUT). */
@Component
public class Sysout {

    private final List<String> lines = new ArrayList<>();

    public synchronized void display(String line) {
        lines.add(line);
    }

    public synchronized List<String> lines() {
        return List.copyOf(lines);
    }

    public synchronized void writeTo(Path file) throws IOException {
        StringBuilder text = new StringBuilder();
        lines.forEach(line -> text.append(line).append('\n'));
        Files.writeString(file, text, StandardCharsets.ISO_8859_1);
    }
}
