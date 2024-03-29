# Simple request-reply broker
#
# Author: Lev Givon <lev(at)columbia(dot)edu>

import zmq
import logging
import multiprocessing_logging
import signal
import os
from multiprocessing import Event

def router(frontend_port: int, backend_port: int, routerexit: Event):
    multiprocessing_logging.install_mp_handler()
    signal.signal(signal.SIGINT, signal.SIG_IGN)
    logger = logging.getLogger('router')
    logger.info("PID: {0}".format(os.getpid()))
    # Prepare our context and sockets
    context = zmq.Context()
    frontend = context.socket(zmq.ROUTER)
    backend = context.socket(zmq.DEALER)
    frontend.identity=b'\xFE\xFE\xFE\xFE'
    backend.identity=b'\xEF\xEF\xEF\xEF'
    frontend.bind("tcp://*:{0}".format(frontend_port))
    backend.bind("tcp://*:{0}".format(backend_port))

    # Initialize poll set
    poller = zmq.Poller()
    poller.register(frontend, zmq.POLLIN)
    poller.register(backend, zmq.POLLIN)

    logger.info("Router Started")

    # Switch messages between sockets
    while not routerexit.is_set():
        socks = dict(poller.poll(0.1))

        if socks.get(frontend) == zmq.POLLIN:
            message = frontend.recv_multipart()
            backend.send_multipart(message)
            logger.log(5,"Sent message to backend.")

        if socks.get(backend) == zmq.POLLIN:
            message = backend.recv_multipart()
            frontend.send_multipart(message)
            logger.log(5,"Sent message to frontend.")
    #zmq.proxy(frontend, backend)

    logger.info("Router Quit")
