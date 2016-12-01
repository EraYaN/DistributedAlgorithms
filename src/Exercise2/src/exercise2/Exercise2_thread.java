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
import java.rmi.RemoteException;
import java.util.Date;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Exercise2_thread implements Runnable, InstanceLookupInterface {

    Instance localInstance;
    InstanceMap remoteInstances;
    Exercise2 ex;

    Random rand;

    int delivered = 0;

    String historyFile;
    String logFile;

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
        rand = new Random();
        historyFile = String.format("history-%d.txt", localInstance.id);
        logFile = String.format("log-%d.txt", localInstance.id);
        Path fileToDeletePath = Paths.get(historyFile);
        try {
            Files.delete(fileToDeletePath);
        } catch (NoSuchFileException nsfe) {

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void DumpLog() {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(logFile, false), "utf-8"))) {
            for (String line : ex.log) {
                writer.write(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        for (int i = 0; i < totalMessageCount; i++) {
            ex.log.add(String.format("Sending message set %d of %d.\n", i + 1, totalMessageCount));
            DumpLog();
            BroadcastRequest();
            DumpLog();
            ex.log.add(String.format("Sent message set %d of %d.\n", i + 1, totalMessageCount));
            
            while(ex.criticalSections <= i) {
                try {
                    Thread.sleep(150);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
        while (ex.criticalSections < totalMessageCount || ex.rxReleases < totalMessageCount * localInstance.requestGroup.size()) {
            try {
                System.out.print('.');
                System.out.format("%d", ex.granted ? 1 : 0);
                DumpLog();
                Thread.sleep(250);

            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        ex.log.add("Done.\n");
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
