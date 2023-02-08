/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.dx.command.dexer;

import com.android.dex.DexFormat;
import com.android.dex.util.FileUtils;
import com.android.dx.Version;
import com.android.dx.cf.code.SimException;
import com.android.dx.cf.direct.ClassPathOpener;
import com.android.dx.cf.direct.ClassPathOpener.FileNameFilter;
import com.android.dx.cf.direct.DirectClassFile;
import com.android.dx.cf.direct.StdAttributeFactory;
import com.android.dx.cf.iface.ParseException;
import com.android.dx.command.UsageException;
import com.android.dx.dex.DexOptions;
import com.android.dx.dex.cf.CfOptions;
import com.android.dx.dex.cf.CfTranslator;
import com.android.dx.dex.code.PositionList;
import com.android.dx.dex.file.ClassDefItem;
import com.android.dx.dex.file.DexFile;
import com.android.dx.dex.file.EncodedMethod;
import com.android.dx.rop.annotation.Annotation;
import com.android.dx.rop.annotation.Annotations;
import com.android.dx.rop.annotation.AnnotationsList;
import com.android.dx.rop.cst.CstNat;
import com.android.dx.rop.cst.CstString;

import org.microemu.android.asm.AndroidProducer;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Main class for the class file translator.
 */
public class Main {

    public static final int CONCURRENCY_LEVEL = 4;

    /**
     * {@code non-null;} the lengthy message that tries to discourage
     * people from defining core classes in applications
     */
    private static final String IN_RE_CORE_CLASSES =
        "Ill-advised or mistaken usage of a core class (java.* or javax.*)\n" +
        "when not building a core library.\n\n" +
        "This is often due to inadvertently including a core library file\n" +
        "in your application's project, when using an IDE (such as\n" +
        "Eclipse). If you are sure you're not intentionally defining a\n" +
        "core class, then this is the most likely explanation of what's\n" +
        "going on.\n\n" +
        "However, you might actually be trying to define a class in a core\n" +
        "namespace, the source of which you may have taken, for example,\n" +
        "from a non-Android virtual machine project. This will most\n" +
        "assuredly not work. At a minimum, it jeopardizes the\n" +
        "compatibility of your app with future versions of the platform.\n" +
        "It is also often of questionable legality.\n\n" +
        "If you really intend to build a core library -- which is only\n" +
        "appropriate as part of creating a full virtual machine\n" +
        "distribution, as opposed to compiling an application -- then use\n" +
        "the \"--core-library\" option to suppress this error message.\n\n" +
        "If you go ahead and use \"--core-library\" but are in fact\n" +
        "building an application, then be forewarned that your application\n" +
        "will still fail to build or run, at some point. Please be\n" +
        "prepared for angry customers who find, for example, that your\n" +
        "application ceases to function once they upgrade their operating\n" +
        "system. You will be to blame for this problem.\n\n" +
        "If you are legitimately using some code that happens to be in a\n" +
        "core package, then the easiest safe alternative you have is to\n" +
        "repackage that code. That is, move the classes in question into\n" +
        "your own package namespace. This means that they will never be in\n" +
        "conflict with core system classes. JarJar is a tool that may help\n" +
        "you in this endeavor. If you find that you cannot do this, then\n" +
        "that is an indication that the path you are on will ultimately\n" +
        "lead to pain, suffering, grief, and lamentation.\n";

    /**
     * {@code non-null;} name of the standard manifest file in {@code .jar}
     * files
     */
    private static final String MANIFEST_NAME = "META-INF/MANIFEST.MF";

    /**
     * {@code non-null;} attribute name for the (quasi-standard?)
     * {@code Created-By} attribute
     */
    private static final Attributes.Name CREATED_BY =
        new Attributes.Name("Created-By");

    /**
     * {@code non-null;} list of {@code javax} subpackages that are considered
     * to be "core". <b>Note:</b>: This list must be sorted, since it
     * is binary-searched.
     */
    private static final String[] JAVAX_CORE = {
        "accessibility", "crypto", "imageio", "management", "microedition", "naming", "net",
        "print", "rmi", "security", "sip", "sound", "sql", "swing",
        "transaction", "xml"
    };

    /** number of errors during processing */
    private AtomicInteger errors = new AtomicInteger(0);

    /** {@code non-null;} parsed command-line arguments */
    private Arguments args;

    /** {@code non-null;} output file in-progress */
    private DexFile outputDex;

    /**
     * {@code null-ok;} map of resources to include in the output, or
     * {@code null} if resources are being ignored
     */
    private TreeMap<String, byte[]> outputResources;

    /** Thread pool object used for multi-thread class translation. */
    private ExecutorService classTranslatorPool;

    /** Single thread executor, for collecting results of parallel translation,
     * and adding classes to dex file in original input file order. */
    private ExecutorService classDefItemConsumer;

    /** Futures for {@code classDefItemConsumer} tasks. */
    private List<Future<Boolean>> addToDexFutures =
            new ArrayList<Future<Boolean>>();

    /** true if any files are successfully processed */
    private volatile boolean anyFilesProcessed;

