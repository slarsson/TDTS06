 
import socket
import threading
import urllib.parse

import select

HOST = '127.0.0.1'
PORT = 1337

VALIDDATA = ['text/plain', 'text/html', 'text/plain; charset=UTF-8']


def parse(input):
    headers, body = input.split('\r\n\r\n')

    headers = headers.split('\r\n')

    print(headers)

    test = {}
    for h in headers[1:]:
        x = h.split(':', 1)
        print(x)
        
        test[x[0].lower()] = x[1].strip()
    
    print(test['content-length'], len(body))
    print(test['content-type'])

    ## Edit 
    if test['content-type'] in VALIDDATA:
        print('EDIT')
        input = input.replace('Stockholm', 'jocke')
        print(input)
        return input
    else:
        return input


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
        return (host.hostname, host.port)
    except Exception as e:
        print(e, host, items)
    return False
    

def handler(client_socket, addr):
    inputs = [client_socket] 
    
    first = True
    while inputs:        
        readable, _, _ = select.select(inputs, [], [])

        for connection in readable:
            data = connection.recv(2000)
            if not data:
                connection.close()
                inputs.remove(connection)
                print('ANY OF THE CONNECTIONS CLOSED')
                for x in inputs:
                    x.shutdown(socket.SHUT_RDWR) ## Needed??
                    x.close()
                    inputs.remove(x)
                break
            
            ## Handle the connection from client
            if connection is client_socket:
                if first:
                    first = False
                    text = data.decode('latin1') # https://stackoverflow.com/a/27357138
                    headers = text.split('\n')
                    
                    destination = check_input(headers[0])
                    print(destination)
                    if not destination:
                        client_socket.send(b"HTTP/1.1 400 Bad Request\r\n")
                        client_socket.send(b"Content-Length: 11\r\n")
                        client_socket.send(b'Connection: close\r\n')
                        client_socket.send(b"\r\n")
                        client_socket.send(b"Error\r\n")
                        client_socket.send(b"\r\n\r\n")
                        continue    

                    ## Creates socket to server
                    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)    
                    server_socket.connect((destination[0], 80 if destination[1] is None else destination[1]))
                    
                    inputs.append(server_socket)
                    server_socket.sendall(data) 
                    
                ## If connection reused by client_socket
                else:
                    inputs[1].sendall(data)
            else:

                ## Check and parse the data
                new_data = parse(data.decode('latin1'))

                ## Edit data


                ## Send data
                client_socket.sendall(new_data.encode('latin1'))
               


if __name__ == '__main__':
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind((HOST, PORT))
    server.listen()

    while True:
        conn, addr = server.accept()
        new_thread = threading.Thread(target=handler, args=(conn, addr))
        new_thread.start()
