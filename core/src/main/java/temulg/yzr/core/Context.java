/*
 * Copyright (c) 2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.core;

import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class Context implements Executor {
	@Override
	public void execute(Runnable command) {
		exec.execute(command);
	}

	public void awaitTermination() {
		exec.awaitQuiescence(5, TimeUnit.MINUTES);
	}

	private final ForkJoinPool exec = new ForkJoinPool();
}
