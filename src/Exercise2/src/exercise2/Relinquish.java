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
public class Relinquish extends Message implements Comparable<Message> {

    public Inquire i;

    public Relinquish(int SrcID, int Timestamp, Inquire I) {
        super(SrcID, Timestamp);
        i = I;
    }

    @Override
    public int hashCode() {
        int hash = super.hashCode();
        return hash * 7 + i.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Relinquish)) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        Relinquish rhs = (Relinquish) obj;
        return this.srcID == rhs.srcID
                && this.timestamp == rhs.timestamp
                && rhs.i == this.i;
    }
}
