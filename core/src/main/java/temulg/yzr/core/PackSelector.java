/*
 * Copyright (c) 2018-2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.core;

import temulg.yzr.core.MarkPack.Ref;

public interface PackSelector {
	Mark get(MarkPack pack);

	MarkPack.Ref put(MarkPack pack, Mark m);

	public static PackSelector named(String name_) {
		return new PackSelector() {
			@Override
			public Ref put(MarkPack pack, Mark m) {
				return pack.put(name, m);
			}
		
			@Override
			public Mark get(MarkPack pack) {
				return pack.get(name);
			}

			private final String name = name_;
		};
	}

	public static PackSelector positional(int pos_) {
		return new PackSelector() {
			@Override
			public Ref put(MarkPack pack, Mark m) {
				return pack.put(pos, m);
			}
		
			@Override
			public Mark get(MarkPack pack) {
				return pack.get(pos);
			}

			private final int pos = pos_;
		};
	}
}
