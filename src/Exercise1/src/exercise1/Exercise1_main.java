/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package exercise1;

import java.rmi.*;

/**
 *
 * @author Erwin
 */
public class Exercise1_main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // Create and install a security manager
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        /*if (System.getSecurityManager() == null) {
        System.setSecurityManager(new RMISecurityManager());
        }*/
        
        try {
            java.rmi.registry.LocateRegistry.createRegistry(1099);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        
        System.out.println("Running");
        
        while(true){}
         
    }
    
}
