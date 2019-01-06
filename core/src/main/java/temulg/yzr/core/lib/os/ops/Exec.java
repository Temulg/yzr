/*
 * Copyright (c) 2018-2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.core.lib.os.ops;

import temulg.yzr.core.Operator;

import java.util.ArrayList;

import temulg.yzr.core.Entity;
import temulg.yzr.core.MarkPack;
import temulg.yzr.core.MarkPackSimple;

public class Exec extends Entity implements Operator {
	@Override
	public MarkPack newRequisites() {
		return preparePack(reqs);
	}

	@Override
	public MarkPack newProducts() {
		return preparePack(prods);
	}

	@Override
	public void apply(
		Operator.Action act, MarkPack requisites, MarkPack products
	) {
		try {
			
		} catch (Exception ex) {
			act.failed(ex);
		}
	}

	private static MarkPack preparePack(ArrayList<Selector> ss) {
		var b = MarkPackSimple.builder().positionalCount(ss.size());

		int pos = 0;
		for (var s: ss) {
			if (s.name != null)
				b.named(s.name, pos);

			pos++;
		}
		return b.build();
	}

	public static Builder builder() {
		return new Builder();
	}

	private static class Selector {
		Selector(int pos, String name_) {
			argvPos = pos;
			name = name_;
		}

		private final int argvPos;
		private final String name;
	}

	public static class Builder {
		public Builder addRequisite() {
			reqs.add(new Selector(argvPos++, null));
			return this;
		}

		public Builder addRequisite(String name) {
			reqs.add(new Selector(argvPos++, name));
			return this;
		}

		public Builder addProduct() {
			prods.add(new Selector(argvPos++, null));
			return this;
		}

		public Builder addProduct(String name) {
			prods.add(new Selector(argvPos++, name));
			return this;
		}

		public Exec build() {
			return new Exec(this);
		}

		private int argvPos;
		private final ArrayList<Selector> reqs = new ArrayList<>();
		private final ArrayList<Selector> prods = new ArrayList<>();
	}

	private Exec(Builder b) {
		reqs = b.reqs;
		prods = b.prods;
	}

	private final ArrayList<Selector> reqs;
	private final ArrayList<Selector> prods;
}
