/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package jnetention;

import com.google.common.base.Predicate;
import java.util.List;
import java.util.Set;

/**
 * Tag = data class
 * @author me
 */
public class NTag extends NObject {

    static NTag asNObject(final Tag sysTag) {
        //TODO cache
        return new NTag(sysTag.name(), sysTag.name());
    }
    
    public String description;

    protected NTag(String id) {
        this(id, id);
    }
    
    protected NTag(String id, String name) {
        this(id, name, (String)null);
    }

    public NTag(String id, String name, String extend) {
        super(name, id);
        

        addDefaultTags();
        if (extend!=null)
            add(extend, 1.0);
        
    }
    
    protected void addDefaultTags() {
        add(Tag.tag.toString(), 1.0);        
    }
        
    public NTag(String id, String name, List<String> extend) {
        this(id, name, (String)null);
        
        for (String c : extend) {
            c = c.trim();
            if (c.length() == 0) continue;
            add(c, 1.0);            
        }
    }
    
    public void mergeFrom(NTag c) {
        //TODO
    }

    public Set<String> getSuperTags() {
        return getTags(new Predicate<String>() {
            @Override public boolean apply(String t) {
                return (!t.equals(Tag.tag.toString()));
            }
        });
    }

    
}
