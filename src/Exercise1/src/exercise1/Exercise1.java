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
public class Exercise1 extends UnicastRemoteObject
        implements Exercise1_RMI, Runnable {

    Instance localInstance;
    Map<Integer, Instance> remoteInstances;
    int acknowledgements = 0;

    private static final long serialVersionUID = 7526471155622776147L;
    private static final int maxDelay = 1000;
    private static final SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss.mmmm");

    public Exercise1(Instance LocalInstance, Map<Integer, Instance> RemoteInstances) throws RemoteException {
        localInstance = LocalInstance;
        localInstance.object = (Exercise1_RMI) this;
        localInstance.host = "localhost";
        try {
            localInstance.Bind();
        } catch (Exception e) {
            e.printStackTrace();
        }
        remoteInstances = RemoteInstances;
    }

    public Exercise1(int ID, String Project, String Host, int Port, Map<Integer, Instance> RemoteInstances) throws RemoteException {
        localInstance = new Instance(ID, Project, Host, Port, this);
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

    @Override
    public void run() {
        for (Map.Entry<Integer, Instance> entry : remoteInstances.entrySet()) {
            try {
                Integer id = entry.getKey();
                Instance remoteInstance = entry.getValue();
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

                Message m = new Message(timestamp, localInstance.id);

                //TODO make object hang until connected.
                if (remoteInstance.HasObject()) {
                    ((Exercise1_RMI) remoteInstance.object).rxMessage(m);
                    System.out.format("Sent packet to %d.\n", id);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
