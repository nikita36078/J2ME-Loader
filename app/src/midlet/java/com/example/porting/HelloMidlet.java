/*
 *  Copyright 2022 Yury Kharchenko
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.example.porting;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.Form;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.shell.AppClassLoader;

public class HelloMidlet extends MIDlet implements CommandListener {

	private Command exitCmd;

	@Override
	public void startApp() throws MIDletStateChangeException {
		Form form = new Form("Midlet Porting Sample");
		form.append("Hello World!");
		form.append(getText());
		exitCmd = new Command("Exit", Command.EXIT, 1);
		form.addCommand(exitCmd);
		form.setCommandListener(this);
		Display.getDisplay(this).setCurrent(form);
	}

	@Override
	public void pauseApp() {

	}

	@Override
	public void destroyApp(boolean unconditional) throws MIDletStateChangeException {

	}

	private String getText() {
		// calls clazz.getResourcesAsStream(name) must not be called on system classes
		InputStream stream = getClass().getResourceAsStream("sample.ext");
		if (stream == null) {
			return "Resource file not found.";
		}
		DataInputStream dis = null;
		try {
			dis = new DataInputStream(stream);
			return dis.readUTF();
		} catch (Exception e) {
			e.printStackTrace();
			return "Error read resource file.";
		} finally {
			if (dis != null) {
				try {
					dis.close();
				} catch (IOException ignored) {
				}
			}
		}
	}

	@Override
	public void commandAction(Command c, Displayable d) {
		if (c == exitCmd) {
			notifyDestroyed();
		}
	}
}
