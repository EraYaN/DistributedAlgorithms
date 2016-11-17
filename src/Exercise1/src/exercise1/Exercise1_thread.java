/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package exercise1;

import java.rmi.NoSuchObjectException;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Map;
import java.util.Random;

/**
 *
 * @author Erwin
 */
public class Exercise1_thread implements Runnable{
    
    Instance localInstance;
    Map<Integer, Instance> remoteInstances;
    Exercise1 ex; 
    
    private static final int maxDelay = 1000;
    
    public Exercise1_thread(Instance LocalInstance, Map<Integer, Instance> RemoteInstances) throws RemoteException {
        localInstance = LocalInstance;
        localInstance.object = ex = new Exercise1(localInstance.id,RemoteInstances.size());
        localInstance.host = "localhost";
        try {
            localInstance.Bind();
        } catch (Exception e) {
            e.printStackTrace();
        }
        remoteInstances = RemoteInstances;
    }    
    
    @Override
    public void run() {
        for (Instance remoteInstance : remoteInstances.values()) {
            try {                
                if (!remoteInstance.HasObject()) {
                    if (remoteInstance.host != "localhost") {
                        remoteInstance.Lookup();
                    } else {
                        remoteInstance.Bind();
                    }
                }                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        for (Map.Entry<Integer, Instance> entry : remoteInstances.entrySet()) {
            try {
                Integer id = entry.getKey();
                Instance remoteInstance = entry.getValue();
                
                Random rand = new Random();
                int delay = rand.nextInt(maxDelay);
                Thread.sleep(delay);

                long timestamp = (new Date()).getTime();

                Message m = new Message(localInstance.id, id, timestamp, localInstance.object);

                //TODO make object hang until connected.
                if (remoteInstance.HasObject()) {
                    try{
                    ((Exercise1_RMI) remoteInstance.object).rxMessage(m);
                    } catch(NoSuchObjectException nsoe){ 
                        remoteInstance.Lookup();
                        ((Exercise1_RMI) remoteInstance.object).rxMessage(m);
                    }
                    System.out.format("Sent packet to %d.\n", id);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }            
        }
        
        while(ex.acknowledgements < remoteInstances.size()){
            try{
                Thread.sleep(25); 
            } catch(Exception e){
                e.printStackTrace();            
            }
        }
        System.out.println("Done.");  

    }
}
