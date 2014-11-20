// $Id: JarElement.java 37298 2013-08-08 20:25:55Z abram $
package javassist.tools;

import java.util.ArrayList;
import java.util.List;

public class JarElement extends FileElement {
  public JarElement(String source, String target) {
		super(source, target);
  }
  
  public JarElement()
  {
	  super();
  }

  public List<FileElement> files = new ArrayList<FileElement>();
}

