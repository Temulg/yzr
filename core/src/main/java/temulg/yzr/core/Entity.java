/*
 * Copyright (c) 2018-2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.core;

import java.util.UUID;

public abstract class Entity {
	protected Entity() {
	}

	@Override
	public boolean equals(Object other) {
		return eid.equals(((Entity)other).eid);
	}

	@Override
	public int hashCode() {
		return eid.hashCode();
	}

	public final UUID getEID() {
		return eid;
	}

	private final UUID eid = UUID.randomUUID();
}
