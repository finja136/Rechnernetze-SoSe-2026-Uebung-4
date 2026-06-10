import java.io.*;
import java.net.*;


public class TcpClient {

    public static void main(String[] args) {

        if (args.length != 2) {
            System.out.println("Usage: java TcpClient <serverIp> <serverPort>");
            return;
        }

        String serverIp   = args[0];
        int    serverPort = Integer.parseInt(args[1]);

        try {
            Socket socket = new Socket(serverIp, serverPort);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(
                    socket.getOutputStream(), true);
            BufferedReader keyboard = new BufferedReader(
                    new InputStreamReader(System.in));

            System.out.println("Verbunden mit " + serverIp + ":" + serverPort);
            System.out.println("Befehle: send <name> <msg> | sendall <msg> | clientlist |");
            System.out.println("         dice invite <name> | dice join | dice decline");

            // ===== RECEIVER THREAD =====
            new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        System.out.println(line);
                    }
                } catch (IOException e) {
                    System.out.println("Verbindung verloren.");
                }
            }).start();

            // ===== INPUT LOOP =====
            while (true) {
                String input = keyboard.readLine();
                if (input == null) continue;
                out.println(input);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
