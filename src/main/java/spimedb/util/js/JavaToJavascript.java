package spimedb.util.js;

import org.infinispan.commons.util.concurrent.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.teavm.cache.DiskRegularMethodNodeCache;
import org.teavm.cache.FileNameEncoder;
import org.teavm.cache.ProgramIO;
import org.teavm.cache.SymbolTable;
import org.teavm.diagnostics.DefaultProblemTextConsumer;
import org.teavm.diagnostics.Problem;
import org.teavm.diagnostics.ProblemSeverity;
import org.teavm.javascript.MethodNodeCache;
import org.teavm.javascript.ast.AsyncMethodNode;
import org.teavm.javascript.ast.RegularMethodNode;
import org.teavm.model.*;
import org.teavm.model.instructions.*;
import org.teavm.parsing.ClassDateProvider;
import org.teavm.parsing.ClasspathClassHolderSource;
import org.teavm.vm.TeaVM;
import org.teavm.vm.TeaVMBuilder;
import spimedb.util.HTTP;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** convenient wrapper for TeaVM compiler which produces client-side JS from Java src */
public class JavaToJavascript {

    static final Logger logger = LoggerFactory.getLogger(JavaToJavascript.class);

    static final ClassLoader cl = JavaToJavascript.class.getClassLoader();
    public static final int FILE_BUFFER_SIZE = 32 * 1024;

    final TeaVMBuilder builder;
    final ProgramCache programCache;
    final MethodNodeCache methodCache;
    private final Properties properties;

    public static JavaToJavascript build() {

        ClasspathClassHolderSource chs = new ClasspathClassHolderSource();
        MyFileSymbolTable sym = new MyFileSymbolTable(HTTP.tmpCacheFile("sym"));
        MyFileSymbolTable file = new MyFileSymbolTable(HTTP.tmpCacheFile("file"));

        return new JavaToJavascript(
                chs,
                new MyDiskProgramCache(HTTP.tmpCacheDir().toFile(),
                        sym,
                        file,
                        chs
                ),
                new MyMemoryRegularMethodNodeCache()

                //doesnt work yet:
                /*new DiskRegularMethodNodeCache(
                        HTTP.tmpCacheDir().toFile(),
                        sym,
                        file,
                        chs
                )*/
        );
    }

    public JavaToJavascript() {
        this(new ClasspathClassHolderSource(), new MyMemoryProgramCache(new ConcurrentHashMap<>()), new MyMemoryRegularMethodNodeCache());
    }

//    public JavaToJavascript(Map<MethodReference, Program> cache) {
//        this(new MyMemoryProgramCache(cache));
//    }
//
//    public JavaToJavascript(ProgramCache programCache) {
//        this(new ClasspathClassHolderSource(), programCache );
//    }

    public JavaToJavascript(ClasspathClassHolderSource chs, ProgramCache programCache, MethodNodeCache methodCache) {
        builder = new TeaVMBuilder()
            .setClassLoader(cl)
            .setClassSource(
                new PreOptimizingClassHolderSource(chs)
            );
        this.properties = new Properties();

        this.methodCache = methodCache;
        this.programCache = programCache;


    }


