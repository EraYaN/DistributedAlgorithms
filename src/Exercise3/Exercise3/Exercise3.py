import multiprocessing as mp

import logging
import time

import mplog
import ex3

import atexit
import signal
import sys

FORMAT = '%(asctime)s - %(processName)s - %(levelname)s - %(message)s'
logging.basicConfig(level=logging.DEBUG, format=FORMAT)
logger = logging.getLogger('main')

exitevent = None
NUM_NODES = 50

def node_wrapper(node: ex3.Node, synchronizer, exitevent):
    """Run function under the pool

    Wrapper around function to catch exceptions that don't inherit from
    Exception (which aren't caught by multiprocessing, so that you end
    up hitting the timeout).
    """
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
        node.connect_clients()
        # wait for all connects
        synchronizer.wait()
        if node.info.id == 1:
            logger.info("All processes connected.")
        node.run(exitevent)
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
    logger.info("Starting system.")
    nis = {}
    for x in range(NUM_NODES):
        ni = ex3.NodeInfo(x+1,ex3.Transport.TCP,'localhost',32516+x)
        nis[x+1] = ni

    nodes = list()

    for ni in nis:
        nodes.append(ex3.Node(nis.get(ni),nis))

    synchronizer = mp.Barrier(len(nodes))
    #serializer = mp.Lock()

    procs = list()
    exitevent = mp.Event()
    with mplog.open_queue() as log_queue:
        try:
            for n in nodes:
                proc = mp.Process(target=mplog.logged_call, daemon=True, name="Node-{0}".format(n.info.id),args=(log_queue, node_wrapper, n, synchronizer, exitevent))
                proc.start()
                logger.info("Stared Process {0}.".format(n.info.id))
                procs.append(proc)

            logger.info("Started nodes.")


            all(map(ProcessJoin, procs))
        except (KeyboardInterrupt, SystemExit):
            exitevent.set()
            logger.info("Forced Exit.")
        logger.info("Done.")

if __name__ == "__main__":
    main()
