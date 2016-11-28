package exercise2;

import java.io.Serializable;

public class Message implements Serializable {

    public int srcID = 0;
    public int destID = 0;
    public int timestamp = 0;
    public Exercise2_RMI sender = null;
    private static final long serialVersionUID = 7526471155622776147L;

    public Message(int SrcID, int DestID, int Timestamp, Exercise2_RMI Sender) {
        srcID = SrcID;
        destID = DestID;
        timestamp = Timestamp;
        sender = Sender;
    }
    
    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * 3 + srcID;
        hash = hash * 5 + timestamp;
        return hash;       
    }

    @Override
    public boolean equals(Object obj) {
       if (!(obj instanceof Message))
            return false;
        if (obj == this)
            return true;

        Message rhs = (Message) obj;
        return this.srcID == rhs.srcID &&
                this.timestamp == rhs.timestamp;
    }
}
