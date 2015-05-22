package automenta.climatenet.p2p;


import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * Created by me on 4/23/15.
 */
public class FileCache {

    private final DB db;
    private final HTreeMap<String, byte[]> cache;

    public FileCache(String path, int timeUnits, TimeUnit unit) {

        //init off-heap store with 2GB size limit
        db = DBMaker
                .newFileDB(new File(path))    //use off-heap memory, on-heap is `.memoryDB()`
                .cacheDisable()
                .compressionEnable()
                        .closeOnJvmShutdown()
                                //.mmapFileEnableIfSupported()
                //.transactionDisable()   //better performance
                .make();

        //There is also maximal size limit to prevent OutOfMemoryException
        cache = db
                .createHashMap("cache")
                .expireMaxSize(8129 * 1024)
                .expireAfterWrite(timeUnits, unit)
                .expireAfterAccess(timeUnits, unit)
                .makeOrGet();

//        //load stuff
//        for(int i = 0;i<100000;i++){
//            map.put(i, randomString(1000));
//        }
//

//        //one can monitor two space usage numbers:
//
//        //free space in store
//        long freeSize = Store.forDB(db).getFreeSize();
//
//        //current size of store (how much memory it has allocated
//        long currentSize = Store.forDB(db).getCurrSize();

        System.out.println("FileCache: " + this);
    }


    public byte[] get(String uri) {
        return cache.get(uri);
    }

    public void put(String uri, byte[] b) {
        cache.put(uri, b);

        db.commit();
    }

    @Override
    public String toString() {
        return (cache.keySet().toString());
    }
}
