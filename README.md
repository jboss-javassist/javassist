[![Java CI with Maven](https://github.com/jboss-javassist/javassist/actions/workflows/maven.yml/badge.svg)](https://github.com/jboss-javassist/javassist/actions/workflows/maven.yml)

Java bytecode engineering toolkit
### [Javassist version 3](http://www.javassist.org)

Copyright (C) 1999-2022 by Shigeru Chiba, All rights reserved.

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

This software is distributed under the Mozilla Public License Version 1.1,
the GNU Lesser General Public License Version 2.1 or later, or
the Apache License Version 2.0.

#### Files

  * [README.md](README.md)
This readme file.

  * [Changes.md](Changes.md)
  Release notes.

  * [License.html](License.html)
License file.

  * [tutorial/tutorial.html](https://www.javassist.org/tutorial/tutorial.html)
Tutorial.

  * ./javassist.jar
The Javassist jar file (class files).

  * ./src/main
The source files

  * [Examples.md](Examples.md)
How to run examples.

  * [html/index.html](https://www.javassist.org/html/index.html)
The top page of the Javassist API document.

#### Hints

To print the version number, type this command:

```
java -jar javassist.jar
```

#### Acknowledgments

The development of this software is sponsored in part by the PRESTO
and CREST programs of [Japan
Science and Technology Agency](http://www.jst.go.jp/).

I'd like to thank Michiaki Tatsubori, Johan Cloetens,
Philip Tomlinson, Alex Villazon, Pascal Rapicault, Dan HE, Eric Tanter,
Michael Haupt, Toshiyuki Sasaki, Renaud Pawlak, Luc Bourlier,
Eric Bui, Lewis Stiller, Susumu Yamazaki, Rodrigo Teruo Tomita,
Marc Segura-Devillechaise, Jan Baudisch, Julien Blass, Yoshiki Sato,
Fabian Crabus, Bo Norregaard Jorgensen, Bob Lee, Bill Burke,
Remy Sanlaville, Muga Nishizawa, Alexey Loubyansky, Saori Oki,
Andreas Salathe, Dante Torres estrada, S. Pam, Nuno Santos,
Denis Taye, Colin Sampaleanu, Robert Bialek, Asato Shimotaki,
Howard Lewis Ship, Richard Jones, Marjan Sterjev,
Bruce McDonald, Mark Brennan, Vlad Skarzhevskyy,
Brett Randall, Tsuyoshi Murakami, Nathan Meyers, Yoshiyuki Usui
Yutaka Sunaga, Arjan van der Meer, Bruce Eckel, Guillaume Pothier,
Kumar Matcha, Andreas Salathe, Renat Zubairov, Armin Haaf,
Emmanuel Bernard, Jason T. Greene, Omer Kaspi,
and all other contributors for their contributions.

by [Shigeru Chiba](https://github.com/chibash)
