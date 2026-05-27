import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpClient {

    public static void main(String[] args) throws Exception {

        if (args.length != 2) {

            System.out.println(
                    "Usage: java UdpClient <name> <port>"
            );

            return;
        }

        String ownName = args[0];
        int ownPort = Integer.parseInt(args[1]);

        DatagramSocket socket =
                new DatagramSocket(ownPort);

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
                            new DatagramPacket(
                                    buffer,
                                    buffer.length
                            );

                    socket.receive(packet);

                    String message = new String(
                            packet.getData(),
                            0,
                            packet.getLength(),
                            "UTF-8"
                    );

                    System.out.println(
                            "\nEmpfangen von "
                                    + packet.getAddress()
                                    .getHostAddress()
                                    + ":"
                                    + packet.getPort()
                    );

                    System.out.println(message);

                    System.out.print("> ");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        receiver.start();

        BufferedReader keyboard =
                new BufferedReader(
                        new InputStreamReader(System.in)
                );

        // =====================================================
        // INPUT LOOP
        // =====================================================

        while (true) {

            System.out.print("> ");

            String input = keyboard.readLine();

            // =================================================
            // SEND
            // =================================================

            if (input.startsWith("send ")) {

                String[] parts =
                        input.split(" ", 4);

                if (parts.length != 4) {

                    System.out.println(
                            "Usage: send <ip> <port> <message>"
                    );

                    continue;
                }

                String ip = parts[1];

                int port =
                        Integer.parseInt(parts[2]);

                String message = parts[3];

                String finalMessage =
                        ownName + ": " + message;

                byte[] data =
                        finalMessage.getBytes("UTF-8");

                DatagramPacket packet =
                        new DatagramPacket(
                                data,
                                data.length,
                                InetAddress.getByName(ip),
                                port
                        );

                socket.send(packet);
            }

            // =================================================
            // EXIT
            // =================================================

            else if (input.equals("exit")) {

                socket.close();

                System.exit(0);
            }

            // =================================================
            // HELP
            // =================================================

            else {

                System.out.println("Befehle:");
                System.out.println(
                        "send <ip> <port> <message>"
                );
                System.out.println("exit");
            }
        }
    }
}