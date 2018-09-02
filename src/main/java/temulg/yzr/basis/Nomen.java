/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.basis;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.io.IOException;
import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.nio.charset.StandardCharsets;
import java.nio.channels.WritableByteChannel;

public class Nomen implements Iterable<Nomen> {
	public static Nomen from(Iterable<CharSequence> elements) {
		int byteCount = 0;
		for (var el: elements)
			byteCount += Utf8Helper.encodedByteLength(
				el, 0, el.length()
			);

		var n = makeFromByteSize(byteCount);
		if (n.isEmpty())
			return n;

		var ins = n.new Inserter(0, 0);

		for (var el: elements) {
			ins.delimit();
			el.codePoints().forEachOrdered(ins::acceptCodePoint);
		}

		ins.delimit();
		return n;
	}

	public static Nomen from(CharSequence... elements) {
		return from(Arrays.asList(elements));
	}

	public static Nomen from(CharSequence element) {
		var byteCount = Utf8Helper.encodedByteLength(
			element, 0, element.length()
		);

		var n = makeFromByteSize(byteCount);
		if (n.isEmpty())
			return n;

		var ins = n.new Inserter(0, 0);
		ins.delimit();
		element.codePoints().forEachOrdered(ins::acceptCodePoint);
		ins.delimit();

		return n;
	}

	public static Comparator<Nomen> lexicographicOrder() {
		return LEXICOGRAPHIC_ORDER;
	}

	public boolean isEmpty() {
		return value.length == 0;
	}

	public int size() {
		if (isEmpty())
			return 0;

		int sz = 0;

		for (long w: value)
			sz += ByteHelper.onesCount((byte)w);

		return sz - 1;
	}

	@Override
	public int hashCode() {
		int h = hash;
		if (h == 0 && value.length > 0)
			hash = h = HashCode32.of(value);

		return h;
	}

	@Override
	public String toString() {
		return toString("/");
	}

	public String toString(String delim) {
		StringBuilder sb = new StringBuilder();

		forEachElementRange(range -> {
			sb.append(delim);
			range.forEachCodePoint(sb::appendCodePoint);
		});

		return sb.toString();
	}

	@Override
	public boolean equals(Object other) {
		if (other instanceof Nomen) {
			return (other == this) || Arrays.equals(
				value, ((Nomen)other).value
			);
		} else
			return false;
	}

	public void toUtf8Channel(
		WritableByteChannel ch, String delim_
	) throws IOException {
		var delim = ByteBuffer.wrap(delim_.getBytes(
			StandardCharsets.UTF_8
		));
		forEachElementRangeEx(range -> {
			ch.write(delim);
			delim.rewind();
			ch.write(range.toByteBuffer());
		});
	}

	public Nomen cat(Nomen other) {
		var oz = other.byteSize();
		if (oz == 0)
			return this;

		int bytePos = byteSize();
		if (bytePos == 0)
			return other;

		var n = makeFromByteSize(bytePos + oz);

		System.arraycopy(value, 0, n.value, 0, value.length);

		int wordPos = bytePos / 7;
		bytePos %= 7;

		if (bytePos != 0)
			copyShiftByteRange(
				n.value, wordPos, bytePos, other.value
			);
		else {
			n.value[wordPos - 1] = (
				n.value[wordPos - 1] | 0x80
			) ^ 0x80;
			System.arraycopy(
				other.value, 0, n.value, wordPos,
				other.value.length
			);
		}

		if (((bytePos + oz) % 7) == 0)
			n.value[n.value.length - 1] |= 0x80;

		return n;
	}

	public Nomen cat(Nomen... others) {
		if (isEmpty()) {
			int pos = 1;
			for (var other: others) {
				if (other.isEmpty())
					pos++;
				else
					return other.cat(pos, others);
			}
			return EMPTY;
		} else
			return cat(0, others);
	}

