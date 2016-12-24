package spimedb.web;

/**
 * Created by me on 12/19/16.
 */
public class MousePointer {
    public int _x;
    public int _y;

    public MousePointer() {

    }

    public MousePointer(int x, int y) {
        this._x = x;
        this._y = y;
    }

    public int get_x() {
        return _x;
    }

    public int get_y() {
        return _y;
    }
}
