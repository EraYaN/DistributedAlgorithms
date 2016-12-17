import zmq
import threading
import queue
import time
from message import Message


def threaded(fn):
    def wrapper(*args, **kwargs):
        threading.Thread(target=fn, args=args, kwargs=kwargs).start()
    return wrapper

class Node(object):
    """Describes a single node"""
    
    id = 0
    level = 0
    owner_id = 0
    untraversed = []
    father = None
    potential_father = None
    candidate = False
    killed = False

    def __init__(self, id):
        self.id = id
        # TODO: build untraversed list

    def handle_ordinary(self, message):
        if message.level == self.level and message.id == self.id:
            self.father = self.potential_father

            # TODO: send Message(self.id, message.level, message.id)

        if message.level > self.level or (message.level == self.level and message.id > self.id):
            self.potential_father = message.sender
            self.level = message.level
            self.owner_id = message.id
            if self.father == None:
                self.father = self.potential_father

            # TODO: send Message(self.id, message.level, message.id)

    def handle_candidate(self, message):
        if message.id == self.id and not self.killed:
            self.level += 1
            self.untraversed.pop()
        elif not (message.level < self.level or (message.level == self.level and message.id < self.id)):
            self.killed = True
            # TODO: send Message(self.id, message.level, message.id) 
        
    @threaded
    def server_thread(self):
        context = zmq.Context()
        socket = context.socket(zmq.ROUTER)
        socket.bind("tcp://*:{0}".format(port))

        while True:
            num_untraversed = len(self.untraversed)
            if self.candidate and num_untraversed > 0:
                tx_id = self.untraversed[num_untraversed - 1]
                socket.send_pyobj(Message(self.id, self.level, self.id))

            time.sleep(1)

    @threaded
    def client_thread(self):
        context = zmq.Context()
        print("Connecting to server...")
        socket = context.socket(zmq.DEALER)
        identity = 'node-{}'.format(self.id)
        socket.identity = identity.encode('ascii')
        socket.connect ("tcp://localhost:{0}".format(port))

        poll = zmq.Poller()
        poll.register(socket, zmq.POLLIN)

        while True:
            sockets = dict(poll.poll())
            if socket in sockets:
                message = socket.recv()
                self.handle_ordinary(message)
                self.handle_candidate(message)

            time.sleep(1)

    
        

