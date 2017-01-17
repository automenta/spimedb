/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package spimedb.util.js;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spimedb.SpimeDB;

import javax.script.ScriptEngine;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Provides a javascript context with a DB reference for querying, populating, and/or transforming it
 */
public class SpimeJS extends JSScript {

    public final static Logger logger = LoggerFactory.getLogger(SpimeJS.class);

    private final SpimeDB db;

    public SpimeJS(SpimeDB db) {
        super();
        this.db = db;

        engine.put("db", db);
    }

    @Override
    public void run(String jsCode) {
        SpimeDB.runLater(()->{
            try {
                super.run(jsCode);
            } catch (ScriptException e) {
                logger.error("{}: {}", jsCode, e);
            }
        });
    }

    public static void setImports(ScriptEngine js) throws Exception {
        js.eval("load('nashorn:mozilla_compat.js')");

        js.eval("importPackage('java.lang')");
        js.eval("importPackage('java.util')");
        js.eval("importPackage('java.io')");

//        js.eval("importPackage('nars.core')");
//        js.eval("importPackage('nars.core.build')");
//        js.eval("importPackage('nars.io')");
//        js.eval("importPackage('nars.gui')");

    }

    public Object eval(String s) throws ScriptException {
        return engine.eval(s);
    }

    public static void printHelp() {
        System.out.println("Help coming soon.");
    }

    public static void repl(ScriptEngine js) {

        System.out.println(SpimeDB.VERSION +  " javascript console - :h for help, :q to exit");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.print("> ");

        String s;
        try {
            while ((s = br.readLine())!=null) {


                try {
                    if (s.equals(":q"))
                        break;
                    else if (s.startsWith(":h")) {
                        printHelp();
                        continue;
                    }

                    Object ret = js.eval(s);

                    if (ret != null) {
                        System.out.println(ret);
                    }
                } catch (Exception e) {
                    System.out.println(e.getClass().getName() + " in parsing: " + e.getMessage());
                } finally {


                    System.out.print("> ");

                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        try {
            br.close();
        } catch (IOException ex) {
        }
        System.exit(0);

    }

    public static void main(String[] args) throws Exception {
        repl(new SpimeJS(new SpimeDB()).engine);
    }

}
