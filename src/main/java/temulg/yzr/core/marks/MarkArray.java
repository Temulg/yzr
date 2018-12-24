/*
 * Copyright (c) 2018-2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.core.marks;

import java.util.Arrays;

/**
 * MarkArray is a simple immutable collection of Marks of the same type.
 */
public class MarkArray implements Mark {
	private MarkArray(Mark[] nested_) {
		nested = Arrays.copyOf(nested_, nested_.length);
	}

	private final Mark[] nested;
}
