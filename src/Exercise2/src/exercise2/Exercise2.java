package exercise2;

import java.net.MalformedURLException;
import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

class Exercise2 extends UnicastRemoteObject implements Exercise2_RMI {

    int localID;
    int swarmSize;
    int totalMessageCount;

    boolean granted = false;
    boolean postponed = false;
    boolean inquiring = false;
    Request currentGrant;
    Inquire currentInquire;
    int numGrants = 0;

    InstanceIDArrayList requestGroup;

    RequestQueue requestQueue;

    int clk = 0;

    public int criticalSections = 0;
    public int releases = 0;

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
        clk++;
        requestGroup.forEach((Integer id) -> {
            try {
                Instance remoteInstance = lookupCallBack.LookupInstance(id);

                Request r = new Request(localID, clk);

                InitRemoteObject(remoteInstance);

                try {
                    ((Exercise2_RMI) remoteInstance.object).rxRequest(r);
                } catch (NoSuchObjectException nsoe) {
                    System.err.format("Connect lost to %d.\n", id);
                }
                System.out.format("%6d: Sent request to %d at %d.\n", r.hashCode(), id, r.timestamp);

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }

    public void txRelease() {
        requestGroup.forEach((Integer id) -> {
            try {
                Instance remoteInstance = lookupCallBack.LookupInstance(id);

                Release r = new Release(localID, clk);

                InitRemoteObject(remoteInstance);

                try {
                    ((Exercise2_RMI) remoteInstance.object).rxRelease(r);
                } catch (NoSuchObjectException nsoe) {
                    System.err.format("Connect lost to %d.\n", id);
                }
                System.out.format("%6d: Sent release to %d at %d.\n", r.hashCode(), id, r.timestamp);

            } catch (RemoteException e) {
                e.printStackTrace();
            }
        });
    }

    public void txGrant(Request r) throws RemoteException {
        Instance dest = lookupCallBack.LookupInstance(r.srcID);
        InitRemoteObject(dest);
        Grant g = new Grant(localID, clk, r);
        ((Exercise2_RMI) dest.object).rxGrant(g);
        System.out.format("%6d: Sent grant to %d at %d for request %d sent from %d at %d.\n", g.hashCode(), r.srcID, g.timestamp, r.hashCode(), r.srcID, r.timestamp);
    }

    public void txPostponed(Request r) throws RemoteException {
        Instance dest = lookupCallBack.LookupInstance(r.srcID);
        InitRemoteObject(dest);
        Postponed p = new Postponed(localID, clk, r);
        ((Exercise2_RMI) dest.object).rxPostponed(p);
        System.out.format("%6d: Sent postponed to %d at %d for request %d at %d.\n", p.hashCode(), r.srcID, p.timestamp, r.hashCode(), r.timestamp);
    }

    public void txInquire(Request r) throws RemoteException {
        Instance dest = lookupCallBack.LookupInstance(r.srcID);
        InitRemoteObject(dest);
        Inquire i = new Inquire(localID, clk, r);
        ((Exercise2_RMI) dest.object).rxInquire(i);
        System.out.format("%6d: Sent inquire to %d at %d for request %d sent at %d.\n", i.hashCode(), r.srcID, i.timestamp, r.hashCode(), r.timestamp);
    }

    public void txRelinquish(Inquire i) throws RemoteException {
        Instance dest = lookupCallBack.LookupInstance(i.srcID);
        InitRemoteObject(dest);
        Relinquish r = new Relinquish(localID, clk, i);
        ((Exercise2_RMI) dest.object).rxRelinquish(r);
        System.out.format("%6d: Sent relinquish to %d at %d for inquire %d sent at %d.\n", r.hashCode(), i.srcID, r.timestamp, i.hashCode(), i.timestamp);
    }

    private void CriticalSection() throws InterruptedException {
        System.out.format("Entered critical section at %d.\n", clk);
        Thread.sleep(100);
        System.out.format("Stopped critical section at %d.\n", clk);
        criticalSections++;
    }

    private void UpdateClk(int NewClk) {
        clk = Math.max(NewClk + 1, clk + 1);
    }

    @Override
    public void rxRequest(Request r) throws RemoteException {
        System.out.format("%6d: Received request from %d at %d.\n", r.hashCode(), r.srcID, r.timestamp);
        UpdateClk(r.timestamp);

        if (!granted) {
            currentGrant = r;
            txGrant(r);
            granted = true;
        } else {
            requestQueue.add(r);
            Request head = requestQueue.peek();
            if (currentGrant.compareTo(r) < 0 || head.compareTo(r) < 0) {
                txPostponed(r);
            } else if (!inquiring) {
                inquiring = true;
                txInquire(currentGrant);
            }
        }
    }

    @Override
    public void rxGrant(Grant g) {
        System.out.format("%6d: Received grant from %d at %d for request %d sent from %d at %d.\n", g.hashCode(), g.srcID, g.timestamp, g.r.hashCode(), g.r.srcID, g.r.timestamp);
        UpdateClk(g.timestamp);

        numGrants++;
        if (numGrants == requestGroup.size()) {
            postponed = false;
            try {
                CriticalSection();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
            txRelease();
        }
    }

    @Override
    public void rxRelease(Release r) throws RemoteException {
        System.out.format("%6d: Received release from %d at %d.\n", r.hashCode(), r.srcID, r.timestamp);
        UpdateClk(r.timestamp);
        granted = false;
        inquiring = false;

        if (!requestQueue.isEmpty()) {
            currentGrant = requestQueue.peek();
            txGrant(currentGrant);
            granted = true;
        } else {
            currentGrant = null;
        }

        releases++;
    }

    @Override
    public void rxPostponed(Postponed p) throws RemoteException {
        System.out.format("%6d: Received grant from %d at %d for request %d sent at %d.\n", p.hashCode(), p.srcID, p.timestamp, p.r.hashCode(), p.r.timestamp);
        UpdateClk(p.timestamp);

        postponed = true;
        if (currentInquire != null) {
            numGrants--;
            txRelinquish(currentInquire);
            currentInquire = null;
        }
    }

    @Override
    public void rxInquire(Inquire i) throws RemoteException {
        System.out.format("%6d: Received inquire from %d at %d for request %d sent at %d.\n", i.hashCode(), i.srcID, i.timestamp, i.r.hashCode(), i.r.timestamp);
        UpdateClk(i.timestamp);

        if (postponed) {
            numGrants--;
            txRelinquish(i);
        } else {
            currentInquire = i;
        }
    }

    @Override
    public void rxRelinquish(Relinquish r) throws RemoteException {
        System.out.format("%6d: Received relinquish from %d at %d for inquire %d sent at %d.\n", r.hashCode(), r.srcID, r.timestamp, r.i.hashCode(), r.i.timestamp);
        UpdateClk(r.timestamp);

        inquiring = false;
        granted = false;
        requestQueue.add(r.i.r);
        currentGrant = requestQueue.peek();
        granted = true;
        txGrant(currentGrant);
    }
}