    /** class files older than this must be defined in the target dex file. */
    private long minimumFileAge = 0;

    private OutputStreamWriter humanOutWriter = null;

    private final DxContext context;

    public Main(DxContext context) {
        this.context = context;
    }

    /**
     * Run and exit if something unexpected happened.
     * @param argArray the command line arguments
     */
    public static void main(String[] argArray) throws IOException {
        DxContext context = new DxContext();
        Arguments arguments = new Arguments(context);
        arguments.parse(argArray);

        int result = new Main(context).runDx(arguments);

        if (result != 0) {
            throw new IOException();
        }
    }

    /**
     * Run and return a result code.
     * @param arguments the data + parameters for the conversion
     * @return 0 if success &gt; 0 otherwise.
     */
    public static int run(Arguments arguments) throws IOException {
        return new Main(new DxContext()).runDx(arguments);
    }

    public int runDx(Arguments arguments) throws IOException {

        // Reset the error count to start fresh.
        errors.set(0);

        args = arguments;
        args.makeOptionsObjects();

        OutputStream humanOutRaw = null;
        if (args.humanOutName != null) {
            humanOutRaw = openOutput(args.humanOutName);
            humanOutWriter = new OutputStreamWriter(humanOutRaw);
        }

        try {
            return runMonoDex();
        } finally {
            closeOutput(humanOutRaw);
        }
    }

    private int runMonoDex() throws IOException {

        if (!processAllFiles()) {
            return 1;
        }

        // this array is null if no classes were defined
        byte[] outArray = null;

        if (!outputDex.isEmpty() || (args.humanOutName != null)) {
            outArray = writeDex(outputDex);

            if (outArray == null) {
                return 2;
            }
        }

        if (args.jarOutput) {
            // Effectively free up the (often massive) DexFile memory.
            outputDex = null;

            if (outArray != null) {
                outputResources.put(DexFormat.DEX_IN_JAR_NAME, outArray);
            }
            if (!createJar(args.outName)) {
                return 3;
            }
        } else if (outArray != null && args.outName != null) {
            OutputStream out = openOutput(args.outName);
            out.write(outArray);
            closeOutput(out);
        }

        return 0;
    }

    private static void readPathsFromFile(String fileName, Collection<String> paths) throws IOException {
        BufferedReader bfr = null;
        try {
            FileReader fr = new FileReader(fileName);
            bfr = new BufferedReader(fr);

            String line;

            while (null != (line = bfr.readLine())) {
                paths.add(fixPath(line));
            }

        } finally {
            if (bfr != null) {
                bfr.close();
            }
        }
    }

    /**
     * Constructs the output {@link DexFile}, fill it in with all the
     * specified classes, and populate the resources map if required.
     *
     * @return whether processing was successful
     */
    private boolean processAllFiles() {
        createDexFile();

        if (args.jarOutput) {
            outputResources = new TreeMap<String, byte[]>();
        }

        anyFilesProcessed = false;
        String[] fileNames = args.fileNames;
        Arrays.sort(fileNames);

        // translate classes in parallel
        classTranslatorPool = new ThreadPoolExecutor(args.numThreads,
               args.numThreads, 0, TimeUnit.SECONDS,
               new ArrayBlockingQueue<Runnable>(2 * args.numThreads, true),
               new ThreadPoolExecutor.CallerRunsPolicy());
        // collect translated and write to dex in order
        classDefItemConsumer = Executors.newSingleThreadExecutor();


        try {
            for (int i = 0; i < fileNames.length; i++) {
                processOne(fileNames[i], path -> path.endsWith(".class"));
            }
        } catch (StopProcessing ex) {
            /*
             * Ignore it and just let the error reporting do
             * their things.
             */
        }

        try {
            classTranslatorPool.shutdown();
            classTranslatorPool.awaitTermination(600L, TimeUnit.SECONDS);
            classDefItemConsumer.shutdown();
            classDefItemConsumer.awaitTermination(600L, TimeUnit.SECONDS);

            for (Future<Boolean> f : addToDexFutures) {
                try {
                    f.get();
                } catch(ExecutionException ex) {
                    // Catch any previously uncaught exceptions from
                    // class translation and adding to dex.
                    int count = errors.incrementAndGet();
                    if (count < 10) {
                        if (args.debug) {
                            context.err.println("Uncaught translation error:");
                            ex.getCause().printStackTrace(context.err);
                        } else {
                            context.err.println("Uncaught translation error: " + ex.getCause());
                        }
                    } else {
                        throw new InterruptedException("Too many errors");
                    }
                }
            }

        } catch (InterruptedException ie) {
            classTranslatorPool.shutdownNow();
            classDefItemConsumer.shutdownNow();
            throw new RuntimeException("Translation has been interrupted", ie);
        } catch (Exception e) {
            classTranslatorPool.shutdownNow();
            classDefItemConsumer.shutdownNow();
            e.printStackTrace(context.out);
            throw new RuntimeException("Unexpected exception in translator thread.", e);
        }

        int errorNum = errors.get();
        if (errorNum != 0) {
            context.err.println(errorNum + " error" +
                    ((errorNum == 1) ? "" : "s") + "; aborting");
            return false;
        }

        if (!(anyFilesProcessed || args.emptyOk)) {
            context.err.println("no classfiles specified");
            return false;
        }

        if (args.optimize && args.statistics) {
            context.codeStatistics.dumpStatistics(context.out);
        }

        return true;
    }

