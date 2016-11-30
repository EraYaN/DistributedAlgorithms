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
    Request current_grant;
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
        System.out.format("%6d: Sent postponed to %d at %d for request %d sent from %d at %d.\n", p.hashCode(), p.srcID, p.timestamp, r.hashCode(), r.srcID, r.timestamp);
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
            current_grant = r;
            txGrant(r);
            granted = true;
        } else {
            requestQueue.add(r);
            Request head = requestQueue.peek();
            if (current_grant.compareTo(r) < 0 || head.compareTo(r) < 0) {

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
        current_grant = null;
        releases++;
    }

    @Override
    public void rxPostponed(Postponed p) throws RemoteException {
        System.out.format("%6d: Received grant from %d at %d for request %d sent from %d at %d.\n", p.hashCode(), p.srcID, p.timestamp, p.r.hashCode(), p.r.srcID, p.r.timestamp);
        postponed = true;
    }

}
