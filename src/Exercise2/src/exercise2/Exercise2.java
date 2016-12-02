package exercise2;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

class Exercise2 extends UnicastRemoteObject implements Exercise2_RMI {

    int localID;
    int swarmSize;
    int totalMessageCount;

    String criticalFile = "critical.txt";

    volatile boolean granted = false;
    volatile boolean postponed = false;
    volatile boolean inquiring = false;
    volatile boolean busy = false;
    Request currentGrant;
    volatile int numGrants;

    InstanceIDArrayList requestGroup;

    RequestQueue requestQueue;

    public CustomLogger log = new CustomLogger();

    int clk = 0;

    Object lockObject = new Object();

    public int criticalSections = 0;
    public int rxReleases = 0;

    InstanceLookupInterface lookupCallBack;

    public Exercise2(int LocalID, int SwarmSize, int TotalMessageCount, InstanceLookupInterface LookupCallBack, InstanceIDArrayList RequestGroup) throws RemoteException {
        localID = LocalID;
        swarmSize = SwarmSize;
        totalMessageCount = TotalMessageCount;
        lookupCallBack = LookupCallBack;
        requestGroup = RequestGroup;
        requestQueue = new RequestQueue();
    }

    private void InitRemoteObject(Instance remoteInstance) {
        try {
            if (!remoteInstance.HasObject()) {
                if (localID != remoteInstance.id) {
                    remoteInstance.Lookup();
                } else {
                    remoteInstance.Bind();
                }
            }
        } catch (MalformedURLException | AlreadyBoundException | NotBoundException | RemoteException e) {
            e.printStackTrace();
        }
    }

