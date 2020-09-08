## CLIENT
# HOST = 'slarsson.me'
# PORT = 80

# s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
# s.connect((HOST, PORT))
# print(s.getsockname(), s.getpeername())
# print('connect ok?')
# # s.send(b'GET /index.html \r\n')

# s.sendall(b'GET /index.html HTTP/1.1')
# s.sendall(b'Host: slarsson.me')
# s.sendall(b'\r\n')
# s.sendall(b'hejsan svejsan..')
# s.sendall(b'\r\n')

# #s.send(b'GET /index.html HTTP/1.1 \r\n Host: slarsson.me \r\n')
# #s.send(b'\r\n')


# data = s.recv(1024)

# print('test:', data)
# # print('Received', repr(data))

# s.close()  