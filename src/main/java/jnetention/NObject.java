/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package jnetention;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;
import static com.google.common.collect.Iterators.*;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Sets;
import com.google.common.primitives.Longs;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author me
 */
public class NObject extends Value implements Serializable, Comparable {
        
    public long createdAt;
    //public long modifiedAt;
    public String name;
    public String author;
    private String subject;
    
    public NObject() {
        this("");
    }
    
    public NObject(String name) {
        this(name, UUID());
    }
    
    public NObject(String name, String id) {
        this.name = name;
        this.id = id;
        this.createdAt = System.currentTimeMillis();
        this.value = LinkedHashMultimap.create();        
    }

    public void add(Tag tag) {
        add(tag.toString());
    }
    
    public void add(String tag) {
        add(tag, 1.0);
    }
    
    public void add(String tag, double strength) {
        value.put(tag, strength);
    }

    public void add(String tag, Object v) {
        value.put(tag, v);
    }
    
    @Override
    public int compareTo(final Object o) {
        if (o instanceof NObject) {
            NObject n = (NObject)o;
            return id.compareTo(n.id);
        }
        return -1;
    }

    @Override
    public int hashCode() {
        return id.hashCode(); 
    }
    
    public boolean hasTag(final Tag t) {
        return hasTag(t.toString());
    }
    
    public boolean hasTag(final String t) {
        return Iterators.contains(iterateTags(true), t);
    }

    public Set<String> getTags() {
        return Sets.newHashSet(iterateTags(true));
    }
    
    public Iterator<String> iterateTags(boolean includeObjectValues) {
        Iterator<String> i = value.keySet().iterator();
        if (includeObjectValues) {
            i = concat(i, filter(transform(value.entries().iterator(), new Function<Map.Entry<String,Object>, String>() {

                @Override
                public String apply(Map.Entry<String, Object> e) {
                    Object v = e.getValue();
                    if (v instanceof Ref)
                        return ((Ref)v).object;
                    return null;
                }

            }), Predicates.notNull()));
        }
        return i;
    }
    
    public Set<String> getTags(final Predicate<String> p) {
        Set<String> s = new HashSet();
        for (Map.Entry<String, Object> v : value.entries()) {
            if (v.getValue() instanceof Double) {
                if (p.apply(v.getKey()))
                    s.add(v.getKey());
            }
        }
        return s;        
    }    

    @Override
    public String toString() {
        return name;
    }
    
    public String toStringDetailed() {
        return id + "," + name + "," + author + "," + subject + "," + new Date(createdAt).toString() + "=" + value;
    }

    public <X> List<X> values(Class<X> c) {
        List<X> x = new ArrayList();
        for (Object o : value.values()) {
            if (c.isInstance(o))
                x.add((X)o);
        }
        return x;
    }

    public <X> X firstValue(Class<X> c) {
        for (Object o : value.values()) {
            if (c.isInstance(o))
                return (X)o;
        }        
        return null;        
    }

    public boolean isClass() {
        return hasTag(Tag.tag);
    }
    public boolean isProperty() {
        return hasTag(Tag.property);
    }    

    public Map<String, Double> getTagStrengths() {
        Map<String,Double> s = new HashMap();
        for (Map.Entry<String, Object> e: value.entries()) {
            if (e.getValue() instanceof Double) {
                //TODO calculate maximum value if repeating keys?
                s.put(e.getKey(), (Double)e.getValue());
            }
        }
        return s;
    }
    
    
    public static String UUID() {        
        long a = (long)(Math.random() * Long.MAX_VALUE);
        long b = (long)(Math.random() * Long.MAX_VALUE);
        
        return Base64.getEncoder().encodeToString( Longs.toByteArray(a) ).substring(0, 11) 
                + Base64.getEncoder().encodeToString( Longs.toByteArray(b) ).substring(0, 11);
    }



    public void setSubject(String id) {
        subject = id;
    }
    
}
