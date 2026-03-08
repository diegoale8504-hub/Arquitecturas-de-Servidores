package edu.tdse.lab6;
import java.io.*;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.nio.file.*;
import java.util.*;

/**
 * MicroSpringBoot - Minimal IoC Web Framework
 *
 * Demonstrates Java reflection capabilities by:
 * 1. Auto-discovering @RestController components from the classpath
 * 2. Registering @GetMapping routes dynamically
 * 3. Resolving @RequestParam method parameters at runtime
 * 4. Serving static files (HTML, PNG, CSS, JS)
 *
 * Usage (single class mode):
 *   java -cp target/classes co.edu.escuelaing.reflexionlab.MicroSpringBoot co.edu.escuelaing.reflexionlab.FirstWebService
 *
 * Usage (auto-discovery mode):
 *   java -cp target/classes co.edu.escuelaing.reflexionlab.MicroSpringBoot
 */
public class MicroSpringBoot {

    private static final int PORT = 8080;
    private static final String WEBROOT = "src/main/resources/webroot";

    // Route registry: URI -> (controller instance, method)
    private static final Map<String, RouteEntry> routes = new HashMap<>();

    public static void main(String[] args) throws Exception {
        System.out.println("===========================================");
        System.out.println("  MicroSpringBoot IoC Framework Starting  ");
        System.out.println("===========================================");

        if (args.length > 0) {
            // Mode 1: Explicit class from command line
            System.out.println("[IoC] Loading explicit component: " + args[0]);
            loadComponent(Class.forName(args[0]));
        } else {
            // Mode 2: Auto-discover @RestController classes from classpath
            System.out.println("[IoC] Auto-discovering @RestController components...");
            discoverComponents();
        }

        if (routes.isEmpty()) {
            System.out.println("[WARN] No routes registered. Make sure @RestController classes are on the classpath.");
        }

        System.out.println("\n[Server] Registered routes:");
        routes.forEach((path, entry) ->
                System.out.println("  GET " + path + " -> " + entry.controllerClass().getSimpleName() + "." + entry.method().getName() + "()"));

        System.out.println("\n[Server] Listening on http://localhost:" + PORT);
        System.out.println("[Server] Static files served from: " + WEBROOT);
        System.out.println("===========================================\n");

        startServer();
    }

    // -------------------------------------------------------------------------
    // IoC: Component loading via reflection
    // -------------------------------------------------------------------------

    /**
     * Loads a single component class, inspects it for @GetMapping methods,
     * and registers each as a route.
     */
    private static void loadComponent(Class<?> clazz) throws Exception {
        if (!clazz.isAnnotationPresent(RestController.class)) {
            System.out.println("[WARN] Class " + clazz.getName() + " is not annotated with @RestController. Skipping.");
            return;
        }

        Object instance = clazz.getDeclaredConstructor().newInstance();
        System.out.println("[IoC] Instantiated: " + clazz.getSimpleName());

        for (Method method : clazz.getDeclaredMethods()) {
            if (method.isAnnotationPresent(GetMapping.class)) {
                String path = method.getAnnotation(GetMapping.class).value();
                routes.put(path, new RouteEntry(clazz, instance, method));
                System.out.println("[IoC]   Mapped GET " + path + " -> " + method.getName() + "()");
            }
        }
    }

