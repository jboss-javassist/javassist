# Javassist version 3

### Copyright (C) 1999-2013 by Shigeru Chiba, All rights reserved.

  

Javassist (JAVA programming ASSISTant) makes Java bytecode manipulation
simple. It is a class library for editing bytecodes in Java; it enables Java
programs to define a new class at runtime and to modify a class file when the
JVM loads it. Unlike other similar bytecode editors, Javassist provides two
levels of API: source level and bytecode level. If the users use the source-
level API, they can edit a class file without knowledge of the specifications
of the Java bytecode. The whole API is designed with only the vocabulary of
the Java language. You can even specify inserted bytecode in the form of
source text; Javassist compiles it on the fly. On the other hand, the
bytecode-level API allows the users to directly edit a class file as other
editors.

  

## Files

  * [License.html](License.html)
License file (Also see the copyright notices below)

  * [tutorial/tutorial.html](tutorial/tutorial.html)
Tutorial

  * ./javassist.jar
The Javassist jar file (class files)

  * ./src/main
The source files

  * [html/index.html](html/index.html)
The top page of the Javassist API document.

  * ./sample/
Sample programs

  

## How to run sample programs

JDK 1.4 or later is needed.

### 0. If you have Apache Ant

Run the sample-all task. Otherwise, follow the instructions below.

### 1. Move to the directory where this Readme.html file is located.

In the following instructions, we assume that the javassist.jar file is
included in the class path. For example, the javac and java commands must
receive the following `classpath` option:

    
    
    -classpath ".:javassist.jar"
    

If the operating system is Windows, the path separator must be not `:` (colon)
but `;` (semicolon). The java command can receive the `-cp` option as well as
`-classpath`.

If you don't want to use the class-path option, you can make `javassist.jar`
included in the `CLASSPATH` environment:

    
    
    export CLASSPATH=.:javassist.jar
    

or if the operating system is Windows:

    
    
    set CLASSPATH=.;javassist.jar
    

Otherwise, you can copy javassist.jar to the directory

<_java-home_>/jre/lib/ext.

<_java-home_> depends on the system. It is usually /usr/local/java,
c:\j2sdk1.4\, etc.

### 2. sample/Test.java

This is a very simple program using Javassist.

To run, type the commands:

    
    
    % javac sample/Test.java
    % java sample.Test
    

For more details, see [sample/Test.java](sample/Test.java)

### 3. sample/reflect/*.java

This is the "verbose metaobject" example well known in reflective programming.
This program dynamically attaches a metaobject to a Person object. The
metaobject prints a message if a method is called on the Person object.

