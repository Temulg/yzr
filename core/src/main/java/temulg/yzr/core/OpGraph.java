/*
 * Copyright (c) 2018-2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.core;

import java.util.HashMap;
import java.util.UUID;

import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DirectedMultigraph;

public class OpGraph extends Entity {
	public boolean verify() {
		var cycleDetector = new CycleDetector<>(g);

		if (!cycleDetector.detectCycles())
			return true;

		System.out.println("Cycles detected.");
		var iter = cycleDetector.findCycles().iterator();
		while (iter.hasNext()) {
			System.out.println("Cycle:");

			var v = iter.next();
			System.out.println("   " + v);
		}
		return false;
	}

	public ActionTracker makeActionTracker(Context context) {
		var at = new ActionTracker(context, vertexMap.size());
		HashMap<UUID, ActionTracker.Item> imap = new HashMap<>();

		vertexMap.forEach((k, v) -> {
			var prevCount = g.inDegreeOf(v);

			var it = at.new Item(v, prevCount, g.outDegreeOf(v));
			imap.put(k, it);
			if (prevCount == 0)
				at.roots.add(it);
		});

		g.edgeSet().forEach(edge -> {
			var src = g.getEdgeSource(edge).op.entity().getEID();
			var dst = g.getEdgeTarget(edge).op.entity().getEID();
			imap.get(src).next.add(imap.get(dst));
		});

		return at;
	}

	public void Add(
		Operator src_, PackSelector prodSel,
		Operator dst_, PackSelector reqSel,
		Mark m
	) {
		var src = getVertex(src_);
		var dst = getVertex(dst_);

		g.addEdge(src, dst, new Edge(
			m,
			prodSel.put(src.products, m),
			reqSel.put(dst.requisites, m)
		));
	}

	private Vertex getVertex(Operator op) {
		return vertexMap.computeIfAbsent(op.entity().getEID(), k -> {
			var v = new Vertex(op);
			g.addVertex(v);
			return v;
		});
	}

	static class Vertex {
		private Vertex(Operator op_) {
			op = op_;
			products = op.newProducts();
			requisites = op.newRequisites();
		}

		@Override
		public boolean equals(Object other) {
			return op.equals(((Vertex)other).op);
		}

		@Override
		public int hashCode() {
			return op.hashCode();
		}

		final Operator op;
		final MarkPack products;
		final MarkPack requisites;
	}

	static class Edge extends DefaultEdge {
		private Edge(
			Mark mark_, MarkPack.Ref prod_, MarkPack.Ref req_
		) {
			mark = mark_;
			prod = prod_;
			req = req_;
		}

		private static final long serialVersionUID = 0x23426dfdf837eaaaL;

		final transient Mark mark;
		final transient MarkPack.Ref prod;
		final transient MarkPack.Ref req;
	}

	private final HashMap<UUID, Vertex> vertexMap = new HashMap<>();
	private final DirectedMultigraph<
		Vertex, Edge
	> g = new DirectedMultigraph<>(null, null, false);
}
