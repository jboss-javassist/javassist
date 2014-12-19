package javassist.tools;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

import javassist.Modifier;
import javassist.NotFoundException;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.AttributeInfo;
import javassist.bytecode.ClassFile;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.InstructionEqualizer;
import javassist.bytecode.MethodInfo;

/**
 * Utility class that provide methods to compare java class files and jar files,
 * including not only class files. In case of class file comparison, only
 * returns human readable difference information. Returns empty string if the
 * classes are equal. In case of jar file comparison, it creates file system
 * hierarchy containing files those are different between the jars. Compares
 * nested jars.
 * 
 * @author Sviatoslav Abramenkov
 * 
 */
public class Comparer {

	private String tmpDir = "tmp";
	private String outputDir = "output";
	List<Pattern> diffIgnoreFiles = new ArrayList<Pattern>();
	boolean ignoreLineNumberDifferenceForMethods = true;

	/**
	 * Default constructor
	 */
	public Comparer() {
		super();
		diffIgnoreFiles.add(Pattern.compile("[/\\\\]pom\\.properties$"));
	}

	/**
	 * Custom constructor that allows to set the folowing members
	 * 
	 * @param tmpdir
	 *            - temporary file system directory, is not cleaned up
	 *            automatically "tmp" is value by the default
	 * @param outputDir
	 *            - file system directory where results of Jar comparison will
	 *            be stored
	 * @param fileDiffIgnorePatterns
	 *            - list of regular expression patterns to suppress comparison
	 *            of certain class files in a jar file by file name
	 */
	public Comparer(String tmpdir, String outputDir,
			List<Pattern> fileDiffIgnorePatterns, boolean ignoreLineNumberDifferenceForMethods) {
		super();
		this.tmpDir = tmpdir;
		this.outputDir = outputDir;
		this.diffIgnoreFiles = fileDiffIgnorePatterns;
		this.ignoreLineNumberDifferenceForMethods = ignoreLineNumberDifferenceForMethods;
	}

	/**
	 * Compares 2 classfiles by the name and returns human readable diff
	 * information
	 * 
	 * @param class1
	 *            - name of first classfile
	 * @param class2
	 *            - name of second classfile
	 * @return human readable information about difference between class1 and
	 *         class2
	 * @throws java.io.FileNotFoundException
	 * @throws java.io.IOException
	 */
	public static String CompareClasses(String class1, String class2, boolean ignoreLineNumbersForMethods)
			throws java.io.FileNotFoundException, java.io.IOException {
		BufferedInputStream fin = new BufferedInputStream(new FileInputStream(
				class1));
		ClassFile cf1 = new ClassFile(new DataInputStream(fin));
		fin = new BufferedInputStream(new FileInputStream(class2));
		ClassFile cf2 = new ClassFile(new DataInputStream(fin));
		return CompareClasses(cf1, cf2, ignoreLineNumbersForMethods);
	}

