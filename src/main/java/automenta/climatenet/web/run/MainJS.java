/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package automenta.climatenet.web.run;

import automenta.climatenet.Spacetime;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 *
 * @author me
 */
public class MainJS {
    final static ScriptEngineManager factory = new ScriptEngineManager();
    
    final ScriptEngine js = factory.getEngineByName("JavaScript");

    public MainJS() throws Exception {
        js.eval("load('nashorn:mozilla_compat.js')");
        
        js.eval("importPackage('java.lang')");
        js.eval("importPackage('java.util')");
        js.eval("importPackage('java.io')");

        js.eval("importPackage('nars.core')");
        js.eval("importPackage('nars.core.build')");
        js.eval("importPackage('nars.io')");
        js.eval("importPackage('nars.gui')");
        
    }

    public Object eval(String s) throws ScriptException {
        return js.eval(s);
    }
    
    public static void printHelp() {
        System.out.println("Help coming soon.");
    }
    
    public void repl() {
        System.out.println(Spacetime.VERSION +  " Javascript Console - :h for help, :q to exit");
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

                    Object ret = eval(s);

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
            
        }
    
        try {
            br.close();
        } catch (IOException ex) {
        }
        System.exit(0);
        
    }
    
    public static void main(String[] args) throws Exception {
        new MainJS().repl();
    }    
}
