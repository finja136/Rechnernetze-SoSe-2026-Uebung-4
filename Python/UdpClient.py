import socket
import threading
import sys

# =========================================================
# PEER STORAGE
# =========================================================

peers = {}


class Peer:
    def __init__(self, ip, port):
        self.ip = ip
        self.port = port


# =========================================================
# RECEIVE THREAD
# =========================================================

def receiver(sock):
    while True:
        try:
            data, addr = sock.recvfrom(4096)
            msg = data.decode("utf-8")

            print(f"\nEmpfangen von {addr[0]}:{addr[1]}")
            print(msg)
            print("> ", end="", flush=True)

        except:
            break


# =========================================================
# MAIN
# =========================================================

def main():

    if len(sys.argv) != 3:
        print("Usage: python udp_client.py <name> <port>")
        return

    own_name = sys.argv[1]
    own_port = int(sys.argv[2])

    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.bind(("0.0.0.0", own_port))

    print("UDP gestartet:")
    print("Name:", own_name)
    print("Port:", own_port)

    threading.Thread(target=receiver, args=(sock,), daemon=True).start()

    while True:

        print("> ", end="")
        input_line = input().strip()

        # =====================================================
        # REGISTER
        # =====================================================

        if input_line.startswith("register "):

            parts = input_line.split()

            if len(parts) != 4:
                print("Usage: register <name> <ip> <port>")
                continue

            name = parts[1]
            ip = parts[2]
            port = int(parts[3])

            peers[name] = Peer(ip, port)

            print(f"Registriert: {name}")

        # =====================================================
        # SEND
        # =====================================================

        elif input_line.startswith("send "):

            parts = input_line.split(" ", 2)

            if len(parts) != 3:
                print("Usage: send <name> <message>")
                continue

            target = parts[1]
            message = parts[2]

            if target not in peers:
                print("Unbekannter Kontakt.")
                continue

            peer = peers[target]

            final_msg = f"{own_name}: {message}".encode("utf-8")

            sock.sendto(final_msg, (peer.ip, peer.port))

        # =====================================================
        # EXIT
        # =====================================================

        elif input_line == "exit":

            sock.close()
            break

        # =====================================================
        # HELP
        # =====================================================

        else:
            print("Befehle:")
            print("register <name> <ip> <port>")
            print("send <name> <message>")
            print("exit")


if __name__ == "__main__":
    main()