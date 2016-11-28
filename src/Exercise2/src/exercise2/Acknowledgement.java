
package exercise2;

public class Acknowledgement extends Message {

    Message m = null;

    public Acknowledgement(Message M, int Timestamp, Exercise2_RMI Sender) {
        super(M.destID, M.srcID, Timestamp, Sender);
        m = M;
    }
}
