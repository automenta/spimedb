/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package jnetention;

import automenta.climatenet.Core;
import com.google.common.base.Predicate;
import com.google.common.primitives.Longs;
import mjson.Json;
import org.hypergraphdb.HGHandle;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiConsumer;

/**
 *
 * @author me
 */
public interface NObject<O> extends Serializable, Map<String, O> {
        
//    public long createdAt;
//    //public long modifiedAt;
//    public String name;
//    public String author;
//
//    /** subject as in 'subject/predicate', not email or document */
//    private String subject;
    
//    public NObject() {
//        this("");
//    }
//
//    public NObject(String name) {
//        this(UUID(), name);
//    }
//
//    public NObject(String id, String name) {
//        this.name = name;
//        this.id = id;
//        this.createdAt = System.currentTimeMillis();
//        this.value = LinkedHashMultimap.create();
//    }

//    public void add(Tag tag) {
//        add(tag.toString());
//    }
//
//    public void add(String tag) {
//        add(tag, 1.0);
//    }
//
//    public void add(String tag, double strength) {
//        value.put(tag, strength);
//    }
//
//    public NObject add(String tag, Object v) {
//        value.put(tag, v);
//        return this;
//    }
//
//    @Override
//    public int compareTo(final Object o) {
//        if (o instanceof NObject) {
//            NObject n = (NObject)o;
//            return id.compareTo(n.id);
//        }
//        return -1;
//    }

    //
//    public Set<String> getTags() {
//        return Sets.newHashSet(iterateTags(true));
//    }
//
//    public Iterator<String> iterateTags(boolean includeObjectValues) {
//        Iterator<String> i = value.keySet().iterator();
//        if (includeObjectValues) {
//            i = concat(i, filter(transform(value.entries().iterator(), new Function<Map.Entry<String,Object>, String>() {
//
//                @Override
//                public String apply(Map.Entry<String, Object> e) {
//                    Object v = e.getValue();
//                    if (v instanceof Ref)
//                        return ((Ref)v).object;
//                    return null;
//                }
//
//            }), Predicates.notNull()));
//        }
//        return i;
//    }
//
    default public Set<String> getTags(final Predicate<String> p) {
        //TODO lazy allocate 's'
        Set<String> s = new HashSet();
        for (Map.Entry<String, O> v : entrySet()) {
            if (v.getValue() instanceof Double) {
                if (p.apply(v.getKey()))
                    s.add(v.getKey());
            }
        }
        return s;
    }
//
//    @Override
//    public String toString() {
//        return name;
//    }
//
//    public String toStringDetailed() {
//        return id + "," + name + "," + author + "," + subject + "," + new Date(createdAt).toString() + "=" + value;
//    }
//
//    public String toJSON() {
//        return Core.toJSON(this);
//    }
//
//    public <X> List<X> values(Class<X> c) {
//        List<X> x = new ArrayList();
//        for (Object o : value.values()) {
//            if (c.isInstance(o))
//                x.add((X)o);
//        }
//        return x;
//    }
//
//    public <X> X firstValue(Class<X> c) {
//        for (Object o : value.values()) {
//            if (c.isInstance(o))
//                return (X)o;
//        }
//        return null;
//    }

    default public boolean isClass() {
        return containsKey(Tag.tag);
    }
    default public boolean isProperty() {
        return containsKey(Tag.property);
    }    

    default public Map<String, Double> getTagStrengths() {
        Map<String,Double> s = new HashMap();
        for (Map.Entry<String, O> e: entrySet()) {
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



    /*public void setSubject(String id) {
        subject = id;
    }*/

    public static String getOrDefault(Map<String, Json> m, String key, String defaultValue) {
        Json x = m.get(key);
        if (x!=null)
            return x.toString();
        return defaultValue;
    }


    public static NObject from(HGHandle hgHandle, Map<String, Json> m) {
        String uuid = hgHandle.getPersistent().toString();
        return from(uuid, m);
    }

    public static NObject from(String uuid, Map<String, Json> m) {
        //String name = getOrDefault(m, "name", uuid);
        NObject n = new MapNObject(uuid, m);
//        for (Map.Entry<String,Json> e : m.entrySet()) {
//            String k = e.getKey();
//            Json v = e.getValue();
//            if (k.equals("name")) continue;
//            n.add(k, v);
//        }
        return n;
    }

    String id();

    default public String name() {
        Object n = get("name");
        if (n!=null) return n.toString();
        return id();
    }

    public static class HashNObject extends HashMap<String,Object> implements NObject<Object> {

        public final String id;

        public HashNObject(String id, String name) {
            this.id = id;
            put("name", name);

        }

        public HashNObject(String name) {
            this(Core.uuid(), name);
        }

        @Override
        public String id() {
            return id;
        }
    }

    public static class MapNObject<O> implements NObject<O>, Cloneable, Serializable {

        private final Map<String, O> map;
        private final String id;

        public MapNObject(String id, Map<String, O> adapted) {
            this.id = id;
            this.map = adapted;
        }

        @Override
        public String id() {
            return id;
        }

        public int size() {
            return map.size();
        }

        public boolean isEmpty() {
            return map.isEmpty();
        }

        public boolean containsValue(Object value) {
            return map.containsValue(value);
        }

        public boolean containsKey(Object key) {
            return map.containsKey(key);
        }

        public O get(Object key) {
            return map.get(key);
        }

        public O put(String key, O value) {
            return map.put(key, value);
        }

        public O remove(Object key) {
            return map.remove(key);
        }

        @Override
        public void putAll(Map<? extends String, ? extends O> m) {
            map.putAll(m);
        }


        public void clear() {
            map.clear();
        }

        public Set<String> keySet() {
            return map.keySet();
        }

        public Collection<O> values() {
            return map.values();
        }

        public Set<Entry<String, O>> entrySet() {
            return map.entrySet();
        }

        public boolean equals(Object o) {
            return map.equals(o);
        }

        public int hashCode() {
            return map.hashCode();
        }

        public String toString() {
            return map.toString();
        }



        public O getOrDefault(Object key, O defaultValue) {
            return map.getOrDefault(key, defaultValue);
        }

        public O putIfAbsent(String key, O value) {
            return map.putIfAbsent(key, value);
        }

        public boolean remove(Object key, Object value) {
            return map.remove(key, value);
        }

        public boolean replace(String key, O oldValue, O newValue) {
            return map.replace(key, oldValue, newValue);
        }

        public O replace(String key, O value) {
            return map.replace(key, value);
        }



        public void forEach(BiConsumer<? super String, ? super O> action) {
            map.forEach(action);
        }


    }
}
