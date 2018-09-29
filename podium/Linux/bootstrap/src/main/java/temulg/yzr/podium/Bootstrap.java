/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.podium;

public class Bootstrap {
	public Bootstrap() {
		System.out.println("Bootstrap in");
		for (var entry: System.getProperties().entrySet()) {
			System.out.format(
				"%s: %s\n", entry.getKey(), entry.getValue()
			);
		}
	}

	public void prepare(String... args) {
		System.out.println("Prepare in");
		for (var s: args) {
			System.out.format("arg %s\n", s);
		}
	}

	public void start() {
		System.out.println("Started");
	}
}
