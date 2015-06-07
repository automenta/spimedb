/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package automenta.netention.run;

import javafx.application.Application;
import automenta.netention.Self;
import automenta.netention.gui.NetentionJFX;

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

    
    public static void main(String[] args) {


        launch();        
        
    }    

}
