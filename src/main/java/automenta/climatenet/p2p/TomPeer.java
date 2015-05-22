/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet.p2p;

import automenta.climatenet.Spacetime;
import net.tomp2p.dht.FutureSend;
import net.tomp2p.dht.PeerDHT;
import net.tomp2p.futures.BaseFutureListener;
import net.tomp2p.futures.FutureDiscover;
import net.tomp2p.peers.Number160;
import net.tomp2p.peers.PeerAddress;
import net.tomp2p.rpc.ObjectDataReply;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author me
 */
public class TomPeer {
    public static final Random RND = new Random();
    
    public final PeerDHT peer;
    
    final int defaultSearchResults = 64;
    
    public List<Spacetime> db = new CopyOnWriteArrayList<>();

    
    public TomPeer(final PeerDHT peer) {
        this.peer = peer;
        
        peer.peer().objectDataReply(new ObjectDataReply() {
            @Override
            public Object reply(PeerAddress sender, Object request) throws Exception {
                try {
                    if (sender.peerId().equals(peer.peerID())) {
                        return null;
                    }
                    
                    //System.err.println("I'm " + peer.peerID() + " and I just got the message [" + request + "] from " + sender.peerId());
                    
                    //return handleMessage(sender, request);
                    return null;
                } catch (Exception e) {
                    return null;
                }
            }
        });
    }

    public FutureDiscover connect(String host, int port) throws UnknownHostException {
        return peer.peer().discover().inetSocketAddress(InetAddress.getByName(host), port).start();
    }
    
    public boolean connect(String host, int port, long timeout) throws UnknownHostException {
        FutureDiscover f = connect(host, port);
        f.awaitUninterruptibly(timeout);
        return f.isSuccess();
    }
    
    public void add(Spacetime e) {
        db.add(e);
    }
            
//    public Object handleMessage(PeerAddress sender, Object request) {
//        if (request instanceof String) {
//            //ASK
//
//            if (db.isEmpty()) return null;
//
//            QueryBuilder qb = QueryBuilders.queryString((String)request);
//
//            Map<String, Object> results = new HashMap();
//            for (Spacetime i : db) {
//                if (i instanceof ElasticSpacetime) {
//                    ElasticSpacetime e = (ElasticSpacetime) i;
//
//                    SearchResponse r = e.search(qb, 0, defaultSearchResults);
//                    for (SearchHit s : r.getHits()) {
//                        Object existing = results.put(s.getId(), s.sourceAsString());
//                        if (existing!=null) {
//                            //TODO handle duplicate values from multiple indices
//                        }
//                    }
//                }
//
//            }
//            if (!results.isEmpty())
//                return results;
//        }
//        return null;
//    }
    
    public FutureSend send(Object value) {
        return send(new Number160(RND), value);
    }
    public void send(String key, Object value) {
        sendBlocked(Number160.createHash(key), value);
    }
    public FutureSend send(Number160 key, Object value) {
        FutureSend futureSend = peer.send(key).object(value).start();
        return futureSend;
    }
    
    public Map<PeerAddress,Object> sendBlocked(Number160 key, Object value) {
        FutureSend futureSend = send(key, value);
        futureSend.awaitUninterruptibly();
        return futureSend.rawDirectData2();        
    }

    /** shutdown method */
    public void close() {
        for (Spacetime d : db) {
            d.close();
        }
    }
    
    public interface Answering {
        public void onAnswer(Map<PeerAddress,Object> x);
    }
    
    public void ask(String query, long timeout, final Answering a) {
        FutureSend f = send(query);
        f.addListener(new BaseFutureListener<FutureSend>() {

            @Override
            public void operationComplete(FutureSend f) throws Exception {
                a.onAnswer(f.rawDirectData2());
            }

            @Override
            public void exceptionCaught(Throwable thrwbl) throws Exception {
                System.out.println(" error: " + thrwbl);
            }
            
        });
        
        try {
            f.await(timeout);
        } catch (InterruptedException ex) {
            Logger.getLogger(TomPeer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }    
    /*
    private FutureGet get(String key, String domain, String content) {
    Number160 locationKey = Number160.createHash(key);
    Number160 domainKey = Number160.createHash(domain);
    Number160 contentKey = Number160.createHash(content);
    return peer.get(locationKey).domainKey(domainKey).contentKey(contentKey).start();
    }
    private FuturePut put(String key, String domain, String content, String data) throws IOException {
    Number160 locationKey = Number160.createHash(key);
    Number160 domainKey = Number160.createHash(domain);
    Number160 contentKey = Number160.createHash(content);
    MyData<String> myData = new MyData<String>().key(key).domain(domain).content(content).data(data);
    return peer.put(locationKey).domainKey(domainKey).data(contentKey, new Data(myData)).start();
    }
    private static class MyData<K> implements Serializable {
    private static final long serialVersionUID = 2098774660703812030L;
    private K key;
    private K domain;
    private K content;
    private K data;
    public K key() {
    return key;
    }
    public MyData<K> key(K key) {
    this.key = key;
    return this;
    }
    public Object domain() {
    return domain;
    }
    public MyData<K> domain(K domain) {
    this.domain = domain;
    return this;
    }
    public K content() {
    return content;
    }
    public MyData<K> content(K content) {
    this.content = content;
    return this;
    }
    public K data() {
    return data;
    }
    public MyData<K> data(K data) {
    this.data = data;
    return this;
    }
    }
     */

  
    
}
