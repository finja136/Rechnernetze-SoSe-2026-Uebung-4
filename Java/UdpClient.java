import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

public class UdpClient {

    // Gespeicherte Kontakte
    private static final Map<String, Peer> peers = new HashMap<>();

    private static class Peer {
        String ip;
        int port;

        Peer(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }
    }

    public static void main(String[] args) throws Exception {

        if (args.length != 2) {
            System.out.println("Usage: java UdpClient <name> <port>");
            return;
        }

        String ownName = args[0];
        int ownPort = Integer.parseInt(args[1]);

        DatagramSocket socket = new DatagramSocket(ownPort);

        System.out.println("UDP gestartet:");
        System.out.println("Name: " + ownName);
        System.out.println("Port: " + ownPort);

        // Empfangsthread
        Thread receiver = new Thread(() -> {

            byte[] buffer = new byte[4096];

            while (true) {

                try {

                    DatagramPacket packet =
                            new DatagramPacket(buffer, buffer.length);

                    socket.receive(packet);

                    String message = new String(
                            packet.getData(),
                            0,
                            packet.getLength(),
                            "UTF-8"
                    );

                    System.out.println("\nEmpfangen von "
                            + packet.getAddress().getHostAddress()
                            + ":" + packet.getPort());

                    System.out.println(message);

                    System.out.print("> ");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        receiver.start();

        BufferedReader keyboard =
                new BufferedReader(new InputStreamReader(System.in));

        while (true) {

            System.out.print("> ");

            String input = keyboard.readLine();

            // ===== REGISTER =====
            if (input.startsWith("register ")) {

                String[] parts = input.split(" ");

                if (parts.length != 4) {
                    System.out.println(
                            "Usage: register <name> <ip> <port>");
                    continue;
                }

                String name = parts[1];
                String ip = parts[2];
                int port = Integer.parseInt(parts[3]);

                peers.put(name, new Peer(ip, port));

                System.out.println("Registriert: " + name);
            }

            // ===== SEND =====
            else if (input.startsWith("send ")) {

                String[] parts = input.split(" ", 3);

                if (parts.length != 3) {
                    System.out.println(
                            "Usage: send <name> <message>");
                    continue;
                }

                String targetName = parts[1];
                String message = parts[2];

                Peer peer = peers.get(targetName);

                if (peer == null) {
                    System.out.println("Unbekannter Kontakt.");
                    continue;
                }

                String finalMessage =
                        ownName + ": " + message;

                byte[] data = finalMessage.getBytes("UTF-8");

                DatagramPacket packet =
                        new DatagramPacket(
                                data,
                                data.length,
                                InetAddress.getByName(peer.ip),
                                peer.port
                        );

                socket.send(packet);
            }

            // ===== EXIT =====
            else if (input.equals("exit")) {

                socket.close();
                System.exit(0);
            }

            else {
                System.out.println("Befehle:");
                System.out.println("register <name> <ip> <port>");
                System.out.println("send <name> <message>");
                System.out.println("exit");
            }
        }
    }
}