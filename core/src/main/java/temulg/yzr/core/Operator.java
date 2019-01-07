/*
 * Copyright (c) 2018-2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.core;

import java.time.Instant;

/**
 * Operator, given a collection of existing {@link Mark marks} will decide,
 * whether some marks are to be made, remade or discarded. It will add some
 * {@link Action actions} to the Yzr execution graph if any work is to be done.
 */
public interface Operator {
	public interface Action {
		Context context();

		void skipped(boolean res);

		void failed(Throwable t);

		Instant requisitesUpdated();

		void productsUpdated(Instant inst);
	}

	ReqPack newRequisites();

	ProdPack newProducts();

	void apply(
		Action act,
		ReqPack.Storage requisites,
		ProdPack.Storage products
	);

	default Entity entity() {
		return (Entity)this;
	}
}
