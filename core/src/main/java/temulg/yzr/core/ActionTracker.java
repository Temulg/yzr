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

	private static class ItemLink {
		ItemLink(Item it_, ProdPack.Getter prod_, ReqPack.Setter req_) {
			it = it_;
			prod = prod_;
			req = req_;
		}
		final Item it;
		final ProdPack.Getter prod;
		final ReqPack.Setter req;
	}

	class Item implements Operator.Action, Runnable {
		Item(
			OpGraph.Vertex v_, int prevCount_, int nextCount
		) {
			v = v_;
			prevCount = prevCount_;
			next = new ArrayList<>(nextCount);
			requisites = v.requisites.allocate();
			products = v.products.allocate();
		}

		@Override
		public void run() {
			if (aborted)
				return;

			actionsStarted.incrementAndGet();
			prodsUpdated = reqsUpdated.get();

			try {
				v.op.apply(this, requisites, products);

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
			next.forEach(next -> {
				var m = next.prod.get(products);
				next.req.set(next.it.requisites, m);

				if (next.it.markReady(prodsUpdated) && !aborted)
					context.execute(next.it);
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

		void linkNext(
			Item it, ProdPack.Getter prod, ReqPack.Setter req
		) {
			next.add(new ItemLink(it, prod, req));
		}

		private final OpGraph.Vertex v;
		private final AtomicInteger prevCompleted = new AtomicInteger();
		private final AtomicReference<
			Instant
		> reqsUpdated = new AtomicReference<>(Instant.MIN);
		private final int prevCount;

		private final ReqPack.Storage requisites;
		private final ProdPack.Storage products;

		final ArrayList<ItemLink> next;
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
