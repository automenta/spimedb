/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package jnetention.run;

import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import static javafx.application.Application.launch;
import jnetention.Self;
import jnetention.NObject;
import jnetention.gui.NetentionJFX;

/**
 *
 * @author me
 */
public class RunMemoryPeer extends NetentionJFX {

    @Override
    protected Self newCore(Application.Parameters p) {
        Self c = new Self();
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    c.online(10001);
                }
                catch (Exception e) {
                    System.err.println(e);
                    System.exit(1);
                }
            }
            
        }).start();
        return c;
    }
    
    protected static void addBot() {
        Self b = new Self();
        try {
            b.online(10010);
            Thread.sleep(2000);
            
            b.connect("localhost",10001);
            
            
            
            do {
                b.publish(new NObject(Math.random() + "x"));
                Thread.sleep(1000);
            }        
            while (true);
            
        } catch (Exception ex) {
            Logger.getLogger(RunMemoryPeer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static void main(String[] args) {
        new Thread(new Runnable() {
            @Override public void run() {
                addBot();
            }            
        }).start();

        launch();        
        
    }    

}
