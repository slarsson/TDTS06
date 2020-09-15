 
import socket
import threading
import urllib.parse

import select
import queue

HOST = '127.0.0.1'
PORT = 1337

VALIDDATA = ['text/plain', 'text/html', 'text/plain; charset=UTF-8']

def parse(_input):
    headers, body = _input.split('\r\n\r\n')

    headers = headers.split('\r\n')

    print(headers)

    test = {}
    for h in headers[1:]:
        x = h.split(':', 1)
        print(x)
        
        test[x[0].lower()] = x[1].strip()
    
    if 'content-length' not in test:
        return _input

    print(test['content-length'], len(body))

    if int(test['content-length']) != len(body):
        print('WE DONT HAVE ALL DATA, WTF!?!??!!')
        return False

    print(test['content-type'])

    ## Edit 
    if test['content-type'] in VALIDDATA:
        print('EDIT')
        _input = _input.replace('samlarsson94@gmail.com', 'samlarsson94@gmail.net')
        print(_input)
        return _input
    else:
        return _input


def check_input(_input):
    
    items = _input.split(sep=None)
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
    
def error_response():
    return (
        b'HTTP/1.1 400 Bad Request\r\n'
        b"Content-Length: 27\r\n"
        b"\r\n"
        b"BAD REQUEST (OR BAD PROXY?)"
    )

def handler(client_socket, id):   
    outgoing_messages = {} # dict to store outgoing message queue
    outgoing_messages[client_socket] = queue.Queue()

    inputs = [client_socket] # read
    outputs = [] # write

    buffer = bytes()
    first = True
    while inputs:        
        readable, writable, _ = select.select(inputs, outputs, [])
        
        # handle incoming data (recv)
        for connection in readable:
            data = connection.recv(1024)
            if not data:
                print(f'CLOSE CONNECTION, THREAD {id}')
                connection.close()
                inputs.remove(connection)
                break

            if connection is client_socket:
                # handle data from client
                if first:
                    first = False
                    
                    ## TODO: check HTTP/1.0 ??
                    text = data.decode('latin1') # https://stackoverflow.com/a/27357138
                    headers = text.split('\n')
                    destination = check_input(headers[0])
                    ## TODO: check this..
                    if not destination:
                        connection.sendall(error_response())
                        continue
                    
                    # init connection to server
                    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)    
                    server_socket.connect((destination[0], 80 if destination[1] is None else destination[1]))

                    # add server to available inputs (recive data)
                    inputs.append(server_socket)

                    # create message queue + add to available outputs (send data)
                    outgoing_messages[server_socket] = queue.Queue()
                    outgoing_messages[server_socket].put(data)
                    if server_socket not in outputs:
                        outputs.append(server_socket)
                else:
                    # add data from client to server queue
                    outgoing_messages[inputs[1]].put(data)
                    if inputs[1] not in outputs:
                        outputs.append(inputs[1])
            else:
                # handle data from server
                buffer += data
                new_data = parse(buffer.decode('latin1'))
                if not new_data:
                    continue

                # add the changed (or not changed) data from the server to client queue
                outgoing_messages[client_socket].put(new_data.encode('latin1'))
                if client_socket not in outputs:
                    outputs.append(client_socket)
                
                # clear buffer
                buffer = bytes()

        # handle outgoing data
        for connection in writable:
            try:
                msg = outgoing_messages[connection].get_nowait()
            except queue.Empty:
                outputs.remove(connection)
            else:
                try:
                    connection.sendall(msg)
                except:
                    pass
    print(f'KILL THREAD {id} 🧵')

if __name__ == '__main__':
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind((HOST, PORT))
    server.listen()

    t_count = 0
    while True:
        conn, _ = server.accept()
        new_thread = threading.Thread(target=handler, args=(conn, t_count))
        new_thread.start()
        t_count += 1
