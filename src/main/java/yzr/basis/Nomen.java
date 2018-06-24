/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package yzr.basis;

import java.util.Arrays;
import java.util.List;

public class Nomen {
	public static Nomen from(List<String> elements) {
		int charCount = 0;
		for (var el: elements)
			charCount += el.length();

		var n = new Nomen(charCount);
		int charPos = charCount;
		int bitPos = 0;
		int symPos = 0;

		for (var el: elements) {
			el.getChars(0, el.length(), n.value, symPos);
			symPos += el.length();
			n.value[charPos] |= (char)(1 << bitPos);
			bitPos += el.length();
			charPos += bitPos >> 4;
			bitPos &= 0xf;
		}

		if (charPos < n.value.length)
			n.value[charPos] |= (char)(1 << bitPos);

		return n;
	}

	public int size() {
		var tailOff = value.length / 17;
		var tailLen = value.length - tailOff;
		var sz = 0;

		for (; tailOff < tailLen; tailOff++)
			sz += Integer.bitCount(value[tailOff]);

		return sz;
	}

	@Override
	public int hashCode() {
		int h = hash;
		if (h == 0 && value.length > 0)
			hash = h = Arrays.hashCode(value);

		return h;
	}

	@Override
	public String toString() {
		return toString("/");
	}

	public String toString(String delim) {
		StringBuilder sb = new StringBuilder();

		forEachElement((off, len) -> {
			sb.append(delim);
			sb.append(value, off, len);
		});

		return sb.toString();
	}

	private Nomen(int charCount) {
		value = new char[charCount + (charCount >> 4) + (
			(charCount & 0xf) > 0 ? 1 : 0
		)];
	}

	private void forEachElement(LocationConsumer cons) {
		int symLen = (value.length << 4) / 17;
		int charPos = symLen;
		int bitPos = 0;
		int symPos = 0;

		while (true) {
			int len = nextSepOffset(charPos, bitPos);
			cons.accept(symPos, len);
			symPos += len;
			if (symPos >= symLen)
				break;

			len += bitPos;
			charPos += len >> 4;
			bitPos = len & 0xf;
		}
	}

	private int nextSepOffset(int charPos, int bitPos) {
		if (bitPos < 15) {
			bitPos++;
		} else {
			bitPos = 0;
			charPos++;

			if (charPos == value.length)
				return 1;
		}

		int w = value[charPos];
		w &= ~((1 << bitPos) - 1);

		if (w != 0) {	
			return Integer.numberOfTrailingZeros(w) - bitPos + 1;
		}

		bitPos = 16 - bitPos + 1;
		for (charPos++; charPos < value.length; charPos++) {
			w = value[charPos];
			if (w != 0)
				return bitPos + Integer.numberOfTrailingZeros(
					w
				);

			bitPos += 16;
		}

		return bitPos;
	}

	private interface LocationConsumer {
		void accept(int offset, int length);
	}

	private final char[] value;
	private int hash;
}
