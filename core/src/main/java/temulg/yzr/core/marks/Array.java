/*
 * Copyright (c) 2018-2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.core.marks;

import java.util.Arrays;

import temulg.yzr.core.Context;
import temulg.yzr.core.Mark;

/**
 * Array is a simple immutable collection of Marks of the same type.
 */
public class Array implements Mark {
	private Array(Mark[] nested_) {
		nested = Arrays.copyOf(nested_, nested_.length);
	}

	@Override
	public Info getInfo(Context context) {
		return null;
	}

	private final Mark[] nested;
}
