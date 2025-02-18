package com.asm.eb.transformer;

import com.asm.eb.logger.ExceptionLogger;
import com.asm.eb.model.Configuration;
import javassist.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
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
        if(configuration.isClassLoaderTracing()) {
            if(className != null) {
                String traceInfo;
                if (protectionDomain != null) {
                    URL jarLocation = protectionDomain.getCodeSource().getLocation();
                    traceInfo = "Class: " + className + "\n" + "ClassLoader Hierarchy: " + getClassLoaderHierarchy(loader) + "\nLoaded from: " + jarLocation.getPath();
                } else {
                    traceInfo = "Class: " + className + "\n" + "ClassLoader Hierarchy: " + getClassLoaderHierarchy(loader);
                }
                exceptionLogger.logClassLoading(traceInfo);
            }
        }
        if (!THROWABLE_CLASS_NAME_FORMATTED.equals(className)) {
            return classfileBuffer;
        }
        try {
            ClassPool classPool = ClassPool.getDefault();
            CtClass throwableClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));
            for (CtConstructor constructor : throwableClass.getDeclaredConstructors()) {
                constructor.insertAfter("com.asm.eb.logger.ExceptionLogger exceptionLogger = com.asm.eb.logger.ExceptionLogger.getInstance(); " +
                        "try {exceptionLogger.logException(this);} catch(Exception e){}");
            }
            exceptionLogger.logInfo("Successfully instrumented java.lang.Throwable");
            return throwableClass.toBytecode();
        } catch (CannotCompileException |IOException e) {
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
