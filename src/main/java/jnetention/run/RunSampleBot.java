package jnetention.run;

import jnetention.NObject;
import jnetention.Self;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by me on 5/22/15.
 */
public class RunSampleBot {

    protected static void newBot(int port) {
        Self b = new Self() {
            protected NObject newAnonymousUser() {
                return newUser("John Doe" + NObject.UUID().substring(0,4));
            }
        };

        try {
            b.online(port);
            Thread.sleep(2000);

            b.connect("localhost", 10001);


            for (int i = 0; i < 3; i++) {
                b.publish(new NObject.HashNObject(Math.random() + "x"));
                Thread.sleep(1000);
            }

        } catch (Exception ex) {
            Logger.getLogger(RunMemoryPeer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) throws IOException {
        newBot(10010);

        System.in.read();
    }
}
