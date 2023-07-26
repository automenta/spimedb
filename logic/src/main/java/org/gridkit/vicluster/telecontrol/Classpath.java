//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.gridkit.vicluster.telecontrol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.ref.WeakReference;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.jar.*;

/**
 * override for jdk9 compatibility
 */
public class Classpath {
    private static final Logger LOGGER = LoggerFactory.getLogger(Classpath.class);
    private static final String DIGEST_ALGO = "SHA-1";
    private static final WeakHashMap<ClassLoader, List<Classpath.ClasspathEntry>> CLASSPATH_CACHE = new WeakHashMap();
    private static final WeakHashMap<URL, WeakReference<Classpath.ClasspathEntry>> CUSTOM_ENTRIES = new WeakHashMap();
    private static final URL JRE_ROOT = getJreRoot();


    public static synchronized List<Classpath.ClasspathEntry> getClasspath(ClassLoader classloader) {





        return CLASSPATH_CACHE.computeIfAbsent(classloader, cl -> {

            List<Classpath.ClasspathEntry> classpath = new ArrayList();

            if (classloader instanceof URLClassLoader)
                fillClasspath(classpath, listCurrentClasspath((URLClassLoader) classloader));
            else if (classloader == Thread.currentThread().getContextClassLoader()){
                //HACK
                String[] jars = System.getProperty("java.class.path").split(":");
                URL[] urls = new URL[jars.length];
                int i = 0;
                for (String x : jars) {
                    try {
                        urls[i++] = new URL("file://" + x);//classloader.getResource(x);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                }
                fillClasspath(classpath, listCurrentClasspath( urls ));
            }

            return Collections.unmodifiableList(classpath);
        });

    }

    public static Collection<URL> listCurrentClasspath(URLClassLoader classLoader) {
        return listCurrentClasspath(classLoader.getURLs());
    }

    public static Collection<URL> listCurrentClasspath(URL[] uu) {
        LinkedHashSet result = new LinkedHashSet();

        //while (true) {
            URL[] arr$ = uu;

            int len$ = arr$.length;

            for (int i$ = 0; i$ < len$; ++i$) {
                URL url = arr$[i$];
                addEntriesFromManifest(result, url);
            }

//            ClassLoader cls = classLoader.getParent();
//            if (!(cls instanceof URLClassLoader) || cls.getClass().getName().endsWith("$ExtClassLoader")) {
//                return new ArrayList(result);
//            }
//
//            classLoader = (URLClassLoader) cls;
        //}
        return new ArrayList(result);
    }

    private static final ConcurrentMap<String, String> MISSING_URL = new ConcurrentHashMap(64, 0.75F, 1);

    private static void addEntriesFromManifest(Set<URL> list, URL url) {
        if (!list.contains(url)) {
            try {
                InputStream is;
                try {
                    is = url.openStream();
                } catch (Exception var12) {
                    String path = url.toString();
                    if (MISSING_URL.put(path, path) == null) {
                        LOGGER.warn("URL not found and will be excluded from classpath: " + path);
                    }

                    throw var12;
                }

                if (is != null) {
                    list.add(url);
                } else {
                    String path = url.toString();
                    if (MISSING_URL.put(path, path) == null) {
                        LOGGER.warn("URL not found and will be excluded from classpath: " + path);
                    }
                }

                JarInputStream jar = new JarInputStream(is);
                Manifest mf = jar.getManifest();
                jar.close();
                if (mf == null) {
                    return;
                }

                String cp = mf.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);
                if (cp != null) {
                    String[] arr$ = cp.split("\\s+");
                    int len$ = arr$.length;

                    for (int i$ = 0; i$ < len$; ++i$) {
                        String entry = arr$[i$];

                        try {
                            URL ipath = new URL(url, entry);
                            addEntriesFromManifest(list, ipath);
                        } catch (Exception var11) {
                        }
                    }
                }
            } catch (Exception var13) {
            }

        }
    }

    public static synchronized Classpath.ClasspathEntry getLocalEntry(String path) throws IOException {
        try {
            URL url = (new File(path)).toURI().toURL();
            WeakReference<Classpath.ClasspathEntry> wr = CUSTOM_ENTRIES.get(url);
            Classpath.ClasspathEntry entry;
            if (wr != null) {
                entry = wr.get();
                return entry;
            } else {
                entry = newEntry(url);
                CUSTOM_ENTRIES.put(url, new WeakReference(entry));
                return entry;
            }
        } catch (MalformedURLException var4) {
            throw new IOException(var4);
        } catch (URISyntaxException var5) {
            throw new IOException(var5);
        }
    }

    public static synchronized FileBlob createBinaryEntry(String name, byte[] data) {
        return new Classpath.ByteBlob(name, data);
    }

    private static void fillClasspath(List<Classpath.ClasspathEntry> classpath, Collection<URL> urls) {
        Iterator i$ = urls.iterator();

        while (i$.hasNext()) {
            URL url = (URL) i$.next();
            if (!isIgnoredJAR(url)) {
                try {
                    Classpath.ClasspathEntry entry = newEntry(url);
                    if (entry == null) {
                        LOGGER.warn("Cannot copy URL content: " + url);
                    } else {
                        classpath.add(entry);
                    }
                } catch (Exception var5) {
                    LOGGER.warn("Cannot copy URL content: " + url, var5);
                }
            }
        }

    }

    private static boolean isIgnoredJAR(URL url) {
        try {
            if ("file".equals(url.getProtocol())) {
                File f = new File(url.toURI());
                if (f.isFile()) {
                    if (belongs(JRE_ROOT, url)) {
                        return true;
                    }

                    if (f.getName().startsWith("surefire") && isManifestOnly(f)) {
                        return true;
                    }
                }
            }
        } catch (URISyntaxException var2) {
        }

        return false;
    }

    private static boolean isManifestOnly(File f) {
        JarFile jar = null;

        boolean var4;
        try {
            boolean var3;
            try {
                jar = new JarFile(f);
                Enumeration<JarEntry> en = jar.entries();
                if (!en.hasMoreElements()) {
                    var3 = false;
                    return var3;
                }

                JarEntry je = en.nextElement();
                if ("META-INF/".equals(je.getName())) {
                    if (!en.hasMoreElements()) {
                        var4 = false;
                        return var4;
                    }

                    je = en.nextElement();
                }

                if ("META-INF/MANIFEST.MF".equals(je.getName())) {
                    var4 = !en.hasMoreElements();
                    return var4;
                }

                var4 = false;
            } catch (IOException var17) {
                var3 = false;
                return var3;
            }
        } finally {
            if (jar != null) {
                try {
                    jar.close();
                } catch (IOException var16) {
                }
            }

        }

        return var4;
    }

    private static boolean belongs(URL base, URL derived) {
        return derived.toString().startsWith(base.toString());
    }

    private static URL getJavaHome() throws MalformedURLException {
        return (new File(System.getProperty("java.home"))).toURI().toURL();
    }

    private static URL getJreRoot() {
        try {
            String jlo = ClassLoader.getSystemResource("java/lang/Object.class").toString();
            if (jlo.startsWith("jar:")) {
                String root = jlo.substring("jar:".length());
                int n = root.indexOf(33);
                root = root.substring(0, n);
                if (root.endsWith("/rt.jar")) {
                    root = root.substring(0, root.lastIndexOf(47));
                    if (root.endsWith("/lib")) {
                        root = root.substring(0, root.lastIndexOf(47));
                        return new URL(root);
                    }
                }
            }

            return getJavaHome();
        } catch (MalformedURLException var3) {
            return null;
        }
    }

    private static Classpath.ClasspathEntry newEntry(URL url) throws IOException, URISyntaxException {
        Classpath.ClasspathEntry entry = new Classpath.ClasspathEntry();
        entry.url = url;
        File file = uriToFile(url.toURI());
        if (file.isFile()) {
            entry.file = file;
            entry.filename = file.getName();
        } else {
            String lname = file.getName();
            if ("classes".equals(lname)) {
                lname = file.getParentFile().getName();
            }

            if ("target".equals(lname)) {
                lname = file.getParentFile().getParentFile().getName();
            }

            if (lname.startsWith(".")) {
                lname = lname.substring(1);
            }

            lname = lname + ".jar";
            entry.file = file;
            entry.filename = lname;
            if (isEmpty(file)) {
                LOGGER.warn("Classpath entry is empty: " + file.getCanonicalPath());
                return null;
            }

            entry.lazyJar = true;
        }

        return entry;
    }

    private static boolean isEmpty(File file) {
        File[] files = file.listFiles();
        if (files == null) {
            return true;
        } else {
            File[] arr$ = files;
            int len$ = files.length;

            for (int i$ = 0; i$ < len$; ++i$) {
                File c = arr$[i$];
                if (c.isFile() || !isEmpty(c)) {
                    return false;
                }
            }

            return true;
        }
    }

    private static File uriToFile(URI uri) {
        if ("file".equals(uri.getScheme())) {
            if (uri.getAuthority() == null) {
                return new File(uri);
            } else {
                String path = "file:////" + uri.getAuthority() + "/" + uri.getPath();

                try {
                    return new File(new URI(path));
                } catch (URISyntaxException var3) {
                    return new File(uri);
                }
            }
        } else {
            return new File(uri);
        }
    }

    static class ByteBlob implements FileBlob {
        private final String filename;
        private final String hash;
        private final byte[] data;

        public ByteBlob(String filename, byte[] data) {
            this.filename = filename;
            this.data = data;
            this.hash = StreamHelper.digest(data, "SHA-1");
        }

        public File getLocalFile() {
            return null;
        }

        public String getFileName() {
            return this.filename;
        }

        public String getContentHash() {
            return this.hash;
        }

        public InputStream getContent() {
            return new ByteArrayInputStream(this.data);
        }

        public long size() {
            return this.data.length;
        }
    }

    public static class ClasspathEntry implements FileBlob {
        private URL url;
        private String filename;
        private String hash;
        private File file;
        private boolean lazyJar;
        private byte[] data;
        private Map<String, Object> marks;

        public ClasspathEntry() {
        }

        public synchronized void setMark(String key, Object value) {
            if (this.marks == null) {
                this.marks = new HashMap();
            }

            this.marks.put(key, value);
        }

//        public synchronized <T> T getMark(String key) {
//            return this.marks == null ? null : this.marks.get(key);
//        }

        public URL getUrl() {
            return this.url;
        }

        public File getLocalFile() {
            return this.file;
        }

        public String getFileName() {
            return this.filename;
        }

        public synchronized String getContentHash() {
            if (this.hash == null) {
                this.hash = StreamHelper.digest(this.getData(), "SHA-1");
            }

            return this.hash;
        }

        public synchronized InputStream getContent() {
            this.ensureData();

            try {
                return this.data != null ? new ByteArrayInputStream(this.data) : new FileInputStream(this.file);
            } catch (FileNotFoundException var2) {
                throw new RuntimeException(var2.getMessage());
            }
        }

        private synchronized void ensureData() {
            if (this.lazyJar) {
                try {
                    this.data = ClasspathUtils.jarFiles(this.file.getPath());
                    this.lazyJar = false;
                } catch (IOException var2) {
                    throw new RuntimeException(var2);
                }
            }

        }

        public long size() {
            this.ensureData();
            return this.data != null ? (long) this.data.length : this.file.length();
        }

        public synchronized byte[] getData() {
            this.ensureData();
            return this.data != null ? this.data : StreamHelper.readFile(this.file);
        }

        public String toString() {
            return this.filename;
        }
    }
}
