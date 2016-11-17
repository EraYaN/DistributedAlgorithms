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

/**
 *
 * @author Erwin
 */
public class Exercise1 extends UnicastRemoteObject implements Exercise1_RMI {

    int localID;
    int swarmSize;
    int totalMessageCount;

    public int acknowledgements = 0;
    public int packetsReceived = 0;

    private final SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss.mmmm");

    public Exercise1(int LocalID, int SwarmSize) throws RemoteException {
        this(LocalID, SwarmSize, 1);
    }

    public Exercise1(int LocalID, int SwarmSize, int TotalMessageCount) throws RemoteException {
        localID = LocalID;
        swarmSize = SwarmSize;
        totalMessageCount = TotalMessageCount;
    }

    @Override
    public void rxMessage(Message m) {
        System.out.format("Received packet from %d at %s\n", m.srcID, formatter.format(m.timestamp));

        try {
            Exercise1_RMI sender = m.sender;
            Acknowledgement a = new Acknowledgement(m, (new Date()).getTime(), this);
            if (sender != null) {
                sender.rxAcknowledgement(a);
            } else {
                System.out.format("Packet from %d does not have an Object.\n", m.sender);
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        if (m.destID == localID) {
            packetsReceived++;

            if (packetsReceived >= swarmSize * totalMessageCount) {
                System.out.println("Messages received from the whole swarm.");
            }
        }
    }

    @Override
    public void rxAcknowledgement(Acknowledgement a) {
        System.out.format("Received acknowledgement for message sent at %s by %d to %d at %s\n", formatter.format(a.m.timestamp), a.m.srcID, a.m.destID, formatter.format(a.timestamp));

        if (a.m.srcID == localID) {
            acknowledgements++;

            if (acknowledgements >= swarmSize * totalMessageCount) {
                System.out.println("Message acknowledgements received from the whole swarm.");
            }
        }
    }
}
