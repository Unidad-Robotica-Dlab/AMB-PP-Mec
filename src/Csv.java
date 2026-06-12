package com.pph.simramjava;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.nio.file.Path;
import java.util.*;

final class Csv {
    private Csv() {}

    static void write(Path path, List<Map<String,Object>> rows) throws IOException {
        if (rows==null || rows.isEmpty()) return;
        ensureParent(path);
        List<String> header = inferHeader(rows);
        try (BufferedWriter w = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writeHeader(w, header);
            writeRows(w, header, rows);
        }
        System.out.println("[CSV] " + path + " ("+rows.size()+" filas)");
    }

    static void append(Path path, List<Map<String,Object>> rows) throws IOException {
        if (rows == null || rows.isEmpty()) return;
        ensureParent(path);
        boolean exists = Files.exists(path) && Files.size(path) > 0;
        List<String> header;
        if (exists) {
            header = readHeader(path);
            ensureCompatibleHeader(header, rows, path);
        } else {
            header = inferHeader(rows);
        }
        try (BufferedWriter w = Files.newBufferedWriter(
                path,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND
        )) {
            if (!exists) {
                writeHeader(w, header);
            }
            writeRows(w, header, rows);
        }
        System.out.println("[CSV] " + path + " (+"+rows.size()+" filas)");
    }

    private static void ensureParent(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) Files.createDirectories(parent);
    }

    private static List<String> inferHeader(List<Map<String,Object>> rows) {
        Set<String> keys = new TreeSet<>();
        for (Map<String,Object> r: rows) keys.addAll(r.keySet());
        return new ArrayList<>(keys);
    }

    private static List<String> readHeader(Path path) throws IOException {
        String firstLine;
        try (java.io.BufferedReader r = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            firstLine = r.readLine();
        }
        if (firstLine == null || firstLine.isEmpty()) {
            return Collections.emptyList();
        }
        return Arrays.asList(firstLine.split(",", -1));
    }

    private static void ensureCompatibleHeader(List<String> header, List<Map<String,Object>> rows, Path path) {
        Set<String> allowed = new HashSet<>(header);
        Set<String> extras = new TreeSet<>();
        for (Map<String,Object> row : rows) {
            for (String key : row.keySet()) {
                if (!allowed.contains(key)) {
                    extras.add(key);
                }
            }
        }
        if (!extras.isEmpty()) {
            throw new IllegalArgumentException("CSV append incompatible para " + path + ". Nuevas columnas: " + extras);
        }
    }

    private static void writeHeader(BufferedWriter w, List<String> header) throws IOException {
        for (int i = 0; i < header.size(); i++) {
            if (i > 0) w.write(',');
            w.write(header.get(i));
        }
        w.write('\n');
    }

    private static void writeRows(BufferedWriter w, List<String> header, List<Map<String,Object>> rows) throws IOException {
        for (Map<String,Object> r : rows) {
            for (int i = 0; i < header.size(); i++) {
                if (i > 0) w.write(',');
                Object v = r.get(header.get(i));
                if (v == null) continue;
                writeCell(w, String.valueOf(v));
            }
            w.write('\n');
        }
    }

    private static void writeCell(BufferedWriter w, String s) throws IOException {
        if (s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0) {
            w.write('"');
            for (int i = 0; i < s.length(); i++) {
                char ch = s.charAt(i);
                if (ch == '"') w.write("\"\"");
                else w.write(ch);
            }
            w.write('"');
            return;
        }
        w.write(s);
    }
}
