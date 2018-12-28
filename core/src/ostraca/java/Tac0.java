/*
 * Copyright (c) 2018-2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

import java.util.concurrent.ForkJoinPool;

import temulg.yzr.core.OpGraph;
import temulg.yzr.core.Operator;
import temulg.yzr.core.PackSelector;
import temulg.yzr.core.lib.fs.marks.Node;
import temulg.yzr.core.lib.fs.ops.FileCreated;
import temulg.yzr.core.lib.fs.ops.FileExists;
import temulg.yzr.core.lib.os.ops.Exec;

public class Tac0 {
	public static void main(String... args) {
		try {
			a();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	static void a() throws Exception {
		var opg = new OpGraph();

		var n0 = Node.of("/bin/cp");
		var n1 = Node.of("t1.txt");
		var n2 = Node.of("t2.txt");

		var op0 = Exec.builder().addRequisite(
			"command"
		).addRequisite().addProduct().build();

		opg.Add(
			new FileExists(), PackSelector.positional(0),
			op0, PackSelector.named("command"),
			n0
		);
		opg.Add(
			new FileExists(), PackSelector.positional(0),
			op0, PackSelector.positional(1),
			n1
		);
		opg.Add(
			op0, PackSelector.positional(0),
			new FileCreated(), PackSelector.positional(0),
			n2
		);

		System.out.println("Verify " + opg.verify());

		var at = opg.makeActionTracker(ForkJoinPool.commonPool());
	}
}
