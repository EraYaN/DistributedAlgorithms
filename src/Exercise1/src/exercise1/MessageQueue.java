package exercise1;

import java.util.concurrent.PriorityBlockingQueue;

/**
 *
 * @author Erwin
 */
public class MessageQueue extends PriorityBlockingQueue<Message> {
    public MessageQueue(){
        super(1, new MessageComparator());
    }
}
