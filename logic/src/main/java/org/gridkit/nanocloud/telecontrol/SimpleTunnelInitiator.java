package org.gridkit.nanocloud.telecontrol;

import org.gridkit.nanocloud.telecontrol.HostControlConsole.Destroyable;
import org.gridkit.nanocloud.telecontrol.HostControlConsole.ProcessHandler;
import org.gridkit.util.concurrent.FutureBox;
import org.gridkit.vicluster.telecontrol.Classpath;
import org.gridkit.vicluster.telecontrol.ClasspathUtils;
import org.gridkit.vicluster.telecontrol.FileBlob;
import org.gridkit.vicluster.telecontrol.StreamCopyService;
import org.gridkit.vicluster.telecontrol.StreamCopyService.Link;
import org.gridkit.vicluster.telecontrol.bootstraper.Tunneller;
import org.gridkit.vicluster.telecontrol.bootstraper.TunnellerConnection;
import org.gridkit.zeroio.LineLoggerOutputStream;
import org.gridkit.zerormi.zlog.ZLogger;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SimpleTunnelInitiator implements TunnellerInitiator {
    private final String javaCmd;
    private final ZLogger logger;
    private final StreamCopyService streamCopyService;

    public SimpleTunnelInitiator(String javaCmd, String fileCachePath, StreamCopyService streamCopyService, ZLogger logger) {
        this.javaCmd = javaCmd;
        this.streamCopyService = streamCopyService;
        this.logger = logger;
    }

    public HostControlConsole initTunnel(HostControlConsole console) {
        String jversion = this.getJavaVersion(console);
        if (jversion != null) {
            //this.verifyVersion(jversion);
            this.logger.debug().log("Host JVM version is " + jversion);
        }

        byte[] bootJar;
        try {
            bootJar = ClasspathUtils.createBootstrapperJar(null, Tunneller.class);
        } catch (IOException var10) {
            throw new RuntimeException("Failed to build tunneller.jar", var10);
        }

        String jarpath = console.cacheFile(Classpath.createBinaryEntry("tunneller.jar", bootJar));
        String cachePath = this.detectCachePath(jarpath);
        final FutureBox<TunnellerConnection> tc = new FutureBox();
        ProcessHandler th = new ProcessHandler() {
            Link diag;

            public void started(final OutputStream stdIn, final InputStream stdOut, InputStream stdErr) {
                LineLoggerOutputStream log = new LineLoggerOutputStream("", SimpleTunnelInitiator.this.logger.getLogger("console").warn());
                final LineLoggerOutputStream dlog = new LineLoggerOutputStream("", SimpleTunnelInitiator.this.logger.getLogger("tunneller").info());
                this.diag = SimpleTunnelInitiator.this.streamCopyService.link(stdErr, log, true);
                Thread thread = new Thread(() -> {
                    try {
                        TunnellerConnection tcon = new TunnellerConnection("tunneller", stdOut, stdIn, new PrintStream(dlog), 10L, TimeUnit.SECONDS);
                        tc.setData(tcon);
                    } catch (IOException | TimeoutException | InterruptedException var2) {
                        tc.setError(var2);
                    }

                    diag.flush();
                });
                thread.setName("Tunnel initializer");
                thread.start();
            }

            public void finished(int exitCode) {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException var3) {
                }

                this.diag.flush();
                tc.setErrorIfWaiting(new RuntimeException("Tunneller exit code: " + exitCode));
            }
        };
        Destroyable proc = console.startProcess(null, this.tunnellerCommand(jarpath), null, th);
        TunnellerConnection conn = fget(tc);
        return new SimpleTunnelInitiator.CosnoleWrapper(new TunnellerControlConsole(conn, cachePath), proc);
    }

