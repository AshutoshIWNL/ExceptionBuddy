package com.asm.eb.transformer;

import com.asm.eb.logger.ExceptionLogger;
import com.asm.eb.model.Configuration;
import javassist.*;

import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.URL;
import java.security.ProtectionDomain;

/**
 * @author asmishra
 * @since 2/13/2025
 */
public class ExceptionTransformer implements ClassFileTransformer {
    private final Configuration configuration;
    private final ExceptionLogger exceptionLogger;

    public ExceptionTransformer(Configuration configuration, ExceptionLogger exceptionLogger) {
        this.configuration = configuration;
        this.exceptionLogger = exceptionLogger;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className,
                            Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if(configuration.isClassLoaderTracing()) {
            if(className != null) {
                URL jarLocation = null;
                String traceInfo = null;
                if (protectionDomain != null) {
                    jarLocation = protectionDomain.getCodeSource().getLocation();
                    traceInfo = "Class: " + className + "\n" + "ClassLoader Hierarchy: " + getClassLoaderHierarchy(loader) + "\nLoaded from: " + jarLocation.getPath();
                } else {
                    traceInfo = "Class: " + className + "\n" + "ClassLoader Hierarchy: " + getClassLoaderHierarchy(loader);
                }
                exceptionLogger.logClassLoading(traceInfo);
            }
        }
        if (!"java/lang/Throwable".equals(className)) {
            return classfileBuffer;
        }
        try {
            ClassPool classPool = ClassPool.getDefault();
            CtClass throwableClass = classPool.get("java.lang.Throwable");
            for (CtConstructor constructor : throwableClass.getDeclaredConstructors()) {
                constructor.insertAfter("com.asm.eb.logger.ExceptionLogger exceptionLogger = com.asm.eb.logger.ExceptionLogger.getInstance(); " +
                        "try {exceptionLogger.logException(this);} catch(Exception e){}");
            }
            exceptionLogger.logInfo("Instrumented java/lang/Throwable, we are set!");
            return throwableClass.toBytecode();
        } catch (NotFoundException | CannotCompileException |IOException e) {
            exceptionLogger.logError("Oh no! We hit a snag while modifying throwable: " + e.getMessage());
        }
        return classfileBuffer;
    }

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
