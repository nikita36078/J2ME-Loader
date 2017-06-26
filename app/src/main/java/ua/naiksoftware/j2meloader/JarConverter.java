package ua.naiksoftware.j2meloader;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import com.android.dx.command.Main;

import org.microemu.android.asm.AndroidProducer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;

import javax.microedition.shell.ConfigActivity;

import ua.naiksoftware.util.Log;
import ua.naiksoftware.util.FileUtils;

public class JarConverter extends AsyncTask<String, String, Boolean> {

	private static final String tag = "JarConverter";

	private final Context context;
	private String err = "Void error";
	private ProgressDialog dialog;

	private String pathToJar;
	private String appDir;
	private final File dirTmp;

	public JarConverter(MainActivity context) {
		this.context = context;
		dirTmp = new File(context.getApplicationInfo().dataDir + "/tmp");
	}

	@Override
	protected Boolean doInBackground(String... p1) {
		pathToJar = p1[0];
		String pathConverted = p1[1];
		Log.d(tag, "doInBackground$ pathToJar=" + pathToJar + " pathConverted="
				+ pathConverted);
		dirTmp.mkdir();
		File tmp = new File(pathToJar);
		File tmp2 = new File(dirTmp, tmp.getName() + ".jar");
		try {
			AndroidProducer.processJar(tmp, tmp2, true);
		} catch (IOException e) {
			e.printStackTrace();
			err = "Can't convert";
			tmp2.delete();
			FileUtils.deleteDirectory(dirTmp);
			return false;
		}
		pathToJar = tmp2.getAbsolutePath();
		try {
			if (!FileUtils.unzip(new FileInputStream(new File(pathToJar)),
					dirTmp)) {
				err = "Brocken jar";
				tmp2.delete();
				FileUtils.deleteDirectory(dirTmp);
				return false;
			}
		} catch (FileNotFoundException e) {
			err = e.getMessage();
			return false;
		}
		appDir = FileUtils.loadManifest(
				new File(dirTmp, "/META-INF/MANIFEST.MF")).get("MIDlet-Name");
		appDir = appDir.replace(":", "");
		File appConverted = new File(pathConverted + appDir);
		FileUtils.deleteDirectory(appConverted);
		appConverted.mkdirs();
		Log.d(tag, "appConverted=" + appConverted.getPath());
		Main.main(new String[]{
				"--dex", "--no-optimize",
				"--output=" + appConverted.getPath()
						+ ConfigActivity.MIDLET_DEX_FILE,
				/* dirForJAssist.getPath() */pathToJar});
		File conf = new File(dirTmp, "/META-INF/MANIFEST.MF");
		try {
			FileUtils.copyFileUsingChannel(conf, new File(appConverted, ConfigActivity.MIDLET_CONF_FILE));
		} catch (IOException e) {
			e.printStackTrace();
		}
		tmp2.delete();
		// Extract other resources from jar.
		FileUtils.moveFiles(dirTmp.getPath(), pathConverted + appDir
				+ ConfigActivity.MIDLET_RES_DIR, new FilenameFilter() {
			public boolean accept(File dir, String fname) {
				if (fname.endsWith(".class")) {
					return false;
				} else {
					return true;
				}
			}
		});
		FileUtils.deleteDirectory(dirTmp);
		File dexTemp = new File(context.getApplicationInfo().dataDir, ConfigActivity.MIDLET_DEX_FILE);
		dexTemp.delete();
		FileUtils.deleteDirectory(dirTmp);
		return true;
	}

	@Override
	public void onPreExecute() {
		// Log.i(tag, "onPreExecute");
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
		// Log.i(tag, "onPostExecute with: " + result);
		Toast t;
		if (result) {
			t = Toast.makeText(context,
					context.getResources().getString(R.string.convert_complete)
							+ " " + appDir, Toast.LENGTH_LONG);
			((MainActivity) context).updateApps();
		} else {
			t = Toast.makeText(context, err, Toast.LENGTH_LONG);

		}
		dialog.dismiss();
		t.show();
	}
}
