/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package yzr.basis;

import java.util.List;
import java.util.Arrays;
import java.io.IOException;
import java.io.OutputStream;
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
		if (n.isEmpty())
			return n;

		var ins = n.new Inserter(0, 0);

		for (var el: elements) {
			ins.delimit();
			el.codePoints().forEachOrdered(ins);
		}

		ins.delimit();
		return n;
	}

	public boolean isEmpty() {
		return value.length == 0;
	}

	public int size() {
		if (isEmpty())
			return 0;

		int sz = 0;

		for (long w: value)
			sz += ByteHelper.onesCount((byte)(w >>> 56));

		return sz - 1;
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

		forEachElement((Consumer<ByteRange>)range -> {
			sb.append(delim);
			range.forEachCodePoint(sb::appendCodePoint);
		});

		return sb.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Nomen)
			return (other == this) || Arrays.equals(
				value, ((Nomen)other).value
			);
		else
			return false;
	}

	public void toUtf8OutputStream(
		OutputStream os, String delim_
	) throws IOException {
		forEachElement((ByteRangeConsumer<IOException>)range -> {
			//sb.append(delim);
			os.write(range.toByteArray());
		});
	}

	/*
	public Nomen add(Nomen other) {
		var n = new Nomen(bitSize() + other.bitSize());
		if (n.isEmpty())
			return n;

		var ins = n.new Inserter(0, 0);

		return n;
	}

	public Nomen add(Nomen... others);

	public Nomen prefixOf(Nomen other);

	public Nomen suffixOf(Nomen other);

	public Nomen commonPrefix(Nomen other);
	*/

	private void forEachElement(Consumer<ByteRange> cons) {
		if (isEmpty())
			return;

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

	private <E extends Exception> void forEachElement(
		ByteRangeConsumer<E> cons
	) throws E {
		if (isEmpty())
			return;

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
		if (wordPos == value.length)
			return 0;

		bytePos++;

		if (bytePos == 7) {
			wordPos++;
			if (wordPos == value.length)
				return 0;

			bytePos = 0;
		}

		byte w = (byte)(value[wordPos] >>> 56);
		w &= (byte)~((1 << bytePos) - 1);
		if (w != 0)
			return ByteHelper.trailingZeros(w) - bytePos + 1;

		bytePos = 8 - bytePos;
		wordPos++;
		if (wordPos == value.length)
			return 0;

		while (true) {
			w = (byte)(value[wordPos] >>> 56);
			if (w != 0)
				return bytePos + ByteHelper.trailingZeros(w);

			bytePos += 7;
			wordPos++;
		}
	}

	private int bitSize() {
		int sz = value.length * 7;
		if (sz > 0)
			sz -= ByteHelper.leadingZeros(
				(byte)(value[value.length - 1] >>> 56)
			);

		return sz << 3;
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
			if (wordPos < value.length)
				value[wordPos] |=  1L << (56 + (bitPos >> 3));
			else
				value[value.length - 1] |= 1L << 63;
		}

		private int wordPos;
		private int bitPos;
	}

	private class ByteRange {
		ByteRange(int wordPos_, int bytePos_, int length_) {
			wordPos = wordPos_;
			bitPos = bytePos_ << 3;
			length = length_ << 3;
		}

		void forEachCodePoint(IntConsumer cons) {
			while (length > 0)
				cons.accept(nextCodePoint());
		}

		byte[] toByteArray() {
			var b = new byte[length];

			while (length > 0) {
				
			}

			return b;
		}

		private int nextCodePoint() {
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

	private static interface ByteRangeConsumer<E extends Exception> {
		void accept(ByteRange range) throws E;
	}

	private static final long CP_MASK = (1L << 56) - 1L;

	private final long[] value;
	private int hash;
}
