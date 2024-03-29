import multiprocessing as mp
import logging
import time
import atexit
import signal
import sys
import multiprocessing_logging
import os
from math import floor

import ex3
from router import router

multiprocessing_logging.install_mp_handler()

FORMAT = '%(asctime)s - %(processName)s - %(levelname)s - %(message)s'
logging.basicConfig(level=logging.INFO,format=FORMAT)
logger = logging.getLogger('main')
#filehandler = logging.FileHandler(filename='exercise3.log',mode='w')
#filehandler.setLevel(logging.DEBUG)
#streamhandler = logging.StreamHandler()
#streamhandler.setLevel(logging.INFO)
#logger.addHandler(filehandler)
#logger.addHandler(streamhandler)


exitevent = None
NUM_NODES = 10
AVG_CANDIDATES = 4#floor(NUM_NODES/5)
CURRENT_HOST = '145.94.213.228'
ROUTER_HOST = '145.94.213.228'
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
        logger.info("PID: {0}".format(os.getpid()))
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
        if x.is_alive():
            logger.info("Joined Process {0}.".format(x.name))
        else:
            logger.info("Process {0} already dead.".format(x.name))
    else:
        logger.info("Joined Process")
    x.join()

def main():
    premature_exit = False
    print("System starting.")
    logger.info("PID: {0}".format(os.getpid()))
    startport = 32516
    routerport = 32514
    dealerport = 32515
    routerdealerhost = ROUTER_HOST
    if routerdealerhost == CURRENT_HOST:
        routerdealerhost = LOCALHOST
    routerinfo = ex3.NodeInfo(0,ex3.Transport.TCP,routerdealerhost,routerport,routerdealerhost == LOCALHOST)
    dealerinfo = ex3.NodeInfo(0,ex3.Transport.TCP,routerdealerhost,dealerport,routerdealerhost == LOCALHOST)

    all_systems = [
        {"host":"145.94.213.228","transport":ex3.Transport.TCP,"startport":startport,"number":NUM_NODES},
        #{"host":"145.94.206.208","transport":ex3.Transport.TCP,"startport":startport,"number":NUM_NODES}
    ]

    if routerinfo.local or dealerinfo.local:
        routerevent = mp.Event()
        router_proc = mp.Process(target=router, name="RouterProcs", args=(routerport,dealerport,routerevent))
        router_proc.start()
        if len(all_systems) > 1:
            logger.log(1000,"Waiting for 5 seconds.")#input("Press enter to continue..")
            time.sleep(5)

    logger.log(1000,"Starting system.")
    
    

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
            proc = mp.Process(target=node_wrapper, name="Node-{0:06}".format(n.info.id),args=(n, synchronizer, exitevent))
            proc.start()
            logger.info("Started Process {0}.".format(n.info.id))
            procs.append(proc)

        logger.info("Started nodes.")

        for p in procs:
            ProcessJoin(p)
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
