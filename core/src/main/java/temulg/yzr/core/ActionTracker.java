/*
 * Copyright (c) 2018-2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.core;

import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class ActionTracker {
	ActionTracker(Context context_, int actionCount_) {
		context = context_;
		actionCount = actionCount_;
	}

	public void start() {
		roots.forEach(context::execute);
		roots.clear();
	}

	public void abort() {
		aborted = true;
	}

	class Item implements Operator.Action, Runnable {
		Item(
			OpGraph.Vertex v_, int prevCount_, int nextCount
		) {
			v = v_;
			prevCount = prevCount_;
			next = new ArrayList<>(nextCount);
		}

		@Override
		public void run() {
			if (aborted)
				return;

			actionsStarted.incrementAndGet();
			prodsUpdated = reqsUpdated.get();

			try {
				v.op.apply(
					this, v.requisites, v.products
				);

				scheduleNext();
			} catch (Exception ex) {
				System.out.println(ex);
				aborted = true;
			} finally {
				actionsCompleted.incrementAndGet();
			}
		}

		private boolean markReady(Instant inst_) {
			reqsUpdated.accumulateAndGet(inst_, (prev, inst) -> {
				return inst.isAfter(prev) ? inst : prev;
			});

			return prevCompleted.incrementAndGet() == prevCount;
		}

		private void scheduleNext() {
			next.forEach(it -> {
				if (it.markReady(prodsUpdated) && !aborted)
					context.execute(it);
			});
			next.clear();
		}

		@Override
		public Context context() {
			return context;
		}

		@Override
		public void skipped(boolean res) {
			skipped = res;
		}

		@Override
		public void failed(Throwable t) {
			System.out.println(t);
			aborted = true;
		}

		@Override
		public Instant requisitesUpdated() {
			return reqsUpdated.get();
		}

		@Override
		public void productsUpdated(Instant inst) {
			prodsUpdated = inst;
		}

		private final OpGraph.Vertex v;
		private final AtomicInteger prevCompleted = new AtomicInteger();
		private final AtomicReference<
			Instant
		> reqsUpdated = new AtomicReference<>(Instant.MIN);
		private final int prevCount;
		final ArrayList<Item> next;
		private volatile boolean skipped;
		private volatile Instant prodsUpdated;
	}

	final ArrayList<Item> roots = new ArrayList<>();
	private final Context context;
	private final int actionCount;
	private final AtomicInteger actionsStarted = new AtomicInteger();
	private final AtomicInteger actionsCompleted = new AtomicInteger();
	private volatile boolean aborted;
}