    /**
     * Scans all .class files on the classpath for @RestController annotations
     * and loads them automatically.
     */
    private static void discoverComponents() {
        try {
            String classpath = System.getProperty("java.class.path");
            String[] classpathEntries = classpath.split(File.pathSeparator);

            for (String entry : classpathEntries) {
                File entryFile = new File(entry);
                if (entryFile.isDirectory()) {
                    scanDirectory(entryFile, entryFile);
                }
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Component scan failed: " + e.getMessage());
        }
    }

    private static void scanDirectory(File root, File current) {
        File[] files = current.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(root, file);
            } else if (file.getName().endsWith(".class")) {
                String relativePath = root.toURI().relativize(file.toURI()).getPath();
                String className = relativePath.replace("/", ".").replace("\\", ".").replace(".class", "");
                try {
                    Class<?> clazz = Class.forName(className);
                    if (clazz.isAnnotationPresent(RestController.class)) {
                        System.out.println("[IoC] Found @RestController: " + className);
                        loadComponent(clazz);
                    }
                } catch (Exception ignored) {
                    // Skip classes that can't be loaded
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // HTTP Server
    // -------------------------------------------------------------------------

    private static void startServer() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    handleRequest(clientSocket);
                } catch (Exception e) {
                    System.out.println("[ERROR] Request handling failed: " + e.getMessage());
                }
            }
        }
    }

    private static void handleRequest(Socket clientSocket) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
        OutputStream out = clientSocket.getOutputStream();

        String requestLine = in.readLine();
        if (requestLine == null || requestLine.isEmpty()) return;

        System.out.println("[HTTP] " + requestLine);

        // Parse: GET /path?query HTTP/1.1
        String[] parts = requestLine.split(" ");
        if (parts.length < 2) return;

        String method = parts[0];
        String fullPath = parts[1];

        if (!"GET".equalsIgnoreCase(method)) {
            sendError(out, 405, "Method Not Allowed");
            return;
        }

        // Split path and query string
        String path = fullPath.contains("?") ? fullPath.substring(0, fullPath.indexOf("?")) : fullPath;
        String queryString = fullPath.contains("?") ? fullPath.substring(fullPath.indexOf("?") + 1) : "";
        Map<String, String> queryParams = parseQueryString(queryString);

        // 1. Check dynamic routes first
        if (routes.containsKey(path)) {
            RouteEntry entry = routes.get(path);
            try {
                String response = invokeRoute(entry, queryParams);
                sendTextResponse(out, 200, "OK", "text/html", response);
            } catch (Exception e) {
                sendError(out, 500, "Internal Server Error: " + e.getMessage());
            }
            return;
        }

        // 2. Serve static files
        serveStaticFile(out, path);
    }

    /**
     * Invokes a controller method using reflection, resolving @RequestParam parameters.
     */
    private static String invokeRoute(RouteEntry entry, Map<String, String> queryParams) throws Exception {
        Method method = entry.method();
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isAnnotationPresent(RequestParam.class)) {
                RequestParam rp = parameters[i].getAnnotation(RequestParam.class);
                String paramName = rp.value().isEmpty() ? parameters[i].getName() : rp.value();
                String value = queryParams.getOrDefault(paramName, rp.defaultValue());
                args[i] = value;
            } else {
                args[i] = null;
            }
        }

        Object result = method.invoke(entry.instance(), args);
        return result != null ? result.toString() : "";
    }

    // -------------------------------------------------------------------------
    // Static File Serving
    // -------------------------------------------------------------------------

    private static void serveStaticFile(OutputStream out, String path) throws IOException {
        // Normalize path
        if (path.equals("/")) path = "/index.html";

        File file = new File(WEBROOT + path);

        if (!file.exists() || !file.isFile()) {
            sendError(out, 404, "File not found: " + path);
            return;
        }

        String contentType = getContentType(path);
        byte[] fileBytes = Files.readAllBytes(file.toPath());

        PrintWriter header = new PrintWriter(new OutputStreamWriter(out));
        header.println("HTTP/1.1 200 OK");
        header.println("Content-Type: " + contentType);
        header.println("Content-Length: " + fileBytes.length);
        header.println("Connection: close");
        header.println();
        header.flush();
        out.write(fileBytes);
        out.flush();
    }

    private static String getContentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".css"))  return "text/css; charset=utf-8";
        if (path.endsWith(".js"))   return "application/javascript";
        if (path.endsWith(".png"))  return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".ico"))  return "image/x-icon";
        return "application/octet-stream";
    }

    // -------------------------------------------------------------------------
    // HTTP Response Helpers
    // -------------------------------------------------------------------------

    private static void sendTextResponse(OutputStream out, int code, String status, String contentType, String body) throws IOException {
        byte[] bodyBytes = body.getBytes("UTF-8");
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(out));
        writer.println("HTTP/1.1 " + code + " " + status);
        writer.println("Content-Type: " + contentType + "; charset=utf-8");
        writer.println("Content-Length: " + bodyBytes.length);
        writer.println("Connection: close");
        writer.println();
        writer.flush();
        out.write(bodyBytes);
        out.flush();
    }

    private static void sendError(OutputStream out, int code, String message) throws IOException {
        String body = "<html><body><h1>" + code + " - " + message + "</h1></body></html>";
        sendTextResponse(out, code, message, "text/html", body);
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private static Map<String, String> parseQueryString(String queryString) {
        Map<String, String> params = new HashMap<>();
        if (queryString == null || queryString.isEmpty()) return params;

        for (String pair : queryString.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(decode(kv[0]), decode(kv[1]));
            } else if (kv.length == 1) {
                params.put(decode(kv[0]), "");
            }
        }
        return params;
    }

    private static String decode(String s) {
        try {
            return java.net.URLDecoder.decode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    // -------------------------------------------------------------------------
    // Inner record for route entries
    // -------------------------------------------------------------------------

    record RouteEntry(Class<?> controllerClass, Object instance, Method method) {}
}
