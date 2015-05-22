/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class Tag {
    Logger logger = LoggerFactory.getLogger(Tag.class);
    
    public final String id;
    public Map<String,Double> inh;   /** intensional inheritance */
    public String name;
    public String description;
    public Map<String,Object> meta = new HashMap();
    private String icon;

    public Tag(String id, String name) {
        this.id = id;
        this.name = name;
        this.description = null;
        this.inh = new HashMap();
    }
    
    public Tag meta(String key, Object value) {
        meta.put(key, value);
        return this;
    }


    public void setDescription(String d) {
        this.description = d;
    }

    public void icon(String icon) {
        this.icon = icon;
    }
    
    
}
