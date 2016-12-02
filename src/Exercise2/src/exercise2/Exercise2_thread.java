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
import java.util.Random;

public class Exercise2_thread implements Runnable, InstanceLookupInterface {

    Instance localInstance;
    InstanceMap remoteInstances;
    Exercise2 ex;

    Random rand;

    int delivered = 0;

    String historyFile;
    String logFile;

    int totalMessageCount;
    private static final int MAX_DELAY = 500;

    public Exercise2_thread(Instance LocalInstance, InstanceMap RemoteInstances, int TotalMessageCount) throws RemoteException {
        totalMessageCount = TotalMessageCount;
        localInstance = LocalInstance;
        localInstance.object = ex = new Exercise2(localInstance.id, RemoteInstances.size(), totalMessageCount, this, localInstance.requestGroup);
        Path fileToDeletePath = Paths.get(ex.criticalFile);
        try {
            Files.delete(fileToDeletePath);
        } catch (NoSuchFileException nsfe) {

        } catch (IOException e) {
            e.printStackTrace();
        }
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
        fileToDeletePath = Paths.get(historyFile);
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

    private void PrintStatusMessage() {
        PrintStatusMessage("");
    }

    private void PrintStatusMessage(String prefix) {
        System.out.format("%sClk: %d; In Queue: %d; Queue Head: %d@%d; Current Grant: %d@%d; Owned Grants: %d; Crit Sec: %d; rxReleases: %d\n",
                prefix, ex.clk, ex.requestQueue.size(), ex.requestQueue.peek() != null ? ex.requestQueue.peek().srcID : 0,
                ex.requestQueue.peek() != null ? ex.requestQueue.peek().timestamp : 0, 
                ex.granted ? ex.currentGrant.srcID : 0,
                ex.granted ? ex.currentGrant.timestamp : 0,
                ex.numGrants, ex.criticalSections, ex.rxReleases);
    }

    @Override
    public void run() {
        for (int i = 0; i < totalMessageCount; i++) {
            ex.log.add(String.format("Sending message set %d of %d.\n", i + 1, totalMessageCount));
            DumpLog();
            BroadcastRequest();
            DumpLog();
            ex.log.add(String.format("Sent message set %d of %d.\n", i + 1, totalMessageCount));
            while (ex.criticalSections <= i) {
                try {
                    System.out.print(i);
                    PrintStatusMessage("[F] ");
                    Thread.sleep(1000);
                } catch (InterruptedException intex) {
                    intex.printStackTrace();
                }
            }
        }
        while (ex.criticalSections < totalMessageCount || ex.rxReleases < totalMessageCount * localInstance.requestGroup.size() || ex.requestQueue.size() > 0) {
            try {
                //System.out.print('.');
                //ex.CheckGrants();
                PrintStatusMessage("[W] ");
                DumpLog();
                Thread.sleep(1000);

            } catch (InterruptedException intex) {
                intex.printStackTrace();
                //} catch (RemoteException ex) {
                //    ex.printStackTrace();
            }
        }
        
        PrintStatusMessage("[Final] ");
        try {
            Thread.sleep(1000); //Grace period
        } catch (InterruptedException intex) {
            intex.printStackTrace();
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
