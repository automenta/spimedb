/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.netention;

import automenta.netention.net.GossipPeer;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import javafx.application.Platform;
import nars.NAR;
import nars.model.impl.Default;
import org.apache.commons.math3.stat.Frequency;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;


/**
 * Unifies DB & P2P features
 */
public class Self extends nars.util.event.EventEmitter.DefaultEventEmitter {
    //private final static String Session_MYSELF = "myself";

    public GossipPeer net;

    public final NAR nar = new NAR(new Default());

    public static class SaveEvent {
        public final NObject object;
        public SaveEvent(NObject object) { this.object = object;        }
    }
    public static class NetworkUpdateEvent {        
        
    }

    public static class HG {
        //dummy class
    }
    
    public final HG db;
    
    private NObject myself;

    /** in-memory Database */
    public Self() {
        this(new HG());
    }
    
    /** file Database */
    public Self(String filePath) {
        this(/*new HG(filePath)*/);
    }
    
    public Self(HG db) {
        
        
        
        
        this.db = db;

        //if (session.get(Session_MYSELF)==null) {
        {
            //first time user
            become(newAnonymousUser());
        }

        
        //    map.put(1, "one");
        //    map.put(2, "two");
        //    // map.keySet() is now [1,2]
        //
        //    db.commit();  //persist changes into disk
        //
        //    map.put(3, "three");
        //    // map.keySet() is now [1,2,3]
        //    db.rollback(); //revert recent changes
        //    // map.keySet() is now [1,2]
        //
        //    db.close();
    }

    protected NObject newAnonymousUser() {
        return newUser("Anonymous " + NObject.UUID().substring(0,4));
    }



    public Self online(int listenPort) throws IOException, UnknownHostException, SocketException, InterruptedException {
        net = new GossipPeer(listenPort) {

            @Override
            public void onUpdate(UUID id, JsonNode j) {
                super.onUpdate(id, j);
                //db.add(j);
                //db.print(System.out);
                System.err.println(j);
            }

        };
        
        return this;
    }
    