    private void createDexFile() {
        outputDex = new DexFile(args.dexOptions);

        if (args.dumpWidth != 0) {
            outputDex.setDumpWidth(args.dumpWidth);
        }
    }

    /**
     * Processes one pathname element.
     *
     * @param pathname {@code non-null;} the pathname to process. May
     * be the path of a class file, a jar file, or a directory
     * containing class files.
     * @param filter {@code non-null;} A filter for excluding files.
     */
    private void processOne(String pathname, FileNameFilter filter) {
        ClassPathOpener opener;

        opener = new ClassPathOpener(pathname, true, filter, new FileBytesConsumer());

        if (opener.process()) {
          updateStatus(true);
        }
    }

    private void updateStatus(boolean res) {
        anyFilesProcessed |= res;
    }


    /**
     * Processes one file, which may be either a class or a resource.
     *
     * @param name {@code non-null;} name of the file
     * @param bytes {@code non-null;} contents of the file
     * @return whether processing was successful
     */
    private boolean processFileBytes(String name, long lastModified, byte[] bytes) {

        boolean isClass = name != null && name.toLowerCase().endsWith(".class");
        boolean keepResources = (outputResources != null);

        if (!isClass && !keepResources) {
            if (args.verbose) {
                context.out.println("ignored resource " + name);
            }
            return false;
        }

        if (args.verbose) {
            context.out.println("processing " + name + "...");
        }

        String fixedName = fixPath(name);

        if (isClass) {

            if (keepResources && args.keepClassesInJar) {
                synchronized (outputResources) {
                    outputResources.put(fixedName, bytes);
                }
            }
            if (lastModified < minimumFileAge) {
                return true;
            }
            processClass(fixedName, bytes);
            // Assume that an exception may occur. Status will be updated
            // asynchronously, if the class compiles without error.
            return false;
        } else {
            synchronized (outputResources) {
                outputResources.put(fixedName, bytes);
            }
            return true;
        }
    }

    /**
     * Processes one classfile.
     *
     * @param name {@code non-null;} name of the file, clipped such that it
     * <i>should</i> correspond to the name of the class it contains
     * @param bytes {@code non-null;} contents of the file
     * @return whether processing was successful
     */
    private boolean processClass(String name, byte[] bytes) {
        if (! args.coreLibrary) {
            checkClassName(name);
        }

        try {
            // modify byte-code with ASM-java
            bytes = AndroidProducer.instrument(bytes, name);

            new DirectClassFileConsumer(name, bytes, null).call(
                    new ClassParserTask(name, bytes).call());
        } catch (ParseException ex) {
            // handled in FileBytesConsumer
            throw ex;
        } catch(IllegalArgumentException e) {
            e.printStackTrace();
        } catch(Exception ex) {
            throw new RuntimeException("Exception parsing classes", ex);
        }

        return true;
    }


    private DirectClassFile parseClass(String name, byte[] bytes) {

        DirectClassFile cf = new DirectClassFile(bytes, name,
                args.cfOptions.strictNameCheck);
        cf.setAttributeFactory(StdAttributeFactory.THE_ONE);
        cf.getMagic(); // triggers the actual parsing
        return cf;
    }

    private ClassDefItem translateClass(byte[] bytes, DirectClassFile cf) {
        try {
            return CfTranslator.translate(context, cf, bytes, args.cfOptions,
                    args.dexOptions, outputDex);
        } catch (ParseException ex) {
            context.err.println("\ntrouble processing:");
            if (args.debug) {
                ex.printStackTrace(context.err);
            } else {
                ex.printContext(context.err);
            }
        }
        errors.incrementAndGet();
        return null;
    }

    private boolean addClassToDex(ClassDefItem clazz) {
        synchronized (outputDex) {
            outputDex.add(clazz);
        }
        return true;
    }

    /**
     * Check the class name to make sure it's not a "core library"
     * class. If there is a problem, this updates the error count and
     * throws an exception to stop processing.
     *
     * @param name {@code non-null;} the fully-qualified internal-form
     * class name
     */
    private void checkClassName(String name) {
        boolean bogus = false;

        if (name.startsWith("java/")) {
            bogus = true;
        } else if (name.startsWith("javax/")) {
            int slashAt = name.indexOf('/', 6);
            if (slashAt == -1) {
                // Top-level javax classes are verboten.
                bogus = true;
            } else {
                String pkg = name.substring(6, slashAt);
                bogus = (Arrays.binarySearch(JAVAX_CORE, pkg) >= 0);
            }
        }

        if (! bogus) {
            return;
        }

        /*
         * The user is probably trying to include an entire desktop
         * core library in a misguided attempt to get their application
         * working. Try to help them understand what's happening.
         */

        context.err.println("\ntrouble processing \"" + name + "\":\n\n" +
                IN_RE_CORE_CLASSES);
        errors.incrementAndGet();
        throw new StopProcessing();
    }

