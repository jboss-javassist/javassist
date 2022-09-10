/*
 * Javassist, a Java-bytecode translator toolkit.
 * Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License.  Alternatively, the contents of this file may be used under
 * the terms of the GNU Lesser General Public License Version 2.1 or later,
 * or the Apache License Version 2.0.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 */
package javassist.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.management.ManagementFactory;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import com.sun.tools.attach.VirtualMachine;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

/**
 * A utility class for dynamically adding a new method
 * or modifying an existing method body.
 * This class provides {@link #redefine(Class, CtClass)}
 * and {@link #redefine(Class[], CtClass[])}, which replace the
 * existing class definition with a new one.
 * These methods perform the replacement by
 * {@code java.lang.instrument.Instrumentation}.  For details
 * of acceptable modification,
 * see the {@code Instrumentation} interface.
 *
 * <p>Before calling the {@code redefine} methods, the hotswap agent
 * has to be deployed.</p>
 *
 * <p>To create a hotswap agent, run {@link #createAgentJarFile(String)}.
 * For example, the following command creates an agent file named {@code hotswap.jar}.
 *
 * <pre>
 * $ jshell --class-path javassist.jar
 * jshell&gt; javassist.util.HotSwapAgent.createAgentJarFile("hotswap.jar") 
 * </pre>
 *
 * <p>Then, run the JVM with the VM argument {@code -javaagent:hotswap.jar}
 * to deploy the hotswap agent.
 * </p>
 *
 * <p>If the {@code -javaagent} option is not given to the JVM, {@code HotSwapAgent}
 * attempts to automatically create and start the hotswap agent on demand.
 * This automated deployment may fail.  If it fails, manually create the hotswap agent
 * and deploy it by {@code -javaagent}.</p>
 *
 * <p>The {@code HotSwapAgent} requires {@code tools.jar} as well as {@code javassist.jar}.</p>
 * 
 * <p>The idea of this class was given by <a href="https://github.com/alugowski">Adam Lugowski</a>.
 * Shigeru Chiba wrote this class by referring
 * to his <a href="https://github.com/turn/RedefineClassAgent">{@code RedefineClassAgent}</a>.
 * For details, see <a href="https://github.com/jboss-javassist/javassist/issues/119">this discussion</a>. 
 * </p>
 *
 * @see #redefine(Class, CtClass)
 * @see #redefine(Class[], CtClass[])
 * @since 3.22
 */
public class HotSwapAgent {
    private static Instrumentation instrumentation = null;

    /**
     * Obtains the {@code Instrumentation} object.
     *
     * @return null             when it is not available. 
     */
    public Instrumentation instrumentation() { return instrumentation; }

    /**
     * The entry point invoked when this agent is started by {@code -javaagent}. 
     */
    public static void premain(String agentArgs, Instrumentation inst) throws Throwable {
        agentmain(agentArgs, inst);
    }

    /**
     * The entry point invoked when this agent is started after the JVM starts.
     */
    public static void agentmain(String agentArgs, Instrumentation inst) throws Throwable {
        if (!inst.isRedefineClassesSupported())
            throw new RuntimeException("this JVM does not support redefinition of classes");

        instrumentation = inst;
    }

    /**
     * Redefines a class.
     */
    public static void redefine(Class<?> oldClass, CtClass newClass)
        throws NotFoundException, IOException, CannotCompileException
    {
        Class<?>[] old = { oldClass };
        CtClass[] newClasses = { newClass };
        redefine(old, newClasses);
    }

    /**
     * Redefines classes.
     */
    public static void redefine(Class<?>[] oldClasses, CtClass[] newClasses)
        throws NotFoundException, IOException, CannotCompileException
    {
        startAgent();
        ClassDefinition[] defs = new ClassDefinition[oldClasses.length];
        for (int i = 0; i < oldClasses.length; i++)
            defs[i] = new ClassDefinition(oldClasses[i], newClasses[i].toBytecode());

        try {
            instrumentation.redefineClasses(defs);
        }
        catch (ClassNotFoundException e) {
            throw new NotFoundException(e.getMessage(), e);
        }
        catch (UnmodifiableClassException e) {
            throw new CannotCompileException(e.getMessage(), e);
        }
    }

    /**
     * Ensures that the agent is ready.
     * It attempts to dynamically start the agent if necessary.
     */
    private static void startAgent() throws NotFoundException {
        if (instrumentation != null)
            return;

        try {
            File agentJar = createJarFile();

            String nameOfRunningVM = ManagementFactory.getRuntimeMXBean().getName();
            String pid = nameOfRunningVM.substring(0, nameOfRunningVM.indexOf('@'));
            VirtualMachine vm = VirtualMachine.attach(pid);
            vm.loadAgent(agentJar.getAbsolutePath(), null);
            vm.detach();
        }
        catch (Exception e) {
            throw new NotFoundException("hotswap agent", e);
        }

        for (int sec = 0; sec < 10 /* sec */; sec++) {
            if (instrumentation != null)
                return;

            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        throw new NotFoundException("hotswap agent (timeout)");
    }

    /**
     * Creates an agent file for using {@code HotSwapAgent}. 
     */
    public static File createAgentJarFile(String fileName)
        throws IOException, CannotCompileException, NotFoundException
    {
        return createJarFile(new File(fileName));
    }

    private static File createJarFile()
        throws IOException, CannotCompileException, NotFoundException
    {
        File jar = File.createTempFile("agent", ".jar");
        jar.deleteOnExit();
        return createJarFile(jar);
    }

    private static File createJarFile(File jar)
        throws IOException, CannotCompileException, NotFoundException
    {
        Manifest manifest = new Manifest();
        Attributes attrs = manifest.getMainAttributes();
        attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attrs.put(new Attributes.Name("Premain-Class"), HotSwapAgent.class.getName());
        attrs.put(new Attributes.Name("Agent-Class"), HotSwapAgent.class.getName());
        attrs.put(new Attributes.Name("Can-Retransform-Classes"), "true");
        attrs.put(new Attributes.Name("Can-Redefine-Classes"), "true");

        JarOutputStream jos = null;
        try {
            jos = new JarOutputStream(new FileOutputStream(jar), manifest);
            String cname = HotSwapAgent.class.getName();
            JarEntry e = new JarEntry(cname.replace('.', '/') + ".class");
            jos.putNextEntry(e);
            ClassPool pool = ClassPool.getDefault();
            CtClass clazz = pool.get(cname);
            jos.write(clazz.toBytecode());
            jos.closeEntry();
        }
        finally {
            if (jos != null)
                jos.close();
        }

        return jar;
    }
}
