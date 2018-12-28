/*
 * Copyright (c) 2018-2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.Executor;

import org.jgrapht.alg.cycle.CycleDetector;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.traverse.TopologicalOrderIterator;

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

	public ActionTracker makeActionTracker(Executor exec) {
		var at = new ActionTracker(vertexMap.size(), exec);
		HashMap<UUID, ActionTracker.Item> imap = new HashMap<>();

		vertexMap.forEach((k, v) -> {
			var prevCount = g.inDegreeOf(v);

			var it = at.new Item(v, prevCount, g.outDegreeOf(v));
			imap.put(k, it);
			if (prevCount == 0)
				at.roots.add(it);
		});

		edgeMap.forEach((k, e) -> {
			var src = ((Entity)g.getEdgeSource(e).op).getEID();
			var dst = ((Entity)g.getEdgeTarget(e).op).getEID();

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

		var edge = getEdge(
			m,
			prodSel.put(src.products, m),
			reqSel.put(dst.requisites, m)
		);

		g.addEdge(src, dst, edge);
	}

	private Vertex getVertex(Operator op) {
		var entity = (Entity)op;

		return vertexMap.computeIfAbsent(entity.getEID(), k -> {
			var v = new Vertex(op);
			g.addVertex(v);
			return v;
		});
	}

	private Edge getEdge(
		Mark mark, MarkPack.Ref prod, MarkPack.Ref req
	) {
		var entity = (Entity)mark;

		return edgeMap.computeIfAbsent(entity.getEID(), k -> {
			return new Edge(mark, prod, req);
		});
	}

	class Vertex {
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

	class Edge {
		private Edge(
			Mark mark_, MarkPack.Ref prod_, MarkPack.Ref req_
		) {
			mark = mark_;
			prod = prod_;
			req = req_;
		}

		@Override
		public boolean equals(Object other) {
			return mark.equals(((Edge)other).mark);
		}
	
		@Override
		public int hashCode() {
			return mark.hashCode();
		}

		final Mark mark;
		final MarkPack.Ref prod;
		final MarkPack.Ref req;
	}

	private final HashMap<UUID, Vertex> vertexMap = new HashMap<>();
	private final HashMap<UUID, Edge> edgeMap = new HashMap<>();
	private final SimpleDirectedGraph<
		Vertex, Edge
	> g = new SimpleDirectedGraph<>(null, null, false);
}
