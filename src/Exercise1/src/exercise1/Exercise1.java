/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package exercise1;

import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

/**
 *
 * @author Erwin
 */
public class Exercise1 extends UnicastRemoteObject
implements Exercise1_RMI, Runnable{
    
    private static final long serialVersionUID = 7526471155622776147L;
    
    public Exercise1() throws RemoteException{        
    
    }
    
    @Override
    public void rxPacket(double t){        
        System.out.format("Received packet: %f",t);
    }
    
    @Override
    public void run(){
        try{
            java.rmi.Naming.bind("rmi://localhost:1099/Exercise1", this);
            System.in.read();
            Exercise1 object = (Exercise1)java.rmi.Naming.lookup("rmi://145.94.234.147:1099/Exercise1");
            object.rxPacket(1.0);
        } catch(Exception e){            
            e.printStackTrace();
        }
        
    }
}
