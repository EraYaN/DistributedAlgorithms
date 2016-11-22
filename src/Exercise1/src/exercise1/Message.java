package exercise1;

import java.io.Serializable;

public class Message implements Serializable {

    public int srcID = 0;
    public int destID = 0;
    public int timestamp = 0;
    public Exercise1_RMI sender = null;
    private static final long serialVersionUID = 7526471155622776147L;

    public Message(int SrcID, int DestID, int Timestamp, Exercise1_RMI Sender) {
        srcID = SrcID;
        destID = DestID;
        timestamp = Timestamp;
        sender = Sender;
    }
}