	private Nomen cat(int pos_, Nomen... others) {
		int bytePos = byteSize();
		int sz = bytePos;

		for (int pos = pos_; pos < others.length; pos++)
			sz += others[pos].byteSize();

		var n = makeFromByteSize(sz);

		System.arraycopy(value, 0, n.value, 0, value.length);

		sz = bytePos;
		int wordPos = bytePos / 7;
		bytePos %= 7;

		for (int pos = pos_; pos < others.length; pos++) {
			var other = others[pos];
			var oz = other.byteSize();

			if (oz == 0)
				continue;

			if (bytePos != 0) {
				copyShiftByteRange(
					n.value, wordPos, bytePos,
					other.value
				);
			} else {
				n.value[wordPos - 1] = (
					n.value[wordPos - 1] | 0x80
				) ^ 0x80;
				System.arraycopy(
					other.value, 0, n.value, wordPos,
					other.value.length
				);
			}
			sz += oz;
			wordPos = sz / 7;
			bytePos = sz % 7;
		}

		if (bytePos == 0)
			n.value[wordPos - 1] |= 0x80;

		return n;
	}

	public Nomen relativize(Nomen other) {
		if (isEmpty() || other.isEmpty())
			return other;

		if (value.length > other.value.length)
			throw new NomenException.NotASubNomen(this, other);

		int wordPos = 0;
		for (; wordPos < value.length; wordPos++) {
			if (value[wordPos] != other.value[wordPos])
				break;
		}

		switch (value.length - wordPos) {
		case 1:
			break;
		case 0:
			if (other.value.length == value.length)
				return EMPTY;
			else {
				wordPos--;
				break;
			}
		default:
			throw new NomenException.NotASubNomen(this, other);
		}

		long w = value[wordPos];
		int bytePos = 7 - ByteHelper.leadingZeros((byte)w);

		if ((bytePos < 7) && (lastCommonSep(
			w, other.value[wordPos]
		) == bytePos)) {
			return new Nomen(copyAlignByteRange(
				other.value, wordPos, bytePos,
				other.remainingByteSize(wordPos, bytePos)
			));
		} else if ((other.value.length > value.length) && ((
			other.value[wordPos + 1] & 0x1) != 0
		)) {
			return makeFromOtherAligned(
				other, wordPos + 1,
				other.value.length - wordPos - 1
			);
		}

		throw new NomenException.NotASubNomen(this, other);
	}

	public Nomen commonPrefix(Nomen other) {
		if (isEmpty())
			return this;
		if (other.isEmpty())
			return other;

		var s = this;
		var l = other;

		if (value.length > other.value.length) {
			s = other;
			l = this;
		}

		int wordPos = 0;
		int goodWordPos = -1;
		int bytePos = -1;

		for (; wordPos < s.value.length; wordPos++) {
			if (s.value[wordPos] == l.value[wordPos]) {
				if ((byte)s.value[wordPos] != 0)
					goodWordPos = wordPos;
			} else
				break;
		}

		if (wordPos == s.value.length)
			return s;

		if ((s.value[wordPos] ^ l.value[wordPos]) == 0x80) {
			if ((
				l.value.length > s.value.length
			) && (
				(l.value[wordPos + 1] & 0x1) != 0
			))
				return s;
		}

		bytePos = lastCommonSep(s.value[wordPos], l.value[wordPos]);

		if (wordPos > 0) {
			if (bytePos > 0)
				return makeFromOtherAdjustLastSep(
					s, wordPos, bytePos
				);
			else if (bytePos == 0) {
				var n = makeFromOtherAligned(s, 0, wordPos);
				n.value[wordPos - 1] |= 0x80;
				return n;
			}

			wordPos = goodWordPos;
			bytePos = 7 - ByteHelper.leadingZeros(
				(byte)s.value[wordPos]
			);
		}

		if (wordPos > 0) {
			if (bytePos > 0)
				return makeFromOtherAdjustLastSep(
					s, wordPos, bytePos
				);
			else {
				var n = makeFromOtherAligned(s, 0, wordPos);
				n.value[wordPos - 1] |= 0x80;
				return n;
			}
		}

		if (bytePos > 0)
			return new Nomen(new long[] {
				adjustLastSep(s.value[0], bytePos)
			});
		else
			return EMPTY;
	}

	public Nomen subNomen(int first) {
		if (first == 0)
			return this;

		int wordPos = -1;
		int elemCount = 0;
		for (int pos = 0; pos < value.length; pos++) {
			int nextCount = elemCount + ByteHelper.onesCount(
				(byte)value[pos]
			);
			if (nextCount > first) {
				wordPos = pos;
				break;
			}
			elemCount = nextCount;
		}

		if (wordPos < 0)
			throw new IndexOutOfBoundsException(first);

		int bytePos = ByteHelper.trailingNthOne(
			(byte)value[wordPos], first - elemCount
		);

		int len = remainingByteSize(wordPos, bytePos);
		if (len == 0)
			throw new IndexOutOfBoundsException(first);

		return new Nomen(copyAlignByteRange(
			value, wordPos, bytePos, len
		));
	}

