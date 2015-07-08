package automenta.netention.net;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Created by me on 4/23/15.
 */
public class TimePoint  {
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
