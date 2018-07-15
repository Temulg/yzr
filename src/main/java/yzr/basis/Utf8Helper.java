/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package yzr.basis;

public class Utf8Helper {
	private Utf8Helper() {
	}

	public static int encodedBitLength(int cp) {
		final int lz = Integer.numberOfLeadingZeros(cp) - 11;
		if (lz < 0)
			throw new IllegalArgumentException(String.format(
				"code point %0x is outside of supported range",
				cp
			));

		return CODEPOINT_BITS_TO_LENGTH[lz];
	}

	public static int encodedBitLength(
		CharSequence src, int srcOffset, int srcLength
	) {
		int seqLength = 0;
		char hs = 0;
		boolean isSimple = true;
		int last = srcOffset + srcLength;

		for (int pos = srcOffset; pos < last; pos++) {
			char ch = src.charAt(pos);

			if (isSimple) {
				if (Character.isHighSurrogate(ch)) {
					hs = ch;
					isSimple = false;
				} else {
					seqLength += encodedBitLength(ch);
				}
			} else {
				if (Character.isLowSurrogate(ch)) {
					seqLength += encodedBitLength(
						Character.toCodePoint(hs, ch)
					);
					isSimple = true;
				} else {
					seqLength += encodedBitLength(hs);
					if (Character.isHighSurrogate(ch)) {
						hs = ch;
					} else {
						seqLength += encodedBitLength(
							ch
						);
						isSimple = true;
					}
				}
			}
		}

		if (!isSimple)
			seqLength += encodedBitLength(hs);

		return seqLength;
	}

	public static long encodeCodepoint(int cp, int encLen) {
		switch (encLen) {
		case 8:
			return cp;
		case 16:
			return (long)((cp >>> 6) | 0xc0)
				| ((long)((cp & 0x3f) | 0x80) << 8);
		case 24:
			return (long)((cp >>> 12) | 0xe0)
				| ((long)(((cp >>> 6) & 0x3f) | 0x80) << 8)
				| ((long)((cp & 0x3f) | 0x80) << 16);
		case 32:
			return (long)((cp >>> 18) | 0xf0)
				| ((long)(((cp >>> 12) & 0x3f) | 0x80) << 8)
				| ((long)(((cp >>> 6) & 0x3f) | 0x80) << 16)
				| ((long)((cp & 0x3f) | 0x80) << 24);
		default:
			throw new IllegalStateException(
				"invalid UTF-8 sequence"
			);
		}
	}

	public static int codepointBits(byte b) {
		int rv = UTF8_LEAD_TO_BITS[b];
		if (rv < 0)
			throw new IllegalStateException(
				"invalid UTF-8 sequence"
			);
		return rv;
	}

	public static int decodeCodepoint(long w) {
		switch (UTF8_LEAD_TO_BITS[(byte)w]) {
		case 8:
			return (int)(w & 0x7f);
		case 16:
			return (int)(((w & 0x1f) << 6) | ((w >> 8) & 0x3f));
		case 24:
			return (int)(
				((w & 0xf) << 12)
				| (((w >> 8) & 0x3f) << 6)
				| ((w >> 16) & 0x3f)
			);
		case 32:
			return (int)(
				((w & 7) << 18)
				| (((w >> 8) & 0x3f) << 12)
				| (((w >> 16) & 0x3f) << 6)
				| ((w >> 24) & 0x3f)
			);
		default:
			throw new IllegalStateException(
				"invalid UTF-8 sequence"
			);
		}
	}

	private static final byte[] CODEPOINT_BITS_TO_LENGTH = new byte[] {
		32, 32, 32, 32, 32,
		24, 24, 24, 24, 24,
		16, 16, 16, 16,
		8, 8, 8, 8, 8, 8, 8
	};

	private static final byte[] UTF8_LEAD_TO_BITS = new byte[] {
		8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
		8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
		8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
		8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
		8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
		8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
		8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
		8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8, 8,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16,
		16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16, 16,
		24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24, 24,
		32, 32, 32, 32, 32, 32, 32, 32, -1, -1, -1, -1, -1, -1, -1, -1
	};
}
