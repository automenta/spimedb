package spimedb;

/**
 * Created by me on 1/7/17.
 */
public class Tag {

    public final String id;

    float pri;

    public Tag(String id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Tag && id.equals(((Tag)obj).id);
    }

    public float pri() { return pri; }

    public void pri(float newPri) {
        this.pri = newPri;
    }
}
