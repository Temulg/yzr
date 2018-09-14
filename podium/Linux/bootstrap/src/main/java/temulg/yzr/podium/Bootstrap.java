/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.podium;

import java.nio.charset.Charset;
import java.util.ArrayList;

public class Bootstrap {
	public Bootstrap() {
		System.out.println("bootstrap");
		String encoding = System.getProperty(ENCODING_PROPERTY_NAME);
		Charset cs = null;

		if (encoding != null) {
			try {
				cs = Charset.forName(encoding);
			} catch (Exception e) {
				cs = Charset.defaultCharset();
			}
		} else
			cs = Charset.defaultCharset();

		charset = cs;
	}

	public void setProgramName(byte[] name) {
		progName = new String(name, charset);
	}

	public void appendArgument(byte[] arg) {
		args.add(new String(arg, charset));
	}

	public void start() {
		System.out.println("Started: " + progName);
		args.forEach(s -> {
			System.out.format("arg %s\n", s);
		});
	}

	public static final String ENCODING_PROPERTY_NAME = "sun.jnu.encoding";

	private String progName = "";
	private final ArrayList<String> args = new ArrayList<>();
	private final Charset charset;
}
