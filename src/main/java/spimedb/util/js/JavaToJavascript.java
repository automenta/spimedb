package spimedb.util.js;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teavm.javascript.MethodNodeCache;
import org.teavm.javascript.ast.AsyncMethodNode;
import org.teavm.javascript.ast.RegularMethodNode;
import org.teavm.model.MethodReference;
import org.teavm.model.PreOptimizingClassHolderSource;
import org.teavm.model.Program;
import org.teavm.model.ProgramCache;
import org.teavm.parsing.ClasspathClassHolderSource;
import org.teavm.vm.DirectoryBuildTarget;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMBuilder;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** convenient wrapper for TeaVM compiler which produces client-side JS from Java src */
public class JavaToJavascript {

    static final Logger logger = LoggerFactory.getLogger(JavaToJavascript.class);

    static final int SOURCE_FILE_BUFFER_LENGTH = 64 * 1024;
    static final ClassLoader cl = JavaToJavascript.class.getClassLoader();

    final TeaVMBuilder builder;
    final MyMemoryProgramCache programCache = new MyMemoryProgramCache();
    final MyMemoryRegularMethodNodeCache astCache = new MyMemoryRegularMethodNodeCache();

    public JavaToJavascript() {
        this(new ClasspathClassHolderSource());
    }

    public JavaToJavascript(ClasspathClassHolderSource chs) {
        builder = new TeaVMBuilder()
            .setClassLoader(cl)
            .setClassSource(
                new PreOptimizingClassHolderSource(chs)
            );
    }

    public StringBuilder compile(MethodReference method) {

        long startTime = System.currentTimeMillis();

        TeaVM t = builder.build();


        //t.installPlugins();


        t.entryPoint(method);
        t.setMinifying(true);
        t.setIncremental(true);
        t.setProgramCache(programCache);
        t.setAstCache(astCache);
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
        t.build(sb, new DirectoryBuildTarget(new File("/tmp/a")));

        long endTime = System.currentTimeMillis();
        logger.info("compiled {} to {} bytes .JS in {} ms", method, sb.length(), endTime-startTime);


        return sb;
    }

    /** compiles the main() method of a class */
    public StringBuilder compileMain(Class c) {
        return compile(new MethodReference(c, "main", String[].class));
    }

    public static class MyMemoryProgramCache implements ProgramCache {

        private final Map<MethodReference, Program> cache = new ConcurrentHashMap<>(512);

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
