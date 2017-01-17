package spimedb.util.js;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;

/**
 * Created by me on 7/19/15.
 */
public class JSScript {
    protected final ScriptEngine engine;

    public JSScript() {
        this.engine = new ScriptEngineManager().getEngineByName("nashorn");
    }

    public void run(File file) throws ScriptException, FileNotFoundException {
        run(new FileReader(file));
    }

    public void run(String jsCode) throws ScriptException {
        run(new StringReader(jsCode));
    }

    public void run(Reader reader) throws ScriptException {
        engine.eval(reader);
    }

}
