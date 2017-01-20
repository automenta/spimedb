package spimedb.client.lodash;

import org.teavm.jso.JSBody;
import org.teavm.jso.browser.TimerHandler;
import org.teavm.jso.core.JSFunction;

/**
 * Created by me on 1/17/17.
 */
public class Lodash {

    /** https://lodash.com/docs/4.17.4#debounce */
    @JSBody(params = { "func", "periodMS" }, script = "return _.debounce(func, periodMS, { leading: true, trailing: true } );")
    public static native JSFunction debounce(TimerHandler func, int periodMS);

    /** https://lodash.com/docs/4.17.4#throttle */
    @JSBody(params = { "func", "periodMS" }, script = "return _.throttle(func, periodMS);")
    public static native JSFunction throttle(TimerHandler func, int periodMS);

}
