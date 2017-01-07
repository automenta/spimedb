package spimedb;

/**
 * Created by me on 1/7/17.
 */
public class Tag extends NObject {

    float pri;

    public float pri() { return pri; }

    public void pri(float newPri) {
        this.pri = newPri;
    }
}