    /**
     * Converts {@link #outputDex} into a {@code byte[]} and do whatever
     * human-oriented dumping is required.
     *
     * @return {@code null-ok;} the converted {@code byte[]} or {@code null}
     * if there was a problem
     */
    private byte[] writeDex(DexFile outputDex) {
        byte[] outArray = null;

        try {
            try {
                if (args.methodToDump != null) {
                    /*
                     * Simply dump the requested method. Note: The call
                     * to toDex() is required just to get the underlying
                     * structures ready.
                     */
                    outputDex.toDex(null, false);
                    dumpMethod(outputDex, args.methodToDump, humanOutWriter);
                } else {
                    /*
                     * This is the usual case: Create an output .dex file,
                     * and write it, dump it, etc.
                     */
                    outArray = outputDex.toDex(humanOutWriter, args.verboseDump);
                }

                if (args.statistics) {
                    context.out.println(outputDex.getStatistics().toHuman());
                }
            } finally {
                if (humanOutWriter != null) {
                    humanOutWriter.flush();
                }
            }
        } catch (Exception ex) {
            if (args.debug) {
                context.err.println("\ntrouble writing output:");
                ex.printStackTrace(context.err);
            } else {
                context.err.println("\ntrouble writing output: " +
                                   ex.getMessage());
            }
            return null;
        }
        return outArray;
    }

    /**
     * Creates a jar file from the resources (including dex file arrays).
     *
     * @param fileName {@code non-null;} name of the file
     * @return whether the creation was successful
     */
    private boolean createJar(String fileName) {
        /*
         * Make or modify the manifest (as appropriate), put the dex
         * array into the resources map, and then process the entire
         * resources map in a uniform manner.
         */

        try {
            Manifest manifest = makeManifest();
            OutputStream out = openOutput(fileName);
            JarOutputStream jarOut = new JarOutputStream(out, manifest);

            try {
                for (Map.Entry<String, byte[]> e :
                         outputResources.entrySet()) {
                    String name = e.getKey();
                    byte[] contents = e.getValue();
                    JarEntry entry = new JarEntry(name);
                    int length = contents.length;

                    if (args.verbose) {
                        context.out.println("writing " + name + "; size " + length + "...");
                    }

                    entry.setSize(length);
                    jarOut.putNextEntry(entry);
                    jarOut.write(contents);
                    jarOut.closeEntry();
                }
            } finally {
                jarOut.finish();
                jarOut.flush();
                closeOutput(out);
            }
        } catch (Exception ex) {
            if (args.debug) {
                context.err.println("\ntrouble writing output:");
                ex.printStackTrace(context.err);
            } else {
                context.err.println("\ntrouble writing output: " +
                                   ex.getMessage());
            }
            return false;
        }

        return true;
    }

    /**
     * Creates and returns the manifest to use for the output. This may
     * modify {@link #outputResources} (removing the pre-existing manifest).
     *
     * @return {@code non-null;} the manifest
     */
    private Manifest makeManifest() throws IOException {
        byte[] manifestBytes = outputResources.get(MANIFEST_NAME);
        Manifest manifest;
        Attributes attribs;

        if (manifestBytes == null) {
            // We need to construct an entirely new manifest.
            manifest = new Manifest();
            attribs = manifest.getMainAttributes();
            attribs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        } else {
            manifest = new Manifest(new ByteArrayInputStream(manifestBytes));
            attribs = manifest.getMainAttributes();
            outputResources.remove(MANIFEST_NAME);
        }

        String createdBy = attribs.getValue(CREATED_BY);
        if (createdBy == null) {
            createdBy = "";
        } else {
            createdBy += " + ";
        }
        createdBy += "dx " + Version.VERSION;

        attribs.put(CREATED_BY, createdBy);
        attribs.putValue("Dex-Location", DexFormat.DEX_IN_JAR_NAME);

        return manifest;
    }

    /**
     * Opens and returns the named file for writing, treating "-" specially.
     *
     * @param name {@code non-null;} the file name
     * @return {@code non-null;} the opened file
     */
    private OutputStream openOutput(String name) throws IOException {
        if (name.equals("-") ||
                name.startsWith("-.")) {
            return context.out;
        }

        return new FileOutputStream(name);
    }

    /**
     * Flushes and closes the given output stream, except if it happens to be
     * {@link System#out} in which case this method does the flush but not
     * the close. This method will also silently do nothing if given a
     * {@code null} argument.
     *
     * @param stream {@code null-ok;} what to close
     */
    private void closeOutput(OutputStream stream) throws IOException {
        if (stream == null) {
            return;
        }

        stream.flush();

        if (stream != context.out) {
            stream.close();
        }
    }

