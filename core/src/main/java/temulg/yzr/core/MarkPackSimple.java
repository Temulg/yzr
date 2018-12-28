/*
 * Copyright (c) 2018-2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MarkPackSimple implements MarkPack {
	public static class Ref implements MarkPack.Ref {
		private Ref(int pos_) {
			pos = pos_;
		}

		private int pos;
	}

	@Override
	public Mark get(String name) {
		var pos = named.get(name);
		if (pos == null)
			throw new IndexOutOfBoundsException();

		return marks[pos];
	}

	@Override
	public Mark get(MarkPack.Ref ref) {
		return marks[((Ref)ref).pos];
	}

	@Override
	public Mark get(int pos) {
		return marks[pos];
	}

	@Override
	public Ref put(String name, Mark m) {
		var pos = named.get(name);
		if (pos == null)
			throw new IndexOutOfBoundsException();

		marks[pos] = m;
		return new Ref(pos);
	}

	@Override
	public Ref put(int pos, Mark m) {
		marks[pos] = m;
		return new Ref(pos);
	}

	@Override
	public int size() {
		return marks.length;
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		public Builder positionalCount(int count) {
			posCount = count;
			return this;
		}

		public Builder named(String name, int pos) {
			if (pos >= posCount)
				throw new IndexOutOfBoundsException();

			if (named == null)
				named = new String[posCount];

			if (named[pos] != null)
				throw new IllegalStateException();

			named[pos] = name;
			return this;
		}

		public MarkPackSimple build() {
			return new MarkPackSimple(this);
		}

		private int posCount;
		private String[] named;
	}

	private MarkPackSimple(Builder b) {
		marks = new Mark[b.posCount];
		named = b.named != null
			? new HashMap<>(b.posCount)
			: Collections.emptyMap();

		if (b.named == null)
			return;

		for (int pos = 0; pos < b.posCount; pos++) {
			if (b.named[pos] != null)
				named.put(b.named[pos], pos);
		}
	}

	private final Mark[] marks;
	private final Map<String, Integer> named;
}