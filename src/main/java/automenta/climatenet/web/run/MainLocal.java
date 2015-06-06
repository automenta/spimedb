/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet.web.run;

/**
 *
 * @author me
 */
public class MainLocal {
 
    public static void main(String[] args) throws Exception {
         //-espath=cache/es1 -webport=9090
        Main.main(new String[]{"-espath=cache/es1", "-webport=9090"});
    }
}
