/*
 * Copyright (c) 2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.core;

public interface ReqPack {
	public interface Storage {
	}

	public interface Selector {
	}

	public interface Setter {
		void set(Storage s, Mark m);
	}

	Setter select(Selector sel);

	Storage allocate();
}