	public Nomen subNomen(int first, int last) {
		int wordPos = -1;
		int elemCount = 0;

		for (int pos = 0; pos < value.length; pos++) {
			int nextCount = elemCount + ByteHelper.onesCount(
				(byte)value[pos]
			);
			if (nextCount > first) {
				wordPos = pos;
				break;
			}
			elemCount = nextCount;
		}

		if (wordPos < 0)
			throw new IndexOutOfBoundsException(first);

		int bytePos = ByteHelper.trailingNthOne(
			(byte)value[wordPos], first - elemCount
		);

		int len = 0;
		int wp = wordPos;
		int bp = bytePos;
		for (; first < last; first++) {
			int nl = nextSepOffset(wp, bp);
			if (nl == 0)
				throw new IndexOutOfBoundsException(first);

			len += nl;
			bp += nl;
			wp += bp / 7;
			bp %= 7;
		}

		return new Nomen(copyAlignByteRange(
			value, wordPos, bytePos, len
		));
	}

	public Nomen get(int pos) {
		return subNomen(pos, pos + 1);
	}

	@Override
	public void forEach(Consumer<? super Nomen> cons) {
		int bytePos = 0;
		int last = byteSize();

		while (bytePos < last) {
			int wp = bytePos / 7;
			int bp = bytePos % 7;
			int len = nextSepOffset(wp, bp);

			if (len == 0)
				break;

			cons.accept(new Nomen(copyAlignByteRange(
				value, wp, bp, len
			)));

			bytePos += len;
		}
	}

	@Override
	public Iterator<Nomen> iterator() {
		return new Iterator<Nomen>() {
			@Override
			public boolean hasNext() {
				return bytePos < last;
			}

			@Override
			public Nomen next() {
				int wp = bytePos / 7;
				int bp = bytePos % 7;
				int len = nextSepOffset(wp, bp);
				if (len == 0)
					return EMPTY;

				var n = new Nomen(copyAlignByteRange(
					value, wp, bp, len
				));

				bytePos += len;

				return n;
			}

			private int bytePos = 0;
			private final int last = byteSize();
		};
	}

	public void dump(PrintStream s) {
		s.append('[');
		int pos = 0;
		if (pos < value.length)
			s.format("%016x", value[pos++]);

		for (; pos < value.length; pos++)
			s.format(", %016x", value[pos]);

		s.append(']');
	}

	private void forEachElementRange(Consumer<ByteRange> cons) {
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

	private <E extends Exception> void forEachElementRangeEx(
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
			if (wordPos == value.length) {
				if ((value[wordPos - 1] & 0x80) !=0)
					return 1;
				else
					return 0;
			}

			bytePos = 0;
		}

		byte w = (byte)value[wordPos];
		w &= (byte)~((1 << bytePos) - 1);
		if (w != 0)
			return ByteHelper.trailingZeros(w) - bytePos + 1;

		bytePos = 8 - bytePos;
		wordPos++;
		if (wordPos == value.length)
			return 0;

		while (true) {
			w = (byte)value[wordPos];
			if (w != 0)
				return bytePos + ByteHelper.trailingZeros(w);

			bytePos += 7;
			wordPos++;
		}
	}

	private int byteSize() {
		return value.length > 0
			? value.length * 7 - ByteHelper.leadingZeros(
				(byte)value[value.length - 1]
			) : 0;
	}

	private int remainingByteSize(
		int wordPos, int bytePos
	) {
		return (value.length - wordPos) * 7 - bytePos
			- ByteHelper.leadingZeros(
				(byte)value[value.length - 1]
			);
	}

	private static int lastCommonSep(long w0, long w1) {
		if ((byte)(w0 & w1)== 0)
			return -1;

		var diff = w0 ^ w1;
		var sep = diff & 0xff;
		var sym = (Long.numberOfTrailingZeros(diff ^ sep) >>> 3) - 1;

		var symMask = (1 << (sym + 1)) - 1;

		if (sep == 0)
			return 7 - ByteHelper.leadingZeros(
				(byte)(w0 & symMask)
			);

		var sepMask = (1 << ByteHelper.trailingZeros((byte)sep)) - 1;
		sep = w0 & sepMask & symMask;
		return 7 - ByteHelper.leadingZeros((byte)sep);
	}

