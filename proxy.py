import socket
import threading
import select
import queue
from parse import check_input, parse, get_first_row, replace_url

HOST = '127.0.0.1'
PORT = 1337

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
            data = connection.recv(4096) ## right size??
            if not data:
                print(f'CLOSE CONNECTION, THREAD {id}')
                connection.close()
                inputs.remove(connection)
                break

            if connection is client_socket:

                # check if message starts with 'GET url HTTP/1.1' 
                # in order to replace smiley-img
                first_row = get_first_row(data)
                if first_row:
                    data = replace_url(first_row[1], data)
                        
                # handle data from client
                if first:
                    first = False

                    # check if valid request
                    destination = check_input(first_row)
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
                    if len(inputs) == 2:
                        outgoing_messages[inputs[1]].put(data)
                        if inputs[1] not in outputs:
                            outputs.append(inputs[1])
                    else: 
                        print(f'CLOSED CONNECTION, SERVER SOCKET NOT AVAILABLE')
                        connection.close()
                        inputs.remove(connection)
                        break
            else:
                # handle data from server
                buffer += data
                new_data = parse(buffer)
                if not new_data:
                    continue

                # add the changed (or not changed) data from the server to client queue
                outgoing_messages[client_socket].put(new_data)  
                if client_socket not in outputs:
                    outputs.append(client_socket)
                #client_socket.sendall(new_data)

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

# set-up
if __name__ == '__main__':    
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind((HOST, PORT))
    server.listen()

    t_count = 0
    while True:
        conn, addr = server.accept()
        print('NEW CONN:', addr)
        new_thread = threading.Thread(target=handler, args=(conn, t_count))
        new_thread.start()
        t_count += 1
