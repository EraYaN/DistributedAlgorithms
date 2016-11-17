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
public class Message {

    public long timestamp = 0;
    public int sender = 0;

    public Message(long Timestamp, int Sender) {
        timestamp = Timestamp;
        sender = Sender;
    }
}
