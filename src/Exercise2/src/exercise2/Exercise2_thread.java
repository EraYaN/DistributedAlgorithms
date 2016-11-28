package exercise2;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.rmi.AlreadyBoundException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Random;

public class Exercise2_thread implements Runnable, AcknowledgementCallback {

    Instance localInstance;
    Map<Integer, Instance> remoteInstances;
    Exercise2 ex;

    Random rand;

    int delivered = 0;

    int clk = 0;

    String historyFile;

    int totalMessageCount;
    private static final int MAX_DELAY = 1000;

    public Exercise2_thread(Instance LocalInstance, Map<Integer, Instance> RemoteInstances, int TotalMessageCount) throws RemoteException {
        totalMessageCount = TotalMessageCount;
        localInstance = LocalInstance;
        localInstance.object = ex = new Exercise2(localInstance.id, RemoteInstances.size(), totalMessageCount, this);
        localInstance.host = "localhost";
        try {
            localInstance.Bind();
        } catch (MalformedURLException | AlreadyBoundException | RemoteException e) {
            e.printStackTrace();
        }
        remoteInstances = RemoteInstances;
        rand = new Random(1);
        historyFile = String.format("history-%d.txt", localInstance.id);
        Path fileToDeletePath = Paths.get(historyFile);
        try {
            Files.delete(fileToDeletePath);
        } catch (NoSuchFileException nsfe) {

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        for (int i = 0; i < totalMessageCount; i++) {
            System.out.format("Sending message set %d of %d.\n", i + 1, totalMessageCount);

            CheckAndDeliver();

            Broadcast();
        }

        while (ex.acknowledgements < remoteInstances.size() * remoteInstances.size() * totalMessageCount || ex.packetsReceived < remoteInstances.size() * totalMessageCount || delivered < totalMessageCount * remoteInstances.size()) {
            try {
                CheckAndDeliver();

                Thread.sleep(25);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Done.");

    }

    private void RandomDelay() throws InterruptedException {
        Thread.sleep(rand.nextInt(MAX_DELAY));
    }

    private void CheckAndDeliver() {
        Message m = ex.Peek();
        if (m == null) {
            return;
        }
        if (ex.GetAcknowledgements(m) >= remoteInstances.size()) {
            Deliver();
        }
    }

    private void InitRemoteObject(Instance remoteInstance) {
        try {
            if (!remoteInstance.HasObject()) {
                if (localInstance.id != remoteInstance.id) {
                    remoteInstance.Lookup();
                } else {
                    remoteInstance.Bind();
                }
            }
        } catch (MalformedURLException | AlreadyBoundException | NotBoundException | RemoteException e) {
            e.printStackTrace();
        }
    }

    private void Deliver() {
        delivered++;
        Message m = ex.Remove();
        System.out.format("%6d: Delivered message received from %d at %d.\n", m.hashCode(), m.srcID, m.timestamp);

        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(historyFile, true), "utf-8"))) {
            writer.write(String.format("hashCode: %6d; Timestamp: %3d; Src: %3d; Dest: %3d\n", m.hashCode(), m.timestamp, m.srcID, m.destID));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void Broadcast() {
        try {
            RandomDelay();
        } catch (InterruptedException e) {

        }
        clk++;
        int current_clock = clk;
        remoteInstances.entrySet().forEach((Map.Entry<Integer, Instance> entry) -> {
                try {
                    Integer id = entry.getKey();
                    Instance remoteInstance = entry.getValue();

                    Message m = new Message(localInstance.id, id, current_clock, localInstance.object);

                    InitRemoteObject(remoteInstance);

                    try {
                        ((Exercise2_RMI) remoteInstance.object).rxMessage(m);
                    } catch (NoSuchObjectException nsoe) {
                        System.err.format("Connect lost to %d.\n", id);
                    }
                    System.out.format("%6d: Sent packet to %d at %d.\n", m.hashCode(), m.destID, m.timestamp);

                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            });
        

    }

    private void BroadcastAcknowledgement(Message m) {
        remoteInstances.entrySet().forEach((Map.Entry<Integer, Instance> entry) -> {
            try {
                Integer id = entry.getKey();
                Instance remoteInstance = entry.getValue();

                Acknowledgement a = new Acknowledgement(m, clk, ex);

                if (!remoteInstance.HasObject()) {
                    InitRemoteObject(remoteInstance);
                }
                try {
                    ((Exercise2_RMI) remoteInstance.object).rxAcknowledgement(a);
                } catch (NoSuchObjectException nsoe) {
                    System.err.format("Connect lost to %d.\n", id);
                }
                System.out.format("%6d: Sent acknowledgement for message to %d at %d.\n", m.hashCode(), id, clk);

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
        );
    }

    @Override
    public void acknowledgementCallback(Message m) {
        clk = Math.max(m.timestamp + 1, clk + 1);
        BroadcastAcknowledgement(m);
    }
}
