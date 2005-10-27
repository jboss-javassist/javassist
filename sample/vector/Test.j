/*
  A sample program using sample.vector.VectorAssistant
  and the sample.preproc package.

  This automatically produces the classes representing vectors of integer
  and vectors of java.lang.String.

  To compile and run this program, do as follows:

    % java sample.preproc.Compiler sample/vector/Test.j
    % javac sample/vector/Test.java
    % java sample.vector.Test

  The first line produces one source file (sample/Test.java) and
  two class files (sample/vector/intVector.class and
  sample/vector/StringVector.class).
*/

package sample.vector;

import java.util.Vector by sample.vector.VectorAssistant(java.lang.String);
import java.util.Vector by sample.vector.VectorAssistant(int);

public class Test {
    public static void main(String[] args) {
	intVector iv = new intVector();
	iv.add(3);
	iv.add(4);
	for (int i = 0; i < iv.size(); ++i)
	    System.out.println(iv.at(i));

	StringVector sv = new StringVector();
	sv.add("foo");
	sv.add("bar");
	for (int i = 0; i < sv.size(); ++i)
	    System.out.println(sv.at(i));
    }
}
