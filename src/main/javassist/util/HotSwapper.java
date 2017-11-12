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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jdi.Bootstrap;
import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.jdi.connect.IllegalConnectorArgumentsException;
import com.sun.jdi.event.Event;
import com.sun.jdi.event.EventIterator;
import com.sun.jdi.event.EventQueue;
import com.sun.jdi.event.EventSet;
import com.sun.jdi.event.MethodEntryEvent;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import com.sun.jdi.request.MethodEntryRequest;

class Trigger {
    void doSwap() {}
}

/**
 * A utility class for dynamically reloading a class by
 * the Java Platform Debugger Architecture (JPDA), or <i>HotSwap</i>.
 * It works only with JDK 1.4 and later.
 *
 * <p><b>Note:</b> The new definition of the reloaded class must declare
 * the same set of methods and fields as the original definition.  The
 * schema change between the original and new definitions is not allowed
 * by the JPDA. 
 *
 * <p>To use this class, the JVM must be launched with the following
 * command line options:
 *
 * <p>For Java 1.4,<br>
 * <pre>java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000</pre>
 * <p>For Java 5,<br>
 * <pre>java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000</pre>
 *
 * <p>Note that 8000 is the port number used by <code>HotSwapper</code>.
 * Any port number can be specified.  Since <code>HotSwapper</code> does not
 * launch another JVM for running a target application, this port number
 * is used only for inter-thread communication.
 *
 * <p>Furthermore, <code>JAVA_HOME/lib/tools.jar</code> must be included
 * in the class path.
 *
 * <p>Using <code>HotSwapper</code> is easy.  See the following example:
 *
 * <pre>
 * CtClass clazz = ...
 * byte[] classFile = clazz.toBytecode();
 * HotSwapper hs = new HostSwapper(8000);  // 8000 is a port number.
 * hs.reload("Test", classFile);
 * </pre>
 *
 * <p><code>reload()</code>
 * first unload the <code>Test</code> class and load a new version of
 * the <code>Test</code> class.
 * <code>classFile</code> is a byte array containing the new contents of
 * the class file for the <code>Test</code> class.  The developers can
 * repatedly call <code>reload()</code> on the same <code>HotSwapper</code>
 * object so that they can reload a number of classes.
 *
 * <p>{@code HotSwap} depends on the debug agent to perform hot-swapping
 * but it is reported that the debug agent is buggy under massively multithreaded
 * environments.  If you encounter a problem, try {@link HotSwapAgent}.
 *
 * @since 3.1
 * @see HotSwapAgent
 */
public class HotSwapper {
    private VirtualMachine jvm;
    private MethodEntryRequest request;
    private Map<ReferenceType,byte[]> newClassFiles;

    private Trigger trigger;

    private static final String HOST_NAME = "localhost";
    private static final String TRIGGER_NAME = Trigger.class.getName();

    /**
     * Connects to the JVM.
     *
     * @param port	the port number used for the connection to the JVM.
     */
    public HotSwapper(int port)
        throws IOException, IllegalConnectorArgumentsException
    {
        this(Integer.toString(port));
    }

    /**
     * Connects to the JVM.
     *
     * @param port	the port number used for the connection to the JVM.
     */
    public HotSwapper(String port)
        throws IOException, IllegalConnectorArgumentsException
    {
        jvm = null;
        request = null;
        newClassFiles = null;
        trigger = new Trigger();
        AttachingConnector connector
            = (AttachingConnector)findConnector("com.sun.jdi.SocketAttach");

        Map<String,Connector.Argument> arguments = connector.defaultArguments();
        arguments.get("hostname").setValue(HOST_NAME);
        arguments.get("port").setValue(port);
        jvm = connector.attach(arguments);
        EventRequestManager manager = jvm.eventRequestManager();
        request = methodEntryRequests(manager, TRIGGER_NAME);
    }

    private Connector findConnector(String connector) throws IOException {
        List<Connector> connectors = Bootstrap.virtualMachineManager().allConnectors();

        for (Connector con:connectors)
            if (con.name().equals(connector))
                return con;

        throw new IOException("Not found: " + connector);
    }

    private static MethodEntryRequest methodEntryRequests(
                                EventRequestManager manager,
                                String classpattern) {
        MethodEntryRequest mereq = manager.createMethodEntryRequest();
        mereq.addClassFilter(classpattern);
        mereq.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
        return mereq;
    }

    /* Stops triggering a hotswapper when reload() is called.
     */
    @SuppressWarnings("unused")
    private void deleteEventRequest(EventRequestManager manager,
                                    MethodEntryRequest request) {
        manager.deleteEventRequest(request);
    }

    /**
     * Reloads a class.
     *
     * @param className		the fully-qualified class name.
     * @param classFile		the contents of the class file.
     */
    public void reload(String className, byte[] classFile) {
        ReferenceType classtype = toRefType(className);
        Map<ReferenceType,byte[]> map = new HashMap<ReferenceType,byte[]>();
        map.put(classtype, classFile);
        reload2(map, className);
    }

    /**
     * Reloads a class.
     *
     * @param classFiles	a map between fully-qualified class names
     *				and class files.  The type of the class names
     *				is <code>String</code> and the type of the
     *				class files is <code>byte[]</code>.
     */
    public void reload(Map<String,byte[]> classFiles) {
        Map<ReferenceType,byte[]> map = new HashMap<ReferenceType,byte[]>();
        String className = null;
        for (Map.Entry<String,byte[]> e:classFiles.entrySet()) {
            className = e.getKey();
            map.put(toRefType(className), e.getValue());
        }

        if (className != null)
            reload2(map, className + " etc.");
    }

    private ReferenceType toRefType(String className) {
        List<ReferenceType> list = jvm.classesByName(className);
        if (list == null || list.isEmpty())
            throw new RuntimeException("no such class: " + className);
        return list.get(0);
    }

    private void reload2(Map<ReferenceType,byte[]> map, String msg) {
        synchronized (trigger) {
            startDaemon();
            newClassFiles = map;
            request.enable();
            trigger.doSwap();
            request.disable();
            Map<ReferenceType,byte[]> ncf = newClassFiles;
            if (ncf != null) {
                newClassFiles = null;
                throw new RuntimeException("failed to reload: " + msg);
            }
        }
    }

    private void startDaemon() {
        new Thread() {
            private void errorMsg(Throwable e) {
                System.err.print("Exception in thread \"HotSwap\" ");
                e.printStackTrace(System.err);
            }

            @Override
            public void run() {
                EventSet events = null;
                try {
                    events = waitEvent();
                    EventIterator iter = events.eventIterator();
                    while (iter.hasNext()) {
                        Event event = iter.nextEvent();
                        if (event instanceof MethodEntryEvent) {
                            hotswap();
                            break;
                        }
                    }
                }
                catch (Throwable e) {
                    errorMsg(e);
                }
                try {
                    if (events != null)
                        events.resume();
                }
                catch (Throwable e) {
                    errorMsg(e);
                }
            }
        }.start();
    }

    EventSet waitEvent() throws InterruptedException {
        EventQueue queue = jvm.eventQueue();
        return queue.remove();
    }

    void hotswap() {
        Map<ReferenceType,byte[]> map = newClassFiles;
        jvm.redefineClasses(map);
        newClassFiles = null;
    }
}
