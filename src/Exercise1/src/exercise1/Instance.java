/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package exercise1;

import java.net.MalformedURLException;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 *
 * @author Erwin
 */
public final class Instance {
    public int id = 0;
    public int port = 0;
    public String host = null;
    public String project = null;
    public String name = null;
    public Exercise1_RMI object = null;
    
    public Instance(int ID, String Project, String Host, int Port, Exercise1_RMI Object){        
        id = ID;
        port = Port;
        host = Host;
        project = Project;
        object = Object;
    }
    
    public Instance(int ID, String Project, String Host, int Port){        
        this(ID,Project,Host,1099,null);
    }
        
    public Instance(int ID, String Project,String Host){        
        this(ID,Project,Host,1099);
    }
    
    public Instance(int ID, String Project){        
        this(ID,Project,"localhost");
    }
    
    public Instance(int ID){        
        this(ID,"P");
    }     
    
    public void FormatName() throws NullPointerException{
        if(id==0)
            throw new NullPointerException("ID");
        if(host==null)
            throw new NullPointerException("Host");
        if(port==0)
            throw new NullPointerException("Port");
        if(project==null)
            throw new NullPointerException("Project");
        
        name = String.format("rmi://%s:%d/%s_ID%d",host,port,project,id);
    }
     
    public void Bind() throws RemoteException,AlreadyBoundException,MalformedURLException{
        if(name==null)
            FormatName();
        if(object==null)
            throw new NullPointerException("Object");
        java.rmi.Naming.rebind(name, object);
    }
    
    public void Lookup() throws RemoteException,NotBoundException,MalformedURLException{
        if(name==null)
            FormatName();
        object = (Exercise1_RMI)java.rmi.Naming.lookup(name);        
    }
    
    public boolean HasObject(){
        return object != null;
    }
    
    //TODO implement boolean IsBound (should be true if the object is actually usable
}
