import java.io.*;
import java.net.*;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// =========================================================
// CLIENT HANDLER
// =========================================================

class ClientHandler {

    String name;
    Socket socket;
    BufferedReader in;
    PrintWriter out;

    ClientHandler(
            String name,
            Socket socket,
            BufferedReader in,
            PrintWriter out
    ) {
        this.name = name;
        this.socket = socket;
        this.in = in;
        this.out = out;
    }
}

// =========================================================
// TCP SERVER
// =========================================================

public class TcpServer {

    private static final Map<String, ClientHandler> clients =
            new ConcurrentHashMap<>();

    public static void main(String[] args) {

        start(5000);
    }

    public static void start(int port) {

        try (ServerSocket serverSocket =
                     new ServerSocket(port)) {

            System.out.println(
                    "TCP Server läuft auf Port " + port
            );

            while (true) {

                Socket socket = serverSocket.accept();

                new Thread(() ->
                        handle(socket)
                ).start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =====================================================
    // CLIENT THREAD
    // =====================================================

    private static void handle(Socket socket) {

        String name = null;

        try (

                BufferedReader in = new BufferedReader(
                        new InputStreamReader(
                                socket.getInputStream()
                        )
                );

                PrintWriter out = new PrintWriter(
                        socket.getOutputStream(),
                        true
                )

        ) {

            // =================================================
            // REGISTRIERUNG
            // =================================================

            while (true) {

                out.println("register <name>");

                String reg = in.readLine();

                if (reg == null) {
                    socket.close();
                    return;
                }

                String[] parts = reg.split(" ");

                // =============================================
                // FORMAT PRÜFEN
                // =============================================

                if (parts.length != 2 ||
                        !parts[0].equalsIgnoreCase("register")) {

                    out.println(
                            "Erwartet: register <name>"
                    );

                    continue;
                }

                name = parts[1];

                // =============================================
                // NAME SCHON VERGEBEN
                // =============================================

                if (clients.containsKey(name)) {

                    out.println(
                            "Name bereits vergeben!"
                    );

                    continue;
                }

                break;
            }

            // =================================================
            // CLIENT REGISTRIEREN
            // =================================================

            ClientHandler handler =
                    new ClientHandler(
                            name,
                            socket,
                            in,
                            out
                    );

            clients.put(name, handler);

            System.out.println(name + " connected");

            // =================================================
            // MESSAGE LOOP
            // =================================================

            String msg;

            while ((msg = in.readLine()) != null) {

                System.out.println(name + ": " + msg);

                // =============================================
                // SEND
                // =============================================

                if (msg.startsWith("send ")) {

                    String[] parts =
                            msg.split(" ", 3);

                    if (parts.length == 3) {

                        String target = parts[1];
                        String text = parts[2];

                        if (clients.containsKey(target)) {

                            clients.get(target).out.println(
                                    name + ": " + text
                            );
                        }
                    }
                }
            }

        } catch (Exception e) {

            System.out.println(name + " disconnected");

        } finally {

            // =================================================
            // DISCONNECT
            // =================================================

            if (name != null) {

                clients.remove(name);

                System.out.println(
                        "Removed: " + name
                );
            }

            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }
}