To run, type the commands:

    
    
    % javac sample/reflect/*.java
    % java javassist.tools.reflect.Loader sample.reflect.Main Joe
    

Compare this result with that of the regular execution without reflection:

    
    % java sample.reflect.Person Joe

For more details, see [sample/reflect/Main.java](sample/reflect/Main.java)

Furthermore, the Person class can be statically modified so that all the
Person objects become reflective without sample.reflect.Main. To do this, type
the commands:

    
    
    % java javassist.tools.reflect.Compiler sample.reflect.Person -m sample.reflect.VerboseMetaobj
    

Then,

    
    
    % java sample.reflect.Person Joe
    

### 4. sample/duplicate/*.java

This is another example of reflective programming.

To run, type the commands:

    
    
    % javac sample/duplicate/*.java
    % java sample.duplicate.Main
    

Compare this result with that of the regular execution without reflection:

    
    % java sample.duplicate.Viewer

For more details, see [sample/duplicate/Main.java](sample/duplicate/Main.java)

### 5. sample/vector/*.java

This example shows the use of Javassit for producing a class representing a
vector of a given type at compile time.

To run, type the commands:

    
    
    % javac sample/vector/*.java
    % java sample.preproc.Compiler sample/vector/Test.j
    % javac sample/vector/Test.java
    % java sample.vector.Test
    

Note: `javassist.jar` is unnecessary to compile and execute
`sample/vector/Test.java`. For more details, see
[sample/vector/Test.j](sample/vector/Test.j) and
[sample/vector/VectorAssistant.java](sample/vector/VectorAssistant.java)

### 6. sample/rmi/*.java

This demonstrates the javassist.rmi package.

To run, type the commands:

    
    
    % javac sample/rmi/*.java
    % java sample.rmi.Counter 5001
    

The second line starts a web server listening to port 5001.

Then, open [sample/rmi/webdemo.html](sample/rmi/webdemo.html) with a web
browser running on the local host. (webdemo.html trys to fetch an applet from
http://localhost:5001/, which is the web server we started above.)

Otherwise, run sample.rmi.CountApplet as an application:

    
    
    % java javassist.web.Viewer localhost 5001 sample.rmi.CountApplet
    

### 7. sample/evolve/*.java

This is a demonstration of the class evolution mechanism implemented with
Javassist. This mechanism enables a Java program to reload an existing class
file under some restriction.

To run, type the commands:

    
    
    % javac sample/evolve/*.java
    % java sample.evolve.DemoLoader 5003
    

The second line starts a class loader DemoLoader, which runs a web server
DemoServer listening to port 5003.

Then, open [http://localhost:5003/demo.html](http://localhost:5003/demo.html)
with a web browser running on the local host. (Or, see
[sample/evolve/start.html](sample/evolve/start.html).)

### 8. sample/hotswap/*.java

This shows dynamic class reloading by the JPDA. It needs JDK 1.4 or later. To
run, first type the following commands:

    
    
    % cd sample/hotswap
    % javac *.java
    % cd logging
    % javac *.java
    % cd ..
    

If your Java is 1.4, then type:

    
    
    % java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8000 Test
    

If you are using Java 5, then type:

    
    
    % java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=8000 Test
    

Note that the class path must include `JAVA_HOME/lib/tools.jar`.

## Hints

To know the version number, type this command:

    
    
    % java -jar javassist.jar
    

Javassist provides a class file viewer for debugging. For more details, see
javassist.Dump.

  

## Changes

-version 3.19 

  * JIRA JASSIST-158, 205, 206, 207, 211, 212, 216, 223. 

-version 3.18 on June 3, 2013 

  * The source code repository has been moved to [GitHub](https://github.com/jboss-javassist/javassist)
.

  * JIRA JASSIST-181, 183, 184, 189, 162, 185, 186, 188, 190, 195, 199, 201. 

-version 3.17.1 on December 3, 2012 

  * JIRA JASSIST-177, 178, 182. 

-version 3.17 on November 8, 2012 

  * OSGi bundle info is now included in the jar file. 
  * A stackmap generator has been rewritten. 
  * JIRA JASSIST-160, 163, 166, 168, 170, 171, 174, 175, 176 have been fixed. 

-version 3.16.1 on March 6, 2012 

  * Maven now works. JIRA JASSIST-44, 106, 156 have been fixed. 

-version 3.16 on February 19, 2012 

  * JIRA JASSIST-126, 127, 144, 145, 146, 147, 149, 150, 151, 152, 153, 155. 
  * `javassist.bytecode.analysis.ControlFlow` was added. 
  * Java 7 compatibility. 

-version 3.15 on July 8, 2011 

  * The license was changed to MPL/LGPL/Apache triple. 
  * JIRA JASSIST-138 and 142 were fixed. 

-version 3.14 on October 5, 2010 

  * JIRA JASSIST-121, 123, 128, 129, 130, 131, 132. 

-version 3.13 on July 19, 2010 

  * JIRA JASSIST-118, 119, 122, 124, 125. 

-version 3.12.1 on June 10, 2010 

-version 3.12 on April 16, 2010 

-version 3.11 on July 3, 2009 

  * JIRA JASSIST-67, 68, 74, 75, 76, 77, 81, 83, 84, 85, 86, 87 were fixed. 
  * Now javassist.bytecode.CodeIterator can insert a gap into a large method body more than 32KB. (JIRA JASSIST-79, 80) 

-version 3.10 on March 5, 2009 

  * JIRA JASSIST-69, 70, 71 were fixed. 

-version 3.9 on October 9, 2008 

  * ClassPool.makeClassIfNew(InputStream) was implemented. 
  * CtNewMethod.wrapped(..) and CtNewConstructor.wrapped(..) implicitly append a method like _added_m$0. This method now has a synthetic attribute. 
  * JIRA JASSIST-66 has been fixed. 

-version 3.8.1 on July 17, 2008 

  * CtClass.rebuildClassFile() has been added. 
  * A few bugs of javassist.bytecode.analysis have been fixed. 3.8.0 could not correctly deal with one letter class name such as I and J. 

-version 3.8.0 on June 13, 2008 

  * javassist.bytecode.analysis was implemented. 
  * JASSIST-45, 47, 51, 54-57, 60, 62 were fixed. 

-version 3.7.1 on March 10, 2008 

  * a bug of javassist.util.proxy has been fixed. 

-version 3.7 on January 20, 2008 

  * Several minor bugs have been fixed. 

-version 3.6.0 on September 13, 2007 

-version 3.6.0.CR1 on July 27, 2007 

  * The stack map table introduced since Java 6 has been supported. 
  * CtClass#getDeclaredBehaviors() now returns a class initializer as well as methods and constructors. 
  * The default status of automatic pruning was made off. Instead of pruning, this version of Javassist compresses the data structure of a class file after toBytecode() is called. The compressed class file is automatically decompressed when needed. This saves memory space better than pruning. 
  * [JIRA JASSIST-33](http://jira.jboss.com/jira/browse/JASSIST-33) has been fixed. 

-version 3.5 on April 29, 2007 

  * Various minor updates. 

-version 3.4 on November 17, 2006 

  * A bug in CodeConverter#replaceFieldRead() and CodeConverter#replaceFieldWrite() was fixed. [JBAOP-284](http://jira.jboss.com/jira/browse/JBAOP-284). 
  * A synchronization bug and a performance bug in `javassist.util.proxy` have been fixed ([JASSIST-28](http://jira.jboss.com/jira/browse/JASSIST-28)). Now generated proxy classes are cached. To turn the caching off, set `ProxyFactory.useCache` to `false`. 

-version 3.3 on August 17, 2006 

  * CtClass#toClass() and ClassPool#toClass() were modified to accept a `ProtectionDomain` ([JASSIST-23](http://jira.jboss.com/jira/browse/JASSIST-23)). Now ClassPool#toClass(CtClass, ClassLoader) should not be overridden. All subclasses of ClassPool must override toClass(CtClass, ClassLoader, ProtectionDomain). 
  * CtClass#getAvailableAnnotations() etc. have been implemented. 
  * A bug related to a way of dealing with a bridge method was fixed ([HIBERNATE-37](http://jira.jboss.com/jira/browse/HIBERNATE-37)). 
  * javassist.scopedpool package was added. 

-version 3.2 on June 21, 2006 

  * The behavior of CtBehavior#getParameterAnnotations() has been changed. It is now compatible to Java Reflection API ([JASSIST-19](http://jira.jboss.com/jira/browse/JASSIST-19)). 

- version 3.2.0.CR2 on May 9, 2006 

  * A bug of replace(String,ExprEditor) in javassist.expr.Expr has been fixed. 
  * Updated ProxyFactory getClassLoader to choose the javassit class loader when the proxy superclass has a null class loader (a jdk/endorsed class) ([JASSIST-18](http://jira.jboss.com/jira/browse/JASSIST-18)). 
  * Updated the throws clause of the javassist.util.proxy.MethodHandler to be Throwable rather than Exception ([JASSIST-16](http://jira.jboss.com/jira/browse/JASSIST-16)). 

- version 3.2.0.CR1 on March 18, 2006 

  * Annotations enhancements to javassist.bytecode.MethodInfo. 
  * Allow a ClassPool to override the "guess" at the classloader to use. 

- version 3.1 on February 23, 2006 

  * getFields(), getMethods(), and getConstructors() in CtClass were changed to return non-private memebers instead of only public members. 
  * getEnclosingClass() in javassist.CtClass was renamed to getEnclosingMethod(). 
  * getModifiers() was extended to return Modifier.STATIC if the class is a static inner class. 
  * The return type of CtClass.stopPruning() was changed from void to boolean. 
  * toMethod() in javassist.CtConstructor has been implemented. 
  * It includes new javassist.util.proxy package similar to Enhancer of CGLIB. 

  * The subpackages of Javassist were restructured. 
    * javassist.tool package was renamed to javassist.tools. 
    * HotSwapper was moved to javassist.util. 
    * Several subpackages were moved to javassist.tools. 
    * javassist.preproc package was elminated and the source was moved to the sample directory. 

- version 3.1 RC2 on September 7, 2005 

  * RC2 is released mainly for an administrative reason. 
  * A few bugs have been fixed. 

- version 3.1 RC1 on August 29, 2005 

  * Better annotation supports. See `CtClass.getAnnotations()`
  * javassist.tool.HotSwapper was added. 
  * javassist.ClassPool.importPackage() was added. 
  * The compiler now accepts array initializers (only one dimensional arrays). 
  * javassist.Dump was moved to javassist.tool.Dump. 
  * Many bugs were fixed. 

- version 3.0 on January 18, 2005 

  * The compiler now supports synchronized statements and finally clauses. 
  * You can now remove a method and a field. 

- version 3.0 RC1 on September 13, 2004. 

  * CtClass.toClass() has been reimplemented. The behavior has been changed. 
  * javassist.expr.NewArray has been implemented. It enables modifying an expression for array creation. 
  * `.class` notation has been supported. The modified class file needs javassist.runtime.DotClass at runtime. 
  * a bug in `CtClass.getMethods()` has been fixed. 
  * The compiler supports a switch statement. 

- version 3.0 beta on May 18th, 2004. 

  * The ClassPool framework has been redesigned. 
    * writeFile(), write(), ... in ClassPool have been moved to CtClass. 
    * The design of javassist.Translator has been changed. 
  * javassist.bytecode.annotation has been added for meta tags. 
  * CtClass.makeNestedClass() has been added. 
  * The methods declared in javassist.bytecode.InnerClassesAttribute have been renamed a bit. 
  * Now local variables were made available in the source text passed to CtBehavior.insertBefore(), MethodCall.replace(), etc. 
  * CtClass.main(), which prints the version number, has been added. 
  * ClassPool.SimpleLoader has been public. 
  * javassist.bytecode.DeprecatedAttribute has been added. 
  * javassist.bytecode.LocalVariableAttribute has been added. 
  * CtClass.getURL() and javassist.ClassPath.find() has been added. 
  * CtBehavior.insertAt() has been added. 
  * CtClass.detach() has been added. 
  * CodeAttribute.computeMaxStack() has been added. 

- version 2.6 in August, 2003. 

  * The behavior of CtClass.setSuperclass() was changed. To obtain the previous behavior, call CtClass.replaceClassName(). 
  * CtConstructor.setBody() now works for class initializers. 
  * CtNewMethod.delegator() now works for static methods. 
  * javassist.expr.Expr.indexOfBytecode() has been added. 
  * javassist.Loader has been modified so that getPackage() returns a package object. 
  * Now, the compiler can correctly compile a try statement and an infinite while-loop. 

- version 2.5.1 in May, 2003.   
Simple changes for integration with JBoss AOP

  * Made ClassPool.get0 protected so that subclasses of ClassPool can call it. 
  * Moved all access to the class cache (the field ClassPool.classes) to a method called getCached(String classname). This is so subclasses of ClassPool can override this behavior. 

- version 2.5 in May, 2003.   
From this version, Javassist is part of the JBoss project.

  * The license was changed from MPL to MPL/LGPL dual. 
  * ClassPool.removeClassPath() and ClassPath.close() have been added. 
  * ClassPool.makeClass(InputStream) has been added. 
  * CtClass.makeClassInitializer() has been added. 
  * javassist.expr.Expr has been changed to a public class. 
  * javassist.expr.Handler has been added. 
  * javassist.expr.MethodCall.isSuper() has been added. 
  * CtMethod.isEmpty() and CtConstructor.isEmpty() have been added. 
  * LoaderClassPath has been implemented. 

- version 2.4 in February, 2003. 

  * The compiler included in Javassist did not correctly work with interface methods. This bug was fixed. 
  * Now javassist.bytecode.Bytecode allows more than 255 local variables in the same method. 
  * javassist.expr.Instanceof and Cast have been added. 
  * javassist.expr.{MethodCall,NewExpr,FieldAccess,Instanceof,Cast}.where() have been added. They return the caller-side method surrounding the expression. 
  * javassist.expr.{MethodCall,NewExpr,FieldAccess,Instanceof,Cast}.mayThrow() have been added. 
  * $class has been introduced. 
  * The parameters to replaceFieldRead(), replaceFieldWrite(), and redirectFieldAccess() in javassist.CodeConverter are changed. 
  * The compiler could not correctly handle a try-catch statement. This bug has been fixed. 

- version 2.3 in December, 2002. 

  * The tutorial has been revised a bit. 
  * SerialVersionUID class was donated by Bob Lee. Thanks. 
  * CtMethod.setBody() and CtConstructor.setBody() have been added. 
  * javassist.reflect.ClassMetaobject.useContextClassLoader has been added. If true, the reflection package does not use Class.forName() but uses a context class loader specified by the user. 
  * $sig and $type are now available. 
  * Bugs in Bytecode.write() and read() have been fixed. 

- version 2.2 in October, 2002. 

  * The tutorial has been revised. 
  * A new package `javassist.expr` has been added. This is replacement of classic `CodeConverter`. 
  * javassist.ConstParameter was changed into javassist.CtMethod.ConstParameter. 
  * javassist.FieldInitializer was renamed into javassist.CtField.Initializer. 
  * A bug in javassist.bytecode.Bytecode.addInvokeinterface() has been fixed. 
  * In javassist.bytecode.Bytecode, addGetfield(), addGetstatic(), addInvokespecial(), addInvokestatic(), addInvokevirtual(), and addInvokeinterface() have been modified to update the current statck depth. 

- version 2.1 in July, 2002. 

  * javassist.CtMember and javassist.CtBehavior have been added. 
  * javassist.CtClass.toBytecode() has been added. 
  * javassist.CtClass.toClass() and javassist.ClassPool.writeAsClass() has been added. 
  * javassist.ByteArrayClassPath has been added. 
  * javassist.bytecode.Mnemonic has been added. 
  * Several bugs have been fixed. 

- version 2.0 (major update) in November, 2001. 

  * The javassist.bytecode package has been provided. It is a lower-level API for directly modifying a class file although the users must have detailed knowledge of the Java bytecode. 
  * The mechanism for creating CtClass objects have been changed. 
  * javassist.tool.Dump moves to the javassist package. 

version 1.0 in July, 2001.

  * javassist.reflect.Metaobject and ClassMetaobject was changed. Now they throw the same exception that they receive from a base-level object. 

- version 0.8 

  * javassist.tool.Dump was added. It is a class file viewer. 
  * javassist.FiledInitializer.byNewArray() was added. It is for initializing a field with an array object. 
  * javassist.CodeConverter.redirectMethodCall() was added. 
  * javassist.Run was added. 

- version 0.7 

  * javassit.Loader was largely modified. javassist.UserLoader was deleted. Instead, Codebase was renamed to ClassPath and UserClassPath was added. Now programmers who want to customize Loader must write a class implementing UserClassPath instead of UserLoader. This change is for sharing class search paths between Loader and CtClass.CtClass(String). 
  * CtClass.addField(), addMethod(), addConstructor(), addWrapper() were also largely modified so that it receives CtNewMethod, CtNewConstructor, or CtNewField. The static methods for creating these objects were added to the API. 
  * Constructors are now represented by CtConstructor objects. CtConstructor is a subclass of CtMethod. 
  * CtClass.getUserAttribute() was removed. Use CtClass.getAttribute(). 
  * javassist.rmi.RmiLoader was added. 
  * javassist.reflect.Metalevel._setMetaobject() was added. Now metaobjects can be replaced at runtime. 

- version 0.6 

  * Javassist was modified to correctly deal with array types appearing in signatures. 
  * A bug crashed resulting bytecode if a class includes a private static filed. It has been fixed. 
  * javassist.CtNewInterface was added. 
  * javassist.Loader.recordClass() was renamed into makeClass(). 
  * javassist.UserLoader.loadClass() was changed to take the second parameter. 

- version 0.5 

  * a bug-fix version. 

- version 0.4 

  * Major update again. Many classes and methods were changed. Most of methods taking java.lang.Class have been changed to take javassist.CtClass. 

- version 0.3 

  * Major update. Many classes and methods were changed. 

- version 0.2 

  * Jar/zip files are supported. 

-version 0.1 on April 16, 1999. 

  * The first release. 

  

## Copyright notices

Javassist, a Java-bytecode translator toolkit.

Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.

The contents of this software, Javassist, are subject to the Mozilla Public
License Version 1.1 (the "License");

you may not use this software except in compliance with the License. You may
obtain a copy of the License at

http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF

ANY KIND, either express or implied. See the License for the specific language
governing rights and

limitations under the License.

The Original Code is Javassist.

The Initial Developer of the Original Code is Shigeru Chiba. Portions created
by the Initial Developer are

Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.

Contributor(s): __Bill Burke, Jason T. Greene______________.

Alternatively, the contents of this software may be used under the terms of
the GNU Lesser General Public License Version 2.1 or later (the "LGPL"), or
the Apache License Version 2.0 (the "AL"), in which case the provisions of the
LGPL or the AL are applicable instead of those above. If you wish to allow use
of your version of this software only under the terms of either the LGPL or
the AL, and not to allow others to use your version of this software under the
terms of the MPL, indicate your decision by deleting the provisions above and
replace them with the notice and other provisions required by the LGPL or the
AL. If you do not delete the provisions above, a recipient may use your
version of this software under the terms of any one of the MPL, the LGPL or
the AL.

If you obtain this software as part of JBoss, the contents of this software
may be used under only the terms of the LGPL. To use them under the MPL, you
must obtain a separate package including only Javassist but not the other part
of JBoss.

All the contributors to the original source tree have agreed to the original
license term described above.

  

## Acknowledgments

The development of this software is sponsored in part by the PRESTO and CREST
programs of [Japan Science and Technology Corporation](http://www.jst.go.jp/).

I'd like to thank Michiaki Tatsubori, Johan Cloetens, Philip Tomlinson, Alex
Villazon, Pascal Rapicault, Dan HE, Eric Tanter, Michael Haupt, Toshiyuki
Sasaki, Renaud Pawlak, Luc Bourlier, Eric Bui, Lewis Stiller, Susumu Yamazaki,
Rodrigo Teruo Tomita, Marc Segura-Devillechaise, Jan Baudisch, Julien Blass,
Yoshiki Sato, Fabian Crabus, Bo Norregaard Jorgensen, Bob Lee, Bill Burke,
Remy Sanlaville, Muga Nishizawa, Alexey Loubyansky, Saori Oki, Andreas
Salathe, Dante Torres estrada, S. Pam, Nuno Santos, Denis Taye, Colin
Sampaleanu, Robert Bialek, Asato Shimotaki, Howard Lewis Ship, Richard Jones,
Marjan Sterjev, Bruce McDonald, Mark Brennan, Vlad Skarzhevskyy, Brett
Randall, Tsuyoshi Murakami, Nathan Meyers, Yoshiyuki Usui Yutaka Sunaga, Arjan
van der Meer, Bruce Eckel, Guillaume Pothier, Kumar Matcha, Andreas Salathe,
Renat Zubairov, Armin Haaf, Emmanuel Bernard, Jason T. Greene and all other
contributors for their contributions.

  

* * *

[Shigeru Chiba](http://www.javassist.org) (Email: chiba@javassist.org)

