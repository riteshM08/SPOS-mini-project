package com.airline.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class StaticFileHandler implements HttpHandler {

    private final String baseDir;
    private static final Map<String, String> MIME_TYPES = new HashMap<>();

    static {
        MIME_TYPES.put("html", "text/html; charset=utf-8");
        MIME_TYPES.put("htm", "text/html; charset=utf-8");
        MIME_TYPES.put("css", "text/css; charset=utf-8");
        MIME_TYPES.put("js", "application/javascript; charset=utf-8");
        MIME_TYPES.put("json", "application/json; charset=utf-8");
        MIME_TYPES.put("svg", "image/svg+xml");
        MIME_TYPES.put("png", "image/png");
        MIME_TYPES.put("jpg", "image/jpeg");
        MIME_TYPES.put("jpeg", "image/jpeg");
        MIME_TYPES.put("ico", "image/x-icon");
        MIME_TYPES.put("txt", "text/plain; charset=utf-8");
    }

    public StaticFileHandler(String baseDir) {
        this.baseDir = baseDir;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String path = uri.getPath();

        if (path.equals("/") || path.isEmpty()) {
            path = "/index.html";
        }

        // Prevent directory traversal
        File file = new File(baseDir, path.replace('/', File.separatorChar)).getCanonicalFile();
        File base = new File(baseDir).getCanonicalFile();

        if (!file.getPath().startsWith(base.getPath()) || !file.exists() || file.isDirectory()) {
            // SPA fallback: serve index.html
            file = new File(baseDir, "index.html");
            if (!file.exists()) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
        }

        String ext = "";
        int dot = file.getName().lastIndexOf('.');
        if (dot > 0) {
            ext = file.getName().substring(dot + 1).toLowerCase();
        }
        String contentType = MIME_TYPES.getOrDefault(ext, "application/octet-stream");

        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.sendResponseHeaders(200, file.length());

        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = exchange.getResponseBody()) {
            byte[] buf = new byte[8192];
            int n;
            while ((n = fis.read(buf)) != -1) {
                os.write(buf, 0, n);
            }
        }
    }
}
