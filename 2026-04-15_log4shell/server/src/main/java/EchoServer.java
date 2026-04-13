import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoServer {

    private static final Logger log = LogManager.getLogger("echo");

    public static void main(String[] args) throws IOException {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 34598;

        try (ServerSocket server = new ServerSocket(port)) {
            log.info("listening on :{}", port);
            while (true) {
                Socket client = server.accept();
                new Thread(() -> handle(client)).start();
            }
        }
    }

    private static void handle(Socket client) {
        String addr = client.getRemoteSocketAddress().toString();
        log.info("connect from {}", addr);

        try (
            BufferedReader in = new BufferedReader(
                new InputStreamReader(client.getInputStream()));
            PrintWriter out = new PrintWriter(client.getOutputStream(), true)
        ) {
            out.println("ECHO // type anything");

            String line;
            while ((line = in.readLine()) != null) {
                log.info("{} says: {}", addr, line);
                out.println(line);
            }
        } catch (IOException ignored) {
        }

        log.info("disconnect {}", addr);
    }
}
