/*
 * Copyright (c) 2018-2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.core;

import org.jgrapht.graph.SimpleDirectedGraph;

public class OpGraph {

	static class Vertex {
	}

	static class Edge {
	}

	private final SimpleDirectedGraph<
		Vertex, Edge
	> g = new SimpleDirectedGraph<>(null, null, false);
}
