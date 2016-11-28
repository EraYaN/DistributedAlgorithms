package exercise1;

import java.rmi.RemoteException;

/**
 *
 * @author Erwin
 */
public class MessageBroker {

    InstanceMap instances;
    MessageQueue message;

    public MessageBroker(InstanceMap Instances) {
        instances = Instances;
        
    }

    public boolean Send(int ID, Message Msg) {
        if (!instances.containsKey(ID)) {
            //throw new ArgumentException();
            return false;
        }
        Instance destination = instances.get(ID);
        try {
            if (destination.HasObject()) {
                destination.object.rxMessage(Msg);
                return true;
            } else {
                return false;
            }
        } catch (RemoteException re) {
            re.printStackTrace();
            return false;
        }
    }
    
    

}
