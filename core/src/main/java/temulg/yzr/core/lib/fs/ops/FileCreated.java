/*
 * Copyright (c) 2018-2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.core.lib.fs.ops;

import temulg.yzr.core.MarkPack;
import temulg.yzr.core.MarkPackSimple;
import temulg.yzr.core.Operator;

public class FileCreated implements Operator {
	@Override
	public MarkPack newRequisites() {
		return MarkPackSimple.builder().positionalCount(1).build();
	}

	@Override
	public MarkPack newProducts() {
		return MarkPack.EMPTY;
	}
}
