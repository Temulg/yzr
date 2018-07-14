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
		int byteCount = 0;
		for (var el: elements)
			byteCount += Utf8Helper.encodedLength(
				el, 0, el.length()
			);

		var n = new Nomen(byteCount);
		int bytePos = byteCount;
		int bitPos = 0;
		int symPos = 0;

		for (var el: elements) {
			int symPosPrev = symPos;
			symPos = Utf8Helper.encode(
				n.value, symPos, el, 0, el.length()
			);
			n.value[bytePos] |= (char)(1 << bitPos);
			bitPos += symPos - symPosPrev;
			bytePos += bitPos >> 3;
			bitPos &= 7;
		}

		if (bytePos < n.value.length)
			n.value[bytePos] |= (char)(1 << bitPos);

		return n;
	}

	public int size() {
		var tailOff = value.length / 9;
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
			Utf8Helper.decode(
				sb::appendCodePoint, value, off, len
			);
		});

		return sb.toString();
	}

	private Nomen(int byteCount) {
		value = new byte[byteCount + (byteCount >> 3) + (
			(byteCount & 7) > 0 ? 1 : 0
		)];
	}

	private void forEachElement(LocationConsumer cons) {
		int symLen = (value.length << 3) / 9;
		int bytePos = symLen;
		int bitPos = 0;
		int symPos = 0;

		while (true) {
			int len = nextSepOffset(bytePos, bitPos);
			cons.accept(symPos, len);
			symPos += len;
			if (symPos >= symLen)
				break;

			len += bitPos;
			bytePos += len >> 3;
			bitPos = len & 7;
		}
	}

	private int nextSepOffset(int bytePos, int bitPos) {
		if (bitPos < 7) {
			bitPos++;
		} else {
			bitPos = 0;
			bytePos++;

			if (bytePos == value.length)
				return 1;
		}

		int w = value[bytePos];
		w &= ~((1 << bitPos) - 1);

		if (w != 0) {	
			return Integer.numberOfTrailingZeros(w) - bitPos + 1;
		}

		bitPos = 8 - bitPos + 1;
		for (bytePos++; bytePos < value.length; bytePos++) {
			w = value[bytePos];
			if (w != 0)
				return bitPos + Integer.numberOfTrailingZeros(
					w
				);

			bitPos += 8;
		}

		return bitPos;
	}

	private interface LocationConsumer {
		void accept(int offset, int length);
	}

	private final byte[] value;
	private int hash;
}
