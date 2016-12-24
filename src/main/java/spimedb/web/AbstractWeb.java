package spimedb.web;

import com.caucho.v5.json.JsonEngine;
import com.caucho.v5.json.JsonReader;

import javax.inject.Inject;
import java.io.IOException;
import java.io.StringReader;

/**
 * Created by me on 12/19/16.
 */
public class AbstractWeb {

    @Inject
    public JsonEngine JSON;


    public <X> X object(String json, Class<? extends X> c) {
        JsonReader jr = JSON.newReader();
        jr.init(new StringReader(json));
        try {
            return jr.readObject(c);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
