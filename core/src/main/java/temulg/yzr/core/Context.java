/*
 * Copyright (c) 2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.core;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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

	public Mark.Info getInfo(Mark m) {
		return markInfo.computeIfAbsent(
			m.entity().getEID(), k -> {
				return m.getInfo(this);
			}
		);
	}

	public Mark.Info updateInfo(Mark m) {
		return markInfo.merge(
			m.entity().getEID(),
			m.getInfo(this),
			(prev, next) -> {
				return next.lastModified().isAfter(
					prev.lastModified()
				) ? next : prev;
			}
		);
	}

	private final ForkJoinPool exec = new ForkJoinPool();
	private final ConcurrentHashMap<
		UUID, Mark.Info
	> markInfo = new ConcurrentHashMap<>();
}
