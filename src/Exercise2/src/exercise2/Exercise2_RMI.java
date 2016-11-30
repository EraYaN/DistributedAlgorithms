package exercise2;

import java.rmi.Remote;

public interface Exercise2_RMI extends Remote {

    public void rxRequest(Request r) throws java.rmi.RemoteException;

    public void rxGrant(Grant g) throws java.rmi.RemoteException;

    public void rxRelease(Release r) throws java.rmi.RemoteException;

    public void rxPostponed(Postponed p) throws java.rmi.RemoteException;

    public void rxInquire(Inquire i) throws java.rmi.RemoteException;

    public void rxRelinquish(Relinquish r) throws java.rmi.RemoteException;

}
