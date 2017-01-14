package spimedb.util.js;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teavm.cache.DiskProgramCache;
import org.teavm.diagnostics.DefaultProblemTextConsumer;
import org.teavm.diagnostics.Problem;
import org.teavm.javascript.MethodNodeCache;
import org.teavm.javascript.ast.AsyncMethodNode;
import org.teavm.javascript.ast.RegularMethodNode;
import org.teavm.model.MethodReference;
import org.teavm.model.PreOptimizingClassHolderSource;
import org.teavm.model.Program;
import org.teavm.model.ProgramCache;
import org.teavm.parsing.ClasspathClassHolderSource;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMBuilder;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

/** convenient wrapper for TeaVM compiler which produces client-side JS from Java src */
public class JavaToJavascript {

    static final Logger logger = LoggerFactory.getLogger(JavaToJavascript.class);

    static final int SOURCE_FILE_BUFFER_LENGTH = 64 * 1024;
    static final ClassLoader cl = JavaToJavascript.class.getClassLoader();

    final TeaVMBuilder builder;
    final ProgramCache programCache;
    final MyMemoryRegularMethodNodeCache astCache = new MyMemoryRegularMethodNodeCache();
    private final Properties properties;

    public JavaToJavascript() {
        this(new MyMemoryProgramCache(new ConcurrentHashMap<>()));
    }

    public JavaToJavascript(Map<MethodReference, Program> cache) {
        this(new MyMemoryProgramCache(cache));
    }

    public JavaToJavascript(ProgramCache programCache) {
        this(new ClasspathClassHolderSource(), programCache );
    }

    public JavaToJavascript(ClasspathClassHolderSource chs, ProgramCache programCache) {
        builder = new TeaVMBuilder()
            .setClassLoader(cl)
            .setClassSource(
                new PreOptimizingClassHolderSource(chs)
            );
        this.properties = new Properties();

        this.programCache = programCache;
                /*new DiskProgramCache(HTTP.tmpCacheDir().toFile(),
                    new FileSymbolTable(HTTP.tmpCacheFile("sym")),
                    new FileSymbolTable(HTTP.tmpCacheFile("file")),
                    chs
                    );*/

    }


    public StringBuilder compile(String entryFunc, MethodReference method) {

        long startTime = System.currentTimeMillis();

        TeaVM t = builder.build();


        t.installPlugins();


        t.setProgramCache(programCache);
        t.setAstCache(astCache);

        t.entryPoint(entryFunc, method);
        t.setMinifying(true);
        t.setIncremental(true);
        t.setProperties(properties);



    /*t.setProgressListener(new TeaVMProgressListener() {

        @Override
        public TeaVMProgressFeedback phaseStarted(TeaVMPhase phase, int count) {
            logger.info("start {} {}", phase, count);
            return TeaVMProgressFeedback.CONTINUE;
        }

        @Override
        public TeaVMProgressFeedback progressReached(int progress) {
            //logger.info("@ {}", progress);
            return TeaVMProgressFeedback.CONTINUE;
        }
    });*/

        StringBuilder sb = new StringBuilder(SOURCE_FILE_BUFFER_LENGTH);
        t.build(sb, null);

        //t.build(new File("/tmp/a"), "main.js");

        List<Problem> problems = t.getProblemProvider().getProblems();
        if (!problems.isEmpty()) {
            DefaultProblemTextConsumer pc = new DefaultProblemTextConsumer();

            problems.forEach(p -> {
                p.render(pc);
                logger.error("problem: {}", pc.getText());
                pc.clear();
            });
        }

        if (t.wasCancelled()) {
            logger.error("cancelled: {}", problems);
            return null;
        }


        long endTime = System.currentTimeMillis();
        logger.info("compiled {} to {} bytes .JS in {} ms", method, sb.length(), endTime-startTime);


        if (programCache instanceof DiskProgramCache) {
            try {
                ((DiskProgramCache) programCache).flush();
            } catch (IOException e) {
                logger.error("flush: {}", e);
            }
        }

//        System.out.println(t.getClasses());
//        System.out.println(t.getDependencyInfo().getCallGraph());
//        System.out.println(t.getDependencyInfo().getReachableMethods());
//        System.out.println(t.getWrittenClasses().getClassNames());

        return sb;
    }

    /** compiles the static void main() method of a class */
    public StringBuilder compileMain(Class c) {
        MethodReference method = new MethodReference(c, "main", String[].class, void.class);

        //return compile(null, new MethodReference(c, "main", String[].class));

        return compile("main", method );
    }

    public static class MyMemoryProgramCache implements ProgramCache {

        private final Map<MethodReference, Program> cache;

        public MyMemoryProgramCache(Map<MethodReference, Program> cache) {
            this.cache = cache;
        }

        @Override
        public Program get(MethodReference method) {
            return cache.get(method);
        }

        @Override
        public void store(MethodReference method, Program program) {
            cache.put(method, /*ProgramUtils.copy*/(program));
        }

        @Override
        public String toString() {
            return "MyMemoryProgramCache{" +
                    "size=" + cache.size() +
                    '}';
        }
    }

    public static class MyMemoryRegularMethodNodeCache implements MethodNodeCache {
        private final Map<MethodReference, RegularMethodNode> cache = new ConcurrentHashMap<>(1024);
        private final Map<MethodReference, AsyncMethodNode> asyncCache = new ConcurrentHashMap<>(1024);

        @Override
        public RegularMethodNode get(MethodReference methodReference) {
            return cache.get(methodReference);
        }

        @Override
        public void store(MethodReference methodReference, RegularMethodNode node) {
            cache.put(methodReference, node);
        }

        @Override
        public AsyncMethodNode getAsync(MethodReference methodReference) {
            return null;
        }

        @Override
        public void storeAsync(MethodReference methodReference, AsyncMethodNode node) {
            asyncCache.put(methodReference, node);
        }

        @Override
        public String toString() {
            return "MyMemoryRegularMethodNodeCache{size=" + cache.size() + "+" + asyncCache.size() + '}';
        }
    }

}
