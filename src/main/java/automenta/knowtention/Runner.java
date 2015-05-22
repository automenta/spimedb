/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.knowtention;

/**
 * Channel which runs its own thread
 * @author me
 */
abstract public class Runner implements Runnable {

    public Thread start() {
        Thread t = new Thread(this);
        t.start();
        return t;
    }
    
    
}
