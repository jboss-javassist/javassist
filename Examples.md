How to run program examples
---------------------------

JDK 1.4 or later is needed.

### 0. If you have Apache Ant

Run the sample-all task.
Otherwise, follow the instructions below.

### 1. Move to the directory where this `Examples.md` file is located.

In the following instructions, we assume that the `javassist.jar`
file is included in the class path.
For example, the javac and java commands must receive
the following `classpath` option:

```
-classpath ".:javassist.jar"
```

If the operating system is Windows, the path
separator must be not `:` (colon) but
`;` (semicolon).  The java command can receive
the `-cp` option
as well as `-classpath`.

If you don't want to use the class-path option, you can make
`javassist.jar` included in the `CLASSPATH`
environment:

```
export CLASSPATH=.:javassist.jar
```

or if the operating system is Windows:

```
set CLASSPATH=.;javassist.jar
```

Otherwise, you can copy  `javassist.jar` to the directory
<_java-home_>`/jre/lib/ext`.


<_java-home_> depends on the system.  It is usually
`/usr/local/java` or `c:\j2sdk1.4\`, etc.

### 2. `sample/Test.java`

This is a very simple program using Javassist.

To run, type the commands:

```
% javac sample/Test.java
% java sample.Test
```

For more details, see `sample/Test.java`

### 3. `sample/reflect/*.java`

This is the "verbose metaobject" example well known in reflective
programming.  This program dynamically attaches a metaobject to
a Person object.  The metaobject prints a message if a method
is called on the Person object.

To run, type the commands:

```
% javac sample/reflect/*.java
% java javassist.tools.reflect.Loader sample.reflect.Main Joe
```

Compare this result with that of the regular execution without reflection:

```
% java sample.reflect.Person Joe
```

For more details, see sample/reflect/Main.java

Furthermore, the Person class can be statically modified so that
all the Person objects become reflective without sample.reflect.Main.
To do this, type the commands:

```
% java javassist.tools.reflect.Compiler sample.reflect.Person -m sample.reflect.VerboseMetaobj
```

Then,

```
% java sample.reflect.Person Joe
```

### 4. `sample/duplicate/*.java`

This is another example of reflective programming.
To run, type the commands:

```
% javac sample/duplicate/*.java
% java sample.duplicate.Main
```

Compare this result with that of the regular execution without reflection:

```
% java sample.duplicate.Viewer
```

For more details, see
`sample/duplicate/Main.java`

### 5. `sample/vector/*.java`

This example shows the use of Javassit for producing a class representing
a vector of a given type at compile time.

To run, type the commands:

```
% javac sample/vector/*.java
% java sample.preproc.Compiler sample/vector/Test.j
% javac sample/vector/Test.java
% java sample.vector.Test
```

Note: `javassist.jar` is unnecessary to compile and execute
`sample/vector/Test.java`.
For more details, see
`sample/vector/Test.j` and `sample/vector/VectorAssistant.java`.

### 6. `sample/rmi/*.java`

This demonstrates the `javassist.rmi` package.
To run, type the commands:

```
% javac sample/rmi/*.java
% java sample.rmi.Counter 5001
```

The second line starts a web server listening to port 5001.

Then, open sample/rmi/webdemo.html
with a web browser running
on the local host.  (`webdemo.html` trys to fetch an applet from
`http://localhost:5001/`, which is the web server we started above.)

Otherwise, run `sample.rmi.CountApplet` as an application:

```
% java javassist.web.Viewer localhost 5001 sample.rmi.CountApplet
```

### 7. `sample/evolve/*.java`

This is a demonstration of the class evolution mechanism implemented
with Javassist.  This mechanism enables a Java program to reload an
existing class file under some restriction.

To run, type the commands:

```
% javac sample/evolve/*.java
% java sample.evolve.DemoLoader 5003
```

The second line starts a class loader DemoLoader, which runs a web
server DemoServer listening to port 5003.

Then, open `http://localhost:5003/demo.html` with a web browser running
on the local host.
(Or, see sample/evolve/start.html.)

### 8. `sample/hotswap/*.java`

This shows dynamic class reloading by the JPDA.  It needs JDK 1.4 or later.
To run, first type the following commands:

```
% cd sample/hotswap
% javac *.java
% cd logging
% javac *.java
% cd ..
```

If your Java is 1.4, then type:

```
% java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000 Test
```

If you are using Java 5, then type:

```
% java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000 Test
```

Note that the class path must include `JAVA_HOME/lib/tools.jar`.
