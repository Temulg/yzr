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
			imap.get(src).linkNext(
				imap.get(dst), edge.prod, edge.req
			);
		});

		return at;
	}

	public void Add(
		Operator src_, ProdPack.Selector prodSel,
		Operator dst_, ReqPack.Selector reqSel
	) {
		var src = getVertex(src_);
		var dst = getVertex(dst_);

		g.addEdge(src, dst, new Edge(
			src.products.select(prodSel),
			dst.requisites.select(reqSel)
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
			requisites = op.newRequisites();
			products = op.newProducts();
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
		final ReqPack requisites;
		final ProdPack products;
	}

	static class Edge extends DefaultEdge {
		private Edge(ProdPack.Getter prod_, ReqPack.Setter req_) {
			if (prod_ == null || req_ == null)
				throw new IllegalArgumentException(
					"Getter or setter is not valid"
				);

			prod = prod_;
			req = req_;
		}

		private static final long serialVersionUID = 0x23426dfdf837eaaaL;

		final transient ProdPack.Getter prod;
		final transient ReqPack.Setter req;
	}

	private final HashMap<UUID, Vertex> vertexMap = new HashMap<>();
	private final DirectedMultigraph<
		Vertex, Edge
	> g = new DirectedMultigraph<>(null, null, false);
}
