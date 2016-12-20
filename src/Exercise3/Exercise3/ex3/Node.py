
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
    candidates_num_appox = 0

    # NodeInfo
    info = None
    # Level int
    level = -1
    # TODO: Wat doet dit? naar father ik zie alleen een set.
    owner_id = None
    # List of IDs (int)
    untraversed = None
    # NodeInfo
    father = None
    # NodeInfo
    potential_father = None
    candidate = False
    killed = False

    def __init__(self, info: NodeInfo, nodeinfos: dict):
        self.info = info
        self.nodeinfos = nodeinfos

        # nodeinfos is a dict so for ... in returns ids
        self.untraversed = []
        for id in self.nodeinfos:
            if id == self.info.id:
                continue
            self.untraversed.append(id)



    def run(self,exitevent):
        self.logger.debug("Run started with {0} untraversed nodes.".format(len(self.untraversed)))
        # TODO sort messages from ordinary to candidate. and start candidate
        random_int = struct.unpack('I',os.urandom(4))[0]
        maxint = (2**32)-1
        random_double = float(random_int)/(maxint) # random between 0 and 1
        self.logger.debug("Random int was {0} max({1}). Random double was {2}.".format(random_int, maxint, random_double))
        # Node #1 is always candidate
        if random_double < self.candidates_num_appox/len(self.nodeinfos) or self.info.id==1:
            self.logger.info("Became candidate.")
            self.candidate = True
            self.level += 1
            for ni in self.nodeinfos:
                if ni == self.info.id:
                    continue
                self.send(Message(self.info, self.nodeinfos[ni], self.level, self.info.id))

        while not exitevent.is_set():
            try:
                message = self.q_rx.get(block=True, timeout=1) # waits for message
            except Empty:
                pass
            else:
                if self.candidate:
                    self.logger.debug("Handled packet as candidate")
                    is_elected = self.handle_candidate(message)
                    if is_elected:
                        exitevent.set()
                else:
                    self.logger.debug("Handled packet as ordinary")
                    self.handle_ordinary(message)
        self.logger.debug("Run ended")

    def handle_ordinary(self, message: Message):
        if message.level == self.level and message.id == self.info.id:
            self.father = self.potential_father

            #sent a reply
            self.send(Message(self.info, self.father, message.level, message.id))
            #TODO volgens mij was dit kill naar oude vader.
            self.logger.debug("Sent message to father (such potential) {0}".format(self.father.id))
            return

        if message.level > self.level or (message.level == self.level and message.id > self.info.id):
            self.potential_father = message.src
            self.level = message.level
            self.owner_id = message.id
            if self.father == None:
                self.father = self.potential_father

            #sent a reply
            self.send(Message(self.info, self.father, message.level, message.id))
            self.logger.debug("Sent message to father (found one) {0}".format(self.father.id))
            return

        self.logger.debug("Did nothing as ordinary")

    def handle_candidate(self, message: Message):
        if message.id == self.info.id and not self.killed:
            self.level += 1
            link_id = self.untraversed.pop()
            self.logger.info("Removed link to {0} from untraversed links {1} remaining.".format(link_id,len(self.untraversed)))
            if len(self.untraversed) == 0:
                self.logger.info("Elected?")
                return True

            return False
        elif not (message.level < self.level or (message.level == self.level and message.id < self.info.id)):
            self.killed = True
            self.send(Message(self.info, message.src, message.level, message.id))
            self.logger.debug("Sent message to to source")
            return False
        self.logger.debug("Did nothing as candidate")
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
        # Setup connections, remeber nodeinfos is a dict so for ... in return keys
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
                self.server_socket.send_string('') # bogus reply. Maybe flag(s) NOBLOCK or SNDMORE could work. Infinite multipart.
                if type(message) is Message:
                    self.q_rx.put(message)
                    self.logger.debug("[SERVER] got packet from {0}.".format(message.src.id))
                else:
                    self.logger.warning("[SERVER] got bad packet.")
                #TODO for testing only.
                time.sleep(0.050)
        else:
            self.logger.warning("[SERVER] can not start thread.")

    @threaded
    def client_thread(self):
        while True:
            message = self.q_tx.get(block=True) # waits for message
            if message.dst.id in self.sockets:
                client_socket = self.sockets.get(message.dst.id)
                client_socket.send_pyobj(message, flags=0)
                self.logger.debug("[CLIENT] sent packet to {0}.".format(message.dst.id))
                ack = client_socket.recv_string() # Recieve bullshit ACK
            #TODO for testing only.
            time.sleep(0.050)





