
#import socket
#import sys

# ## SERVER
# HOST = '127.0.0.1'
# PORT = 1337

# s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
# s.bind((HOST, PORT))
# s.listen()
# conn, addr = s.accept()

# proxy = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
# proxy.connect(('slarsson.me', 80))

# while True:
#     data = conn.recv(1024)
#     print(data)
#     if not data:
#         print('not data wtf?', data)
#         break

#     proxy.sendall(b'GET /index.html HTTP/1.1 \r\n')
#     proxy.sendall(b'Host: slarsson.me \r\n')
#     proxy.sendall(b'\r\n')
#     proxy.sendall(b'hejsan svejsan..')
#     proxy.sendall(b'\r\n')
    
#     while True:
#         data = proxy.recv(4096)
#         conn.sendall(data)
#     #print(sys.getsizeof(data), data)

# s.close() 

import socket
import threading
import urllib.parse

HOST = '127.0.0.1'
PORT = 1337

VALIDDATA = ['text/plain', 'text/html']


def parse(input):
    test = input.split('\n')

    ## Split header from body


    ## Check headers if validdata
    

    # rows = input.split('\n')
    # for item in rows[1:]:
    #     print(item)
    



def check_input(input):
    
    items = input.split(sep=None)
    if len(items) != 3:
        return False

    #Check request is GET and HTTP
    if items[0] != 'GET' or not (items[2] == 'HTTP/1.1' or items[2] == 'HTTP/1.0'):
        return False

    #Parse host
    try:
        host = urllib.parse.urlparse(items[1])
        return host.hostname, host.port
    except Exception as e:
        print(e, host, items)
    return False
    

def handler(conn, addr):
    while True:
        payload = conn.recv(8192) 
        if not payload:
            break
        
        text = payload.decode('latin1') # https://stackoverflow.com/a/27357138
        headers = text.split('\n')
        #print('INCOMING:', headers)
        host, port = check_input(headers[0])

    

        if not host:
            conn.send(b"HTTP/1.1 400 Bad Request\r\n")
            conn.send(b"Content-Length: 11\r\n")
            conn.send(b'Connection: close\r\n')
            conn.send(b"\r\n")
            conn.send(b"Error\r\n")
            conn.send(b"\r\n\r\n")      
        else:
            outgoing = socket.socket(socket.AF_INET, socket.SOCK_STREAM)    
            outgoing.connect((host, 80 if port is None else port))
            #print('CONNETED TO', host)
            outgoing.sendall(payload)
            
            
            while True:
                data = outgoing.recv(16384)
                if not data:
                    break
                #print(data)

                ## Check and parse the data
                parse(data.decode('latin1'))

                ## Edit data

                ## Send data
                conn.sendall(data)



            
    

s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
s.bind((HOST, PORT))
s.listen()

while True:
    connection, address = s.accept()
    new_thread = threading.Thread(target=handler, args=(connection, address))
    new_thread.start()    

