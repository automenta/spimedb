package automenta.netention.net.proxy;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

public class URLSensor implements Serializable {

    public String icon;
    @JsonIgnore
    public String id;
    public String name;
    public String url;
    public long lastUpdate;

    public URLSensor(String id, String name, String url, String icon) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.icon = icon;
    }

    //loads file or something
    void update() {

    }
}
