/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package automenta.netention.gui.browser.browser;

import javafx.scene.Node;
import javafx.scene.control.Tab;
import automenta.netention.Self;
import automenta.netention.gui.browser.navigation.History;

/**
 *
 * @author me
 */
public class UITab<N extends Node> extends Tab {
    public final Self core;
    private final N contnt;
    

    public UITab(Self c, N content) {
        super();
        this.core = c;
        this.contnt = content;
        
    }
    
    public N content() {
        return contnt;
    }    
    
    protected void init() {   }
    
    public History getHistory() {
        return null;
    }
    public void go(String loc) {
        //...
    }

    public String getLocation() {
        return "about:?";
    }
    
    
}