	/**
	 * Compares 2 classfiles and returns human readable diff information
	 * 
	 * @param cf1
	 *            - first classfile to compare
	 * @param cf2
	 *            - second classfile to compare
	 * @return human readable information about difference between cf1 and cf2
	 */
	public static String CompareClasses(ClassFile cf1, ClassFile cf2, boolean ignoreLineNumbersForMethods) {
		String res = "";
		if (cf1.getMajorVersion() != cf2.getMajorVersion()) {
			res += "Major version is different: " + cf1.getMajorVersion()
					+ " -> " + cf2.getMajorVersion() + "\n";
		}

		if (cf1.getMinorVersion() != cf2.getMinorVersion()) {
			res += "Minor version is different: " + cf1.getMinorVersion()
					+ " -> " + cf2.getMinorVersion() + "\n";
		}

		if (cf1.getAccessFlags() != cf2.getAccessFlags()) {
			res += "Access flags are different: " + cf1.getAccessFlags()
					+ " -> " + cf2.getAccessFlags() + "\n";
		}

		if (!cf1.getName().equals(cf2.getName())) {
			res += "Name is different: " + cf1.getName() + " -> "
					+ cf2.getName() + "\n";
		}

		if (!cf1.getSuperclass().equals(cf2.getSuperclass())) {
			res += "Superclass is different: " + cf1.getSuperclass() + " -> "
					+ cf2.getSuperclass() + "\n";
		}

		res += AttributeInfo.CompareStringArrays(cf1.getInterfaces(),
				cf2.getInterfaces(), "Interface");
		res += CompareFields(cf1.getFields(), cf2.getFields());
		res += CompareMethods(cf1.getMethods(), cf2.getMethods(), ignoreLineNumbersForMethods);

		ArrayList<String> ignoreList = new ArrayList<String>();
		ignoreList.add("RuntimeVisibleAnnotations");
		ArrayList<Pattern> ignorePatterns = new ArrayList<Pattern>();
		ignorePatterns.add(Pattern.compile(".*com.softcomputer.totalqc.common.TQCVersion.*"));

		res += AttributeInfo.CompareAttributes(cf1.getAttributes(),
				cf2.getAttributes(), ignoreList, ignorePatterns);

		return res;
	}

	private static String CompareFields(List<FieldInfo> fields1,
			List<FieldInfo> fields2) {
		String res = "";
		int f1Count = fields1.size();
		int f2Count = fields2.size();
		for (int i = 0; i < f1Count; ++i) {
			FieldInfo f1info = fields1.get(i);
			boolean matched = false;
			String fieldDesc = "Field: " + f1info.getName() + " "
					+ f1info.getDescriptor();
			for (int j = 0; j < f2Count; ++j) {
				FieldInfo f2info = fields2.get(j);
				if (f1info.getName().equals(f2info.getName())
						&& f1info.getDescriptor()
								.equals(f2info.getDescriptor())) {
					if (f1info.getAccessFlags() == f2info.getAccessFlags()) {
						String attrDiff = AttributeInfo.CompareAttributes(
								f1info.getAttributes(), f2info.getAttributes());
						if (attrDiff.length() > 0) {
							res += fieldDesc + "\n" + attrDiff;
						}
					} else {
						res += fieldDesc
								+ "\nAccess flags: "
								+ Modifier.toString(AccessFlag
										.toModifier(f1info.getAccessFlags()))
								+ " -> "
								+ Modifier.toString(AccessFlag
										.toModifier(f2info.getAccessFlags()));
					}
					matched = true;
				}
			}
			if (!matched) {
				res += fieldDesc + " has been removed\n";
			}
		}
		for (int i = 0; i < f2Count; ++i) {
			FieldInfo f2info = fields2.get(i);
			boolean matched = false;
			String fieldDesc = "Field: " + f2info.getName() + " "
					+ f2info.getDescriptor();
			for (int j = 0; j < f1Count; ++j) {
				FieldInfo f1info = fields1.get(j);
				if (f2info.getName().equals(f1info.getName())
						&& f2info.getDescriptor()
								.equals(f1info.getDescriptor())) {
					matched = true;
				}
			}
			if (!matched) {
				res += fieldDesc + " has been removed\n";
			}
		}
		return res;
	}

