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

public class Exercise2_thread implements Runnable, InstanceLookupInterface {

    Instance localInstance;
    InstanceMap remoteInstances;
    Exercise2 ex;

    Random rand;

    int delivered = 0;

    String historyFile;

    int totalMessageCount;
    private static final int MAX_DELAY = 1000;

    public Exercise2_thread(Instance LocalInstance, InstanceMap RemoteInstances, int TotalMessageCount) throws RemoteException {
        totalMessageCount = TotalMessageCount;
        localInstance = LocalInstance;
        localInstance.object = ex = new Exercise2(localInstance.id, RemoteInstances.size(), totalMessageCount, this, localInstance.requestGroup);
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
            BroadcastRequest();
        }

        while (ex.criticalSections < totalMessageCount || ex.releases < totalMessageCount * localInstance.requestGroup.size()) {

        }
        System.out.println("Done.");
    }

    private void RandomDelay() throws InterruptedException {
        Thread.sleep(rand.nextInt(MAX_DELAY));
    }

    private void BroadcastRequest() {
        try {
            RandomDelay();
        } catch (InterruptedException e) {

        }
        ex.txRequest();
    }

    @Override
    public Instance LookupInstance(int ID) {
        if (!remoteInstances.containsKey(ID)) {
            return null; //TODO Ex
        }
        return remoteInstances.get(ID);
    }
}
