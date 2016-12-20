
import threading
def threaded(fn):
    def wrapper(*args, **kwargs):
        t = threading.Thread(target=fn, args=args, kwargs=kwargs)
        t.setDaemon(True)
        t.start()
    return wrapper

import time
import logging
import zmq
import os
import struct
import random

from ex3 import NodeInfo
from ex3 import Message
from queue import Queue, Empty

class Node(object):
    """Describes a single node"""

    # Messaging system variables
    context = zmq.Context()
    q_tx = Queue()
    q_rx = Queue()
    nodeinfos = {}
    sockets = {}
    server_socket = None

    # Debug facilities
    logger = None

    # Number extra candidates (approx)
    candidates_num_appox = 5

    # NodeInfo
    info = None
    # Level int
    level = 0
    owner_id = 0
    # List of IDs (int)
    untraversed = None
    # NodeInfo
    father = None
    # NodeInfo
    potential_father = None
    candidate = False
    killed = False
    elected = False

    def __init__(self, info: NodeInfo, nodeinfos: dict):
        self.info = info
        self.nodeinfos = nodeinfos
        
        self.untraversed = [id for id in self.nodeinfos if id != self.info.id]
        random.shuffle(self.untraversed)

    def run(self,exitevent):
        # TODO sort messages from ordinary to candidate.  and start candidate
        random_int = struct.unpack('I',os.urandom(4))[0]
        maxint = (2 ** 32) - 1
        random_double = float(random_int) / (maxint) # random between 0 and 1
        # Node #1 is always candidate
        if random_double < self.candidates_num_appox / len(self.nodeinfos) or self.info.id == 1:
            self.logger.info("Became candidate, going to attempt to capture {0} nodes.".format(len(self.untraversed)))
            self.candidate = True
            capture_new_link = True

        while not exitevent.is_set():
            # Attempt to capture next link if required.
            if self.candidate and capture_new_link:
                new_link = self.untraversed[-1]
                self.send(Message(self.info, self.nodeinfos[new_link], self.level, self.info.id))
                self.logger.debug("Sent capture attempt to {}.".format(new_link))

            try:
                message = self.q_rx.get(block=True, timeout=1) # waits for message
            except Empty:
                pass
            else:
                if self.candidate:
                    # self.logger.debug("Handled packet as candidate.")
                    capture_new_link = self.handle_candidate(message)

                    # Links exhausted, we are elected
                    if len(self.untraversed) == 0:
                        self.elected = True
                        self.logger.info("Elected!")
                        exitevent.set()
                else:
                    # self.logger.debug("Handled packet as ordinary.")
                    self.handle_ordinary(message)
    
        if self.elected:
            self.logger.debug("Run ended. Became elected.")
        else:
            self.logger.debug("Run ended.")

    def handle_ordinary(self, message: Message):
        if message.level == self.level and message.id == self.owner_id:
            self.logger.debug("Received acknowledgement of kill attempt on behalf of {} from old father {}, changing father...".format(self.owner_id, self.father.id))
            self.father = self.potential_father

            self.send(Message(self.info, self.father, message.level, message.id))
            self.logger.debug("Sent acknowledgement to new father {0}.".format(self.father.id))
            return

        self.logger.debug("Received capture attempt from {}.".format(message.src.id))

        if message.level > self.level or (message.level == self.level and message.id > self.owner_id):
            self.potential_father = message.src
            self.level = message.level
            self.owner_id = message.id

            if self.father == None:
                self.father = self.potential_father
                self.logger.debug("Sent acknowledgement to new father {0}.".format(self.father.id))
            else:
                self.logger.debug("Sent message attempting to kill old father {0} on behalf of {1}.".format(self.father.id, self.owner_id))

            self.send(Message(self.info, self.father, message.level, message.id))
            return

        self.logger.debug("Ignoring capture attempt from {0}, since ({1},{2}) < ({3},{4}).".format(message.src.id, message.level, message.id, self.level, self.owner_id))

    def handle_candidate(self, message: Message):
        if message.id == self.info.id:
            if self.killed:
                self.logger.debug("Ignoring acknowledgement for capture attempt from {}, since I was killed in the meantime.".format(message.src.id))
                return False
            else:
                self.level += 1
                link_id = self.untraversed.pop()
                self.logger.info("Received acknowledgement for capture attempt, captured {0}, {1} remaining.".format(link_id, len(self.untraversed)))    
                return True

        elif message.level < self.level or (message.level == self.level and message.id < self.info.id):
            self.logger.debug("Ignoring kill attempt from {0} on behalf of {1}, since ({2},{1}) < ({3},{4}).".format(message.src.id, message.id, message.level, self.level, self.info.id))
            return False

        else:
            self.killed = True
            self.send(Message(self.info, message.src, message.level, message.id))
            self.logger.debug("Killed by {0} on behalf of {1}, since ({2},{1}) > ({3},{4}), sent acknowledgement.".format(message.src.id, message.id, message.level, self.level, self.info.id))
            return False

    def send(self, message):
        self.q_tx.put(message)

    def setup_logging(self):
        self.logger = logging.getLogger('sub')

    def start_server(self):
        # Setup listener:
        self.server_socket = self.context.socket(zmq.REP)
        self.server_socket.bind(self.info.bind_uri)
        # Run Async
        self.server_thread()

    def connect_clients(self):
        # Setup connections, remeber nodeinfos is a dict so for ...  in return
        # keys
        for remote_id in self.nodeinfos:
            if remote_id == self.info.id:
                # skip self
                continue
            remote_info = self.nodeinfos.get(remote_id)
            client_socket = self.context.socket(zmq.REQ)
            client_socket.connect(remote_info.connect_uri)
            self.sockets[remote_info.id] = client_socket
        #Run Async
        self.client_thread()

    @threaded
    def server_thread(self):
        if self.server_socket is not None:
            while True:
                message = self.server_socket.recv_pyobj(flags=0)
                self.server_socket.send_string('') # Send protocol ACK so execution can continue
                if type(message) is Message:
                    self.q_rx.put(message)
                    # self.logger.debug("[SERVER] got packet from {0}.".format(message.src.id))
                else:
                    self.logger.warning("[SERVER] got bad packet.")
        else:
            self.logger.warning("[SERVER] can not start thread.")

    @threaded
    def client_thread(self):
        while True:
            message = self.q_tx.get(block=True) # waits for message
            if message.dst.id in self.sockets:
                client_socket = self.sockets.get(message.dst.id)
                client_socket.send_pyobj(message, flags=0)
                # self.logger.debug("[CLIENT] sent packet to {0}.".format(message.dst.id))
                ack = client_socket.recv_string() # Recieve and discard protocol ACK
