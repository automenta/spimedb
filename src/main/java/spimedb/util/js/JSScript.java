package spimedb.util.js;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import java.io.*;

/**
 * Created by me on 7/19/15.
 */
public class JSScript extends SimpleBindings {

    protected final ScriptEngine engine;

    public JSScript(ScriptEngine engine) {
        this.engine = engine;
    }

    public JSScript() {
        this(new ScriptEngineManager().getEngineByName("nashorn"));
    }

    public JSScript with(String key, Object value) {
        put(key, value);
        return this;
    }

    public void run(File file) throws ScriptException, FileNotFoundException {
        run(new FileReader(file));
    }

    public void run(String jsCode) throws ScriptException {
        run(new StringReader(jsCode));
    }

    public void run(Reader reader) throws ScriptException {
        engine.eval(reader, this);
    }

}