    /**
     * Returns the "fixed" version of a given file path, suitable for
     * use as a path within a {@code .jar} file and for checking
     * against a classfile-internal "this class" name. This looks for
     * the last instance of the substring {@code "/./"} within
     * the path, and if it finds it, it takes the portion after to be
     * the fixed path. If that isn't found but the path starts with
     * {@code "./"}, then that prefix is removed and the rest is
     * return. If neither of these is the case, this method returns
     * its argument.
     *
     * @param path {@code non-null;} the path to "fix"
     * @return {@code non-null;} the fixed version (which might be the same as
     * the given {@code path})
     */
    private static String fixPath(String path) {
        /*
         * If the path separator is \ (like on windows), we convert the
         * path to a standard '/' separated path.
         */
        if (File.separatorChar == '\\') {
            path = path.replace('\\', '/');
        }

        int index = path.lastIndexOf("/./");

        if (index != -1) {
            return path.substring(index + 3);
        }

        if (path.startsWith("./")) {
            return path.substring(2);
        }

        return path;
    }

    /**
     * Dumps any method with the given name in the given file.
     *
     * @param dex {@code non-null;} the dex file
     * @param fqName {@code non-null;} the fully-qualified name of the
     * method(s)
     * @param out {@code non-null;} where to dump to
     */
    private void dumpMethod(DexFile dex, String fqName,
            OutputStreamWriter out) {
        boolean wildcard = fqName.endsWith("*");
        int lastDot = fqName.lastIndexOf('.');

        if ((lastDot <= 0) || (lastDot == (fqName.length() - 1))) {
            context.err.println("bogus fully-qualified method name: " +
                               fqName);
            return;
        }

        String className = fqName.substring(0, lastDot).replace('.', '/');
        String methodName = fqName.substring(lastDot + 1);
        ClassDefItem clazz = dex.getClassOrNull(className);

        if (clazz == null) {
            context.err.println("no such class: " + className);
            return;
        }

        if (wildcard) {
            methodName = methodName.substring(0, methodName.length() - 1);
        }

        ArrayList<EncodedMethod> allMeths = clazz.getMethods();
        TreeMap<CstNat, EncodedMethod> meths =
            new TreeMap<CstNat, EncodedMethod>();

        /*
         * Figure out which methods to include in the output, and get them
         * all sorted, so that the printout code is robust with respect to
         * changes in the underlying order.
         */
        for (EncodedMethod meth : allMeths) {
            String methName = meth.getName().getString();
            if ((wildcard && methName.startsWith(methodName)) ||
                (!wildcard && methName.equals(methodName))) {
                meths.put(meth.getRef().getNat(), meth);
            }
        }

        if (meths.size() == 0) {
            context.err.println("no such method: " + fqName);
            return;
        }

        PrintWriter pw = new PrintWriter(out);

        for (EncodedMethod meth : meths.values()) {
            // TODO: Better stuff goes here, perhaps.
            meth.debugPrint(pw, args.verboseDump);

            /*
             * The (default) source file is an attribute of the class, but
             * it's useful to see it in method dumps.
             */
            CstString sourceFile = clazz.getSourceFile();
            if (sourceFile != null) {
                pw.println("  source file: " + sourceFile.toQuoted());
            }

            Annotations methodAnnotations =
                clazz.getMethodAnnotations(meth.getRef());
            AnnotationsList parameterAnnotations =
                clazz.getParameterAnnotations(meth.getRef());

            if (methodAnnotations != null) {
                pw.println("  method annotations:");
                for (Annotation a : methodAnnotations.getAnnotations()) {
                    pw.println("    " + a);
                }
            }

            if (parameterAnnotations != null) {
                pw.println("  parameter annotations:");
                int sz = parameterAnnotations.size();
                for (int i = 0; i < sz; i++) {
                    pw.println("    parameter " + i);
                    Annotations annotations = parameterAnnotations.get(i);
                    for (Annotation a : annotations.getAnnotations()) {
                        pw.println("      " + a);
                    }
                }
            }
        }

        pw.flush();
    }

    /**
     * Exception class used to halt processing prematurely.
     */
    private static class StopProcessing extends RuntimeException {
        // This space intentionally left blank.
    }

    /**
     * Command-line argument parser and access.
     */
    public static class Arguments {

        private static final String NUM_THREADS_OPTION = "--num-threads";

        private static final String INPUT_LIST_OPTION = "--input-list";

        public final DxContext context;

        /** whether to run in debug mode */
        public boolean debug = false;

        /** whether to emit warning messages */
        public boolean warnings = true;

        /** whether to emit high-level verbose human-oriented output */
        public boolean verbose = false;

        /** whether to emit verbose human-oriented output in the dump file */
        public boolean verboseDump = false;

        /** whether we are constructing a core library */
        public boolean coreLibrary = false;

        /** {@code null-ok;} particular method to dump */
        public String methodToDump = null;

        /** max width for columnar output */
        public int dumpWidth = 0;

        /** {@code null-ok;} output file name for binary file */
        public String outName = null;

        /** {@code null-ok;} output file name for human-oriented dump */
        public String humanOutName = null;

        /** whether strict file-name-vs-class-name checking should be done */
        public boolean strictNameCheck = true;

        /**
         * whether it is okay for there to be no {@code .class} files
         * to process
         */
        public boolean emptyOk = false;

