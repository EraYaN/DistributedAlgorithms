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
    
    public Acknowledgement(Message M, long Timestamp, int Sender) {
        super(Timestamp, Sender);
        m = M;     
    }
}
