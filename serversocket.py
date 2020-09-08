
import socket
import sys

## SERVER
HOST = '127.0.0.1'
PORT = 1337

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.bind((HOST, PORT))
s.listen()
conn, addr = s.accept()

proxy = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
proxy.connect(('slarsson.me', 80))

while True:
    data = conn.recv(1024)
    print(data)
    if not data:
        print('not data wtf?', data)
        break

    proxy.sendall(b'GET /index.html HTTP/1.1 \r\n')
    proxy.sendall(b'Host: slarsson.me \r\n')
    proxy.sendall(b'\r\n')
    proxy.sendall(b'hejsan svejsan..')
    proxy.sendall(b'\r\n')
    
    while True:
        data = proxy.recv(4096)
        conn.sendall(data)
    #print(sys.getsizeof(data), data)

s.close() 