    protected void broadcastSelf() {
        if (myself!=null)
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    broadcast(myself);
                }
            });        
    }
    
    public void connect(String host, int port) throws UnknownHostException {
        net.connect(host, port);

    }

    
    /*public Core offline() {
        
        return this;
    }*/

    public Iterable<NObject> netValues() {
        return Collections.EMPTY_LIST;
//        
//        //dht.storageLayer().checkTimeout();
//        return Iterables.filter(Iterables.transform(dht.storageLayer().get().values(), 
//            new Function<Data,NObject>() {
//                @Override public NObject apply(final Data f) {
//                    try {
//                        final Object o = f.object();
//                        if (o instanceof NObject) {
//                            NObject n = (NObject)o;
//                            
//                            if (data.containsKey(n.id))
//                                return null;                                
//                            
//                            /*System.out.println("net value: " + f.object() + " " + f.object().getClass() + " " + data.containsKey(n.id));*/
//                            return n;
//                        }
//                        /*else {
//                            System.out.println("p: " + o + " " + o.getClass());
//                        }*/
//                        /*else if (o instanceof String) {
//                            Object p = dht.get(Number160.createHash((String)o));
//                            System.out.println("p: " + p + " " + p.getClass());
//                        }*/
//                        return null;
//                    } catch (Exception ex) {
//                        ex.printStackTrace();
//                        return null;
//                    }
//                }                
//        }), Predicates.notNull());        
    }
    
    public Iterator<NObject> allValues() {
        /*if (net!=null) {
            return Iterables.concat(data.values(), netValues());
        }
        else {
            return data.values();
        }*/
        //return db.allValues();
        return Iterators.emptyIterator();
    }
    
    public Iterator<NObject> tagged(final String tagID) {
        return Iterators.filter(allValues(), new Predicate<NObject>() {
            @Override
            public boolean apply(final NObject o) {
                if (o == null) return false;
                //return Iterators.contains(iterateTags(true), t);
                return o.containsKey(tagID);
            }
        });        
    }    
    public Iterator<NObject> tagged(final String tagID, final String author) {
        return Iterators.filter(allValues(), new Predicate<NObject>(){
            @Override public boolean apply(final NObject o) {
                if (o == null) return false;
                if (author!=null)
                    if (!author.equals(o.get("author")))
                        return false;
                //return Iterators.contains(iterateTags(true), t);
                return o.containsKey(tagID);
            }
        });        
    }

    public List<NObject> getUsers() {        
        return Lists.newArrayList(tagged(Tag.User));
    }
    
    public List<NObject> getSubjects() {        
        //TODO list all possible subjects, not just users
        return getUsers();
    }
    
    public List<NObject> getTags() {         
        List<NObject> c = Lists.newArrayList(tagged(Tag.tag));
        
        //for (Tag sysTag : Tag.values())
          //  c.add(NTag.asNObject(sysTag));
        
        return c;
    }
    
    public NObject newUser(String name) {
        NObject n = new NObject.HashNObject.HashNObject(name);
        n.put("author",  n.id());
        n.put(Tag.User, null);
        n.put(Tag.Human, null);
        Object v = new SpacePoint(40, -80);
        n.put("@", v);
        publish(n);
        return n;
    }
    
    /** creates a new anonymous object, but doesn't publish it yet */
    public NObject newAnonymousObject(String name) {
        NObject n = new NObject.HashNObject.HashNObject(name);
        return n;
    }
    
    /** creates a new object (with author = myself), but doesn't publish it yet */
    public NObject newObject(String name) {
        if (myself==null)
            throw new RuntimeException("Unidentified; can not create new object");
        
        NObject n = new NObject.HashNObject.HashNObject(name);
        n.put("author", myself.id());
        return n;
    }
    
    public void become(NObject user) {
        //db.add(user);

        //System.out.println("Become: " + user);
        myself = user;
        //session.put(Session_MYSELF, user.id);
    }

    
    public void remove(String nobjectID) {
        //data.remove(nobjectID);
    }
    
    public void remove(NObject x) {
        remove(x.id());
    }


    
   

    
    /** save nobject to database */
    public void save(NObject x) {
        //NObject removed = data.put(x.id, x);
        //index(removed, x);
        //db.add(x);
        index(null, x);
        
        emit(SaveEvent.class, x);
    }
    
    /** batch save nobject to database */    
    public void save(Iterable<NObject> y) {
        for (NObject x : y) {
            //NObject removed = data.put(x.id, x);
            //index(removed, x);

            //db.add(x);
            index(null, x);
        }            
        emit(SaveEvent.class);
    }

    
    public void broadcast(NObject x) {
        broadcast(x, false);
    }

    public synchronized void broadcast(NObject x, boolean block) {
        if (net!=null) {
            System.err.println("broadcasting " + x);
            net.put(x); //"obj0", x.toJSON());
//            try {
//                
//                    
//            }
//            catch (IOException e) {
//                System.err.println("publish: " + e);
//            }
        }        
    }
    
    /** save to database and publish in DHT */
    public void publish(NObject x, boolean block) {
        save(x);
    
        broadcast(x, block);
        
        
        //TODO save to geo-index
    }
    public void publish(NObject x) {
        publish(x, false);        
    }
    
    /*
    public int getNetID() {
        if (net == null)
            return -1;
        return net.
    }
    */

    public NObject getMyself() {
        return myself;
    }

    protected <O> void index(NObject<O> previous, NObject<O> o) {
        if (previous!=null) {
            if (previous.isClass()) {
                
            }
        }
        
        if (o!=null) {
            
            if ((o.isClass()) || (o.isProperty())) {

                //TODO index the 'inh'

                /*
                for (Map.Entry<String, O> e : o.entrySet()) {
                    String superclass = e.getKey();
                    if (superclass.equals("tag"))
                        continue;
                    
                    if (get(superclass)==null) {
                        System.out.println("saving superclass tag: " + superclass);
                        save(new NTag(superclass));
                    }
                    
                }*/
                
            }
            
        }
        
    }

    

    public static Frequency tokenBag(String x, int minLength, int maxTokenLength) {
        String[] tokens = tokenize(x);
        Frequency f = new Frequency();
        for (String t : tokens) {
            if (t==null) continue;
            if (t.length() < minLength) continue;
            if (t.length() > maxTokenLength) continue;
            t = t.toLowerCase();
            f.addValue(t);            
        }
        return f;
    }

    public static String[] tokenize(String value) {
            String v = value.replaceAll(","," \uFFEB ").
                        replaceAll("\\."," \uFFED").
                        replaceAll("\\!"," \uFFED").  //TODO alternate char
                        replaceAll("\\?"," \uFFED")   //TODO alternate char
                    ;
            return v.split(" ");
        }    
    

    public Object get(final String id) {
        //return db.get(id);
        return null;
//        NObject tag = data.get(tagID);
//        if (tag!=null && tag.isClass())
//            return tag;
    }

    public Iterable<NObject> getTagRoots() {
        return Iterables.filter(getTags(), new Predicate<NObject>() {
            @Override public boolean apply(NObject t) {
                //try {
                if (t instanceof NTag) {
                    NTag tag = (NTag) t;
                    return tag.getSuperTags().isEmpty();
                }
                return false;
                /*}
                catch (Exception e) { }
                return false;*/
            }            
        });
    }
    
}
