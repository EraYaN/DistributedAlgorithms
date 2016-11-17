/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package exercise1;

import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Random;

/**
 *
 * @author Erwin
 */
public class Exercise1 extends UnicastRemoteObject implements Exercise1_RMI {
    
    Instance localInstance;
    Map<Integer, Instance> remoteInstances;
    
    public int acknowledgements = 0;

    
    private static final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.mmmm");
       
    public Exercise1(Instance LocalInstance, Map<Integer, Instance> RemoteInstances) throws RemoteException {
        localInstance = LocalInstance;       
        remoteInstances = RemoteInstances;
    }  

    @Override
    public void rxMessage(Message m) {
        System.out.format("Received packet from %d at %s\n", m.sender, format.format(m.timestamp));

        try {
            Instance senderInstance = remoteInstances.get(m.sender);
            Acknowledgement a = new Acknowledgement(m, (new Date()).getTime(), localInstance.id);
            if (senderInstance.HasObject()) {
                ((Exercise1_RMI) senderInstance.object).rxAcknowledgement(a);
            } else {                
                System.out.format("Packet from %d does not have an Object.\n", m.sender);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void rxAcknowledgement(Acknowledgement a) {
        System.out.format("Received acknowledgement for message sent at %s by %d from %d at %s\n", format.format(a.m.timestamp), a.m.sender, a.sender, format.format(a.timestamp));

        if (a.m.sender == localInstance.id) {
            acknowledgements++;

            if (acknowledgements == remoteInstances.size()) {
                System.out.format("Message sent at %s has been acknowledged by all instances\n", format.format(a.m.timestamp));
            }
        }
    }
}
