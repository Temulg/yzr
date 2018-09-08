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

/*
 * Nomen packs UTF-8 bytes and separator markers into 64 bit words, 7 bytes
 * per word. Each separator marker is implied to precede the corresponding
 * byte. 'z' marker is only allowed in the very last word of Nomen.
 * 
 * Word layout (b - UTF-8 byte, s - separator marker):
 * MSB
 * [ b0 ] [ b1 ] [ b2 ] [ b3 ] [ b4] [ b5 ] [ b6 ] [ s0 s1 s2 s3 s4 s5 s6 z]
 *                                                                       LSB
 */
public class Nomen implements Iterable<Nomen> {
	public static Nomen from(Iterable<CharSequence> elements) {
		long bitCount = 0;
		for (var el: elements)
			bitCount += Utf8Helper.encodedBitLength(
				el, 0, el.length()
			);

		var n = makeFromByteSize(bitCount >>> 3);
		if (n.isEmpty())
			return n;

		var ins = n.new Inserter();

		for (var el: elements) {
			ins.delimit();
			el.codePoints().forEachOrdered(ins::acceptCodePoint);
		}

		ins.delimit();
		return n;
	}

	public static Nomen from(CharSequence... elements) {
		switch (elements.length) {
		case 0:
			return EMPTY;
		case 1: {
			long bitCount = Utf8Helper.encodedBitLength(
				elements[0], 0, elements[0].length()
			);

			var n = makeFromByteSize(bitCount >>> 3);
			if (n.isEmpty())
				return n;

			var ins = n.new Inserter();
			ins.delimit();
			elements[0].codePoints().forEachOrdered(
				ins::acceptCodePoint
			);
			ins.delimit();

			return n;
		}
		default:
			return from(Arrays.asList(elements));
		}
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
		var sb = new StringBuilder();
		long ppos = 0;

		while (true) {
			long pos = nextSepPos(ppos);

			if (pos == ppos)
				break;

			sb.append(delim);

			int wordPos = (int)(ppos >>> 3);
			int bitPos = (int)(ppos & 7) << 3;

			do {
				int wrem = 56 - bitPos;
				int blen = Utf8Helper.codepointBits(
					(byte)(value[wordPos] >>> wrem)
				);

				long w = value[wordPos] << bitPos;
				if (blen >= wrem) {
					w &= BYTE_MASK << bitPos;
					wordPos++;
					bitPos = blen - wrem;
					if (bitPos > 0)
						w |= value[wordPos] >>> wrem;
				} else
					bitPos += blen;

				sb.appendCodePoint(
					Utf8Helper.decodeCodepointBE(w, blen)
				);

				ppos = makePos(wordPos, bitPos >>> 3);
			} while (ppos < pos);
			ppos = pos;
		}

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
		if (isEmpty())
			return;

		var delim = ByteBuffer.wrap(delim_.getBytes(
			StandardCharsets.UTF_8
		));

		long ppos = 0;
		while (true) {
			long pos = nextSepPos(ppos);

			if (pos == ppos)
				break;

			ch.write(delim);
			delim.rewind();
			ch.write(rangeToByteBuffer(ppos, pos));

			ppos = pos;
		}

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
				n.value[wordPos - 1] | 1
			) ^ 1;
			System.arraycopy(
				other.value, 0, n.value, wordPos,
				other.value.length
			);
		}

		if (((bytePos + oz) % 7) == 0)
			n.value[n.value.length - 1] |= 1;

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
					n.value[wordPos - 1] | 1
				) ^ 1;
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
			n.value[wordPos - 1] |= 1;

		return n;
	}

	public boolean isPrefixOf(Nomen other) {
		if (isEmpty() || other.isEmpty())
			return true;

		if (value.length > other.value.length)
			return false;

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
				return true;
			else {
				wordPos--;
				break;
			}
		default:
			return false;
		}

		long w = value[wordPos];
		int bytePos = 7 - ByteHelper.trailingZeros((byte)w);

		if ((bytePos < 7) && (lastCommonSep(
			w, other.value[wordPos]
		) == bytePos))
			return true;
		else if ((other.value.length > value.length) && (
			(other.value[wordPos + 1] & 0x80) != 0
		))
			return true;

		return false;
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
		int bytePos = 7 - ByteHelper.trailingZeros((byte)w);

		if ((bytePos < 7) && (lastCommonSep(
			w, other.value[wordPos]
		) == bytePos)) {
			return new Nomen(copyAlignByteRange(
				other.value, makePos(wordPos, bytePos),
				other.lastSepPos()
			));
		} else if ((other.value.length > value.length) && (
			(other.value[wordPos + 1] & 0x80) != 0
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

		if ((s.value[wordPos] ^ l.value[wordPos]) == 1) {
			if ((
				l.value.length > s.value.length
			) && (
				(l.value[wordPos + 1] & 0x80) != 0
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
				n.value[wordPos - 1] |= 1;
				return n;
			}

			wordPos = goodWordPos;
			bytePos = 7 - ByteHelper.trailingZeros(
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
				n.value[wordPos - 1] |= 1;
				return n;
			}
		}

		return (bytePos > 0)
			? new Nomen(new long[] {
				adjustLastSep(s.value[0], bytePos)
			}) : EMPTY;
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

		int bytePos = ByteHelper.leadingNthOne(
			(byte)value[wordPos], first - elemCount
		);

		long pos = makePos(wordPos, bytePos);
		long last = lastSepPos();

		if (pos >= last)
			throw new IndexOutOfBoundsException(first);

		return new Nomen(copyAlignByteRange(value, pos, last));
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

		int bytePos = ByteHelper.leadingNthOne(
			(byte)value[wordPos], first - elemCount
		);

		long firstPos = makePos(wordPos, bytePos);
		long lastPos = firstPos;

		for (; first < last; first++) {
			long pos = nextSepPos(lastPos);
			if (pos == lastPos)
				throw new IndexOutOfBoundsException(last);

			lastPos = pos;
		}

		return new Nomen(copyAlignByteRange(value, firstPos, lastPos));
	}

	public Nomen get(int pos) {
		return subNomen(pos, pos + 1);
	}

	@Override
	public void forEach(Consumer<? super Nomen> cons) {
		long ppos = 0;

		while (true) {
			long pos = nextSepPos(ppos);

			if (pos == ppos)
				break;

			cons.accept(new Nomen(copyAlignByteRange(
				value, ppos, pos
			)));

			ppos = pos;
		}
	}

	@Override
	public Iterator<Nomen> iterator() {
		return new Iterator<Nomen>() {
			@Override
			public boolean hasNext() {
				return ppos < last;
			}

			@Override
			public Nomen next() {
				long pos = nextSepPos(ppos);
				if (pos == ppos)
					return EMPTY;

				var n = new Nomen(copyAlignByteRange(
					value, ppos, pos
				));

				ppos = pos;

				return n;
			}

			private long ppos = 0;
			private final long last = lastSepPos();
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

	private static long makePos(int wordPos, int bytePos) {
		return (((long)wordPos) << 3) | (bytePos & 7);
	}

	private long nextSepPos(long ppos) {
		int wordPos = (int)(ppos >>> 3);
		int bytePos = (int)(ppos & 7);

		if (wordPos == value.length)
			return ppos;

		bytePos++;

		if (bytePos == 7) {
			if ((wordPos + 1) == value.length)
				return ppos + (value[wordPos] & 1);

			bytePos = 0;
			wordPos++;
		}

		byte sep = (byte)value[wordPos];
		sep &= (byte)(0xff >>> bytePos);
		if (sep != 0)
			return makePos(wordPos, ByteHelper.leadingZeros(sep));

		wordPos++;
		if (wordPos == value.length)
			return ppos;

		while (true) {
			sep = (byte)value[wordPos];
			if (sep != 0)
				return makePos(
					wordPos, ByteHelper.leadingZeros(sep)
				);

			wordPos++;
		}
	}

	private long lastSepPos() {
		if (value.length > 0)
			return makePos(
				value.length - 1,
				7 - ByteHelper.trailingZeros(
					(byte)value[value.length - 1]
				)
			);
		else
			return 0;
	}

	private ByteBuffer rangeToByteBuffer(long begin, long end) {
		int wb = (int)(begin >>> 3);
		int we = (int)(end >>> 3);

		var b = ByteBuffer.allocate((we - wb + 1) << 3).order(
			ByteOrder.BIG_ENDIAN
		);

		if (wb == we) {
			b.putLong(0, value[wb] << ((begin & 7) << 3));
			b.limit((int)((end & 7) - (begin & 7)));
			return b;
		}

		int bytePos = (int)(begin & 7);

		if (bytePos > 0) {
			b.putLong(0, value[wb] << (bytePos << 3));
			b.position(7 - bytePos);
			wb++;
		}

		for (; wb < we; wb++) {
			b.putLong(b.position(), value[wb]);
			b.position(b.position() + 7);
		}

		bytePos = (int)(end & 7);
		if (bytePos > 0) {
			b.putLong(b.position(), value[we]);
			b.position(b.position() + bytePos);
		}

		b.flip();
		return b;
	}

	private int byteSize() {
		if (value.length > 0) {
			return 7 * value.length - ByteHelper.trailingZeros(
				(byte)value[value.length - 1]
			);
		} else {
			return 0;
		}
	}

	private static int lastCommonSep(long w0, long w1) {
		if ((byte)(w0 & w1) == 0)
			return -1;

		var diff = w0 ^ w1;
		byte sep = (byte)(diff & 0xff);
		var byteCount = Long.numberOfLeadingZeros(diff) >>> 3;

		var byteMask = (0xff00 >> (byteCount + 1)) & 0xff;

		if (sep == 0)
			return 7 - ByteHelper.trailingZeros(
				(byte)(w0 & byteMask)
			);

		var sepMask = (
			0xff00 >> (7 - ByteHelper.trailingZeros(sep))
		) & 0xff;

		sep = (byte)(w0 & sepMask & byteMask);
		return 7 - ByteHelper.trailingZeros(sep);
	}

	private static long adjustLastSep(long w, int bytePos) {
		var sep = (w & 0xff) & (0xff00 >> (bytePos + 1));
		var mask = BYTE_MASK << ((7 - bytePos) << 3);
		return (w & mask) | sep;
	}

	private static long trimWord(long w, int bytePos) {
		long sep = w & (0xff00L >>> (bytePos + 1)) & 0xff;
		return (w & (BYTE_MASK << ((7 - bytePos) << 3))) | sep;
	}

	private static long shiftWordRight(long w, int byteShift) {
		long sep = (w >>> byteShift) & (0xff >>> byteShift) | 1;
		return ((w >>> (byteShift << 3)) & BYTE_MASK) | sep ^ 1;
	}

	private static long shiftWordLeft(long w, int byteShift) {
		long sep = (w << byteShift) & 0xff;
		int bitShift = byteShift << 3;
		return ((w << bitShift) & (BYTE_MASK << bitShift)) | sep;
	}

	private static Nomen makeFromByteSize(long byteSize) {
		return new Nomen(new long[(int)(
			byteSize % 7 > 0 ? byteSize / 7 + 1 : byteSize / 7
		)]);
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
		long[] src, long begin, long end
	) {
		int wb = (int)(begin >>> 3);
		int we = (int)(end >>> 3);
		int offb = (int)(begin & 7);
		int offe = (int)(end & 7);

		if (wb == we) {
			long[] dst = new long[] {
				shiftWordLeft(
					trimWord(src[wb], offe), offb
				)
			};
			if ((offe - offb) == 7)
				dst[0] |= 1;

			return dst;
		}

		int tailFlag = offe > offb ? 1 : 0;
		long[] dst = new long[we - wb + tailFlag];

		if (offb > 0) {
			int pos = 0;
			long w = shiftWordLeft(src[wb], offb);

			for (; pos < (dst.length - 1); pos++) {
				wb++;
				dst[pos] = w | shiftWordRight(
					src[wb], 7 - offb
				);
				w = shiftWordLeft(src[wb], offb);
			}

			if (tailFlag == 0)
				dst[pos] = w | shiftWordRight(
					trimWord(src[we], offe), 7 - offb
				);
			else
				dst[pos] = shiftWordLeft(
					trimWord(src[we], offe), offb
				);

			if (offb == offe)
				dst[pos] |= 1;
		} else {
			System.arraycopy(src, wb, dst, 0, dst.length);
			if (offe > 0)
				dst[dst.length - 1] = trimWord(
					dst[dst.length - 1], offe
				);
			else
				dst[dst.length - 1] |= 1;
		}

		return dst;
	}

	private static void copyShiftByteRange(
		long[] dst, int wordPos, int byteShift, long[] src
	) {
		int diff = 7 - byteShift;
		long w = shiftWordRight(src[0], byteShift);
		dst[wordPos++] |= w;
		int srcPos = 0;

		while (true) {
			w = shiftWordLeft(src[srcPos++], diff);
			if (srcPos == src.length) {
				if (wordPos < dst.length)
					dst[wordPos] = w;

				return;
			}

			w |= shiftWordRight(src[srcPos], byteShift);
			dst[wordPos++] = w;
		}
	}

	private Nomen(long[] value_) {
		value = value_;
	}

	private class Inserter {
		void acceptCodePoint(int cp) {
			int blen = Utf8Helper.encodedBitLength(cp);
			long w = Utf8Helper.encodeCodepointBE(cp, blen);

			value[wordPos] |= (w >>> bitPos) & BYTE_MASK;
			bitPos += blen;
			wordPos += bitPos / 56;
			bitPos %= 56;

			if (bitPos > 0)
				value[wordPos] |= w << (blen - bitPos);
		}

		void delimit() {
			if (wordPos < value.length)
				value[wordPos] |= 0x80 >>> (bitPos >> 3);
			else
				value[value.length - 1] |= 1;
		}

		private int wordPos = 0;
		private int bitPos = 0;
	}

	public static final Comparator<
		Nomen
	> LEXICOGRAPHIC_ORDER = new LexicographicComparator();

	private static class LexicographicComparator
	implements Comparator<Nomen> {
		@Override
		public int compare(Nomen l, Nomen r) {
			int flag = (l.isEmpty() ? 0 : 1)
				| (r.isEmpty() ? 0 : 2);

			switch (flag) {
			case 0:
				return 0;
			case 1:
				return 1;
			case 2:
				return -1;
			}

			if (l.value.length <= r.value.length)
				return compareImpl(l, r);
			else
				return -compareImpl(r, l);
		}

		private int compareImpl(Nomen l, Nomen r) {
			int last = l.value.length - 1;

			for (int pos = 0; pos < last; pos++) {
				if (l.value[pos] == r.value[pos])
					continue;

				return Long.signum(reorderWord(
					l.value[pos]
				) - reorderWord(r.value[pos]));
			}

			if (l.value[last] != r.value[last]) {
				long w0 = reorderLastWord(l.value[last]);
				if (l.value.length < r.value.length) {
					long w1 = reorderWord(r.value[last]);
					int res = Long.signum(w0 - w1);
					return res != 0 ? res : -1;
				} else {
					long w1 = reorderLastWord(
						r.value[last]
					);
					return Long.signum(w0 - w1);
				}
			} else
				return 0;
		}

		private static long reorderWordPartial(long w, long sepMask) {
			long bmask = 0xff;
			long rw = (w >>> 8) & bmask;
			bmask <<= 9;
			rw |= (w >>> 7) & bmask;
			bmask <<= 9;
			rw |= (w >>> 6) & bmask;
			bmask <<= 9;
			rw |= (w >>> 5) & bmask;
			bmask <<= 9;
			rw |= (w >>> 4) & bmask;
			bmask <<= 9;
			rw |= (w >>> 3) & bmask;
			bmask <<= 9;
			rw |= (w >>> 2) & bmask;
			rw |= sepMask;

			long sep = w & 0xf7;
			sep |= sep << 8;
			sep |= sep << 16;
			sep |= sep << 32;
			sep >>= 1;
			sep &= sepMask;
			return rw ^ sep;
		}

		private static long reorderWord(long w) {
			return reorderWordPartial(w, EXPANDED_SEP_MASK);
		}

		private static long reorderLastWord(long w) {
			return reorderWordPartial(
				w,
				EXPANDED_SEP_MASK
				<< (ByteHelper.trailingZeros((byte)w) * 9)
			);
		}

		private static final long EXPANDED_SEP_MASK
		= 0x4020100804020100L;
	};

	private static final long BYTE_MASK = ~0xffL;

	public static final Nomen EMPTY = new Nomen(new long[0]);

	private final long[] value;
	private int hash;
}
