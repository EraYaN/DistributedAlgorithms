/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package exercise2;

/**
 *
 * @author Erwin
 */
public class Grant extends Message implements Comparable<Message> {

    public Request r;

    public Grant(int SrcID, int Timestamp, Request R) {
        super(SrcID, Timestamp);
        r = R;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        return hash * 7 + r.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Grant)) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        Grant rhs = (Grant) obj;
        return this.srcID == rhs.srcID
                && this.timestamp == rhs.timestamp
                && rhs.r == this.r;
    }
}
