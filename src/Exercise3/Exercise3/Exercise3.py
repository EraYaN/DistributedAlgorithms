import multiprocessing as mp

import logging
import time

import mplog
import ex3

import atexit
import signal
import sys
import multiprocessing_logging

from router import router


from math import floor

multiprocessing_logging.install_mp_handler()

FORMAT = '%(asctime)s - %(processName)s - %(levelname)s - %(message)s'
logging.basicConfig(level=logging.DEBUG,format=FORMAT)
logger = logging.getLogger('main')
#filehandler = logging.FileHandler(filename='exercise3.log',mode='w')
#filehandler.setLevel(logging.DEBUG)
#streamhandler = logging.StreamHandler()
#streamhandler.setLevel(logging.INFO)
#logger.addHandler(filehandler)
#logger.addHandler(streamhandler)


exitevent = None
NUM_NODES = 4
AVG_CANDIDATES = 1#floor(NUM_NODES/5)
CURRENT_HOST = '192.168.178.13'
LOCALHOST = 'localhost'

def node_wrapper(node: ex3.Node, synchronizer, exitevent):
    """Run function under the pool

    Wrapper around function to catch exceptions that don't inherit from
    Exception (which aren't caught by multiprocessing, so that you end
    up hitting the timeout).
    """
    multiprocessing_logging.install_mp_handler()
    try:
        # Ignore Keyboard Int
        signal.signal(signal.SIGINT, signal.SIG_IGN)
        # wait for all process starts
        synchronizer.wait()
        if node.info.id == 1:
            logger.info("All processes started.")
        node.setup_logging()
        # wait for all loggers
        synchronizer.wait()
        if node.info.id == 1:
            logger.info("All processes logging enabled.")
        node.start_server()
        # wait for all binds
        synchronizer.wait()
        if node.info.id == 1:
            logger.info("All processes listening.")
        node.connect_client()
        # wait for all connects
        synchronizer.wait()
        if node.info.id == 1:
            logger.info("All processes connected.")
        
        node.run(exitevent)

        logger.debug("I'm elected: {0}.".format("yes" if node.elected else 'no'))

        if node.elected:
            elapsed_time = (node.end_time-node.start_time)
            logger.debug("Took {0:.2f} ms to elect me.".format(elapsed_time*1000))

        # wait for all nodes to quit
        synchronizer.wait()
        if node.info.id == 1:
            logger.info("All processes exited.")

    except:
        cls, exc, tb = sys.exc_info()
        if issubclass(cls, Exception):
            raise # No worries
        # Need to wrap the exception with something multiprocessing will recognise
        import traceback
        #logger.error("Unhandled exception {0} ({1}):\n{2}".format(cls.__name__, exc, traceback.format_exc()))
        raise Exception("Unhandled exception: {0} ({1})".format(cls.__name__, exc))

def ProcessJoin(x):
    if type(x) is mp.Process:
        logger.info("Joined Process {0}.".format(x.name))
    else:
        logger.info("Joined Process")
    x.join()

def main():
    premature_exit = False
    print("System starting.")

    startport = 32516
    routerport = 32514
    dealerport = 32515
    routerdealerhost = '192.168.178.13'
    if routerdealerhost == CURRENT_HOST:
        routerdealerhost = LOCALHOST
    routerinfo = ex3.NodeInfo(0,ex3.Transport.TCP,routerdealerhost,routerport,routerdealerhost != CURRENT_HOST)
    dealerinfo = ex3.NodeInfo(0,ex3.Transport.TCP,routerdealerhost,dealerport,routerdealerhost != CURRENT_HOST)

    if routerinfo.local or dealerinfo.local:
        routerevent = mp.Event()
        router_proc = mp.Process(target=router, name="Router", args=(routerport,dealerport,routerevent))

    router_proc.start()

    logger.log(1000,"Starting system.")

    all_systems = [
        {"host":"192.168.178.13","transport":ex3.Transport.TCP,"startport":startport,"number":NUM_NODES},
        #{"host":"192.168.178.3","transport":ex3.Transport.TCP,"startport":startport,"number":NUM_NODES}
    ]

    nis = {}
    i = 0
    for system in all_systems:
        for x in range(system['number']):
            ni = ex3.NodeInfo(i+1,ex3.Transport.TCP,system['host'] if system['host'] != CURRENT_HOST else LOCALHOST,system['startport']+x,system['host'] == CURRENT_HOST)
            nis[i+1] = ni
            i += 1

    nodes = list()

    for ni in nis:
        if nis.get(ni).local:
            nodes.append(ex3.Node(nis.get(ni),nis,AVG_CANDIDATES,routerinfo.connect_uri,dealerinfo.connect_uri))

    synchronizer = mp.Barrier(len(nodes))
    #serializer = mp.Lock()

    procs = list()
    exitevent = mp.Event()
    #with mplog.open_queue() as log_queue:
    try:
        for n in nodes:
            #proc = mp.Process(target=mplog.logged_call, daemon=True, name="Node-{0}".format(n.info.id),args=(log_queue, node_wrapper, n, synchronizer, exitevent))
            proc = mp.Process(target=node_wrapper, name="Node-{0:06}".format(n.info.id),args=(n, synchronizer, exitevent))
            proc.start()
            logger.info("Stared Process {0}.".format(n.info.id))
            procs.append(proc)

        logger.info("Started nodes.")


        all(map(ProcessJoin, procs))
    except (KeyboardInterrupt, SystemExit):
        exitevent.set()
        logger.info("Forced Exit.")
    logger.info("Done.")
    if routerinfo.local or dealerinfo.local:
        routerevent.set()
        router_proc.join()
    print("System shutdown.")
if __name__ == "__main__":
    main()
