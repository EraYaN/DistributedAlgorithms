import zmq

import threading as thr

import queue

import time

q = queue.Queue()
port = 32516

def server():
    context = zmq.Context()
    socket = context.socket(zmq.REP)
    socket.bind("tcp://*:{0}".format(port))

    while True:
        #  Wait for next request from client
        message = socket.recv()
        print("Received request: {0}".format(message));
        time.sleep(1)  
        socket.send_string("World from {0}".format(port))
    
def client():
    context = zmq.Context()
    print("Connecting to server...")
    socket = context.socket(zmq.REQ)
    socket.connect ("tcp://localhost:{0}".format(port))
    
    for request in range (1,10):
        print("Sending request ", request,"...");
        socket.send_string("Hello")
        #  Get the reply.
        message = socket.recv()
        print("Received reply ", request, "[", message, "]")


if __name__ == "__main__":
    server_thread = thr.Thread(target=server)
    client_thread = thr.Thread(target=client)

    server_thread.daemon = True

    server_thread.start()
    client_thread.start()

    #server_thread.join()
    client_thread.join()

    print("Done!");