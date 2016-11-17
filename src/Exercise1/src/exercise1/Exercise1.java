/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package exercise1;

import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.Map;

/**
 *
 * @author Erwin
 */
public class Exercise1 extends UnicastRemoteObject
        implements Exercise1_RMI, Runnable {

    Instance localInstance;
    Map<Integer, Instance> remoteInstances;

    private static final long serialVersionUID = 7526471155622776147L;

    public Exercise1(Instance LocalInstance, Map<Integer, Instance> RemoteInstances) throws RemoteException {
        localInstance = LocalInstance;
        localInstance.object = this;
        localInstance.host = "localhost";
        remoteInstances = RemoteInstances;
    }

    public Exercise1(int ID, String Project, String Host, int Port, Map<Integer, Instance> RemoteInstances) throws RemoteException {
        localInstance = new Instance(ID, Project, Host, Port, this);
        remoteInstances = RemoteInstances;
    }

    @Override
    public void rxPacket(double t) {
        System.out.format("Received packet: %f\n", t);
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
                //TODO make delay random
                Thread.sleep(200);
                //TODO make packet random (or time)
                //TODO make object hang until connected.
                if (remoteInstance.HasObject()) {
                    ((Exercise1) remoteInstance.object).rxPacket(1.0);
                    System.out.format("Sent packet to %d.\n", id);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}