    public StringBuilder compile(String entryFunc, MethodReference method) {

        long startTime = System.currentTimeMillis();

        TeaVM t = builder.build();


        t.installPlugins();



        t.setProgramCache(programCache);
        t.setAstCache(methodCache);

        t.entryPoint(entryFunc, method);
        t.setMinifying(false); //when true, mangles leaflet's L variable
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

        StringBuilder sb = new StringBuilder(FILE_BUFFER_SIZE);
        t.build(sb, null);


        //t.build(new File("/tmp/a"), "main.js");

        List<Problem> problems = t.getProblemProvider().getSevereProblems();
        if (!problems.isEmpty()) {
            DefaultProblemTextConsumer pc = new DefaultProblemTextConsumer();

            problems.forEach(p -> {
                p.render(pc);

                if (p.getSeverity() == ProblemSeverity.ERROR)
                    logger.error("{}: {}", p.getLocation(), pc.getText());
                else if (p.getSeverity() == ProblemSeverity.WARNING)
                    logger.warn("{}: {}", p.getLocation(), pc.getText());

                pc.clear();
            });
        }

        if (t.wasCancelled()) {
            logger.error("cancelled: {}", problems);
            return null;
        }


        long endTime = System.currentTimeMillis();
        logger.info("compiled {} to {} bytes .JS in {} ms", method, sb.length(), endTime-startTime);


        if (programCache instanceof MyDiskProgramCache) {
            try {
                ((MyDiskProgramCache) programCache).commit();
            } catch (IOException e) {
                logger.error("flush programCache: {}", e);
            }
        }
        if (methodCache instanceof DiskRegularMethodNodeCache) {
            try {
                ((DiskRegularMethodNodeCache) methodCache).flush();
            } catch (IOException e) {
                logger.error("flush methodCache: {}", e);
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

    public static class MyFileSymbolTable implements SymbolTable {
        private final File file;
        private final List<String> symbols = Collections.synchronizedList(new ArrayList<>());
        private final Map<String, Integer> symbolMap = new ConcurrentHashMap<>();


        public MyFileSymbolTable(File file) {
            this.file = file;
        }

        public void update() throws IOException {
            synchronized (symbols) {
                symbols.clear();
                symbolMap.clear();
                try (DataInputStream input = new DataInputStream(new BufferedInputStream(new FileInputStream(file), 32 * 1024))) {
                    //StringBuilder sb = new StringBuilder();
                    int nextNum = symbols.size();
                    while (true) {
//                        int length = input.read();
//                        if (length == -1) {
//                            break;
//                        }
                        try {
                            String symbol = input.readUTF();//sb.toString();
                            if (symbolMap.putIfAbsent(symbol, nextNum) == null) {
                                symbols.add(symbol);
                                nextNum++;
                            }
                            //assert(symbols.size() == symbolMap.size());
                        } catch (EOFException ee) {
                            break;
                        }
                    }
                }
            }
        }

        public void commit() throws IOException {
            synchronized (symbols) {

                try (BufferedOutputStream bos = new BufferedOutputStream( new FileOutputStream(file, true))) {

                    DataOutputStream output = new DataOutputStream(bos);
                    for (String s : symbols)
                        output.writeUTF(s);
                    output.close();
                }
            }
        }

        @Override
        public String at(int index) {
            return symbols.get(index);
        }

        @Override
        public int lookup(String symbol) {
            return symbolMap.computeIfAbsent(symbol, s -> {
                int index;
                synchronized (symbols) {
                    index = symbols.size();
                    symbols.add(s);
                }
                return index;
            });
        }
    }

    /**
     *
     * @author Alexey Andreev
     */
    static public class MyDiskProgramCache implements ProgramCache {
        private final File directory;
        private final ProgramIO programIO;
        private final Map<MethodReference, Item> cache = new ConcurrentHashMap<>();
        private final Set<MethodReference> newMethods = new ConcurrentHashSet();
        private final ClassDateProvider classDateProvider;
        private final MyFileSymbolTable fileTable, symbolTable;

        public MyDiskProgramCache (File directory, MyFileSymbolTable symbolTable, MyFileSymbolTable fileTable,
                                ClassDateProvider classDateProvider) {
            this.directory = directory;

            programIO = new ProgramIO(symbolTable, fileTable);

            this.fileTable = fileTable;
            this.symbolTable = symbolTable;

            this.classDateProvider = classDateProvider;

            update(symbolTable, fileTable);

        }

        private void update(MyFileSymbolTable symbolTable, MyFileSymbolTable fileTable) {
            try {
                fileTable.update();
                symbolTable.update();
            } catch (IOException e) {
                logger.error("file and symbol table: {}", e);
            }
        }


        @Override
        public Program get(MethodReference method) {
            return cache.computeIfAbsent(method, m -> {
                Item item = new Item();
                File file = getMethodFile(method);
                if (file.exists()) {
                    try (InputStream stream = new BufferedInputStream(new FileInputStream(file))) {
                        DataInput input = new DataInputStream(stream);
                        int depCount = input.readShort();
                        boolean dependenciesChanged = false;
                        for (int i = 0; i < depCount; ++i) {
                            String depClass = input.readUTF();
                            Date depDate = classDateProvider.getModificationDate(depClass);
                            if (depDate == null || depDate.after(new Date(file.lastModified()))) {
                                dependenciesChanged = true;
                                break;
                            }
                        }
                        if (!dependenciesChanged) {
                            item.program = programIO.read(stream);
                        }
                    } catch (IOException e) {
                        // we could not read program, just leave it empty
                    }
                }
                return item;
            }).program;
        }

        @Override
        public void store(MethodReference method, Program program) {
            Item item = new Item();
            item.program = program;
            if (cache.put(method, item)==null) {
                newMethods.add(method);
            }
        }

        public void commit() throws IOException {
            for (Iterator<MethodReference> iterator = newMethods.iterator(); iterator.hasNext(); ) {
                MethodReference method = iterator.next();
                iterator.remove();

                ProgramDependencyAnalyzer analyzer = new ProgramDependencyAnalyzer();

                Program program = cache.get(method).program;
                for (int i = 0; i < program.basicBlockCount(); ++i) {
                    BasicBlock block = program.basicBlockAt(i);
                    List<Instruction> instructions = block.getInstructions();
                    for (int i1 = 0, instructionsSize = instructions.size(); i1 < instructionsSize; i1++) {
                        instructions.get(i1).acceptVisitor(analyzer);
                    }
                }

                Set<String> deps = analyzer.dependencies;
                deps.add(method.getClassName());

                File file = getMethodFile(method);
                file.getParentFile().mkdirs();
                try (BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(file), FILE_BUFFER_SIZE)) {
                    DataOutputStream output = new DataOutputStream(stream);
                    output.writeShort(deps.size());
                    for (String dep : deps) {
                        output.writeUTF(dep);
                    }
                    programIO.write(program, output);
                    output.close();
                }
            }

            newMethods.clear();

            fileTable.commit();
            symbolTable.commit();
        }

        private File getMethodFile(MethodReference method) {
            File dir = new File(directory, method.getClassName().replace('.', '/'));
            return new File(dir, FileNameEncoder.encodeFileName(method.getDescriptor().toString()) + ".teavm-opt");
        }

        static class Item {
            Program program;
        }

        static class ProgramDependencyAnalyzer implements InstructionVisitor {
            final Set<String> dependencies = new ConcurrentHashSet<>();

            public void clear() {
                dependencies.clear();
            }

            @Override public void visit(GetFieldInstruction insn) {
                dependencies.add(insn.getField().getClassName());
            }
            @Override public void visit(PutFieldInstruction insn) {
                dependencies.add(insn.getField().getClassName());
            }
            @Override public void visit(InvokeInstruction insn) {
                dependencies.add(insn.getMethod().getClassName());
            }

            @Override
            public void visit(InvokeDynamicInstruction insn) {
                List<RuntimeConstant> bootstrapArguments = insn.getBootstrapArguments();
                for (int i = 0, bootstrapArgumentsSize = bootstrapArguments.size(); i < bootstrapArgumentsSize; i++) {
                    RuntimeConstant cst = bootstrapArguments.get(i);
                    if (cst.getKind() == RuntimeConstant.METHOD_HANDLE) {
                        MethodHandle handle = cst.getMethodHandle();
                        dependencies.add(handle.getClassName());
                    }
                }
            }
            @Override public void visit(EmptyInstruction insn) { }
            @Override public void visit(ClassConstantInstruction insn) { }
            @Override public void visit(NullConstantInstruction insn) { }
            @Override public void visit(IntegerConstantInstruction insn) { }
            @Override public void visit(LongConstantInstruction insn) { }
            @Override public void visit(FloatConstantInstruction insn) { }
            @Override public void visit(DoubleConstantInstruction insn) { }
            @Override public void visit(StringConstantInstruction insn) { }
            @Override public void visit(BinaryInstruction insn) { }
            @Override public void visit(NegateInstruction insn) { }
            @Override public void visit(AssignInstruction insn) { }
            @Override public void visit(CastInstruction insn) { }
            @Override public void visit(CastNumberInstruction insn) { }
            @Override public void visit(CastIntegerInstruction insn) { }
            @Override public void visit(BranchingInstruction insn) { }
            @Override public void visit(BinaryBranchingInstruction insn) { }
            @Override public void visit(JumpInstruction insn) { }
            @Override public void visit(SwitchInstruction insn) { }
            @Override public void visit(ExitInstruction insn) { }
            @Override public void visit(RaiseInstruction insn) { }
            @Override public void visit(ConstructArrayInstruction insn) { }
            @Override public void visit(ConstructInstruction insn) { }
            @Override public void visit(ConstructMultiArrayInstruction insn) { }
            @Override public void visit(ArrayLengthInstruction insn) { }
            @Override public void visit(CloneArrayInstruction insn) { }
            @Override public void visit(UnwrapArrayInstruction insn) { }
            @Override public void visit(GetElementInstruction insn) { }
            @Override public void visit(PutElementInstruction insn) { }
            @Override public void visit(IsInstanceInstruction insn) { }
            @Override public void visit(InitClassInstruction insn) { }
            @Override public void visit(NullCheckInstruction insn) { }
            @Override public void visit(MonitorEnterInstruction insn) { }
            @Override public void visit(MonitorExitInstruction insn) { }
        }
    }

}
