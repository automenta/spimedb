/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.netention.data;

import automenta.netention.geo.SpimeBase;

import javax.script.Bindings;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.*;

/**
 * Provides a javascript context with a DB reference for querying, populating, and/or transforming it
 */
public class SpimeScript {


    private final ScriptEngine engine;
    private final Bindings js;


    public SpimeScript(SpimeBase db) {

        this.engine = new ScriptEngineManager().getEngineByName("nashorn");

        this.js = engine.createBindings();

        js.put("db", db);
    }

    public void run(File file) throws ScriptException, FileNotFoundException {
        run(new FileReader(file));
    }

    public void run(String jsCode) throws ScriptException {
        run(new StringReader(jsCode));
    }

    public void run(Reader reader) throws ScriptException {
        engine.eval(reader, js);
    }


}
