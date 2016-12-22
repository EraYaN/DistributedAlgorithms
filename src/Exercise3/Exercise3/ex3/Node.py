
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
import pickle
import copy

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
    server_socket = None
    client_socket = None
    routeruri = None # To send messages to
    dealeruri = None # To receive messages from

    # Debug facilities
    logger = None

    # Number extra candidates (approx)
    candidates_num_appox = 0

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

    # Timing (use time.perf_counter)
    start_time = None
    end_time = None

    #Stats
    stat_times_captured = 0
    stat_sent_cap = 0
    stat_recv_cap = 0
    stat_sent_kill = 0
    stat_recv_kill = 0
    stat_sent_ack = 0
    stat_recv_ack = 0

    stat_sent = 0
    stat_recv = 0

    # DEBUG
    quitting = False

    def __init__(self, info: NodeInfo, nodeinfos: dict, candidates_num_appox: int, routeruri: str, dealeruri: str):
        self.info = info
        self.nodeinfos = nodeinfos
        self.candidates_num_appox = candidates_num_appox
        self.routeruri = routeruri
        self.dealeruri = dealeruri

        self.untraversed = [id for id in self.nodeinfos if id != self.info.id]
        random.shuffle(self.untraversed)

    def run(self,exitevent):
        #random_int = struct.unpack('I',os.urandom(4))[0]
        #maxint = (2 ** 32) - 1
        #random_double = float(random_int) / (maxint) # random between 0 and 1
        # Node #1 is always candidate
        #if random_double < self.candidates_num_appox / len(self.nodeinfos) or self.info.id == 1:
        if self.info.id <= self.candidates_num_appox:
            self.logger.info("Became candidate, going to attempt to capture {0} nodes.".format(len(self.untraversed)))
            self.candidate = True
            capture_new_link = True
        self.start_time = time.perf_counter()

        while not self.quitting and not exitevent.is_set():
            self.random_delay()
            # Attempt to capture next link if required.
            if self.candidate and capture_new_link:
                new_link = self.untraversed[-1]
                self.send(Message(self.info, self.nodeinfos[new_link], self.level, self.info.id))
                self.logger.debug("Sent capture attempt to {}.".format(new_link))
                self.stat_sent_cap += 1
            try:
                #self.logger.debug("Waiting for package.")
                message = self.q_rx.get(block=True, timeout=0.05) # waits for message, timeout is to reduce CPU load.
                self.stat_recv += 1
                if message.done == True:
                    self.logger.debug("Got broadcast message from elected node.")
                    self.quitting=True
                    break;
            except Empty:
                if self.candidate:
                    capture_new_link = False
            else:
                self.random_delay()
                if self.candidate:
                    # self.logger.debug("Handled packet as candidate.")
                    capture_new_link = self.handle_candidate(message)

                    # Links exhausted, we are elected
                    if len(self.untraversed) == 0:
                        self.elected = True
                        self.logger.info("Elected!")
                        self.end_time = time.perf_counter()
                        self.quitting = True
                        time.sleep(0.5)
                        self.broadcast(Message(self.info,self.info,self.level,self.info.id,True))
                else:
                    #self.logger.debug("Handled packet as ordinary.")
                    self.handle_ordinary(message)

        if self.elected:
            elapsed_time = (self.end_time-self.start_time)
            self.logger.info("Run ended. Became elected in {0:.2f} ms.".format(elapsed_time*1000))
        else:
            self.logger.debug("Run ended.")

        if self.elected:
            print("##Header;id;level;times_captured;sent_ack;recv_ack;sent_kill;recv_kill;sent_cap;recv_cap;sent;recv;q_tx;q_rx")

        time.sleep(5)

        print("##Stats;{: 3};{: 3};{: 3};{: 3};{: 3};{: 3};{: 3};{: 3};{: 3};{: 3};{: 3};{: 3};{: 3}".format(
                    self.info.id,
                    self.level,
                    self.stat_times_captured,
                    self.stat_sent_ack,
                    self.stat_recv_ack,
                    self.stat_sent_kill,
                    self.stat_recv_kill,
                    self.stat_sent_cap,
                    self.stat_recv_cap,
                    self.stat_sent,
                    self.stat_recv,
                    len(list(self.q_tx.queue)),
                    len(list(self.q_rx.queue))
                    )
                )

    def random_delay(self):
        # Random delay between 0 and 0.1 seconds
        time.sleep(random.random()/10)

    def handle_ordinary(self, message: Message):
        if message.level == self.level and message.id == self.owner_id:
            self.logger.debug("Received acknowledgement of kill attempt on behalf of {} from old father {}, changing father...".format(self.owner_id, self.father.id))
            self.stat_recv_ack += 1
            self.father = self.potential_father

            self.send(Message(self.info, self.father, message.level, message.id))
            self.logger.debug("Sent acknowledgement to new father {0}.".format(self.father.id))
            self.stat_sent_ack += 1
            return

        self.logger.debug("Received capture attempt from {}.".format(message.src.id))
        self.stat_recv_cap += 1

        if message.level > self.level or (message.level == self.level and message.id > self.owner_id):
            self.potential_father = message.src
            self.level = message.level
            self.owner_id = message.id

            if self.father == None:
                self.father = self.potential_father
                self.logger.debug("Sent acknowledgement to new father {0}.".format(self.father.id))
                self.stat_sent_ack += 1
                self.stat_times_captured += 1
            else:
                self.logger.debug("Sent message attempting to kill old father {0} on behalf of {1}.".format(self.father.id, self.owner_id))
                self.stat_sent_kill += 1

            self.send(Message(self.info, self.father, message.level, message.id))
            return

        self.logger.debug("Ignoring capture attempt from {0}, since ({1},{2}) < ({3},{4}).".format(message.src.id, message.level, message.id, self.level, self.owner_id))

    def handle_candidate(self, message: Message):
        if message.id == self.info.id:
            if self.killed:
                self.logger.debug("Ignoring acknowledgement for capture attempt from {}, since I was killed in the meantime.".format(message.src.id))
                self.stat_recv_ack += 1
                return False
            else:
                self.level += 1
                link_id = self.untraversed.pop()
                self.logger.info("Received acknowledgement for capture attempt, captured node {0}, {1} remaining.".format(link_id, len(self.untraversed)))
                self.stat_recv_ack += 1
                return True

        elif message.level < self.level or (message.level == self.level and message.id < self.info.id):
            self.logger.debug("Ignoring kill attempt from {0} on behalf of {1}, since ({2},{1}) < ({3},{4}).".format(message.src.id, message.id, message.level, self.level, self.info.id))
            self.stat_recv_kill += 1
            return False

        else:
            self.killed = True
            self.send(Message(self.info, message.src, message.level, message.id))
            self.logger.debug("Killed by {0} on behalf of {1}, since ({2},{1}) > ({3},{4}), sent acknowledgement.".format(message.src.id, message.id, message.level, self.level, self.info.id))
            self.stat_sent_ack += 1
            self.stat_recv_kill += 1
            return False

    def send(self, message):
        self.q_tx.put(message)
        self.stat_sent += 1

    def broadcast(self, message):
        for id in self.nodeinfos:
            ni = self.nodeinfos.get(id)
            if id == self.info.id:
                continue
            tmp = copy.deepcopy(message)
            tmp.dst = ni
            self.q_tx.put(tmp)
            self.stat_sent += 1

    def setup_logging(self):
        self.logger = logging.getLogger('sub{}'.format(self.info.id))

    def start_server(self):
        # Setup listener:
        self.server_socket = self.context.socket(zmq.DEALER)
        self.server_socket.identity = struct.pack('i',self.info.id)
        self.server_socket.connect(self.routeruri)
        # Run Async
        self.server_thread()

    def connect_client(self):
        # Setup connection
        self.client_socket = self.context.socket(zmq.ROUTER)
        self.client_socket.identity = struct.pack('i',-self.info.id)
        self.client_socket.connect(self.dealeruri)
        # Run Async
        self.client_thread()

    @threaded
    def server_thread(self):
        if self.server_socket is not None:
            while True:
                envelope = self.server_socket.recv_multipart(flags=0)
                #print(envelope)
                found_null = False
                i = 0
                message = None
                for part in envelope:
                    if found_null:
                        message = pickle.loads(part)#self.server_socket.recv_pyobj(flags=0)
                        break
                    else:
                        if part == b'':
                            found_null = True # next is app data.
                    i += 1


                if type(message) is Message:
                    if self.info.id == message.dst.id:
                        self.q_rx.put(message)
                    else:
                        self.logger.error("[SERVER] Got packet for someone else.")
                    self.logger.log(5,"[SERVER] received packet ({}) from node {} for node {} ({}), data was at index {}.".format(message.rand,message.src.id,message.dst.id,'good' if self.info.id ==message.dst.id else 'bad',i))

                else:
                    self.logger.warning("[SERVER] got bad packet.")
        else:
            self.logger.warning("[SERVER] can not start thread.")

    @threaded
    def client_thread(self):
        if self.client_socket is not None:
            while True:
                message = self.q_tx.get(block=True) # waits for message                
                self.client_socket.send(b"\xEF\xEF\xEF\xEF", flags=zmq.SNDMORE) # Dealer IDentity
                self.client_socket.send(struct.pack('i',message.dst.id), flags=zmq.SNDMORE) # Destination
                self.client_socket.send(b"", flags=zmq.SNDMORE)
                self.client_socket.send_pyobj(message, flags=0)
                #self.client_socket.recv_string()
                self.logger.log(5,"[CLIENT] sent packet ({}) to node {}.".format(message.rand,message.dst.id))
        else:
            self.logger.warning("[CLIENT] can not start thread.")

