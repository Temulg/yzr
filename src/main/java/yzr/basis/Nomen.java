/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package yzr.basis;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

public class Nomen {
	public static Nomen from(List<String> elements) {
		int bitCount = 0;
		for (var el: elements)
			bitCount += Utf8Helper.encodedBitLength(
				el, 0, el.length()
			);

		var n = new Nomen(bitCount);
		var ins = n.new Inserter(0, 0);

		for (var el: elements) {
			ins.delimit();
			el.codePoints().forEachOrdered(ins);
		}

		ins.delimit();
		return n;
	}

	public int size() {
		int sz = 0;

		for (long w: value)
			sz += ByteHelper.bitCount((byte)(w >>> 56));

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

		forEachElement((range) -> {
			sb.append(delim);
			range.forEachRemaining(sb::appendCodePoint);
		});

		return sb.toString();
	}

	private void forEachElement(Consumer<ByteRange> cons) {
		int wordPos = 0;
		int bytePos = 0;

		for (
			int len = nextSepOffset(wordPos, bytePos);
			len != 0;
			len = nextSepOffset(wordPos, bytePos)
		) {
			cons.accept(new ByteRange(wordPos, bytePos, len));
			bytePos += len;
			wordPos += bytePos / 7;
			bytePos = bytePos % 7;
		}
	}

	private int nextSepOffset(int wordPos, int bytePos) {
		if (bytePos < 6) {
			bytePos++;
		} else {
			if (wordPos < value.length) {
				bytePos = 0;
				wordPos++;

				if (wordPos == value.length)
					return value[wordPos - 1] < 0 ? 1 : 0;
			} else
				return 0;
		}

		int w = (int)(value[wordPos] >>> 56);
		w &= ~((1 << bytePos) - 1);

		if (w != 0)
			return ByteHelper.trailingZeroes(
				(byte)w
			) - bytePos + 1;

		bytePos = 8 - bytePos;

		for (wordPos++; wordPos < value.length; wordPos++) {
			w = (int)(value[wordPos] >>> 56);

			if (w != 0)
				return bytePos + ByteHelper.trailingZeroes(
					(byte)w
				);

			bytePos += 7;
		}

		return bytePos;
	}

	private Nomen(int bitCount) {
		value = new long[
			(bitCount / 56) + ((bitCount % 56) > 0 ? 1 : 0)
		];
	}

	private class Inserter implements IntConsumer {
		Inserter(int wordPos_, int bitPos_) {
			wordPos = wordPos_;
			bitPos = bitPos_;
		}

		@Override
		public void accept(int cp) {
			int blen = Utf8Helper.encodedBitLength(cp);
			long w = Utf8Helper.encodeCodepoint(cp, blen);
			int wrem = 56 - bitPos;

			value[wordPos] |= (w << bitPos) & CP_MASK;
			bitPos += blen;
			wordPos += bitPos / 56;
			bitPos %= 56;

			if (wrem < blen)
				value[wordPos] = w >>> wrem;
		}

		public void delimit() {
			value[wordPos] |=  1L << (56 + (bitPos >> 3));
		}

		private int wordPos;
		private int bitPos;
	}

	private class ByteRange implements Iterator<Integer> {
		ByteRange(int wordPos_, int bytePos_, int length_) {
			wordPos = wordPos_;
			bitPos = bytePos_ << 3;
			length = length_ << 3;
		}

		@Override
		public boolean hasNext() {
			return length > 0;
		}

		@Override
		public Integer next() {
			long w = value[wordPos] >>> bitPos;
			int wrem = 56 - bitPos;
			int blen = Utf8Helper.codepointBits((byte)w);

			length -= blen;
			bitPos += blen;
			wordPos += bitPos / 56;
			bitPos %= 56;

			if (blen > wrem) {
				w &= (1L << wrem) - 1L;
				w |= (
					value[wordPos]
					& ((1L << (blen - wrem)) - 1L)
				) << wrem;
			}

			return Utf8Helper.decodeCodepoint(w);
		}

		private int wordPos;
		private int bitPos;
		private int length;
	}

	private static final long CP_MASK = (1L << 56) - 1L;

	private final long[] value;
	private int hash;
}
