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
public class Request extends Message implements Comparable<Message> {

    public Request(int SrcID, int Timestamp) {
        super(SrcID, Timestamp);
    }
}
