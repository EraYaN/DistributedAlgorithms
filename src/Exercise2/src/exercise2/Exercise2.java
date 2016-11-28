package exercise2;

import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;

class Exercise2 extends UnicastRemoteObject implements Exercise2_RMI {

    int localID;
    int swarmSize;
    int totalMessageCount;

    MessageQueue queue;
    AcknowledgeMap acknowledgeMap;
    AcknowledgementCallback callBack;

    public int acknowledgements = 0;
    public int packetsReceived = 0;

    public Exercise2(int LocalID, int SwarmSize, AcknowledgementCallback callBack) throws RemoteException {
        this(LocalID, SwarmSize, 1, callBack);
    }

    public Exercise2(int LocalID, int SwarmSize, int TotalMessageCount, AcknowledgementCallback CallBack) throws RemoteException {
        localID = LocalID;
        swarmSize = SwarmSize;
        totalMessageCount = TotalMessageCount;
        queue = new MessageQueue();
        acknowledgeMap = new AcknowledgeMap();
        callBack = CallBack;
    }

    public int GetAcknowledgements(Message m) {
        if (acknowledgeMap.containsKey(m.hashCode())) {
            return acknowledgeMap.get(m.hashCode());
        }
        return 0;
    }

    public Message Peek() {
        return queue.peek();
    }

    public Message Remove() {
        return queue.remove();
    }

    @Override
    public void rxMessage(Message m) {
        System.out.format("%6d: Received packet from %d at %d\n", m.hashCode(), m.srcID, m.timestamp);

        queue.add(m);
        callBack.acknowledgementCallback(m);

        if (m.destID == localID) {
            packetsReceived++;
            if (packetsReceived >= swarmSize * totalMessageCount) {
                System.out.println("Messages received from the whole swarm.");
            }
        }
    }

    @Override
    public void rxAcknowledgement(Acknowledgement a) {
        System.out.format("%6d: Received acknowledgement for message sent at %d by %d to %d at %d\n", a.m.hashCode(), a.m.timestamp, a.m.srcID, a.m.destID, a.timestamp);

        acknowledgements++;

        if (acknowledgeMap.containsKey(a.m.hashCode())) {
            acknowledgeMap.put(a.m.hashCode(), acknowledgeMap.get(a.m.hashCode()) + 1);
        } else {
            acknowledgeMap.put(a.m.hashCode(), 1);
        }

        if (acknowledgements >= swarmSize * swarmSize * totalMessageCount) {
            System.out.println("Message acknowledgements received from the whole swarm.");
        }
    }
}