        /**
         * whether the binary output is to be a {@code .jar} file
         * instead of a plain {@code .dex}
         */
        public boolean jarOutput = false;

        /**
         * when writing a {@code .jar} file, whether to still
         * keep the {@code .class} files
         */
        public boolean keepClassesInJar = false;

        /** what API level to target */
        public int minSdkVersion = DexFormat.API_NO_EXTENDED_OPCODES;

        /** how much source position info to preserve */
        public int positionInfo = PositionList.LINES;

        /** whether to keep local variable information */
        public boolean localInfo = true;

        /** whether to force generation of const-string/jumbo for all indexes,
         *  to allow merges between dex files with many strings. */
        public boolean forceJumbo = false;

        /** {@code non-null} after {@link #parse}; file name arguments */
        public String[] fileNames;

        /** whether to do SSA/register optimization */
        public boolean optimize = true;

        /** Filename containg list of methods to optimize */
        public String optimizeListFile = null;

        /** Filename containing list of methods to NOT optimize */
        public String dontOptimizeListFile = null;

        /** Whether to print statistics to stdout at end of compile cycle */
        public boolean statistics;

        /** Options for class file transformation */
        public CfOptions cfOptions;

        /** Options for dex file output */
        public DexOptions dexOptions;

        /** number of threads to run with */
        public int numThreads = 1;

        /** Optional list containing inputs read in from a file. */
        private List<String> inputList = null;

        private boolean outputIsDirectory = false;

        public Arguments(DxContext context) {
            this.context = context;
        }

        public Arguments() {
            this(new DxContext());
        }

        private static class ArgumentsParser {

            /** The arguments to process. */
            private final String[] arguments;
            /** The index of the next argument to process. */
            private int index;
            /** The current argument being processed after a {@link #getNext()} call. */
            private String current;
            /** The last value of an argument processed by {@link #isArg(String)}. */
            private String lastValue;

            public ArgumentsParser(String[] arguments) {
                this.arguments = arguments;
                index = 0;
            }

            public String getCurrent() {
                return current;
            }

            public String getLastValue() {
                return lastValue;
            }

            /**
             * Moves on to the next argument.
             * Returns false when we ran out of arguments that start with --.
             */
            public boolean getNext() {
                if (index >= arguments.length) {
                    return false;
                }
                current = arguments[index];
                if (current.equals("--") || !current.startsWith("--")) {
                    return false;
                }
                index++;
                return true;
            }

            /**
             * Similar to {@link #getNext()}, this moves on the to next argument.
             * It does not check however whether the argument starts with --
             * and thus can be used to retrieve values.
             */
            private boolean getNextValue() {
                if (index >= arguments.length) {
                    return false;
                }
                current = arguments[index];
                index++;
                return true;
            }

            /**
             * Returns all the arguments that have not been processed yet.
             */
            public String[] getRemaining() {
                int n = arguments.length - index;
                String[] remaining = new String[n];
                if (n > 0) {
                    System.arraycopy(arguments, index, remaining, 0, n);
                }
                return remaining;
            }

            /**
             * Checks the current argument against the given prefix.
             * If prefix is in the form '--name=', an extra value is expected.
             * The argument can then be in the form '--name=value' or as a 2-argument
             * form '--name value'.
             */
            public boolean isArg(String prefix) {
                int n = prefix.length();
                if (n > 0 && prefix.charAt(n-1) == '=') {
                    // Argument accepts a value. Capture it.
                    if (current.startsWith(prefix)) {
                        // Argument is in the form --name=value, split the value out
                        lastValue = current.substring(n);
                        return true;
                    } else {
                        // Check whether we have "--name value" as 2 arguments
                        prefix = prefix.substring(0, n-1);
                        if (current.equals(prefix)) {
                            if (getNextValue()) {
                                lastValue = current;
                                return true;
                            } else {
                                System.err.println("Missing value after parameter " + prefix);
                                throw new UsageException();
                            }
                        }
                        return false;
                    }
                } else {
                    // Argument does not accept a value.
                    return current.equals(prefix);
                }
            }
        }

