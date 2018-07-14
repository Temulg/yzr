/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package yzr.basis;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.DataProvider;

public class StringGenerator {
	@DataProvider(name = "randomStringList")
	public static Iterator<Object[]> randomStringList() {
		return new Iterator<Object[]>() {
			@Override
			public boolean hasNext() {
				return remaining > 0;
			}

			@Override
			public Object[] next() {
				var r = ThreadLocalRandom.current();

				var ls = r.nextInt(1, 16);
				var out = new ArrayList<String>(ls);
				for (; ls > 0; ls--)
					out.add(RandomStringUtils.random(
						r.nextInt(1, 16)
					));

				remaining--;
				return new Object[] {out};
			}

			private int remaining = 10;
		};
	}
}
