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
	@Test
	public void constructBasic() throws Exception {
		var n0 = Nomen.from(List.of());
		Assert.assertEquals(n0.toString(), "");
		Assert.assertEquals(n0.size(), 0);

		var n1 = Nomen.from(List.of("abcdefg"));
		Assert.assertEquals(n1.toString(), "/abcdefg");
		Assert.assertEquals(n1.size(), 1);

		var n2 = Nomen.from(List.of("abcdefgh"));
		Assert.assertEquals(n2.toString(), "/abcdefgh");
		Assert.assertEquals(n2.size(), 1);

		var n3 = Nomen.from(List.of("abcdefg", "hijklmn"));
		Assert.assertEquals(n3.toString(), "/abcdefg/hijklmn");
		Assert.assertEquals(n3.size(), 2);

		var n4 = Nomen.from(List.of("abcdefgh", "i", "j"));
		Assert.assertEquals(n4.toString(), "/abcdefgh/i/j");
		Assert.assertEquals(n4.size(), 3);
	}

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
		Assert.assertEquals(n.size(), ss.size());
	}
}
