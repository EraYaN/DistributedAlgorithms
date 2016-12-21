import multiprocessing as mp

import logging
import time

import mplog
import ex3

import atexit
import signal
import sys
from math import floor
from queue import Queue, Empty

FORMAT = '%(asctime)s - %(processName)s - %(levelname)s - %(message)s'
logging.basicConfig(level=logging.DEBUG, format=FORMAT, filename='exercise3.log',filemode='w')
logger = logging.getLogger('main')

exitevent = None
NUM_NODES = 50
NUM_RUNS = 1
AVG_CANDIDATES = floor(NUM_NODES/5)

def node_wrapper(node: ex3.Node, synchronizer, serializer, exitevent, progressevent, num_runs, resulting_times, round_candidates):
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
            progressevent.set()
            progressevent.clear()
        node.setup_logging()
        # wait for all loggers
        synchronizer.wait()
        if node.info.id == 1:
            logger.info("All processes logging enabled.")
            progressevent.set()
            progressevent.clear()
        node.start_server()
        # wait for all binds
        synchronizer.wait()
        if node.info.id == 1:
            logger.info("All processes listening.")
            progressevent.set()
            progressevent.clear()
        node.connect_clients()
        # wait for all connects
        synchronizer.wait()
        if node.info.id == 1:
            logger.info("All processes connected.")
            progressevent.set()
            progressevent.clear()
        #t1 = time.perf_counter()
        for i in range(num_runs):
            node.reset_state(i)
            logger.debug("Waiting 1.".format(i))
            synchronizer.wait()
            node.run(exitevent)
            logger.debug("Waiting 2.".format(i))
            synchronizer.wait()
            if node.candidate:
                round_candidates.put(node.info)
            logger.debug("Waiting 3.".format(i))
            synchronizer.wait()
            # Store time result only if elected, resulting_times is queue so no
            # serializer needed.
            if node.elected:
                data = {"node":node.info.id,"round":i,"time":node.end_time - node.start_time,"candidates":[]}
                try:
                    while True:
                        data["candidates"].append(round_candidates.get(False))
                except Empty:
                    pass
                resulting_times.put(data)

            logger.debug("Waiting 4.".format(i))
            synchronizer.wait()
            if node.info.id == 1:
                exitevent.clear()
                logger.info("All processes completed run {}.".format(i))
                progressevent.set()
                progressevent.clear()

        # wait for all nodes to quit
        logger.debug("Waiting for quit.".format(i))
        synchronizer.wait()

        if node.info.id == 1:
            logger.info("All processes exited.")
            progressevent.set()
            progressevent.clear()
    except:
        cls, exc, tb = sys.exc_info()
        if issubclass(cls, Exception):
            raise # No worries
        # Need to wrap the exception with something multiprocessing will
                         # recognise
        import traceback
        #logger.error("Unhandled exception {0}
        #({1}):\n{2}".format(cls.__name__, exc, traceback.format_exc()))
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
    logger.log(1000,"Starting system.")
    nis = {}
    for x in range(NUM_NODES):
        ni = ex3.NodeInfo(x + 1,ex3.Transport.TCP,'localhost',32516 + x)
        nis[x + 1] = ni

    nodes = list()

    for ni in nis:
        nodes.append(ex3.Node(nis.get(ni),nis,AVG_CANDIDATES))

    synchronizer = mp.Barrier(len(nodes))
    serializer = mp.Lock()

    procs = list()
    exitevent = mp.Event()
    progressevent = mp.Event()
    with mp.Manager() as manager:
        resulting_times = manager.Queue()
        round_candidates = manager.Queue()

        with mplog.open_queue() as log_queue:
            try:
                for n in nodes:
                    proc = mp.Process(target=mplog.logged_call, daemon=True, name="Node-{0}".format(n.info.id),args=(log_queue, node_wrapper, n, synchronizer, serializer, exitevent, progressevent, NUM_RUNS, resulting_times, round_candidates))
                    proc.start()
                    logger.info("Stared Process {0}.".format(n.info.id))
                    sys.stdout.write("Started Process {0} out of {1}\r".format(n.info.id,len(nodes)))
                    procs.append(proc)
                sys.stdout.write("\n")
                logger.info("Started nodes.")
                print("Started Processes.")
                progressevent.wait()
                print("Processes Running.")
                progressevent.wait()
                print("Processes Logging Enabled.")
                progressevent.wait()
                print("Processes Server Started.")
                progressevent.wait()
                print("Processes Connected.")
                for i in range(NUM_RUNS):
                    progressevent.wait()
                    sys.stdout.write("Finished Run {0} out of {1}\r".format(i+1,NUM_RUNS))
                sys.stdout.write("\n")
                progressevent.wait()
                print("Processes Exited.")
                all(map(ProcessJoin, procs))
            except (KeyboardInterrupt, SystemExit):
                exitevent.set()
                logger.info("Forced Exit.")
                premature_exit = True
            logger.info("Done.")
            print("Finished.")

        # Dirty pre-log sort hack, we need to dump to file.
        if not premature_exit:
            time.sleep(1)

            with serializer:
                results_list = []
                try:
                    while True:
                        results_list.append(resulting_times.get(False))
                except Empty:
                    pass

                for result in results_list:
                    logger.log(1000,"Round {0}: Took {1:.2f} ms to elect node {2} with {3} candidates.".format(result["round"],result["time"] * 1000 / NUM_RUNS,result["node"],len(result['candidates'])))
                if len(results_list) > 0:
                    try:
                        import numpy
                        times = numpy.array([n['time'] * 1000 / NUM_RUNS for n in results_list])
                        candidates = numpy.array([len(n['candidates']) for n in results_list])
                        logger.log(1000,"Time Mean: {1:.2f} ms per run with {2} nodes measured over {3} runs.".format(id,numpy.mean(times, axis=0),NUM_NODES,NUM_RUNS))
                        logger.log(1000,"Time StdDev: {1:.2f} ms with {2} nodes measured over {3} runs.".format(id,numpy.std(times, axis=0),NUM_NODES,NUM_RUNS))
                        logger.log(1000,"Candidate Mean: {1:.2f} with {2} nodes measured over {3} runs.".format(id,numpy.mean(candidates, axis=0),NUM_NODES,NUM_RUNS))
                        logger.log(1000,"Candidate StdDev: {1:.2f} with {2} nodes measured over {3} runs.".format(id,numpy.std(candidates, axis=0),NUM_NODES,NUM_RUNS))
                    except:
                        logger.error("Could not load NumPy, results limited.")
                        avg_time = (sum([n['time'] * 1000 / NUM_RUNS for n in results_list]) / len(results_list))
                        logger.log(1000,"Average: {1:.2f} ms per run with {2} nodes measured over {3} runs.".format(id,avg_time,NUM_NODES,NUM_RUNS))
                else:
                    logger.error("No result times have been recorded.")
            print("Collected results.")
        print("System shutdown.")
if __name__ == "__main__":
    main()
