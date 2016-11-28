package exercise1;

import java.util.Comparator;

/**
 *
 * @author Erwin
 */
public class MessageComparator implements Comparator<Message> {

    @Override
    public int compare(Message m1, Message m2) {
        if (m1 == m2) {
            return 0;
        }
        if (m1.timestamp < m2.timestamp) {
            return -1;
        } else if (m1.timestamp > m2.timestamp) {
            return 1;
        } else if (m1.srcID > m2.srcID) {
            return 1;
        } else if (m1.srcID < m2.srcID) {
            return -1;
        } else {
            return 0;
        }
    }
}
