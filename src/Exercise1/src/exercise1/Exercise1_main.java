/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package exercise1;

import java.rmi.*;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Erwin
 */
public class Exercise1_main {
    //constants
    public static final String projectId = "Exercise1";
    public static final int localPort = 32516;
    public static final int messageCount = 50;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception{
        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        //Deprecated
        /*if (System.getSecurityManager() == null) {
        System.setSecurityManager(new RMISecurityManager());
        }*/
        
        int localID = 1;
        if (args.length > 0) {
            try {
                localID = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Argument" + args[0] + " must be an integer [localID].");
                System.exit(1);
            }
        }
        String localHost = "localhost"; 
        Instance localInstance;
        Map<Integer, Instance> allInstances = new HashMap<Integer, Instance>();
        //TODO: Implament read from file (csv?, json?) Probably whatever is easier in java.
        allInstances.put(1, new Instance(1,projectId,"192.168.178.13",localPort));
        allInstances.put(2, new Instance(2,projectId,"192.168.178.12",localPort));       
        
               
        Registry rmiRegistry = null;
        Exercise1_thread obj = null;
        boolean exportedRMI = false;
        try {
            System.out.println("Starting RMI Registry...");
            rmiRegistry = java.rmi.registry.LocateRegistry.createRegistry(1099);
            exportedRMI = true;
        } catch (RemoteException e) {
            rmiRegistry = java.rmi.registry.LocateRegistry.getRegistry(1099);
        }
        try {  
            if(rmiRegistry!=null){
                System.out.println("Running...");
                localInstance = allInstances.get(localID);
                allInstances.remove(localID);
                obj = new Exercise1_thread(localInstance,allInstances,messageCount);
                
                System.out.format("Listening on port %s.\n", localPort);
                
                System.out.format("Press enter to continue to send %d messages to %d hosts...\n",messageCount,allInstances.size());
                System.in.read();
                //This actually starts sending one message to each remote.
                Thread t = new Thread(obj);
                t.start();
                t.join();
                System.out.println("Exiting...");
                System.exit(0);
            } else {                
                System.err.println("RMI Registry not available.");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
           // if(obj!=null)
            //    UnicastRemoteObject.unexportObject(obj.ex, true);
            //if(rmiRegistry!=null && exportedRMI)
            //    UnicastRemoteObject.unexportObject(rmiRegistry, true);
        }               
    }
    
}
