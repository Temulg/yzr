/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package yzr.basis;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

public class NomenTest {
	@Test(
		dataProvider = "randomStringList",
		dataProviderClass = StringGenerator.class
	)
	public void construct(List<String> ss) throws Exception {
		var n = Nomen.from(ss);
		var sb = new StringBuilder();
		ss.forEach(s -> {
			sb.append('/').append(s);
		});

		Assert.assertEquals(n.toString(), sb.toString());
	}
}
