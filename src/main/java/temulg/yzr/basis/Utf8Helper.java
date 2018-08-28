/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.basis;

public class Utf8Helper {
	private Utf8Helper() {
	}

	public static int encodedByteLength(int cp) {
		final int lz = Integer.numberOfLeadingZeros(cp) - 11;
		if (lz < 0)
			throw new IllegalArgumentException(String.format(
				"code point %0x is outside of supported range",
				cp
			));

		return CODEPOINT_BITS_TO_LENGTH[lz];
	}

	public static int encodedByteLength(
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
					seqLength += encodedByteLength(ch);
				}
			} else {
				if (Character.isLowSurrogate(ch)) {
					seqLength += encodedByteLength(
						Character.toCodePoint(hs, ch)
					);
					isSimple = true;
				} else {
					seqLength += encodedByteLength(hs);
					if (Character.isHighSurrogate(ch)) {
						hs = ch;
					} else {
						seqLength += encodedByteLength(
							ch
						);
						isSimple = true;
					}
				}
			}
		}

		if (!isSimple)
			seqLength += encodedByteLength(hs);

		return seqLength;
	}

	public static long encodeCodepoint(int cp, int encLen) {
		switch (encLen) {
		case 1:
			return cp;
		case 2:
			return (long)((cp >>> 6) | 0xc0)
				| ((long)((cp & 0x3f) | 0x80) << 8);
		case 3:
			return (long)((cp >>> 12) | 0xe0)
				| ((long)(((cp >>> 6) & 0x3f) | 0x80) << 8)
				| ((long)((cp & 0x3f) | 0x80) << 16);
		case 4:
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

	public static int codepointBytes(byte b) {
		int rv = UTF8_LEAD_TO_BYTES[b & 0xff];
		if (rv < 0)
			throw new IllegalStateException(
				"invalid UTF-8 sequence"
			);
		return rv;
	}

	public static int decodeCodepoint(long w) {
		switch (UTF8_LEAD_TO_BYTES[(int)w & 0xff]) {
		case 1:
			return (int)(w & 0x7f);
		case 2:
			return (int)(((w & 0x1f) << 6) | ((w >> 8) & 0x3f));
		case 3:
			return (int)(
				((w & 0xf) << 12)
				| (((w >> 8) & 0x3f) << 6)
				| ((w >> 16) & 0x3f)
			);
		case 4:
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
		4, 4, 4, 4, 4,
		3, 3, 3, 3, 3,
		2, 2, 2, 2,
		1, 1, 1, 1, 1, 1, 1, 1
	};

	private static final byte[] UTF8_LEAD_TO_BYTES = new byte[] {
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
		2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
		2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2,
		3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3,
		4, 4, 4, 4, 4, 4, 4, 4, -1, -1, -1, -1, -1, -1, -1, -1
	};
}
