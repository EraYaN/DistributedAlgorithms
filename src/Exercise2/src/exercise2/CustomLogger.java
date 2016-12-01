package exercise2;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

/**
 *
 * @author Erwin
 */
public class CustomLogger extends ArrayList<String>{
    
    SimpleDateFormat formatter = new SimpleDateFormat("HH:mm:ss.SS");
    @Override
    public boolean add(String s){ 
        System.out.print(s);
        return super.add(String.format("%s: %s", formatter.format(new Date()), s));        
    }
}
