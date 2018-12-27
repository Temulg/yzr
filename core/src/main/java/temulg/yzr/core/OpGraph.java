/*
 * Copyright (c) 2018-2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.core;

import org.jgrapht.graph.SimpleDirectedGraph;

public class OpGraph {
	public ActionGraph toActionGraph() {
		return null;
	}

	public void Add(
		Operator src_, PackSelector prodSel,
		Operator dst_, PackSelector reqSel,
		Mark m
	) {
		var src = new Vertex(src_);
		var dst = new Vertex(dst_);
		var edge = new Edge(
			m,
			prodSel.put(src.products, m),
			reqSel.put(dst.requisites, m)
		);

		g.addEdge(src, dst, edge);
	}

	class Vertex {
		Vertex(Operator op_) {
			op = op_;
			products = op.newProducts();
			requisites = op.newRequisites();
		}

		final Operator op;
		final MarkPack products;
		final MarkPack requisites;
	}

	class Edge {
		Edge(Mark mark_, MarkPack.Ref prod_, MarkPack.Ref req_) {
			mark = mark_;
			prod = prod_;
			req = req_;
		}

		final Mark mark;
		final MarkPack.Ref prod;
		final MarkPack.Ref req;
	}

	private final SimpleDirectedGraph<
		Vertex, Edge
	> g = new SimpleDirectedGraph<>(null, null, false);
}
