package spimedb.server;

import spimedb.SpimeDB;

/**
 * Created by me on 3/4/17.
 */
public class Admin extends Session {

    public Admin(SpimeDB db) {
        super(db);
        set("me", new API());
    }

    public class API {

        public String version() {
            return SpimeDB.VERSION;
        }

        public Object stat() {
            return db.exe.summary();
        }

        public void rebuild() {
            db.clear(true);
        }

    }


}
