/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.basis;

public class NomenException {
	private NomenException() {
	}

	public static class NotASubNomen extends RuntimeException {
		NotASubNomen(Nomen base, Nomen rel) {
			super(String.format(
				"Nomen %s is not a sub-nomen of %s",
				rel, base
			));
		}

		public static final long serialVersionUID
		= 0x68eba01778ae8ef7L;
	}
}