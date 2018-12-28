/*
 * Copyright (c) 2018-2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.core;

import java.util.ArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

class ActionTracker {
	ActionTracker(int actionCount_, Executor exec_) {
		actionCount = actionCount_;
		exec = exec_;
	}

	void start() {
		roots.forEach(exec::execute);
		roots.clear();
	}

	class Item implements Runnable {
		Item(OpGraph.Vertex op_, int prevCount_, int nextCount) {
			op = op_;
			prevCount = prevCount_;
			next = new ArrayList<>(nextCount);
		}

		@Override
		public void run() {
		}

		private boolean markReady() {
			return prevCompleted.incrementAndGet() == prevCount;
		}

		private void scheduleNext() {
			next.forEach(it -> {
				if (it.markReady() && !aborted)
					exec.execute(it);
			});
			next.clear();
		}

		private final OpGraph.Vertex op;
		private final AtomicInteger prevCompleted = new AtomicInteger();
		private final int prevCount;
		final ArrayList<Item> next;
	}

	final ArrayList<Item> roots = new ArrayList<>();
	private final int actionCount;
	private final AtomicInteger actionsCompleted = new AtomicInteger();
	private final Executor exec;
	private volatile boolean aborted;
}
