import socket
import threading

# =========================================================
# CLIENT HANDLER
# =========================================================

class ClientHandler:

    def __init__(self, name, conn):
        self.name = name
        self.conn = conn


# =========================================================
# CLIENT MAP
# =========================================================

clients = {}


# =========================================================
# CLIENT THREAD
# =========================================================

def handle_client(conn):

    name = None

    try:

        # =====================================================
        # REGISTRIERUNG
        # =====================================================

        while True:

            conn.sendall(b"register <name>\n")

            reg = conn.recv(1024).decode().strip()

            if not reg:
                conn.close()
                return

            parts = reg.split(" ")

            # ===== FORMAT PRUEFEN =====

            if len(parts) != 2 or parts[0].lower() != "register":

                conn.sendall(
                    b"Erwartet: register <name>\n"
                )

                continue

            name = parts[1]

            # ===== NAME SCHON VERGEBEN =====

            if name in clients:

                conn.sendall(
                    b"Name bereits vergeben!\n"
                )

                continue

            break

        # =====================================================
        # CLIENT REGISTRIEREN
        # =====================================================

        clients[name] = ClientHandler(name, conn)

        print(f"{name} connected")

        # =====================================================
        # MESSAGE LOOP
        # =====================================================

        while True:

            data = conn.recv(1024)

            if not data:
                break

            msg = data.decode().strip()

            print(f"{name}: {msg}")

            # =================================================
            # SEND
            # =================================================

            if msg.startswith("send "):

                parts = msg.split(" ", 2)

                if len(parts) == 3:

                    target = parts[1]
                    text = parts[2]

                    if target in clients:

                        clients[target].conn.sendall(
                            f"{name}: {text}\n".encode()
                        )

                continue

    except Exception as e:

        print(f"{name} disconnected")

    finally:

        # =====================================================
        # DISCONNECT
        # =====================================================

        if name is not None:

            clients.pop(name, None)

            print(f"Removed: {name}")

        conn.close()


# =========================================================
# TCP SERVER
# =========================================================

def tcp_server(port=5000):

    server = socket.socket(
        socket.AF_INET,
        socket.SOCK_STREAM
    )

    server.bind(("0.0.0.0", port))

    server.listen()

    print(f"TCP Server läuft auf Port {port}")

    while True:

        conn, addr = server.accept()

        threading.Thread(
            target=handle_client,
            args=(conn,)
        ).start()


# =========================================================
# MAIN
# =========================================================

if __name__ == "__main__":

    tcp_server()