        private void parseFlags(ArgumentsParser parser) {

            while(parser.getNext()) {
                if (parser.isArg("--debug")) {
                    debug = true;
                } else if (parser.isArg("--no-warning")) {
                    warnings = false;
                } else if (parser.isArg("--verbose")) {
                    verbose = true;
                } else if (parser.isArg("--verbose-dump")) {
                    verboseDump = true;
                } else if (parser.isArg("--no-files")) {
                    emptyOk = true;
                } else if (parser.isArg("--no-optimize")) {
                    optimize = false;
                } else if (parser.isArg("--no-strict")) {
                    strictNameCheck = false;
                } else if (parser.isArg("--core-library")) {
                    coreLibrary = true;
                } else if (parser.isArg("--statistics")) {
                    statistics = true;
                } else if (parser.isArg("--optimize-list=")) {
                    if (dontOptimizeListFile != null) {
                        context.err.println("--optimize-list and "
                                + "--no-optimize-list are incompatible.");
                        throw new UsageException();
                    }
                    optimize = true;
                    optimizeListFile = parser.getLastValue();
                } else if (parser.isArg("--no-optimize-list=")) {
                    if (dontOptimizeListFile != null) {
                        context.err.println("--optimize-list and "
                                + "--no-optimize-list are incompatible.");
                        throw new UsageException();
                    }
                    optimize = true;
                    dontOptimizeListFile = parser.getLastValue();
                } else if (parser.isArg("--keep-classes")) {
                    keepClassesInJar = true;
                } else if (parser.isArg("--output=")) {
                    outName = parser.getLastValue();
                    if (new File(outName).isDirectory()) {
                        jarOutput = false;
                        outputIsDirectory = true;
                    } else if (FileUtils.hasArchiveSuffix(outName)) {
                        jarOutput = true;
                    } else if (outName.endsWith(".dex") ||
                               outName.equals("-")) {
                        jarOutput = false;
                    } else {
                        context.err.println("unknown output extension: " +
                                outName);
                        throw new UsageException();
                    }
                } else if (parser.isArg("--dump-to=")) {
                    humanOutName = parser.getLastValue();
                } else if (parser.isArg("--dump-width=")) {
                    dumpWidth = Integer.parseInt(parser.getLastValue());
                } else if (parser.isArg("--dump-method=")) {
                    methodToDump = parser.getLastValue();
                    jarOutput = false;
                } else if (parser.isArg("--positions=")) {
                    String pstr = parser.getLastValue().intern();
                    if (pstr == "none") {
                        positionInfo = PositionList.NONE;
                    } else if (pstr == "important") {
                        positionInfo = PositionList.IMPORTANT;
                    } else if (pstr == "lines") {
                        positionInfo = PositionList.LINES;
                    } else {
                        context.err.println("unknown positions option: " +
                                pstr);
                        throw new UsageException();
                    }
                } else if (parser.isArg("--no-locals")) {
                    localInfo = false;
                } else if (parser.isArg(NUM_THREADS_OPTION + "=")) {
                    numThreads = Integer.parseInt(parser.getLastValue());
                } else if (parser.isArg("--force-jumbo")) {
                    forceJumbo = true;
                } else if(parser.isArg(INPUT_LIST_OPTION + "=")) {
                    File inputListFile = new File(parser.getLastValue());
                    try {
                        inputList = new ArrayList<String>();
                        readPathsFromFile(inputListFile.getAbsolutePath(), inputList);
                    } catch (IOException e) {
                        context.err.println(
                            "Unable to read input list file: " + inputListFile.getName());
                        // problem reading the file so we should halt execution
                        throw new UsageException();
                    }
                } else if (parser.isArg("--min-sdk-version=")) {
                    String arg = parser.getLastValue();
                    int value;
                    try {
                        value = Integer.parseInt(arg);
                    } catch (NumberFormatException ex) {
                        value = -1;
                    }
                    if (value < 1) {
                        System.err.println("improper min-sdk-version option: " + arg);
                        throw new UsageException();
                    }
                    minSdkVersion = value;
                } else {
                    context.err.println("unknown option: " + parser.getCurrent());
                    throw new UsageException();
                }
            }
        }


        /**
         * Parses all command-line arguments and updates the state of the {@code Arguments} object
         * accordingly.
         *
         * @param args {@code non-null;} the arguments
         */
        private void parse(String[] args) {
            ArgumentsParser parser = new ArgumentsParser(args);

            parseFlags(parser);

            fileNames = parser.getRemaining();
            if(inputList != null && !inputList.isEmpty()) {
                // append the file names to the end of the input list
                inputList.addAll(Arrays.asList(fileNames));
                fileNames = inputList.toArray(new String[inputList.size()]);
            }

            if (fileNames.length == 0) {
                if (!emptyOk) {
                    context.err.println("no input files specified");
                    throw new UsageException();
                }
            } else if (emptyOk) {
                context.out.println("ignoring input files");
            }

            if ((humanOutName == null) && (methodToDump != null)) {
                humanOutName = "-";
            }

            if (outputIsDirectory) {
                outName = new File(outName, DexFormat.DEX_IN_JAR_NAME).getPath();
            }

            makeOptionsObjects();
        }

        /**
         * Parses only command-line flags and updates the state of the {@code Arguments} object
         * accordingly.
         *
         * @param flags {@code non-null;} the flags
         */
        public void parseFlags(String[] flags) {
            parseFlags(new ArgumentsParser(flags));
        }

        /**
         * Copies relevant arguments over into CfOptions and DexOptions instances.
         */
        public void makeOptionsObjects() {
            cfOptions = new CfOptions();
            cfOptions.positionInfo = positionInfo;
            cfOptions.localInfo = localInfo;
            cfOptions.strictNameCheck = strictNameCheck;
            cfOptions.optimize = optimize;
            cfOptions.optimizeListFile = optimizeListFile;
            cfOptions.dontOptimizeListFile = dontOptimizeListFile;
            cfOptions.statistics = statistics;

            if (warnings) {
                cfOptions.warn = context.err;
            } else {
                cfOptions.warn = context.noop;
            }

            dexOptions = new DexOptions();
            dexOptions.minSdkVersion = minSdkVersion;
            dexOptions.forceJumbo = forceJumbo;
        }
    }

