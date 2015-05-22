/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package jnetention.gui.demo;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import jnetention.gui.Console;
import jnetention.gui.Console.ConsoleModel;

/**
 *
 * @author me
 */
public class ConsoleDemo extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        
        Console console = new Console(new ConsoleModel() {

            @Override
            public void parse(Console c, String input) {
                c.println("$ " + input);
                
                ShellExec s = new ShellExec(true, true);
                try {                    
                    Process p = s.execute("/bin/bash", null, false, "-s");
                    s.getOut().append(input);
                    s.getOut().append("\n");
                    s.getOut().flush();
                    s.getOut().close();
                    
                    p.waitFor();
                } catch (IOException ex) {                    
                    c.println(ex.toString());
                } catch (InterruptedException ex) {
                    Logger.getLogger(ConsoleDemo.class.getName()).log(Level.SEVERE, null, ex);
                }

                if (s.getOutput()!=null)
                    if (!s.getOutput().isEmpty())
                        c.println(s.getOutput());
                if (s.getError()!=null)
                    if (!s.getError().isEmpty())
                        c.println(s.getError());
                    
            }
            
        });
 
        
        StackPane root = new StackPane();
        root.getChildren().add(console);
        
        Scene scene = new Scene(root, 300, 250);
        

        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
    
}

/**
 * Execute external process and optionally read output buffer.
 */
class ShellExec {
    private int exitCode;
    private boolean readOutput, readError;
    private StreamGobbler errorGobbler, outputGobbler;
    private PrintWriter out;

    public ShellExec() { 
        this(false, false);
    }

    public ShellExec(boolean readOutput, boolean readError) {
        this.readOutput = readOutput;
        this.readError = readError;
    }

    /**
     * Execute a command.
     * @param command   command ("c:/some/folder/script.bat" or "some/folder/script.sh")
     * @param workdir   working directory or NULL to use command folder
     * @param wait  wait for process to end
     * @param args  0..n command line arguments
     * @return  process exit code
     */
    public Process execute(String command, String workdir, boolean wait, String...args) throws IOException {
        String[] cmdArr;
        if (args != null && args.length > 0) {
            cmdArr = new String[1+args.length];
            cmdArr[0] = command;
            System.arraycopy(args, 0, cmdArr, 1, args.length);
        } else {
            cmdArr = new String[] { command };
        }

        ProcessBuilder pb =  new ProcessBuilder(cmdArr);
        File workingDir = (workdir==null ? new File(command).getParentFile() : new File(workdir) );
        pb.directory(workingDir);

        Process process = pb.start();
        out = new PrintWriter(new BufferedOutputStream(process.getOutputStream()));
        // Consume streams, older jvm's had a memory leak if streams were not read,
        // some other jvm+OS combinations may block unless streams are consumed.
        errorGobbler  = new StreamGobbler(process.getErrorStream(), readError);
        outputGobbler = new StreamGobbler(process.getInputStream(), readOutput);
        errorGobbler.start();
        outputGobbler.start();
        

        return process;
    }   

    public int getExitCode() {
        return exitCode;
    }

    public boolean isOutputCompleted() {
        return (outputGobbler != null ? outputGobbler.isCompleted() : false);
    }

    public boolean isErrorCompleted() {
        return (errorGobbler != null ? errorGobbler.isCompleted() : false);
    }

    public PrintWriter getOut() {
        return out;
    }

        
    public String getOutput() {
        return (outputGobbler != null ? outputGobbler.getOutput() : null);        
    }

    public String getError() {
        return (errorGobbler != null ? errorGobbler.getOutput() : null);        
    }

//********************************************
//********************************************    

    /**
     * StreamGobbler reads inputstream to "gobble" it.
     * This is used by Executor class when running 
     * a commandline applications. Gobblers must read/purge
     * INSTR and ERRSTR process streams.
     * http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html?page=4
     */
    private class StreamGobbler extends Thread {
        private InputStream is;
        private StringBuilder output;
        private volatile boolean completed; // mark volatile to guarantee a thread safety

        public StreamGobbler(InputStream is, boolean readStream) {
            this.is = is;
            this.output = (readStream ? new StringBuilder(256) : null);
        }

        public void run() {
            completed = false;
            try {
                String NL = System.getProperty("line.separator", "\r\n");

                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr);
                String line;
                while ( (line = br.readLine()) != null) {
                    if (output != null)
                        output.append(line + NL); 
                }
            } catch (IOException ex) {
                // ex.printStackTrace();
            }
            completed = true;
        }

        /**
         * Get inputstream buffer or null if stream
         * was not consumed.
         * @return
         */
        public String getOutput() {
            return (output != null ? output.toString() : null);
        }

        /**
         * Is input stream completed.
         * @return
         */
        public boolean isCompleted() {
            return completed;
        }

    }

}