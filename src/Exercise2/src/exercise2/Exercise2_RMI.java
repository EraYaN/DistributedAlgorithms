package exercise2;

import java.rmi.Remote;

public interface Exercise2_RMI extends Remote {

    public void rxMessage(Message m) throws java.rmi.RemoteException;

    public void rxAcknowledgement(Acknowledgement a) throws java.rmi.RemoteException;
}
