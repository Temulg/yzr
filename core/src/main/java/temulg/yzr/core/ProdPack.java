/*
 * Copyright (c) 2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.core;

public interface ProdPack {
	public interface Storage {
	}

	public interface Selector {
	}

	public interface Getter {
		<M extends Mark> M get(Storage s);
	}

	Getter select(Selector sel);

	Storage allocate();
}