	private static long adjustLastSep(long w, int bytePos) {
		var sep = (w & 0xff) & ((1 << (bytePos + 1)) - 1);
		var mask = ((1L << (bytePos << 3)) - 1) << 8;
		return w & mask | sep;
	}

	private static void subWordToByteBuffer(
		ByteBuffer dst, long w, int bytePos, int length
	) {
		w >>>= (bytePos + 1) << 3;
		dst.putLong(w);
		dst.position(dst.position() - 8 + length);
	}

	private static void wordToByteBuffer(ByteBuffer dst, long w) {
		dst.putLong(w >>> 8);
		dst.position(dst.position() - 1);
	}

	private static long shiftWordRight(long w, int bytePos) {
		var sep = (w & 0xff) >>> bytePos;
		w >>>= (bytePos + 1) << 3;
		w <<= 8;
		return w | sep;
	}

	private static long shiftWordLeft(long w, int bytePos) {
		var sep = w << bytePos;
		w >>>= 8;
		w <<= (bytePos + 1) << 3;
		return w | (sep & 0xff);
	}

	private static Nomen makeFromByteSize(int byteSize) {
		return new Nomen(
			byteSize % 7 > 0 ? byteSize / 7 + 1 : byteSize / 7
		);
	}

	private static Nomen makeFromOtherAligned(
		Nomen other, int wordOff, int wordLen
	) {
		var n = new Nomen(new long[wordLen]);
		System.arraycopy(other.value, wordOff, n.value, 0, wordLen);
		return n;
	}

	private static Nomen makeFromOtherAdjustLastSep(
		Nomen other, int wordPos, int bytePos
	) {
		var n = new Nomen(new long[wordPos + 1]);
		System.arraycopy(other.value, 0, n.value, 0, wordPos + 1);
		n.value[wordPos] = adjustLastSep(n.value[wordPos], bytePos);
		return n;
	}

	private static long[] copyAlignByteRange(
		long[] src, int wordPos, int bytePos, int byteLen
	) {
		int dstLen = byteLen / 7;
		int dstRem = byteLen % 7;

		if (dstRem > 0)
			dstLen++;

		long[] dst = new long[dstLen];

		if (bytePos != 0) {
			long w = shiftWordRight(src[wordPos++], bytePos);
			byteLen -= 7 - bytePos;
			int dstPos = 0;

			for (; byteLen > 0; byteLen -= 7) {
				w |= shiftWordLeft(src[wordPos], 7 - bytePos);
				w = (w | 0x80) ^ 0x80;
				dst[dstPos++] = w;
				w = shiftWordRight(src[wordPos++], bytePos);
			}

			if (dstPos < dst.length)
				dst[dstPos] = w;
		} else
			System.arraycopy(src, wordPos, dst, 0, dstLen);

		if (dstRem > 0) {
			long mask = ((1L << (dstRem << 3)) - 1) << 8;
			mask |= dst[dstLen - 1] & ((1L << dstRem) - 1);
			dst[dstLen - 1] &= mask;
			dst[dstLen - 1] |= 1L << dstRem;
		} else
			dst[dstLen - 1] |= 0x80;

		return dst;
	}

	private static void copyShiftByteRange(
		long[] dst, int wordPos, int bytePos, long[] src
	) {
		int diff = 7 - bytePos;
		long w = shiftWordLeft(src[0], bytePos);
		dst[wordPos++] |= (w | 0x80) ^ 0x80;
		int srcPos = 0;

		while (true) {
			w = shiftWordRight(src[srcPos++], diff);
			if (srcPos == src.length) {
				if (wordPos < dst.length)
					dst[wordPos] = w;

				return;
			}

			w |= shiftWordLeft(src[srcPos], bytePos);
			w = (w | 0x80) ^ 0x80;
			dst[wordPos++] = w;
		}
	}

	private Nomen(int wordLen) {
		value = new long[wordLen];
	}

	private Nomen(long[] value_) {
		value = value_;
	}

	private class Inserter {
		Inserter(int wordPos_, int bytePos_) {
			wordPos = wordPos_;
			bytePos = bytePos_;
		}

