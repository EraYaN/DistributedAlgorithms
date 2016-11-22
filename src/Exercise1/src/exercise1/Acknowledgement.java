
package exercise1;

public class Acknowledgement extends Message {

    Message m = null;

    public Acknowledgement(Message M, int Timestamp, Exercise1_RMI Sender) {
        super(M.destID, M.srcID, Timestamp, Sender);
        m = M;
    }
}
