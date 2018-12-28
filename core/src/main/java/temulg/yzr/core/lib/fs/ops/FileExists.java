/*
 * Copyright (c) 2018-2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.core.lib.fs.ops;

import temulg.yzr.core.Operator;
import temulg.yzr.core.Entity;
import temulg.yzr.core.MarkPack;
import temulg.yzr.core.MarkPackSimple;

public class FileExists extends Entity implements Operator {
	@Override
	public MarkPack newRequisites() {
		return MarkPack.EMPTY;
	}

	@Override
	public MarkPack newProducts() {
		return MarkPackSimple.builder().positionalCount(1).build();
	}
}