//    private void verifyVersion(String jversion) {
//        String[] split = jversion.split("[.]");
//        if (split.length < 2) {
//            throw new IllegalArgumentException("Unsupported remote Java version: " + jversion);
//        } else {
//            int major = this.toInt(split[0]);
//            int minor = this.toInt(split[1]);
//            if (major < 1 || minor < 6) {
//                throw new IllegalArgumentException("Unsupported remote Java version: " + jversion);
//            }
//        }
//    }

    private int toInt(String n) {
        try {
            return Integer.parseInt(n);
        } catch (NumberFormatException var3) {
            return -1;
        }
    }

    private String[] tunnellerCommand(String jarpath) {
        List<String> cmd = new ArrayList();
        if (this.javaCmd.indexOf(124) >= 0) {
            cmd.addAll(Arrays.asList(this.javaCmd.split("\\|")));
        } else {
            cmd.add(this.javaCmd);
        }

        cmd.addAll(Arrays.asList("-Xmx32m", "-Xms32m", "-cp", jarpath, Tunneller.class.getName()));
        return cmd.toArray(new String[0]);
    }

    private String detectCachePath(String jarpath) {
        String cachePath = jarpath;
        if (jarpath.indexOf(47) >= 0) {
            cachePath = jarpath.substring(0, jarpath.lastIndexOf(47) + 1);
        }

        if (cachePath.indexOf(92) >= 0) {
            cachePath = cachePath.substring(0, cachePath.lastIndexOf(92) + 1);
        }

        cachePath = cachePath + "..";
        return cachePath;
    }

    private static <T> T fget(Future<T> f) {
        try {
            return f.get();
        } catch (InterruptedException var2) {
            throw new RuntimeException(var2);
        } catch (ExecutionException var3) {
            if (var3.getCause() instanceof RuntimeException) {
                throw (RuntimeException) var3.getCause();
            } else if (var3.getCause() instanceof Error) {
                throw (Error) var3.getCause();
            } else {
                throw new RuntimeException("Failed to start remote process", var3.getCause());
            }
        }
    }

    private String getJavaVersion(HostControlConsole console) {
        try {
            final FutureBox<Void> done = new FutureBox();
            final ByteArrayOutputStream stdOut = new ByteArrayOutputStream();
            final ByteArrayOutputStream stdErr = new ByteArrayOutputStream();
            ProcessHandler handler = new ProcessHandler() {
                Link lout;
                Link lerr;

                public void started(OutputStream stdIn, InputStream out, InputStream err) {
                    try {
                        stdIn.close();
                    } catch (IOException var5) {
                    }

                    this.lout = SimpleTunnelInitiator.this.streamCopyService.link(out, stdOut);
                    this.lerr = SimpleTunnelInitiator.this.streamCopyService.link(err, stdErr);
                }

                public void finished(int exitCode) {
                    this.lout.flushAndClose();
                    this.lerr.flushAndClose();
                    done.setData(null);
                }
            };
            console.startProcess(null, new String[]{this.javaCmd, "-version"}, null, handler);

            try {
                done.get();
            } catch (InterruptedException | ExecutionException var11) {
            }

            BufferedReader outr = new BufferedReader(new StringReader(stdOut.toString()));
            BufferedReader errr = new BufferedReader(new StringReader(stdErr.toString()));
            Pattern p = Pattern.compile("(openjdk|java) version \"([^\"]*)\"");

            while(true) {
                String line = errr.readLine();
                if (line == null) {
                    this.logger.fatal().log("JVM verification failed: " + this.javaCmd);

                    while(true) {
                        line = outr.readLine();
                        if (line == null) {
                            return null;
                        }

                        this.logger.critical().log("{java -version} " + line);
                    }
                }

                Matcher m = p.matcher(line);
                if (m.matches()) {
                    return m.group(2);
                }

                this.logger.critical().log("{java -version} " + line);
            }
        } catch (IOException var13) {
            this.logger.warn().log("JVM verification error", var13);
            return null;
        }
    }

    private record CosnoleWrapper(HostControlConsole delegate, Destroyable destroyable) implements HostControlConsole {

        public String cacheFile(FileBlob blob) {
                return this.delegate.cacheFile(blob);
            }

            public List<String> cacheFiles(List<? extends FileBlob> blobs) {
                return this.delegate.cacheFiles(blobs);
            }

            public Destroyable openSocket(SocketHandler handler) {
                return this.delegate.openSocket(handler);
            }

            public Destroyable startProcess(String workDir, String[] command, Map<String, String> env, ProcessHandler handler) {
                return this.delegate.startProcess(workDir, command, env, handler);
            }

            public void terminate() {
                this.delegate.terminate();
                this.destroyable.destroy();
            }
        }
}