    /**
     * Callback class for processing input file bytes, produced by the
     * ClassPathOpener.
     */
    private class FileBytesConsumer implements ClassPathOpener.Consumer {

        @Override
        public boolean processFileBytes(String name, long lastModified,
                byte[] bytes)   {
            return Main.this.processFileBytes(name, lastModified, bytes);
        }

        @Override
        public void onException(Exception ex) {
            if (ex instanceof StopProcessing) {
                throw (StopProcessing) ex;
            } else if (ex instanceof SimException) {
                context.err.println("\nEXCEPTION FROM SIMULATION:");
                context.err.println(ex.getMessage() + "\n");
                context.err.println(((SimException) ex).getContext());
            } else if (ex instanceof ParseException) {
                context.err.println("\nPARSE ERROR:");
                ParseException parseException = (ParseException) ex;
                if (args.debug) {
                    parseException.printStackTrace(context.err);
                } else {
                    parseException.printContext(context.err);
                }
            } else {
                context.err.println("\nUNEXPECTED TOP-LEVEL EXCEPTION:");
                ex.printStackTrace(context.err);
            }
            errors.incrementAndGet();
        }

        @Override
        public void onProcessArchiveStart(File file) {
            if (args.verbose) {
                context.out.println("processing archive " + file + "...");
            }
        }
    }

    /** Callable helper class to parse class bytes. */
    private class ClassParserTask implements Callable<DirectClassFile> {

        String name;
        byte[] bytes;

        private ClassParserTask(String name, byte[] bytes) {
            this.name = name;
            this.bytes = bytes;
        }

        @Override
        public DirectClassFile call() throws Exception {
            DirectClassFile cf =  parseClass(name, bytes);

            return cf;
        }
    }

    /**
     * Callable helper class used to sequentially collect the results of
     * the (optionally parallel) translation phase, in correct input file order.
     * This class is also responsible for coordinating dex file rotation
     * with the ClassDefItemConsumer class.
     * We maintain invariant that the number of indices used in the current
     * dex file plus the max number of indices required by classes passed to
     * the translation phase and not yet added to the dex file, is less than
     * or equal to the dex file limit.
     * For each parsed file, we estimate the maximum number of indices it may
     * require. If passing the file to the translation phase would invalidate
     * the invariant, we wait, until the next class is added to the dex file,
     * and then reevaluate the invariant. If there are no further classes in
     * the translation phase, we rotate the dex file.
     */
    private class DirectClassFileConsumer implements Callable<Boolean> {

        String name;
        byte[] bytes;
        Future<DirectClassFile> dcff;

        private DirectClassFileConsumer(String name, byte[] bytes,
                Future<DirectClassFile> dcff) {
            this.name = name;
            this.bytes = bytes;
            this.dcff = dcff;
        }

        @Override
        public Boolean call() throws Exception {

            DirectClassFile cf = dcff.get();
            return call(cf);
        }

        private Boolean call(DirectClassFile cf) {

            // Submit class to translation phase.
            Future<ClassDefItem> cdif = classTranslatorPool.submit(
                    new ClassTranslatorTask(bytes, cf));
            Future<Boolean> res = classDefItemConsumer.submit(new ClassDefItemConsumer(
                    name, cdif));
            addToDexFutures.add(res);

            return true;
        }
    }


    /** Callable helper class to translate classes in parallel  */
    private class ClassTranslatorTask implements Callable<ClassDefItem> {

        byte[] bytes;
        DirectClassFile classFile;

        private ClassTranslatorTask(byte[] bytes,
                                    DirectClassFile classFile) {
            this.bytes = bytes;
            this.classFile = classFile;
        }

        @Override
        public ClassDefItem call() {
            ClassDefItem clazz = translateClass(bytes, classFile);
            return clazz;
        }
    }

    /**
     * Callable helper class used to collect the results of
     * the parallel translation phase, adding the translated classes to
     * the current dex file in correct (deterministic) file order.
     * This class is also responsible for coordinating dex file rotation
     * with the DirectClassFileConsumer class.
     */
    private class ClassDefItemConsumer implements Callable<Boolean> {

        Future<ClassDefItem> futureClazz;

        private ClassDefItemConsumer(String name, Future<ClassDefItem> futureClazz) {
            this.futureClazz = futureClazz;
        }

        @Override
        public Boolean call() throws Exception {
            try {
                ClassDefItem clazz = futureClazz.get();
                if (clazz != null) {
                    addClassToDex(clazz);
                    updateStatus(true);
                }
                return true;
            } catch(ExecutionException ex) {
                // Rethrow previously uncaught translation exceptions.
                // These, as well as any exceptions from addClassToDex,
                // are handled and reported in processAllFiles().
                Throwable t = ex.getCause();
                throw (t instanceof Exception) ? (Exception) t : ex;
            }
        }
    }
}
