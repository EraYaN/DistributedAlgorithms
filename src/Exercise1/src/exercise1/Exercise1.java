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
    
    int localID;
    int swarmSize;
    
    public int acknowledgements = 0;
    
    private static final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.mmmm");
       
    public Exercise1(int LocalID, int SwarmSize) throws RemoteException{
        localID = LocalID;
        swarmSize = SwarmSize;
    }  

    @Override
    public void rxMessage(Message m) {
        System.out.format("Received packet from %d at %s\n", m.srcID, format.format(m.timestamp));

        try {
            Exercise1_RMI sender = m.sender;
            Acknowledgement a = new Acknowledgement(m, (new Date()).getTime(), this);
            if (sender != null) {
                sender.rxAcknowledgement(a);
            } else {                
                System.out.format("Packet from %d does not have an Object.\n", m.sender);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void rxAcknowledgement(Acknowledgement a) {
        System.out.format("Received acknowledgement for message sent at %s by %d from %d at %s\n", format.format(a.m.timestamp), a.m.srcID, a.m.destID, format.format(a.timestamp));

        //if (a.m.sender.id == localID) {
            acknowledgements++;

            if (acknowledgements == swarmSize) {
                System.out.format("Message sent at %s has been acknowledged by all instances\n", format.format(a.m.timestamp));
            }
        //}
    }
}
