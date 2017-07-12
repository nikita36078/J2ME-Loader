package ua.naiksoftware.j2meloader;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.android.dx.command.Main;

import org.microemu.android.asm.AndroidProducer;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.zip.ZipException;

import javax.microedition.shell.ConfigActivity;

import ua.naiksoftware.util.Log;
import ua.naiksoftware.util.FileUtils;
import ua.naiksoftware.util.ZipUtils;

public class JarConverter extends AsyncTask<String, String, Boolean> {

	private static final String tag = "JarConverter";

	private final Context context;
	private String err = "Void error";
	private ProgressDialog dialog;

	private String appDir;
	private final File dirTmp;

	public JarConverter(MainActivity context) {
		this.context = context;
		dirTmp = new File(context.getApplicationInfo().dataDir, "tmp");
		dirTmp.mkdir();
	}

	@Override
	protected Boolean doInBackground(String... p1) {
		String pathToJar = p1[0];
		String pathConverted = p1[1];
		Log.d(tag, "doInBackground$ pathToJar=" + pathToJar + " pathConverted="
				+ pathConverted);
		File inputJar = new File(pathToJar);
		File fixedJar;
		try {
			fixedJar = fixJar(inputJar);
		} catch (IOException e) {
			e.printStackTrace();
			err = "Can't convert";
			deleteTemp();
			return false;
		}
		if (!ZipUtils.unzip(fixedJar, dirTmp)) {
			err = "Brocken jar";
			deleteTemp();
			return false;
		}
		appDir = FileUtils.loadManifest(
				new File(dirTmp, "/META-INF/MANIFEST.MF")).get("MIDlet-Name");
		appDir = appDir.replace(":", "");
		File appConverted = new File(pathConverted, appDir);
		FileUtils.deleteDirectory(appConverted);
		appConverted.mkdirs();
		Log.d(tag, "appConverted=" + appConverted.getPath());
		Main.main(new String[]{
				"--dex", "--no-optimize", "--output=" + appConverted.getPath()
				+ ConfigActivity.MIDLET_DEX_FILE, fixedJar.getAbsolutePath()});
		File conf = new File(dirTmp, "/META-INF/MANIFEST.MF");
		try {
			FileUtils.copyFileUsingChannel(conf, new File(appConverted, ConfigActivity.MIDLET_CONF_FILE));
		} catch (IOException e) {
			e.printStackTrace();
		}
		// Extract other resources from jar.
		FileUtils.moveFiles(dirTmp.getPath(), pathConverted + appDir
				+ ConfigActivity.MIDLET_RES_DIR, new FilenameFilter() {
			public boolean accept(File dir, String fname) {
				if (fname.endsWith(".class") || fname.endsWith(".jar.jar")) {
					return false;
				} else {
					return true;
				}
			}
		});
		deleteTemp();
		return true;
	}

	@Override
	public void onPreExecute() {
		dialog = new ProgressDialog(context);
		dialog.setIndeterminate(true);
		dialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		dialog.setCancelable(false);
		dialog.setMessage(context.getText(R.string.converting_message));
		dialog.setTitle(R.string.converting_wait);
		dialog.show();
	}

	@Override
	public void onPostExecute(Boolean result) {
		Toast toast;
		if (result) {
			toast = Toast.makeText(context, context.getResources().getString(R.string.convert_complete) + " " + appDir, Toast.LENGTH_LONG);
			((MainActivity) context).updateApps();
		} else {
			toast = Toast.makeText(context, err, Toast.LENGTH_LONG);
		}
		dialog.dismiss();
		toast.show();
	}

	private File fixJar(File inputJar) throws IOException {
		File fixedJar = new File(dirTmp, inputJar.getName() + ".jar");
		try {
			AndroidProducer.processJar(inputJar, fixedJar, true);
		} catch (ZipException e) {
			File unpackedJarFolder = new File(context.getApplicationInfo().dataDir, "tmp_fix");
			ZipUtils.unzip(inputJar, unpackedJarFolder);

			File repackedJar = new File(dirTmp, inputJar.getName());
			ZipUtils.zipFileAtPath(unpackedJarFolder, repackedJar);

			AndroidProducer.processJar(repackedJar, fixedJar, true);
			FileUtils.deleteDirectory(unpackedJarFolder);
			repackedJar.delete();
		}
		return fixedJar;
	}

	private void deleteTemp() {
		// Delete temp files
		FileUtils.deleteDirectory(dirTmp);
		File uriDir = new File(context.getApplicationInfo().dataDir, "uri_tmp");
		if (uriDir.exists()) {
			FileUtils.deleteDirectory(uriDir);
		}
	}
}
