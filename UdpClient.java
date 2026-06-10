import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class UdpClient {

    // name -> "ip:port"
    private static final Map<String, String[]> knownClients =
            new ConcurrentHashMap<>();

    public static void main(String[] args) throws Exception {

        if (args.length != 2) {
            System.out.println("Usage: java UdpClient <name> <port>");
            return;
        }

        String ownName = args[0];
        int ownPort    = Integer.parseInt(args[1]);

        DatagramSocket socket = new DatagramSocket(ownPort);

        System.out.println("UDP gestartet:");
        System.out.println("Name: " + ownName);
        System.out.println("Port: " + ownPort);

        // =====================================================
        // RECEIVER THREAD
        // =====================================================

        Thread receiver = new Thread(() -> {
            byte[] buffer = new byte[4096];
            while (true) {
                try {
                    DatagramPacket packet =
                            new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String message = new String(
                            packet.getData(), 0, packet.getLength(), "UTF-8");

                    System.out.println(
                            "\nEmpfangen von "
                                    + packet.getAddress().getHostAddress()
                                    + ":" + packet.getPort());
                    System.out.println(message);
                    System.out.print("> ");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        receiver.setDaemon(true);
        receiver.start();

        BufferedReader keyboard =
                new BufferedReader(new InputStreamReader(System.in));

        // =====================================================
        // INPUT LOOP
        // =====================================================

        while (true) {

            System.out.print("> ");
            String input = keyboard.readLine();
            if (input == null) continue;

            // =================================================
            // AUFGABE 3a: register <Name> <IP> <Port>
            // =================================================

            if (input.startsWith("register ")) {

                String[] parts = input.split(" ", 4);

                if (parts.length != 4) {
                    System.out.println("Usage: register <name> <ip> <port>");
                    continue;
                }

                String regName = parts[1];
                String regIp   = parts[2];
                String regPort = parts[3];

                knownClients.put(regName, new String[]{regIp, regPort});
                System.out.println("Registriert: " + regName
                        + " -> " + regIp + ":" + regPort);
            }

            // =================================================
            // AUFGABE 3b: clientlist
            // =================================================

            else if (input.equals("clientlist")) {

                if (knownClients.isEmpty()) {
                    System.out.println("Keine Clients gespeichert.");
                } else {
                    System.out.println("Gespeicherte Clients:");
                    knownClients.forEach((n, addr) ->
                            System.out.println("  " + n
                                    + " -> " + addr[0] + ":" + addr[1]));
                }
            }

            // =================================================
            // AUFGABE 3c: sendall <Nachricht>
            // =================================================

            else if (input.startsWith("sendall ")) {

                String message = input.substring(8);
                String finalMessage = ownName + ": " + message;
                byte[] data = finalMessage.getBytes("UTF-8");

                if (knownClients.isEmpty()) {
                    System.out.println("Keine Clients registriert.");
                    continue;
                }

                for (Map.Entry<String, String[]> entry : knownClients.entrySet()) {
                    try {
                        String ip   = entry.getValue()[0];
                        int    port = Integer.parseInt(entry.getValue()[1]);

                        DatagramPacket packet = new DatagramPacket(
                                data, data.length,
                                InetAddress.getByName(ip), port);
                        socket.send(packet);
                        System.out.println("Gesendet an " + entry.getKey());

                    } catch (Exception e) {
                        System.out.println("Fehler bei " + entry.getKey()
                                + ": " + e.getMessage());
                    }
                }
            }

            // =================================================
            // send <ip> <port> <Nachricht>  (wie bisher)
            // =================================================

            else if (input.startsWith("send ")) {

                String[] parts = input.split(" ", 4);

                if (parts.length != 4) {
                    System.out.println("Usage: send <ip> <port> <message>");
                    continue;
                }

                String ip      = parts[1];
                int    port    = Integer.parseInt(parts[2]);
                String message = parts[3];

                String finalMessage = ownName + ": " + message;
                byte[] data = finalMessage.getBytes("UTF-8");

                DatagramPacket packet = new DatagramPacket(
                        data, data.length,
                        InetAddress.getByName(ip), port);
                socket.send(packet);
            }

            // =================================================
            // exit
            // =================================================

            else if (input.equals("exit")) {
                socket.close();
                System.exit(0);
            }

            // =================================================
            // help
            // =================================================

            else {
                System.out.println("Befehle:");
                System.out.println("  register <name> <ip> <port>  – Client speichern");
                System.out.println("  clientlist                   – alle Clients anzeigen");
                System.out.println("  sendall <message>            – an alle senden");
                System.out.println("  send <ip> <port> <message>   – direkt senden");
                System.out.println("  exit");
            }
        }
    }
}
