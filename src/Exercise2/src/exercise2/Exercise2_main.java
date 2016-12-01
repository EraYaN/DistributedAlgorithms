package exercise2;

import static com.mkyong.utils.CSVUtils.parseLine;
import java.io.File;
import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.rmi.*;
import java.rmi.registry.Registry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Exercise2_main {

    //constants
    public static final String PROJECT_ID = "Exercise2";
    public static final int MESSAGE_COUNT = 2;
    public static final int AUTO_START_DELAY = 5; // seconds
    public static final String INSTANCES_FILE = "data/instances.csv";

    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {
        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        //Deprecated
        /*if (System.getSecurityManager() == null) {
        System.setSecurityManager(new RMISecurityManager());
        }*/

        InstanceMap allInstances = new InstanceMap();

        InstanceIDArrayList requestGroup;

        try {
            Scanner scanner = new Scanner(new File(INSTANCES_FILE));
            while (scanner.hasNext()) {
                List<String> line = parseLine(scanner.nextLine());
                requestGroup = new InstanceIDArrayList();
                try {
                    int id = Integer.parseInt(line.get(0));
                    String host = line.get(1);
                    int port = Integer.parseInt(line.get(2));
                    String requestGroupString = line.get(3);
                    String[] requestGroupArray = requestGroupString.split(";");
                    for (String v : requestGroupArray) {
                        requestGroup.add(Integer.parseInt(v));
                    }
                    allInstances.put(id, new Instance(id, PROJECT_ID, host, port, requestGroup));
                    System.out.println("Instance [id= " + line.get(0) + ", host= " + line.get(1) + " , port=" + line.get(2) + ", requestGroup=" + line.get(3) + "]");
                } catch (NumberFormatException e) {
                    System.err.format("While parsing line \'%s\' a format occured.\n", line);
                    System.exit(1);
                }
            }
            scanner.close();
        } catch (IOException e) {
            System.err.format("Error occured while reading instance file %s\n", INSTANCES_FILE);
            System.exit(1);
        }

        if (allInstances.size() == 0) {
            System.err.format("No instances found in instance file %s\n", INSTANCES_FILE);
            System.exit(1);
        }

        int localID = 1;
        boolean autoStarting = false;
        if (args.length > 0) {
            try {
                localID = Integer.parseInt(args[0]);
                if (!allInstances.containsKey(localID)) {
                    System.err.println("Argument" + args[0] + " does not exists in the instance dictiopnary [localID].");
                    System.exit(1);
                }
            } catch (NumberFormatException e) {
                System.err.println("Argument" + args[0] + " must be an integer [localID].");
                System.exit(1);
            }
        }
        if (args.length > 1) {
            if ("autostart".equals(args[1])) {
                autoStarting = true;
            }
        }

        final Instance localInstance = allInstances.get(localID);

        Registry rmiRegistry;
        Exercise2_thread obj;
        try {
            System.out.println("Starting RMI Registry...");
            rmiRegistry = java.rmi.registry.LocateRegistry.createRegistry(localInstance.port);
        } catch (RemoteException e) {
            rmiRegistry = java.rmi.registry.LocateRegistry.getRegistry(localInstance.port);
        }
        try {
            if (rmiRegistry != null) {
                System.out.format("Running as instance %d...\n", localInstance.id);
                //allInstances.remove(localID);
                obj = new Exercise2_thread(localInstance, allInstances, MESSAGE_COUNT);

                System.out.format("Listening on %s:%d.\n", localInstance.host, localInstance.port);

                if (!autoStarting) {
                    System.out.format("Press enter to continue to send %d requests to %d hosts...\n", MESSAGE_COUNT, allInstances.size());
                    System.in.read();
                } else {
                    System.out.format("Waiting %d seconds to send %d requests to %d hosts...\n", AUTO_START_DELAY, MESSAGE_COUNT, allInstances.size());
                    Thread.sleep(AUTO_START_DELAY * 1000);
                }
                //This actually starts sending one message to each remote.
                Thread t = new Thread(obj);
                t.start();
                t.join();
                System.out.println("Press enter to continue.");
                System.in.read();
                System.out.println("Exiting...");
                System.exit(0);
            } else {
                System.err.println("RMI Registry not available.");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

}