	private static String CompareMethods(List<MethodInfo> methods1,
			List<MethodInfo> methods2, boolean ignoreLineNumbers) {
		String res = "";
		int m1Count = methods1.size();
		int m2Count = methods2.size();
		for (int i = 0; i < m1Count; ++i) {
			MethodInfo m1info = methods1.get(i);
			boolean matched = false;
			String methodDesc = "Method: " + m1info.getName() + " "
					+ m1info.getDescriptor();
			for (int j = 0; j < m2Count; ++j) {
				MethodInfo m2info = methods2.get(j);
				if (m1info.getName().equals(m2info.getName())
						&& m1info.getDescriptor()
								.equals(m2info.getDescriptor())) {
					if (m1info.getAccessFlags() == m2info.getAccessFlags()) {
			        	ArrayList<String> ignoreList = new ArrayList<String>();
			        	if (ignoreLineNumbers) {
			        		ignoreList.add("LineNumberTable");
			        	}
						String attrDiff = AttributeInfo.CompareAttributes(
								m1info.getAttributes(), m2info.getAttributes(), ignoreList);
						if (attrDiff.length() > 0) {
							res += methodDesc + "\n" + attrDiff;
						}
					} else {
						res += methodDesc
								+ "\nAccess flags: "
								+ Modifier.toString(AccessFlag
										.toModifier(m1info.getAccessFlags()))
								+ " -> "
								+ Modifier.toString(AccessFlag
										.toModifier(m2info.getAccessFlags()));
					}
					matched = true;
				}
			}
			if (!matched) {
				res += methodDesc + " has been removed\n";
			}
		}
		for (int i = 0; i < m2Count; ++i) {
			MethodInfo m2info = methods2.get(i);
			boolean matched = false;
			String methodDesc = "Method: " + m2info.getName() + " "
					+ m2info.getDescriptor();
			for (int j = 0; j < m1Count; ++j) {
				MethodInfo m1info = methods1.get(j);
				if (m2info.getName().equals(m1info.getName())
						&& m2info.getDescriptor()
								.equals(m1info.getDescriptor())) {
					matched = true;
				}
			}
			if (!matched) {
				res += methodDesc + " has been added\n";
			}
		}
		return res;
	}

	/**
	 * Compares 2 jar files, fills collections addEl, replaceEl, removeEl with
	 * comparison results; performs copying of different classes from jar2path
	 * into the hierarchy under outputDir
	 * 
	 * @param jar1path
	 *            - path to jar that is a comparison base
	 * @param jar2path
	 *            - path to jar that is compared to the base
	 * @param base
	 *            - base path that will be used as a root of output hierarchy
	 * @param target
	 *            - target path in case if we are going to patch using the diff
	 * @param source
	 *            - source path in case if we are going to store classes to
	 *            patch in a different location than target
	 * @param addEl
	 *            - collection with classes/files those exist only in jar2path
	 * @param replaceEl
	 *            - collection with classes/files those are present in both
	 *            jar1path and jar2path, but are different
	 * @param removeEl
	 *            - collection with classes/files those are absent in jar2path,
	 *            but present in jar1path
	 * @throws NotFoundException
	 * @throws IOException
	 */
	public void CompareJars(String jar1path, String jar2path, String base,
			String target, String source, JarElement addEl,
			JarElement replaceEl, JarElement removeEl)
			throws NotFoundException, IOException {
		JarElement resAdd = new JarElement(source, target);
		JarElement resRemove = new JarElement(source, target);
		JarElement resReplace = new JarElement(source, target);
		String baseName = source;
		if ((null == baseName) || baseName.isEmpty()) {
			baseName = target;
		}
		if (!base.isEmpty()) {
			baseName = base + "/" + baseName;
		}

		JarFile jar1 = new JarFile(jar1path);
		JarFile jar2 = new JarFile(jar2path);
		Map<String, String> jar1Entries = getJarEntries(jar1);
		Map<String, String> jar2Entries = getJarEntries(jar2);
		for (String j1entry : jar1Entries.keySet()) {
			if (!jar1Entries.get(j1entry).equals(jar2Entries.get(j1entry))) {
				if (null == jar2Entries.get(j1entry)) {
					resRemove.files.add(new FileElement(null, j1entry));
				} else {
					if (fileIsJar(j1entry)) {
						String tmpBase = tmpDir + "/" + baseName;
						writeJarEntry(jar1, j1entry, tmpBase + "/base"
								+ j1entry);
						writeJarEntry(jar2, j1entry, tmpBase + "/final"
								+ j1entry);
						CompareJars(tmpBase + "/base" + j1entry, tmpBase
								+ "/final" + j1entry, baseName, j1entry, null,
								resAdd, resReplace, resRemove);
					} else {
						if (fileToBeIgnored(j1entry)) {
							continue;
						}
						if (j1entry.endsWith(".class")) {
							JarEntry c1e = jar1.getJarEntry(j1entry);
							JarEntry c2e = jar2.getJarEntry(j1entry);

							ClassFile cf1 = new ClassFile(new DataInputStream(
									jar1.getInputStream(c1e)));
							ClassFile cf2 = new ClassFile(new DataInputStream(
									jar2.getInputStream(c2e)));
							String cDiff = CompareClasses(cf1, cf2, ignoreLineNumberDifferenceForMethods);
							if (cDiff.length() > 0) {
								resReplace.files.add(new FileElement(null,
										j1entry, cDiff));
								addToOutput(jar2, j1entry, baseName);
							}
						} else {
							resReplace.files
									.add(new FileElement(null, j1entry));
							addToOutput(jar2, j1entry, baseName);
						}
					}
				}
			}
		}
		for (String j2entry : jar2Entries.keySet()) {
			if (null == jar1Entries.get(j2entry)) {
				resAdd.files.add(new FileElement(null, j2entry));
				addToOutput(jar2, j2entry, baseName);
			}
		}
		if (!resAdd.files.isEmpty()) {
			addEl.files.add(resAdd);
		}
		if (!resReplace.files.isEmpty()) {
			replaceEl.files.add(resReplace);
		}
		if (!resRemove.files.isEmpty()) {
			removeEl.files.add(resRemove);
		}
	}

