/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.basis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;

import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.DataProvider;

public class StringGenerator {
	public static Iterator<Object[]> randomStringList(
		int listArity, int posArity
	) {
		return new Iterator<Object[]>() {
			@Override
			public boolean hasNext() {
				return remaining > 0;
			}

			@Override
			public Object[] next() {
				var r = ThreadLocalRandom.current();

				var out = new Object[listArity + posArity];
				int pos = 0;
				for (int a = 0; a < listArity; a++) {
					var l = genString(r);
					out[pos++] = l;
					var pPos = pos;
					for (int p = 0; p < posArity; p++) {
						out[pos++] = l.size() > 0
							? r.nextInt(
								0, l.size()
							) : 0;
					}

					if (posArity > 1)
						Arrays.sort(
							out, pPos,
							pPos + posArity
						);
				}

				remaining--;
				return out;
			}

			private ArrayList<String> genString(
				ThreadLocalRandom r
			) {
				var ls = r.nextInt(0, 16);
				var out = new ArrayList<String>(ls);
				for (; ls > 0; ls--)
					out.add(RandomStringUtils.random(// Alphanumeric(
						r.nextInt(1, 32)
					));

				return out;
			}

			private int remaining = 10;
		};
	}

	@DataProvider(name = "randomStringList_1")
	public static Iterator<Object[]> randomStringList_1() {
		return randomStringList(1, 0);
	}

	@DataProvider(name = "randomStringList_2")
	public static Iterator<Object[]> randomStringList_2() {
		return randomStringList(2, 0);
	}

	@DataProvider(name = "randomStringList_3")
	public static Iterator<Object[]> randomStringList_3() {
		return randomStringList(3, 0);
	}

	@DataProvider(name = "randomStringList_1_pos_1")
	public static Iterator<Object[]> randomStringList_1_pos_1() {
		return randomStringList(1, 1);
	}

	@DataProvider(name = "randomStringList_1_pos_2")
	public static Iterator<Object[]> randomStringList_1_pos_2() {
		return randomStringList(1, 2);
	}
}
