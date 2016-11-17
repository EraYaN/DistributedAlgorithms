/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package exercise1;

import java.rmi.Remote;

/**
 *
 * @author Erwin
 */
public interface Exercise1_RMI extends Remote {
    public void rxPacket(double t) throws java.rmi.RemoteException;
}
