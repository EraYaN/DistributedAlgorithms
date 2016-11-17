/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package exercise1;

import java.rmi.*;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;

/**
 *
 * @author Erwin
 */
public class Exercise1_main {

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
        Registry rmiRegistry = null;
        Exercise1 obj = null;
        boolean exportedRMI = false;
        try {
            rmiRegistry = java.rmi.registry.LocateRegistry.getRegistry(1099);
        } catch (RemoteException e) {
            rmiRegistry = java.rmi.registry.LocateRegistry.createRegistry(1099);
            exportedRMI = true;
        }
        try {            
            System.out.println("Running...");
            obj = new Exercise1();
            new Thread(obj).start();
            System.out.format("Listening on %s.","port 44001");
        } catch (RemoteException e) {
            e.printStackTrace();
        } finally {
            //java.rmi.registry.LocateRegistry.
            if(obj!=null)
                UnicastRemoteObject.unexportObject(obj, true);
            if(rmiRegistry!=null && exportedRMI)
                UnicastRemoteObject.unexportObject(rmiRegistry, true);
        }               
    }
    
}
