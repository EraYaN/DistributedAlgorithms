/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package exercise1;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.function.Consumer;

/**
 *
 * @author Erwin
 */
public class Exercise1_thread implements Runnable {

    Instance localInstance;
    Map<Integer, Instance> remoteInstances;
    Exercise1 ex;

    int totalMessageCount;
    private static final int MAX_DELAY = 1000;

    public Exercise1_thread(Instance LocalInstance, Map<Integer, Instance> RemoteInstances, int TotalMessageCount) throws RemoteException {
        totalMessageCount = TotalMessageCount;
        localInstance = LocalInstance;
        localInstance.object = ex = new Exercise1(localInstance.id, RemoteInstances.size(), totalMessageCount);
        localInstance.host = "localhost";
        try {
            localInstance.Bind();
        } catch (MalformedURLException | AlreadyBoundException | RemoteException e) {
            e.printStackTrace();
        }
        remoteInstances = RemoteInstances;
    }

    @Override
    public void run() {
        remoteInstances.values().forEach((remoteInstance) -> {
            try {
                if (!remoteInstance.HasObject()) {
                    if (!"localhost".equals(remoteInstance.host)) {
                        remoteInstance.Lookup();
                    } else {
                        remoteInstance.Bind();
                    }
                }
            } catch (MalformedURLException | AlreadyBoundException | NotBoundException | RemoteException e) {                
                e.printStackTrace();
            }
        });
        for (int i = 0; i < totalMessageCount; i++) {
            System.out.format("Sending message set %d of %d.\n", i + 1, totalMessageCount);
            remoteInstances.entrySet().forEach((Map.Entry<Integer, Instance> entry) -> {
                try {
                    Integer id = entry.getKey();
                    Instance remoteInstance = entry.getValue();

                    Random rand = new Random();
                    int delay = rand.nextInt(MAX_DELAY);
                    Thread.sleep(delay);

                    long timestamp = (new Date()).getTime();

                    Message m = new Message(localInstance.id, id, timestamp, localInstance.object);

                    //TODO make object hang until connected.
                    if (remoteInstance.HasObject()) {
                        try {
                            ((Exercise1_RMI) remoteInstance.object).rxMessage(m);
                        } catch (NoSuchObjectException nsoe) {
                            System.err.format("Need to do reconnect for %d.\n", id);
                            remoteInstance.Lookup();
                            ((Exercise1_RMI) remoteInstance.object).rxMessage(m);
                        }
                        System.out.format("Sent packet to %d.\n", id);
                    }
                } catch (InterruptedException | MalformedURLException | NotBoundException | RemoteException e) {
                    e.printStackTrace();
                }
            });
        }

        while (ex.acknowledgements < remoteInstances.size() * totalMessageCount || ex.packetsReceived < remoteInstances.size() * totalMessageCount) {
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Done.");

    }
}
