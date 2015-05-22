package automenta.climatenet.p2p;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

/**
 * Created by me on 4/23/15.
 */
public class TimePoint implements Serializable {
    public long at;

    public TimePoint(long at) {
        this.at = at;
    }

    @JsonIgnore
    public boolean isInstant() {
        return true;
    }

    @JsonIgnore
    public long getStart() {
        return at;
    }


}
