/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package exercise1;

import java.rmi.server.UnicastRemoteObject;

/**
 *
 * @author Erwin
 */
public class Exercise1 extends UnicastRemoteObject
implements Exercise1_RMI{
    public void rxPacket(double t){        
        System.out.format("Received packet: %f",t);
    }
}
