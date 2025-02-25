package com.asm.eb.transformer;

import com.asm.eb.logger.ExceptionLogger;
import com.asm.eb.model.Configuration;
import javassist.*;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URL;
import java.security.ProtectionDomain;

/**
 * Transformer that modifies the bytecode of java.lang.Throwable to enable exception logging.
 * It also provides optional class loader tracing if enabled in the configuration.
 *
 * @author asmishra
 * @since 2/13/2025
 */
public class ExceptionTransformer implements ClassFileTransformer {
    private final Configuration configuration;
    private final ExceptionLogger exceptionLogger;
    private static final String THROWABLE_CLASS_NAME_FORMATTED = "java/lang/Throwable";
    private static final String EB_PACKAGE = "com/asm/eb";

    /**
     * Constructs an ExceptionTransformer instance with the provided configuration and logger.
     *
     * @param configuration  The configuration settings for exception transformation.
     * @param exceptionLogger The logger instance used for recording class transformations and exceptions.
     */
    public ExceptionTransformer(Configuration configuration, ExceptionLogger exceptionLogger) {
        this.configuration = configuration;
        this.exceptionLogger = exceptionLogger;
    }

    /**
     * Transforms the bytecode of the specified class, injecting exception logging if applicable.
     *
     * @param loader              The defining class loader of the class being transformed.
     * @param className           The name of the class in internal JVM format.
     * @param classBeingRedefined The class being redefined (null if this is a new load).
     * @param protectionDomain    The protection domain of the class being defined.
     * @param classfileBuffer     The input bytecode of the class.
     * @return The transformed bytecode if modifications are made; otherwise, the original bytecode.
     * @throws IllegalClassFormatException If an invalid transformation occurs.
     */
    @Override
    public byte[] transform(ClassLoader loader, String className,
                            Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        //Skipping JDK classes because logging classloading events for JDK classes can cause circular dependencies (ClassCircularityError)
        if(configuration.isClassLoaderTracing() && className != null &&
                !className.startsWith("java/") && !className.startsWith("jdk/") &&
                !className.startsWith("sun/") && !className.startsWith("javax/") && !className.startsWith(EB_PACKAGE)) {
            String traceInfo;
            if (protectionDomain != null) {
                URL jarLocation = protectionDomain.getCodeSource().getLocation();
                traceInfo = "Class: " + className + "\n" + "ClassLoader Hierarchy: " + getClassLoaderHierarchy(loader) + "\nLoaded from: " + jarLocation.getPath();
            } else {
                traceInfo = "Class: " + className + "\n" + "ClassLoader Hierarchy: " + getClassLoaderHierarchy(loader);
            }
            exceptionLogger.logClassLoading(traceInfo);
        }
        if (!THROWABLE_CLASS_NAME_FORMATTED.equals(className)) {
            return classfileBuffer;
        }
        try {
            ClassPool classPool = ClassPool.getDefault();
            CtClass throwableClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
            boolean isJdk9OrLater = Integer.parseInt(System.getProperty("java.version").split("\\.")[0]) >= 9;
            for (CtConstructor constructor : throwableClass.getDeclaredConstructors()) {
                if (isJdk9OrLater) {
                    // In JDK 8, we used inst.appendToBootstrapClassLoaderSearch() to ensure that ExceptionLogger was loaded by the
                    // bootstrap class loader, as Throwable itself is loaded by bootstrap. However, in JDK 9+, this approach no longer
                    // works because the bootstrap class loader does not search added JARs. To work around this, we dynamically load
                    // ExceptionLogger using the thread context class loader instead. Additionally, we explicitly pass (Object[]) null
                    // for no-arg method calls and wrap 'this' in new Object[]{this} for proper reflection-based invocation.
                    // This ensures compatibility with both JDK 8 and JDK 17.
                    constructor.insertAfter(
                            "{ " +
                                    "   try { " +
                                    "       Class loggerClass = Class.forName(\"com.asm.eb.logger.ExceptionLogger\", true, java.lang.Thread.currentThread().getContextClassLoader()); " +
                                    "       java.lang.reflect.Method getInstanceMethod = loggerClass.getMethod(\"getInstance\", (Class[]) null); " +
                                    "       Object loggerInstance = getInstanceMethod.invoke(null, (Object[]) null); " + // FIX: Explicitly passing null arguments
                                    "       java.lang.reflect.Method logMethod = loggerClass.getMethod(\"logException\", new Class[]{Throwable.class}); " +
                                    "       logMethod.invoke(loggerInstance, new Object[]{this}); " + // FIX: Correctly passing arguments to invoke
                                    "   } catch (Throwable ignored) {} " +
                                    "}"
                    );
                } else {
                    constructor.insertAfter(
                            "{ " +
                                    "   com.asm.eb.logger.ExceptionLogger exceptionLogger = com.asm.eb.logger.ExceptionLogger.getInstance(); " +
                                    "   try { exceptionLogger.logException(this); } catch(Exception e) {} " +
                                    "}"
                    );
                }
            }
            exceptionLogger.logInfo("Successfully instrumented java.lang.Throwable");
            return throwableClass.toBytecode();
        } catch (Exception e) {
            exceptionLogger.logError("Error during Throwable modification: " + e.getMessage());
        }
        return classfileBuffer;
    }

    /**
     * Builds a string representation of the class loader hierarchy for a given class loader.
     *
     * @param loader The starting class loader.
     * @return A formatted string showing the class loader hierarchy.
     */
    private String getClassLoaderHierarchy(ClassLoader loader) {
        StringBuilder hierarchy = new StringBuilder();

        while (loader != null) {
            hierarchy.append(loader.getClass().getName()).append(" -> ");
            loader = loader.getParent();
        }

        // Always include the Bootstrap class loader at the end
        hierarchy.append("Bootstrap");

        if (hierarchy.length() > 0 && loader != null) {
            hierarchy.setLength(hierarchy.length() - 4);
        }

        return hierarchy.toString();
    }
}
