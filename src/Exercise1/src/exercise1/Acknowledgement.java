/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package exercise1;

/**
 *
 * @author Robin
 */
public class Acknowledgement extends Message {

    Message m = null;

    public Acknowledgement(Message M, long Timestamp, Exercise1_RMI Sender) {
        super(M.destID, M.srcID, Timestamp, Sender);
        m = M;
    }
}
