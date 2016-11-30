package exercise2;

import java.io.Serializable;

public class Message implements Serializable, Comparable<Message> {

    public int srcID = 0;
    public int timestamp = 0;
    private static final long serialVersionUID = 7526471155622776147L;

    public Message(int SrcID, int Timestamp) {
        srcID = SrcID;
        timestamp = Timestamp;
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
        if (!(obj instanceof Message)) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        Message rhs = (Message) obj;
        return this.srcID == rhs.srcID
                && this.timestamp == rhs.timestamp;
    }

    @Override
    public int compareTo(Message m2) {
        if (this == m2) {
            return 0;
        }
        if (this.timestamp < m2.timestamp) {
            return -1;
        } else if (this.timestamp > m2.timestamp) {
            return 1;
        } else if (this.srcID > m2.srcID) {
            return 1;
        } else if (this.srcID < m2.srcID) {
            return -1;
        } else {
            return 0;
        }
    }
}
