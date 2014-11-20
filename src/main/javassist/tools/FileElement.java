// $Id: FileElement.java 37298 2013-08-08 20:25:55Z abram $
package javassist.tools;

public class FileElement {
  public String target;
  public String source;
  public String liquiSrc;
  public String Details;
  public long Timestamp;
  
  public FileElement()
  {
	  super();
  }
  
  public FileElement(String source, String target)
  {
		super();
		this.source = source;
		this.target = target;
  }
  public FileElement(String source, String target, String details)
  {
	super();
	this.source = source;
	this.target = target;
	Details = details;
  }

  public FileElement(String source, String target, String details, long timestamp)
  {
	super();
	this.source = source;
	this.target = target;
	Details = details;
	Timestamp = timestamp;
  }
}
