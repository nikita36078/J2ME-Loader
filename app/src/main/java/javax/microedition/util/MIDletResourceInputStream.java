package javax.microedition.util;

import android.support.annotation.NonNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class MIDletResourceInputStream extends InputStream {
	private FileInputStream fis;

	public MIDletResourceInputStream(@NonNull File file) throws FileNotFoundException {
		fis = new FileInputStream(file);
	}

	@Override
	public int available() throws IOException {
		return fis.available();
	}

	@Override
	public int read() throws IOException {
		return fis.read();
	}

	@Override
	public int read(@NonNull byte[] b) throws IOException {
		return fis.read(b);
	}

	@Override
	public int read(@NonNull byte[] b, int off, int len) throws IOException {
		return fis.read(b, off, len);
	}

	@Override
	public long skip(long n) throws IOException {
		if (n > available()) {
			return available() * 2;
		} else {
			return fis.skip(n);
		}
	}
}