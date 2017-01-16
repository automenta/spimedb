package spimedb.index;

/**
 * Created by me on 1/15/17.
 */
public class Budget<X> {
    final X id;
    public float pri;

    public Budget(X x) {
        this.id = x;
    }

    public Budget(X x, float pri) {
        this(x);
        this.pri = pri;
    }

    public Budget<X> clone() {
        return new Budget<X>(id, pri);
    }

    public void pri(float newPri) {
        this.pri = newPri;
    }

    public void pri(Budget vv) {
        pri(vv.pri);
    }

    public void priAdd(float toAdd) {
        pri(pri + toAdd);
    }

    public void delete() {
        this.pri = Float.NaN;
    }

    public boolean isDeleted() {
        final float p = pri;
        return p != p;
    }

    public void priMult(float factor) {
        pri(this.pri * factor);
    }

    public float priSafe(float ifDeleted) {
        float p = pri;
        if (p == p) {
            return p;
        } else {
            return ifDeleted;
        }
    }

    @Override
    public String toString() {
        return id + "=" + pri;
    }
}
