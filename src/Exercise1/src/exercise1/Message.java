/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package exercise1;

import java.io.Serializable;

/**
 *
 * @author Robin
 */
public class Message implements Serializable {

    public int srcID = 0;
    public int destID = 0;
    public long timestamp = 0;
    public Exercise1_RMI sender = null;
    private static final long serialVersionUID = 7526471155622776147L;

    public Message(int SrcID, int DestID, long Timestamp, Exercise1_RMI Sender) {
        srcID = SrcID;
        destID = DestID;
        timestamp = Timestamp;
        sender = Sender;
    }
}
