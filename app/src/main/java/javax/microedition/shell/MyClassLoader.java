package javax.microedition.shell;

import java.io.File;
import java.io.InputStream;

import javax.microedition.util.ContextHolder;

import dalvik.system.DexClassLoader;

public class MyClassLoader extends DexClassLoader {

	private static File resFolder;
	private static String resFolderName;

	public MyClassLoader(String paths, String tmpDir, String libs, ClassLoader parent, String resDir) {
		super(paths, tmpDir, libs, parent);
		resFolder = new File(resDir);
		String[] segments = resDir.split("/");
		resFolderName = "/" + segments[segments.length - 2] + "/";
	}

	@Override
	public InputStream getResourceAsStream(String resName) {
		return ContextHolder.getResourceAsStream(null, resName);
	}

	public static File getResFolder() {
		return resFolder;
	}

	public static String getName() {
		return resFolderName;
	}
}
