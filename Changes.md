### Changes

#### version 3.29.2 on September 14, 2022

- GitHub Issue #427.

#### version 3.29.1 on August 11, 2022

* GitHub Issue #423.

* `Readme.html` was deleted (GitHub Issue #414).

#### version 3.29 on May 13, 2022

* GitHub Issue #378, PR #278, #299, #382, #383, #390, #391, #395, #399, #409.

#### version 3.28 on May 8, 2021

* GitHub Issue #305, #328, #339, #350, #357, and PR #363.

#### version 3.27 on March 19, 2020

* GitHub Issue #271 (PR #279), #280 (PR #281), #282, and PR #294.

#### version 3.26 on October 3, 2019

* GitHub Issue #270 (PR #272), #265 (PR #267), #271, #222, and #275.

#### version 3.25 on April 16, 2019

* GitHub Issue #72 (PR #231), #241, #242 (PR #243), PR #244,
#246 (PR #247), PR #250, #252 (PR #253), PR #254.

#### version 3.24.1 on December 9, 2018

* GitHub Issue #228, #229

#### version 3.24 on November 1, 2018

* Java 11 supports.
* JIRA JASSIST-267.
* Github PR #218.

#### version 3.23.1 on July 2, 2018

* Github PR #171.

#### version 3.23 on June 21, 2018

* Fix leaking file handlers in ClassPool and removed ClassPath.close(). Github issue #165.

#### version 3.22 on October 10, 2017

* Java 9 supports.
* JIRA JASSIST-261.

#### version 3.21 on October 4, 2016

* JIRA JASSIST-244, 245, 248, 250, 255, 256, 259, 262.
* `javassist.tools.Callback` was modified to be Java 1.4 compatible.
    The parameter type of `Callback#result()` was changed.
* The algorithm for generating a stack-map table was modified to fix github issue #83.
* A bug of ProxyFactory related to default methods was fixed.  It is github issue #45. 

#### version 3.20 on June 25, 2015

* JIRA JASSIST-241, 242, 246.

#### version 3.19 on January 6, 2015

* JIRA JASSIST-158, 205, 206, 207, 208, 209, 211, 212, 216, 220, 223, 224,
            227, 230, 234, 235, 236, 237, 238, 240.

#### version 3.18 on June 3, 2013

* The source code repository has been moved to [GitHub](https://github.com/jboss-javassist/javassist).

* JIRA JASSIST-181, 183, 184, 189, 162, 185, 186, 188, 190, 195, 199, 201.

#### version 3.17.1 on December 3, 2012

* JIRA JASSIST-177, 178, 182.

#### version 3.17 on November 8, 2012

* OSGi bundle info is now included in the jar file.

* A stackmap generator has been rewritten.

* JIRA JASSIST-160, 163, 166, 168, 170, 171, 174, 175, 176 have been fixed.

#### version 3.16.1 on March 6, 2012

* Maven now works.  JIRA JASSIST-44, 106, 156 have been fixed.

#### version 3.16 on February 19, 2012

* JIRA JASSIST-126, 127, 144, 145, 146, 147, 149, 150, 151, 152, 153, 155.

* `javassist.bytecode.analysis.ControlFlow` was added.

* Java 7 compatibility.

#### version 3.15 on July 8, 2011

* The license was changed to MPL/LGPL/Apache triple.

* JIRA JASSIST-138 and 142 were fixed.

#### version 3.14 on October 5, 2010

* JIRA JASSIST-121, 123, 128, 129, 130, 131, 132.

#### version 3.13 on July 19, 2010

* JIRA JASSIST-118, 119, 122, 124, 125.

#### version 3.12.1 on June 10, 2010

#### version 3.12 on April 16, 2010

#### version 3.11 on July 3, 2009

* JIRA JASSIST-67, 68, 74, 75, 76, 77, 81, 83, 84, 85, 86, 87 were fixed.

* Now javassist.bytecode.CodeIterator can insert a gap into
    	a large method body more than 32KB.  (JIRA JASSIST-79, 80)

#### version 3.10 on March 5, 2009

* JIRA JASSIST-69, 70, 71 were fixed.

#### version 3.9 on October 9, 2008

* ClassPool.makeClassIfNew(InputStream) was implemented.

* CtNewMethod.wrapped(..) and CtNewConstructor.wrapped(..)
    	implicitly append a method like _added_m$0.
    	This method now has a synthetic attribute.

* JIRA JASSIST-66 has been fixed.

#### version 3.8.1 on July 17, 2008

* CtClass.rebuildClassFile() has been added.

* A few bugs of javassist.bytecode.analysis have been fixed.
    	3.8.0 could not correctly deal with one letter class name
    	such as I and J.

#### version 3.8.0 on June 13, 2008

* javassist.bytecode.analysis was implemented.

* JASSIST-45, 47, 51, 54-57, 60, 62 were fixed.

#### version 3.7.1 on March 10, 2008

* a bug of javassist.util.proxy has been fixed.

#### version 3.7 on January 20, 2008

* Several minor bugs have been fixed.

#### version 3.6.0 on September 13, 2007

#### version 3.6.0.CR1 on July 27, 2007

* The stack map table introduced since Java 6 has been supported.

* CtClass#getDeclaredBehaviors() now returns a class initializer
            as well as methods and constructors.

* The default status of automatic pruning was made off.
    Instead of pruning, this version of Javassist compresses
    the data structure of a class file after toBytecode() is called.
    The compressed class file is automatically decompressed when needed.
    This saves memory space better than pruning.

* [JIRA JASSIST-33](http://jira.jboss.com/jira/browse/JASSIST-33) has been fixed.

#### version 3.5 on April 29, 2007

* Various minor updates.

#### version 3.4 on November 17, 2006

* A bug in CodeConverter#replaceFieldRead() and CodeConverter#replaceFieldWrite()
    	was fixed. [JBAOP-284](http://jira.jboss.com/jira/browse/JBAOP-284).



* A synchronization bug and a performance bug in `javassist.util.proxy`
        have been fixed
        ([JASSIST-28](http://jira.jboss.com/jira/browse/JASSIST-28)).
        Now generated proxy classes are cached.  To turn the caching off,
        set `ProxyFactory.useCache` to `false`.

#### version 3.3 on August 17, 2006

* CtClass#toClass() and ClassPool#toClass() were modified to accept a
        `ProtectionDomain`
        ([JASSIST-23](http://jira.jboss.com/jira/browse/JASSIST-23)).
        Now ClassPool#toClass(CtClass, ClassLoader) should not be overridden.  All
        subclasses of ClassPool must override toClass(CtClass, ClassLoader,
        ProtectionDomain).



* CtClass#getAvailableAnnotations() etc. have been implemented.



* A bug related to a way of dealing with a bridge method was fixed
        ([HIBERNATE-37](http://jira.jboss.com/jira/browse/HIBERNATE-37)).



* javassist.scopedpool package was added.

#### version 3.2 on June 21, 2006

* The behavior of CtBehavior#getParameterAnnotations() has been changed.
    	It is now compatible to Java Reflection API
    	([JASSIST-19](http://jira.jboss.com/jira/browse/JASSIST-19)).

#### version 3.2.0.CR2 on May 9, 2006

* A bug of replace(String,ExprEditor) in javassist.expr.Expr has been fixed.

* Updated ProxyFactory getClassLoader to choose the javassit class loader
       when the proxy superclass has a null class loader (a jdk/endorsed class)
       ([JASSIST-18](http://jira.jboss.com/jira/browse/JASSIST-18)).

* Updated the throws clause of the javassist.util.proxy.MethodHandler
       to be Throwable rather than Exception
       ([JASSIST-16](http://jira.jboss.com/jira/browse/JASSIST-16)).

#### version 3.2.0.CR1 on March 18, 2006

* Annotations enhancements to javassist.bytecode.MethodInfo.

* Allow a ClassPool to override the "guess" at the classloader to use.

#### version 3.1 on February 23, 2006

* getFields(), getMethods(), and getConstructors() in CtClass
        were changed to return non-private memebers instead of only
        public members.

* getEnclosingClass() in javassist.CtClass was renamed
          to getEnclosingMethod().

* getModifiers() was extended to return Modifier.STATIC if the class
          is a static inner class.

* The return type of CtClass.stopPruning() was changed from void
        to boolean.

* toMethod() in javassist.CtConstructor has been implemented.

* It includes new javassist.util.proxy package
          similar to Enhancer of CGLIB.

* The subpackages of Javassist were restructured.

    * javassist.tool package was renamed to javassist.tools.

    * HotSwapper was moved to javassist.util.

    * Several subpackages were moved to javassist.tools.

    * javassist.preproc package was elminated and the source was
                moved to the sample directory.


#### version 3.1 RC2 on September 7, 2005

* RC2 is released mainly for an administrative reason.

* A few bugs have been fixed.

#### version 3.1 RC1 on August 29, 2005

* Better annotation supports.  See `CtClass.getAnnotations()`
* javassist.tool.HotSwapper was added.

* javassist.ClassPool.importPackage() was added.

* The compiler now accepts array initializers
        (only one dimensional arrays).

* javassist.Dump was moved to javassist.tool.Dump.

* Many bugs were fixed.

#### version 3.0 on January 18, 2005

* The compiler now supports synchronized statements and finally
      clauses.

* You can now remove a method and a field.

#### version 3.0 RC1 on September 13, 2004.

* CtClass.toClass() has been reimplemented.  The behavior has been
          changed.

* javassist.expr.NewArray has been implemented.  It enables modifying
          an expression for array creation.

* `.class` notation has been supported.  The modified class
          file needs javassist.runtime.DotClass at runtime.

* a bug in `CtClass.getMethods()` has been fixed.

* The compiler supports a switch statement.

#### version 3.0 beta on May 18th, 2004.

* The ClassPool framework has been redesigned.

    * writeFile(), write(), ... in ClassPool have been moved to CtClass.

    * The design of javassist.Translator has been changed.

* javassist.bytecode.annotation has been added for meta tags.

* CtClass.makeNestedClass() has been added.

* The methods declared in javassist.bytecode.InnerClassesAttribute
          have been renamed a bit.

* Now local variables were made available in the source text passed to
      CtBehavior.insertBefore(), MethodCall.replace(), etc.

* CtClass.main(), which prints the version number, has been added.

* ClassPool.SimpleLoader has been public.

* javassist.bytecode.DeprecatedAttribute has been added.

* javassist.bytecode.LocalVariableAttribute has been added.

* CtClass.getURL() and javassist.ClassPath.find() has been added.

* CtBehavior.insertAt() has been added.

* CtClass.detach() has been added.

* CodeAttribute.computeMaxStack() has been added.

#### version 2.6 in August, 2003.

* The behavior of CtClass.setSuperclass() was changed.
          To obtain the previous behavior, call CtClass.replaceClassName().

* CtConstructor.setBody() now works for class initializers.

* CtNewMethod.delegator() now works for static methods.

* javassist.expr.Expr.indexOfBytecode() has been added.

* javassist.Loader has been modified so that getPackage() returns
          a package object.

* Now, the compiler can correctly compile a try statement and an
          infinite while-loop.

#### version 2.5.1 in May, 2003.
Simple changes for integration with JBoss AOP

* Made ClassPool.get0 protected so that subclasses of ClassPool can call it.

* Moved all access to the class cache (the field ClassPool.classes) to a method called getCached(String classname).  This is so subclasses of ClassPool can override this behavior.

#### version 2.5 in May, 2003.
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

#### version 2.4 in February, 2003.

* The compiler included in Javassist did not correctly work with
    	interface methods.  This bug was fixed.

* Now javassist.bytecode.Bytecode allows more than 255 local
    	variables in the same method.

* javassist.expr.Instanceof and Cast have been added.

* javassist.expr.{MethodCall,NewExpr,FieldAccess,Instanceof,Cast}.where()
            have been added.  They return the caller-side method surrounding the
    	expression.

* javassist.expr.{MethodCall,NewExpr,FieldAccess,Instanceof,Cast}.mayThrow()
            have been added.

* $class has been introduced.

* The parameters to replaceFieldRead(), replaceFieldWrite(),
          and redirectFieldAccess() in javassist.CodeConverter are changed.

* The compiler could not correctly handle a try-catch statement.
          This bug has been fixed.

#### version 2.3 in December, 2002.

* The tutorial has been revised a bit.

* SerialVersionUID class was donated by Bob Lee.  Thanks.

* CtMethod.setBody() and CtConstructor.setBody() have been added.

* javassist.reflect.ClassMetaobject.useContextClassLoader has been added.
      If true, the reflection package does not use Class.forName() but uses
      a context class loader specified by the user.

* $sig and $type are now available.

* Bugs in Bytecode.write() and read() have been fixed.

#### version 2.2 in October, 2002.

* The tutorial has been revised.

* A new package `javassist.expr` has been added.
            This is replacement of classic `CodeConverter`.

* javassist.ConstParameter was changed into
    	javassist.CtMethod.ConstParameter.

* javassist.FieldInitializer was renamed into
    	javassist.CtField.Initializer.

* A bug in javassist.bytecode.Bytecode.addInvokeinterface() has been
    	fixed.

* In javassist.bytecode.Bytecode, addGetfield(), addGetstatic(),
    	addInvokespecial(), addInvokestatic(), addInvokevirtual(),
    	and addInvokeinterface()
    	have been modified to update the current statck depth.

#### version 2.1 in July, 2002.

* javassist.CtMember and javassist.CtBehavior have been added.

* javassist.CtClass.toBytecode() has been added.

* javassist.CtClass.toClass() and javassist.ClassPool.writeAsClass()
    	has been added.

* javassist.ByteArrayClassPath has been added.

* javassist.bytecode.Mnemonic has been added.

* Several bugs have been fixed.

#### version 2.0 (major update) in November, 2001.

* The javassist.bytecode package has been provided.  It is a
        lower-level API for directly modifying a class file although
        the users must have detailed knowledge of the Java bytecode.

* The mechanism for creating CtClass objects have been changed.

* javassist.tool.Dump moves to the javassist package.

version 1.0 in July, 2001.

* javassist.reflect.Metaobject and ClassMetaobject was changed.
        Now they throw the same exception that they receive from a
        base-level object.

#### version 0.8

* javassist.tool.Dump was added.  It is a class file viewer.

* javassist.FiledInitializer.byNewArray() was added.  It is for
        initializing a field with an array object.

* javassist.CodeConverter.redirectMethodCall() was added.

* javassist.Run was added.

#### version 0.7

* javassit.Loader was largely modified.  javassist.UserLoader was
        deleted.  Instead, Codebase was renamed to ClassPath
        and UserClassPath was added.  Now programmers who want to
        customize Loader must write a class implementing UserClassPath
        instead of UserLoader.  This change is for sharing class search paths
        between Loader and CtClass.CtClass(String).

* CtClass.addField(), addMethod(), addConstructor(), addWrapper() were
        also largely modified so that it receives CtNewMethod, CtNewConstructor,
        or CtNewField.  The static methods for creating these objects were
        added to the API.

* Constructors are now represented by CtConstructor objects.
  CtConstructor is a subclass of CtMethod.

* CtClass.getUserAttribute() was removed.  Use CtClass.getAttribute().

* javassist.rmi.RmiLoader was added.

* javassist.reflect.Metalevel._setMetaobject() was added.  Now
        metaobjects can be replaced at runtime.

#### version 0.6

* Javassist was modified to correctly deal with array types appearing
        in signatures.

* A bug crashed resulting bytecode if a class includes a private static
        filed.  It has been fixed.

* javassist.CtNewInterface was added.

* javassist.Loader.recordClass() was renamed into makeClass().

* javassist.UserLoader.loadClass() was changed to take the second
parameter.

#### version 0.5

* a bug-fix version.

#### version 0.4

* Major update again.  Many classes and methods were changed.
Most of methods taking java.lang.Class have been changed to
take javassist.CtClass.

#### version 0.3

* Major update.  Many classes and methods were changed.

#### version 0.2

* Jar/zip files are supported.

#### version 0.1 on April 16, 1999.

* The first release.
