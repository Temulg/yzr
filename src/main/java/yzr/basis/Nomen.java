/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package yzr.basis;

import java.util.List;
import java.util.Arrays;
import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;

public class Nomen {
	public static Nomen from(List<CharSequence> elements) {
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
			el.codePoints().forEachOrdered(ins::acceptCodePoint);
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

		forEachElement(range -> {
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
		forEachElementEx(range -> {
			ch.write(delim);
			delim.rewind();
			ch.write(range.toByteBuffer());
		});
	}

	public Nomen cat(Nomen other) {
		var n = new Nomen(bitSize() + other.bitSize());
		if (n.isEmpty())
			return n;

		var ins = n.initCatInserter(this);

		other.forEachElement(range -> {
			ins.acceptByteRange(range);
			ins.delimit();
		});

		return n;
	}

	public Nomen cat(Nomen... others) {
		int sz = bitSize();
		for (var other: others)
			sz += other.bitSize();

		var n = new Nomen(sz);
		if (n.isEmpty())
			return n;

		var ins = n.initCatInserter(this);

		for (var other: others) {
			other.forEachElement(range -> {
				ins.acceptByteRange(range);
				ins.delimit();
			});
		}

		return n;
	}
/*
	public Nomen relativize(Nomen other) {
		if (isEmpty())
			return this;
		if (other.isEmpty())
			return other;

		if (value.length > other.value.length)
			return new Nomen(0);

		int wordPos = diffWordPos(this, other);
		if (wordPos < value.length)
			return new Nomen(0);

		
	}
*/
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

		int wordPos = diffWordPos(s, l);
		int bytePos = -1;

		if (wordPos < s.value.length) {
			for (; wordPos >= 0; wordPos--) {
				bytePos = lastCommonSep(
					s.value[wordPos], l.value[wordPos]
				);

				if (bytePos >= 0)
					break;
			}
		} else if (wordPos < l.value.length) {
			if ((s.value[wordPos - 1] & 0x80L) != 0) {
				if ((l.value[wordPos] & 0x01L) != 0)
					return s;
			}

			for (wordPos--; wordPos >= 0; wordPos--) {
				bytePos = lastCommonSep(
					s.value[wordPos], l.value[wordPos]
				);

				if (bytePos >= 0)
					break;
			}
		} else
			return s;

		if (bytePos < 0)
			return EMPTY_NOMEN;

		if (wordPos > 0) {
			return new Nomen(s, wordPos, bytePos);
		} else {
			long w = adjustLastSep(s.value[0], bytePos);
			if (ByteHelper.onesCount((byte)w) > 1)
				return new Nomen(new long[] { w });

			return EMPTY_NOMEN;
		}
	}

	private Inserter initCatInserter(Nomen first) {
		if (first.isEmpty()) {
			value[0] |= 1L;
			return new Inserter(0, 0);
		}

		System.arraycopy(first.value, 0, value, 0, first.value.length);

		var wordPos = first.value.length - 1;
		var bytePos = 7 - ByteHelper.leadingZeros(
			(byte)value[wordPos]
		);

		if (bytePos != 7)
			return new Inserter(wordPos, bytePos << 3);

		value[wordPos] ^= 0x80L;
		wordPos++;
		value[wordPos] |= 1L;
		return new Inserter(wordPos, 0);
	}

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

	private <E extends Exception> void forEachElementEx(
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

		byte w = (byte)(value[wordPos]);
		w &= (byte)~((1 << bytePos) - 1);
		if (w != 0)
			return ByteHelper.trailingZeros(w) - bytePos + 1;

		bytePos = 8 - bytePos;
		wordPos++;
		if (wordPos == value.length)
			return 0;

		while (true) {
			w = (byte)(value[wordPos]);
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
				(byte)value[value.length - 1]
			);

		return sz << 3;
	}

	private static int diffWordPos(Nomen shorter, Nomen longer) {
		for (int pos = 0; pos < shorter.value.length; pos++) {
			if (shorter.value[pos] != longer.value[pos])
				return pos;
		}

		return shorter.value.length;
	}

	private static int lastCommonSep(long w0, long w1) {
		if ((byte)w0 == 0)
			return -1;

		var sep = (byte)(w0 ^ w1);
		if (sep == 0)
			return 7 - ByteHelper.leadingZeros((byte)w0);

		var mask = (byte)((1 << ByteHelper.trailingZeros(sep)) - 1);
		sep = (byte)(w0 & mask);
		if (sep != 0)
			return 7 - ByteHelper.leadingZeros(sep);

		return -2;
	}

	private static long adjustLastSep(long w, int bitPos) {
		var sep = (w & 0xff) & ((1 << (bitPos + 1)) - 1);
		return (w & SYM_MASK) | sep;
	}

	private static void subWordToByteBuffer(
		ByteBuffer dst, long w, int bitShift, int bitLen
	) {
		w >>>= bitShift + 8;
		dst.putLong(w);
		dst.position(dst.position() - ((64 - bitLen) >> 3));
	}

	private static void wordToByteBuffer(ByteBuffer dst, long w) {
		dst.putLong(w >>> 8);
		dst.position(dst.position() - 1);
	}

	private Nomen(int bitCount) {
		value = new long[
			(bitCount / 56) + ((bitCount % 56) > 0 ? 1 : 0)
		];
	}

	private Nomen(Nomen other, int wordPos, int bytePos) {
		value = new long[wordPos + 1];
		System.arraycopy(other.value, 0, value, 0, wordPos);
		value[wordPos] = adjustLastSep(value[wordPos], bytePos);
	}

	private Nomen(long[] value_) {
		value = value_;
	}

	private class Inserter {
		Inserter(int wordPos_, int bitPos_) {
			wordPos = wordPos_;
			bitPos = bitPos_;
		}

		void acceptByteRange(ByteRange range) {
			if (bitPos != 0) {
				var acc = new MisalignedByteRangeInserter(
					range
				);
				range.forEachWord(acc);
			} else {
				var lastPos = wordPos;
				var bitLength = range.bitLength;

				range.forEachWord(w -> {
					value[wordPos] |= w << 8;
					wordPos++;
				});

				bitLength -= (wordPos - lastPos) * 56;
				if (bitLength < 0) {
					wordPos--;
					bitPos = 56 + bitLength;
				}
			}
		}

		void acceptCodePoint(int cp) {
			int blen = Utf8Helper.encodedBitLength(cp);
			long w = Utf8Helper.encodeCodepoint(cp, blen);
			int wrem = 56 - bitPos;

			value[wordPos] |= w << (bitPos + 8);
			bitPos += blen;
			wordPos += bitPos / 56;
			bitPos %= 56;

			if (wrem < blen)
				value[wordPos] = (w >>> wrem) << 8;
		}

		void delimit() {
			if (wordPos < value.length)
				value[wordPos] |=  1L << (bitPos >> 3);
			else
				value[value.length - 1] |= 0x80L;
		}

		private class MisalignedByteRangeInserter
		implements LongConsumer {
			MisalignedByteRangeInserter(ByteRange range) {
				bitLength = range.bitLength;
				mask = (1L << (bitPos + 8)) - 1;
				wrem = 56 - bitPos;
			}

			@Override
			public void accept(long w) {
				value[wordPos] &= mask;
				value[wordPos] |= w << (bitPos + 8);

				if (value.length == (wordPos + 1)) {
					bitPos += bitLength;
					bitLength = 0;
					return;
				}

				wordPos++;
				value[wordPos] = (w >>> wrem) << 8;
				bitLength -= 56;
				if (bitLength < 0) {
					bitPos += bitLength;
					bitLength = 0;
					if (bitPos < 0) {
						wordPos--;
						bitPos = 56 + bitPos;
					}
				}
			}

			long bitLength;
			final long mask;
			final int wrem;
		}

		private int wordPos;
		private int bitPos;
	}

	private class ByteRange {
		ByteRange(int wordPos_, int bytePos, int byteLength) {
			wordPos = wordPos_;
			bitPos = bytePos << 3;
			bitLength = byteLength << 3;
		}

		void forEachCodePoint(IntConsumer cons) {
			while (bitLength > 0)
				cons.accept(nextCodePoint());
		}

		void forEachWord(LongConsumer cons) {
			if (bitPos != 0) {
				var lim = value.length - 1;
				var skip = 56 - bitPos;
				for (; wordPos < lim; wordPos++) {
					var w = value[wordPos + 1] >>> 8;
					w <<= skip;
					w |= value[wordPos] >>> (bitPos + 8);
					cons.accept(w);
					bitLength -= 56;
					if (bitLength <= 0) {
						bitPos += bitLength;
						return;
					}
				}

				if (bitLength > 0) {
					cons.accept(
						value[wordPos] >>> (bitPos + 8)
					);
					bitPos += bitLength;
					bitLength = 0;
				}
			} else {
				for (; bitLength > 0; bitLength -= 56) {
					cons.accept(value[wordPos] >>> 8);
					wordPos++;
				}
				bitLength = 0;
			}
		}

		ByteBuffer toByteBuffer() {
			var cap = bitLength >> 64;
			if ((bitLength & 0x3f) != 0)
				cap++;

			var lim = bitLength >> 3;
			var b = ByteBuffer.allocate(cap << 3).order(
				ByteOrder.LITTLE_ENDIAN
			);

			if (bitPos > 0) {
				var wrem = Math.min(56 - bitPos, bitLength);
				subWordToByteBuffer(
					b, value[wordPos], bitPos, wrem
				);
				bitPos += wrem;
				bitLength -= wrem;
				if (bitPos == 56) {
					bitPos = 0;
					wordPos++;
				}
			}

			for (; bitLength >= 56; bitLength -= 56) {
				wordToByteBuffer(b, value[wordPos]);
				wordPos++;
			}

			if (bitLength > 0) {
				subWordToByteBuffer(
					b, value[wordPos], 0, bitLength
				);
				bitPos += bitLength;
				bitLength = 0;
			}

			b.position(0);
			b.limit(lim);
			return b;
		}

		private int nextCodePoint() {
			long w = value[wordPos] >>> (bitPos + 8);
			int wrem = 56 - bitPos;
			int blen = Utf8Helper.codepointBits((byte)w);

			bitLength -= blen;
			bitPos += blen;
			wordPos += bitPos / 56;
			bitPos %= 56;

			if (blen > wrem) {
				w &= (1L << wrem) - 1L;
				w |= (
					(value[wordPos] >>> 8)
					& ((1L << (blen - wrem)) - 1L)
				) << wrem;
			}

			return Utf8Helper.decodeCodepoint(w);
		}

		private int wordPos;
		private int bitPos;
		private int bitLength;
	}

	private static interface ByteRangeConsumer<E extends Exception> {
		void accept(ByteRange range) throws E;
	}

	public static final Nomen EMPTY_NOMEN = new Nomen(0);
	private static final long SYM_MASK = (~0L) << 8;

	private final long[] value;
	private int hash;
}