	boolean fileToBeIgnored(String fileName) {
		for (Pattern p : diffIgnoreFiles) {
			if (p.matcher(fileName).find()) {
				return true;
			}
		}
		return false;
	}

	void addToOutput(JarFile jar, String jentry, String base)
			throws IOException {
		String fileName = outputDir + "/" + base + "/" + jentry;
		String dirName = fileName.substring(0, fileName.lastIndexOf('/'));
		File f = new File(dirName);
		f.mkdirs();
		writeJarEntry(jar, jentry, fileName);
	}

	void writeJarEntry(JarFile jar, String jentry, String fileName)
			throws IOException {
		FileOutputStream fo = null;
		InputStream ji = null;
		try {
			File f = new File(fileName);
			f = new File(f.getParent());
			f.mkdirs();
			fo = new FileOutputStream(fileName);
			JarEntry je = jar.getJarEntry(jentry);
			ji = jar.getInputStream(je);

			// copy/pasted from IOUtils.copy() to avoid a dependency
			final int EOF = -1;
			int n = 0;
			final byte[] buffer = new byte[1024 * 4];
			while (EOF != (n = ji.read(buffer))) {
				fo.write(buffer, 0, n);
			}
		} finally {
			if (fo != null) {
				fo.close();
			}
			if (ji != null) {
				ji.close();
			}
		}
	}

	Map<String, String> getJarEntries(JarFile jar) throws IOException {
		Map<String, String> res = new TreeMap<String, String>();
		Enumeration<JarEntry> jentries = jar.entries();
		byte[] buffer = new byte[65536];
		while (jentries.hasMoreElements()) {
			JarEntry jarEntry = jentries.nextElement();
			if (jarEntry.isDirectory())
				continue;
			InputStream jis = jar.getInputStream(jarEntry);
			try {
				MessageDigest digest = java.security.MessageDigest
						.getInstance("MD5");
				DigestInputStream dis = new DigestInputStream(jis, digest);
				while (dis.read(buffer) != -1)
					;
				String hash = InstructionEqualizer.getHex(digest.digest());
				res.put(jarEntry.getName(), hash);
			} catch (NoSuchAlgorithmException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				jis.close();
			}
		}
		return res;
	}

	boolean fileIsJar(String name) {
		if (name.endsWith(".jar") || name.endsWith(".war")
				|| name.endsWith(".ear") || name.endsWith(".rar")
				|| name.endsWith(".sar")) {
			return true;
		} else {
			return false;
		}
	}

}
