package exercise1;

import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

class Exercise1 extends UnicastRemoteObject implements Exercise1_RMI {

    int localID;
    int swarmSize;
    int totalMessageCount;
    
    public int clk = 0;
    public int acknowledgements = 0;
    public int packetsReceived = 0;

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
        System.out.format("Received packet from %d at %d\n", m.srcID, m.timestamp);
        clk = Math.max(m.timestamp + 1, clk + 1);
                
        try {
            Exercise1_RMI sender = m.sender;
            Acknowledgement a = new Acknowledgement(m, clk++, this);
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
        System.out.format("Received acknowledgement for message sent at %d by %d to %d at %d\n", a.m.timestamp, a.m.srcID, a.m.destID, a.timestamp);
        clk = Math.max(a.timestamp + 1, clk + 1);
        
        if (a.m.srcID == localID) {
            acknowledgements++;

            if (acknowledgements >= swarmSize * totalMessageCount) {
                System.out.println("Message acknowledgements received from the whole swarm.");
            }
        }
    }
}