    public void txRequest() {
        //numGrants = 0;  
        int tempClk;
        synchronized (lockObject) {
            clk++;
            tempClk = clk;
        }
        requestGroup.forEach((Integer id) -> {
            try {
                Instance remoteInstance = lookupCallBack.LookupInstance(id);

                Request r = new Request(localID, tempClk);

                InitRemoteObject(remoteInstance);

                try {
                    ((Exercise2_RMI) remoteInstance.object).rxRequest(r);
                } catch (NoSuchObjectException nsoe) {
                    log.add(String.format("Connect lost to %d.\n", id));
                }
                log.add(String.format("%6d: Sent request to %d at %d.\n", r.hashCode(), id, r.timestamp));

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }

    public void txRelease() {
        int tempClk;
        synchronized (lockObject) {
            clk++;
            tempClk = clk;
        }
        requestGroup.forEach((Integer id) -> {
            try {
                Instance remoteInstance = lookupCallBack.LookupInstance(id);

                Release r = new Release(localID, tempClk);

                InitRemoteObject(remoteInstance);
                log.add(String.format("%6d: Sending release to %d at %d.\n", r.hashCode(), id, r.timestamp));
                try {
                    ((Exercise2_RMI) remoteInstance.object).rxRelease(r);
                } catch (NoSuchObjectException nsoe) {
                    log.add(String.format("Connect lost to %d.\n", id));
                }
                log.add(String.format("%6d: Sent release to %d at %d.\n", r.hashCode(), id, r.timestamp));

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }

    public void txGrant(Request r) throws RemoteException {
        Grant g = new Grant(localID, clk, r);
        log.add(String.format("%6d: Sending grant to %d at %d for request %d sent from %d at %d.\n", g.hashCode(), r.srcID, g.timestamp, r.hashCode(), r.srcID, r.timestamp));
        Instance dest = lookupCallBack.LookupInstance(r.srcID);
        InitRemoteObject(dest);
        ((Exercise2_RMI) dest.object).rxGrant(g);
        log.add(String.format("%6d: Sent grant to %d at %d for request %d sent from %d at %d.\n", g.hashCode(), r.srcID, g.timestamp, r.hashCode(), r.srcID, r.timestamp));
    }

    public void txPostponed(Request r) throws RemoteException {
        Postponed p = new Postponed(localID, clk, r);
        log.add(String.format("%6d: Sending postponed to %d at %d for request %d at %d.\n", p.hashCode(), r.srcID, p.timestamp, r.hashCode(), r.timestamp));
        Instance dest = lookupCallBack.LookupInstance(r.srcID);
        InitRemoteObject(dest);
        ((Exercise2_RMI) dest.object).rxPostponed(p);
        log.add(String.format("%6d: Sent postponed to %d at %d for request %d at %d.\n", p.hashCode(), r.srcID, p.timestamp, r.hashCode(), r.timestamp));
    }

    public void txInquire(Request r) throws RemoteException {
        Inquire i = new Inquire(localID, clk, r);
        log.add(String.format("%6d: Sending inquire to %d at %d for request %d sent at %d.\n", i.hashCode(), r.srcID, i.timestamp, r.hashCode(), r.timestamp));
        Instance dest = lookupCallBack.LookupInstance(r.srcID);
        InitRemoteObject(dest);
        ((Exercise2_RMI) dest.object).rxInquire(i);
        log.add(String.format("%6d: Sent inquire to %d at %d for request %d sent at %d.\n", i.hashCode(), r.srcID, i.timestamp, r.hashCode(), r.timestamp));
    }

    public void txRelinquish(Inquire i) throws RemoteException {
        Relinquish r = new Relinquish(localID, clk, i);
        log.add(String.format("%6d: Sending relinquish to %d at %d for inquire %d sent at %d.\n", r.hashCode(), i.srcID, r.timestamp, i.hashCode(), i.timestamp));
        Instance dest = lookupCallBack.LookupInstance(i.srcID);
        InitRemoteObject(dest);
        ((Exercise2_RMI) dest.object).rxRelinquish(r);
        log.add(String.format("%6d: Sent relinquish to %d at %d for inquire %d sent at %d.\n", r.hashCode(), i.srcID, r.timestamp, i.hashCode(), i.timestamp));
    }

    private void WriteCriticalFile(String str) {
        try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(criticalFile, true), "utf-8"))) {

            writer.write(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void CriticalSection(int id) throws InterruptedException {
        log.add(String.format("Entered critical section for request %d at %d.\n", id, clk));
        WriteCriticalFile(String.format("\nID %d: Entered critical section for request %d at %d.", localID, id, clk));
        for (int i = 0; i < 3; i++) {
            Thread.sleep(125);
            log.add(String.format("Processing %d out of %d at %d for %d.\n", i, 3, clk, id));
            Thread.sleep(125);
        }
        WriteCriticalFile(String.format(" Exited.\n", localID, id, clk));
        log.add(String.format("Stopped critical section for request %d at %d.\n", id, clk));
        criticalSections++;
    }

    private void UpdateClk(int NewClk) {
        clk = Math.max(NewClk + 1, clk + 1);
    }

    public void CheckGrants(Grant g) {
        boolean do_release = false;
        synchronized (lockObject) {
            if (numGrants >= requestGroup.size() && busy == false) {
                postponed = false;
                busy = true;
                try {
                    CriticalSection(g.r.hashCode());
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
                do_release = true;
                
                numGrants = Math.max(0, numGrants - requestGroup.size());//Don't own these anymore, used some grants.
                log.add(String.format("%6d: Consumed %d grants from the received grants.\n", g.hashCode(), requestGroup.size()));
                busy = false;
            }
        }
        if (do_release) {
            txRelease();
        }
    }

    @Override
    public void rxRequest(Request r) throws RemoteException {
        log.add(String.format("%6d: Receiving request from %d at %d.\n", r.hashCode(), r.srcID, r.timestamp));
        boolean do_grant = false;
        boolean do_inquire = false;
        boolean do_postponed = false;
        synchronized (lockObject) {
            UpdateClk(r.timestamp);
            if (!granted && !busy) {
                currentGrant = r;
                do_grant = true;
                granted = true;
            } else {
                //log.add(String.format("%6d: Postponed request.\n", r.hashCode()));
                requestQueue.add(r);
                Request head = requestQueue.peek();
                //System.out.format("%s; %s; %s\n", r, head, currentGrant);
                if (currentGrant.compareTo(r) < 0 || head.compareTo(r) < 0) {
                    do_postponed = true;
                } else if (!inquiring) {
                    inquiring = true;
                    do_inquire = true;
                }
            }
        }

        if (do_grant) {
            txGrant(r);
        }
        if (do_inquire) {
            txInquire(currentGrant);
        }
        if (do_postponed) {
            txPostponed(r);
        }

        log.add(String.format("%6d: Received request from %d at %d.\n", r.hashCode(), r.srcID, r.timestamp));
    }

    @Override
    public void rxGrant(Grant g) {
        log.add(String.format("%6d: Receiving grant from %d at %d for request %d sent from %d at %d.\n", g.hashCode(), g.srcID, g.timestamp, g.r.hashCode(), g.r.srcID, g.r.timestamp));
        synchronized (lockObject) {
            UpdateClk(g.timestamp);
            numGrants++;
        }
        CheckGrants(g);
        log.add(String.format("%6d: Received grant from %d at %d for request %d sent from %d at %d.\n", g.hashCode(), g.srcID, g.timestamp, g.r.hashCode(), g.r.srcID, g.r.timestamp));
    }

    @Override
    public void rxRelease(Release r) throws RemoteException {
        log.add(String.format("%6d: Receiving release from %d at %d.\n", r.hashCode(), r.srcID, r.timestamp));
        boolean do_grant = false;
        synchronized (lockObject) {
            UpdateClk(r.timestamp);
            granted = false;
            inquiring = false;
            //requestQueue.remove(currentGrant);
            rxReleases++;

            if (!requestQueue.isEmpty()) {
                currentGrant = requestQueue.poll();
                if (currentGrant != null) {
                    do_grant = true;
                    granted = true;
                }
            }
        }
        if (do_grant) {
            txGrant(currentGrant);
        }
        log.add(String.format("%6d: Received release from %d at %d.\n", r.hashCode(), r.srcID, r.timestamp));
    }

    @Override
    public void rxPostponed(Postponed p) throws RemoteException {
        log.add(String.format("%6d: Receiving postponed from %d at %d for request %d sent at %d.\n", p.hashCode(), p.srcID, p.timestamp, p.r.hashCode(), p.r.timestamp));
        synchronized (lockObject) {
            UpdateClk(p.timestamp);
            postponed = true;
        }
        log.add(String.format("%6d: Received postponed from %d at %d for request %d sent at %d.\n", p.hashCode(), p.srcID, p.timestamp, p.r.hashCode(), p.r.timestamp));
    }

    @Override
    public void rxInquire(Inquire i) throws RemoteException {
        log.add(String.format("%6d: Receiving inquire from %d at %d for request %d sent at %d.\n", i.hashCode(), i.srcID, i.timestamp, i.r.hashCode(), i.r.timestamp));

        synchronized (lockObject) {
            UpdateClk(i.timestamp);

        }
        //while (!postponed && numGrants != requestGroup.size()) {
        while (!postponed && !busy) {
            try {
                log.add("Waiting in Inquire.\n");
                Thread.sleep(250);
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
        boolean do_inquire = false;
        synchronized (lockObject) {
            if (postponed) {
                numGrants--;
                do_inquire = true;
            }
        }
        if (do_inquire) {
            txRelinquish(i);
        }
        log.add(String.format("%6d: Received inquire from %d at %d for request %d sent at %d.\n", i.hashCode(), i.srcID, i.timestamp, i.r.hashCode(), i.r.timestamp));
    }

    @Override
    public void rxRelinquish(Relinquish r) throws RemoteException {
        log.add(String.format("%6d: Receiving relinquish from %d at %d for inquire %d sent at %d.\n", r.hashCode(), r.srcID, r.timestamp, r.i.hashCode(), r.i.timestamp));
        boolean do_grant = false;
        synchronized (lockObject) {
            UpdateClk(r.timestamp);

            inquiring = false;
            requestQueue.add(r.i.r);
            currentGrant = requestQueue.poll();
            if (currentGrant != null) {
                granted = true;
                do_grant = true;
            }
        }
        if (do_grant) {
            txGrant(currentGrant);
        }
        log.add(String.format("%6d: Received relinquish from %d at %d for inquire %d sent at %d.\n", r.hashCode(), r.srcID, r.timestamp, r.i.hashCode(), r.i.timestamp));
    }
}
