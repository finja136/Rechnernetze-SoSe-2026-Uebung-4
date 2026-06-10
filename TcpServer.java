import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// =========================================================
// CLIENT HANDLER
// =========================================================

class ClientHandler {
    String name;
    Socket socket;
    BufferedReader in;
    PrintWriter out;

    ClientHandler(String name, Socket socket,
                  BufferedReader in, PrintWriter out) {
        this.name   = name;
        this.socket = socket;
        this.in     = in;
        this.out    = out;
    }
}

// =========================================================
// WÜRFELSPIEL – Zustandsklasse
// =========================================================

class DiceGame {
    String inviter;   // wer eingeladen hat
    String invitee;   // wer eingeladen wurde
    boolean pending;  // wartet auf Annahme

    DiceGame(String inviter, String invitee) {
        this.inviter = inviter;
        this.invitee = invitee;
        this.pending = true;
    }
}


public class TcpServer {

    private static final Map<String, ClientHandler> clients =
            new ConcurrentHashMap<>();

    // aktive Würfelspiele: Schlüssel = invitee-Name
    private static final Map<String, DiceGame> diceGames =
            new ConcurrentHashMap<>();

    public static void main(String[] args) {
        start(5000);
    }

    public static void start(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("TCP Server läuft auf Port " + port);
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> handle(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =====================================================
    // BROADCAST – an alle außer Absender
    // =====================================================

    private static void broadcast(String message, String excludeName) {
        clients.forEach((name, handler) -> {
            if (!name.equals(excludeName)) {
                handler.out.println(message);
            }
        });
    }

    // =====================================================
    // CLIENT THREAD
    // =====================================================

    private static void handle(Socket socket) {

        String name = null;

        try (
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(
                        socket.getOutputStream(), true)
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

                if (parts.length != 2 ||
                        !parts[0].equalsIgnoreCase("register")) {
                    out.println("Erwartet: register <name>");
                    continue;
                }

                name = parts[1];

                if (clients.containsKey(name)) {
                    out.println("Name bereits vergeben!");
                    continue;
                }

                break;
            }

            // =================================================
            // CLIENT REGISTRIEREN
            // =================================================

            ClientHandler handler =
                    new ClientHandler(name, socket, in, out);
            clients.put(name, handler);

            System.out.println(name + " connected");

            // AUFGABE 3d: Verbindungsbenachrichtigung
            broadcast("[Server] " + name + " ist dem Chat beigetreten.", name);
            out.println("[Server] Willkommen, " + name + "!");

            // =================================================
            // MESSAGE LOOP
            // =================================================

            String msg;
            while ((msg = in.readLine()) != null) {

                System.out.println(name + ": " + msg);

                // =============================================
                // send <Ziel> <Text>
                // =============================================

                if (msg.startsWith("send ")) {

                    String[] parts = msg.split(" ", 3);
                    if (parts.length == 3) {
                        String target = parts[1];
                        String text   = parts[2];
                        if (clients.containsKey(target)) {
                            clients.get(target).out.println(name + ": " + text);
                        } else {
                            out.println("[Server] Client '" + target + "' nicht gefunden.");
                        }
                    }
                }

                // =============================================
                // AUFGABE 3b: clientlist
                // =============================================

                else if (msg.equalsIgnoreCase("clientlist")) {

                    StringBuilder sb = new StringBuilder("[Server] Verbundene Clients:");
                    clients.forEach((n, h) ->
                            sb.append("\n  - ").append(n));
                    out.println(sb.toString());
                }

                // =============================================
                // AUFGABE 3c: sendall <Nachricht>
                // =============================================

                else if (msg.startsWith("sendall ")) {

                    String text = msg.substring(8);
                    broadcast(name + " (an alle): " + text, name);
                    out.println("[Server] Nachricht an alle gesendet.");
                }

                // =============================================
                // AUFGABE 3e: dice invite <Client>
                // =============================================

                else if (msg.startsWith("dice invite ")) {

                    String target = msg.substring(12).trim();

                    if (!clients.containsKey(target)) {
                        out.println("[Würfel] Client '" + target + "' nicht gefunden.");

                    } else if (target.equals(name)) {
                        out.println("[Würfel] Du kannst dich nicht selbst einladen.");

                    } else if (diceGames.containsKey(target)) {
                        out.println("[Würfel] '" + target + "' hat bereits eine ausstehende Einladung.");

                    } else {
                        DiceGame game = new DiceGame(name, target);
                        diceGames.put(target, game);

                        clients.get(target).out.println(
                                "[Würfel] " + name + " lädt dich zu einem Würfelspiel ein. "
                                        + "Antworte mit 'dice join' oder 'dice decline'.");
                        out.println("[Würfel] Einladung an " + target + " gesendet. Warte auf Antwort...");
                    }
                }

                // =============================================
                // AUFGABE 3e: dice join
                // =============================================

                else if (msg.equalsIgnoreCase("dice join")) {

                    DiceGame game = diceGames.get(name);

                    if (game == null) {
                        out.println("[Würfel] Keine ausstehende Einladung gefunden.");

                    } else {
                        diceGames.remove(name);
                        game.pending = false;

                        // Benachrichtigung an Einladenden
                        if (clients.containsKey(game.inviter)) {
                            clients.get(game.inviter).out.println(
                                    "[Würfel] " + name + " hat die Einladung angenommen!");
                        }

                        out.println("[Würfel] Du hast die Einladung angenommen!");

                        // Würfeln
                        playDice(game.inviter, name);
                    }
                }

                // =============================================
                // AUFGABE 3e: dice decline
                // =============================================

                else if (msg.equalsIgnoreCase("dice decline")) {

                    DiceGame game = diceGames.remove(name);

                    if (game == null) {
                        out.println("[Würfel] Keine ausstehende Einladung gefunden.");

                    } else {
                        out.println("[Würfel] Du hast die Einladung abgelehnt.");

                        if (clients.containsKey(game.inviter)) {
                            clients.get(game.inviter).out.println(
                                    "[Würfel] " + name + " hat die Einladung abgelehnt. Spiel abgebrochen.");
                        }
                    }
                }

                // =============================================
                // Unbekannter Befehl
                // =============================================

                else {
                    out.println("[Server] Unbekannter Befehl. Verfügbar: "
                            + "send <name> <msg> | sendall <msg> | clientlist | "
                            + "dice invite <name> | dice join | dice decline");
                }
            }

        } catch (Exception e) {
            System.out.println(name + " disconnected (exception)");

        } finally {

            // =================================================
            // DISCONNECT
            // =================================================

            if (name != null) {
                clients.remove(name);
                diceGames.remove(name); // ggf. ausstehende Einladung entfernen

                System.out.println("Removed: " + name);

                // AUFGABE 3d: Trennungsbenachrichtigung
                broadcast("[Server] " + name + " hat den Chat verlassen.", name);
            }

            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    // =====================================================
    // AUFGABE 3e: Würfellogik
    // =====================================================

    private static void playDice(String playerA, String playerB) {

        Random rnd = new Random();

        // Zwei Würfe pro Spieler (1–6)
        int a1 = rnd.nextInt(6) + 1;
        int a2 = rnd.nextInt(6) + 1;
        int b1 = rnd.nextInt(6) + 1;
        int b2 = rnd.nextInt(6) + 1;

        int sumA = a1 + a2;
        int sumB = b1 + b2;

        String resultFromA, resultFromB;

        if (sumA > sumB) {
            resultFromA = "Gewonnen";
            resultFromB = "Verloren";
        } else if (sumA < sumB) {
            resultFromA = "Verloren";
            resultFromB = "Gewonnen";
        } else {
            resultFromA = "Unentschieden";
            resultFromB = "Unentschieden";
        }

        // Nachricht an Spieler A (Einladender)
        if (clients.containsKey(playerA)) {
            clients.get(playerA).out.println(
                    "[Würfel] === Ergebnis ===\n"
                            + "  Deine Würfe: " + a1 + " + " + a2 + " = " + sumA + "\n"
                            + "  " + playerB + "'s Würfe: " + b1 + " + " + b2 + " = " + sumB + "\n"
                            + "  Ergebnis: " + resultFromA);
        }

        // Nachricht an Spieler B (Eingeladener)
        if (clients.containsKey(playerB)) {
            clients.get(playerB).out.println(
                    "[Würfel] === Ergebnis ===\n"
                            + "  Deine Würfe: " + b1 + " + " + b2 + " = " + sumB + "\n"
                            + "  " + playerA + "'s Würfe: " + a1 + " + " + a2 + " = " + sumA + "\n"
                            + "  Ergebnis: " + resultFromB);
        }
    }
}
