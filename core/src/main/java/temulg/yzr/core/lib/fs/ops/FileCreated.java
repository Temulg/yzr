/*
 * Copyright (c) 2018-2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.core.lib.fs.ops;

import java.time.Instant;

import temulg.yzr.core.Entity;
import temulg.yzr.core.MarkPack;
import temulg.yzr.core.MarkPackSimple;
import temulg.yzr.core.Operator;
import temulg.yzr.core.lib.fs.marks.Node;

public class FileCreated extends Entity implements Operator {
	@Override
	public MarkPack newRequisites() {
		return MarkPackSimple.builder().positionalCount(1).build();
	}

	@Override
	public MarkPack newProducts() {
		return MarkPack.EMPTY;
	}

	@Override
	public void apply(
		Operator.Action act, MarkPack requisites, MarkPack products
	) {
		try {
			act.productsUpdated(
				requisites.<Node>get(0).lastModifiedTime()
			);
		} catch (Exception ex) {
			act.failed(ex);
		}
	}
}
