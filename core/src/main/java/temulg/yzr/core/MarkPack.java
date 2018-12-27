/*
 * Copyright (c) 2018-2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.core;

public interface MarkPack {
	public interface Ref {}

	Mark get(String name);
	Mark get(Ref ref);
	Mark get(int pos);

	Ref put(String name, Mark m);
	Ref put(int pos, Mark m);

	int size();

	public static MarkPack EMPTY = new MarkPack() {
		@Override
		public Mark get(String name) {
			throw new IndexOutOfBoundsException();
		}

		@Override
		public Mark get(Ref ref) {
			throw new IndexOutOfBoundsException();
		}

		@Override
		public Mark get(int pos) {
			throw new IndexOutOfBoundsException();
		}

		@Override
		public Ref put(String name, Mark m) {
			throw new IndexOutOfBoundsException();
		}

		@Override
		public Ref put(int pos, Mark m) {
			throw new IndexOutOfBoundsException();
		}

		@Override
		public int size() {
			return 0;
		}
	};
}
