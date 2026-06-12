package com.pph.simramjava;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Parser JSON minimal (objetos, arrays, strings, números, boolean, null).
 * Devuelve Map/List/Double/String/Boolean/null.
 */
public final class Json {
    private final String s;
    private int i;

    private Json(String s) { this.s = s; this.i = 0; }

    public static Object parse(String s) { return new Json(s).parseValue(); }
    public static Object parse(Path p) throws IOException { return parse(Files.readString(p, StandardCharsets.UTF_8)); }

    private void skip() { while (i < s.length()) { char c = s.charAt(i); if (c==' '||c=='\n'||c=='\r'||c=='\t') i++; else break; } }
    private char peek() { skip(); return i < s.length() ? s.charAt(i) : '\0'; }
    private char next() { return i < s.length() ? s.charAt(i++) : '\0'; }
    private void expect(char c) { char got = next(); if (got != c) throw new RuntimeException("JSON: esperado '"+c+"' got '"+got+"'"); }

    private Object parseValue() {
        skip();
        char c = peek();
        if (c == '{') return parseObject();
        if (c == '[') return parseArray();
        if (c == '"') return parseString();
        if (c == 't' || c == 'f') return parseBoolean();
        if (c == 'n') return parseNull();
        return parseNumber();
    }

    @SuppressWarnings("unchecked")
    private Map<String,Object> parseObject() {
        expect('{');
        Map<String,Object> m = new LinkedHashMap<>();
        skip();
        if (peek() == '}') { next(); return m; }
        while (true) {
            skip();
            String k = parseString();
            skip(); expect(':');
            Object v = parseValue();
            m.put(k, v);
            skip();
            char c = next();
            if (c == '}') break;
            if (c != ',') throw new RuntimeException("JSON: esperado ',' or '}', got '"+c+"'");
        }
        return m;
    }

    private List<Object> parseArray() {
        expect('[');
        List<Object> a = new ArrayList<>();
        skip();
        if (peek() == ']') { next(); return a; }
        while (true) {
            Object v = parseValue();
            a.add(v);
            skip();
            char c = next();
            if (c == ']') break;
            if (c != ',') throw new RuntimeException("JSON: esperado ',' or ']', got '"+c+"'");
        }
        return a;
    }

    private String parseString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (i < s.length()) {
            char c = next();
            if (c == '"') break;
            if (c == '\\') {
                char e = next();
                switch (e) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u': {
                        int code=0; for (int k=0;k<4;k++){ char h=next(); code = (code<<4) + hex(h);} sb.append((char)code); break; }
                    default: throw new RuntimeException("JSON: escape inválido: "+e);
                }
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private static int hex(char c) {
        if (c>='0'&&c<='9') return c-'0';
        if (c>='a'&&c<='f') return 10 + (c-'a');
        if (c>='A'&&c<='F') return 10 + (c-'A');
        throw new RuntimeException("JSON: hex inválido "+c);
    }

    private Boolean parseBoolean() {
        skip();
        if (s.startsWith("true", i)) { i+=4; return Boolean.TRUE; }
        if (s.startsWith("false", i)) { i+=5; return Boolean.FALSE; }
        throw new RuntimeException("JSON: boolean inválido en pos "+i);
    }

    private Object parseNull() {
        skip();
        if (s.startsWith("null", i)) { i+=4; return null; }
        throw new RuntimeException("JSON: null inválido en pos "+i);
    }

    private Number parseNumber() {
        skip();
        int start = i;
        boolean dot = false, exp = false;
        if (peek()=='-' ) next();
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c=='.') { dot=true; i++; continue; }
            if (c=='e'||c=='E') { exp=true; i++; if (i<s.length() && (s.charAt(i)=='+'||s.charAt(i)=='-')) i++; continue; }
            if (c>='0'&&c<='9') { i++; continue; }
            break;
        }
        String sub = s.substring(start, i);
        try {
            if (!dot && !exp) {
                long v = Long.parseLong(sub);
                if (v >= Integer.MIN_VALUE && v <= Integer.MAX_VALUE) return (int)v;
                return v;
            }
            return Double.parseDouble(sub);
        } catch (NumberFormatException nfe) {
            throw new RuntimeException("JSON: número inválido '"+sub+"'", nfe);
        }
    }
}