		void acceptCodePoint(int cp) {
			int blen = Utf8Helper.encodedByteLength(cp);
			long w = Utf8Helper.encodeCodepoint(cp, blen);
			int wrem = 7 - bytePos;

			value[wordPos] |= w << ((bytePos + 1) << 3);
			bytePos += blen;
			wordPos += bytePos / 7;
			bytePos %= 7;

			if (wrem < blen)
				value[wordPos] = (w >>> (wrem << 3)) << 8;
		}

		void delimit() {
			if (wordPos < value.length)
				value[wordPos] |=  1L << bytePos;
			else
				value[value.length - 1] |= 0x80L;
		}

		private int wordPos;
		private int bytePos;
	}

	private class ByteRange {
		ByteRange(int wordPos_, int bytePos_, int length_) {
			wordPos = wordPos_;
			bytePos = bytePos_;
			length = length_;
		}

		void forEachCodePoint(IntConsumer cons) {
			while (length > 0)
				cons.accept(nextCodePoint());
		}

		ByteBuffer toByteBuffer() {;
			var b = ByteBuffer.allocate(
				length + 8
			).order(
				ByteOrder.LITTLE_ENDIAN
			);
			var length_ = length;

			if (bytePos > 0) {
				var wrem = Math.min(7 - bytePos, length);
				subWordToByteBuffer(
					b, value[wordPos], bytePos, wrem
				);
				bytePos += wrem;
				length -= wrem;
				if (bytePos == 7) {
					bytePos = 0;
					wordPos++;
				}
			}

			for (; length >= 7; length -= 7) {
				wordToByteBuffer(b, value[wordPos]);
				wordPos++;
			}

			if (length > 0) {
				subWordToByteBuffer(
					b, value[wordPos], 0, length
				);
				bytePos += length;
				length = 0;
			}

			b.position(0);
			b.limit(length_);
			return b;
		}

		private int nextCodePoint() {
			long w = value[wordPos] >>> ((bytePos + 1) << 3);
			int wrem = 7 - bytePos;
			int blen = Utf8Helper.codepointBytes((byte)w);

			length -= blen;
			bytePos += blen;
			wordPos += bytePos / 7;
			bytePos %= 7;

			if (blen > wrem)
				w |= (
					(value[wordPos] >>> 8)
					& ((1L << ((blen - wrem) << 3)) - 1L)
				) << (wrem << 3);

			return Utf8Helper.decodeCodepoint(w);
		}

		private int wordPos;
		private int bytePos;
		private int length;
	}

	public static final Comparator<
		Nomen
	> LEXICOGRAPHIC_ORDER = new Comparator<>() {
		@Override
		public int compare(Nomen l, Nomen r) {
			int clen = Math.min(l.value.length, r.value.length);

			for (int pos = 0; pos < clen; pos++) {
				if (l.value[pos] == r.value[pos])
					continue;

				int res = Long.signum(reorderWord(
					l.value[pos]
				) - reorderWord(r.value[pos]));
				if (res != 0)
					return res;
			}

			return Integer.signum(l.value.length - r.value.length);
		}

		private long reorderWord(long w) {
			long rw = ((w >> 8) & 0xff) << 54;
			rw |= ((w >>> 16) & 0xff) << 45;
			rw |= ((w >> 24) & 0xff) << 36;
			rw |= ((w >> 32) & 0xff) << 27;
			rw |= ((w >> 40) & 0xff) << 18;
			rw |= ((w >> 48) & 0xff) << 9;
			rw |= w >>> 56;
			rw |= EXPANDED_SEP_MASK;

			long sep = (long)(ByteHelper.reverse(
				(byte)w
			)) >>> 1;
			sep |= sep << 8;
			sep |= sep << 16;
			sep |= sep << 24;
			sep |= sep << 32;
			sep |= sep << 40;
			sep |= sep << 48;
			sep |= sep << 56;
			sep &= EXPANDED_SEP_MASK;
			return rw ^ sep;
		}

		private static final long EXPANDED_SEP_MASK
		= 0x4020100804020100L;
	};

	private static interface ByteRangeConsumer<E extends Exception> {
		void accept(ByteRange range) throws E;
	}

	public static final Nomen EMPTY = new Nomen(0);

	private final long[] value;
	private int hash;
}
