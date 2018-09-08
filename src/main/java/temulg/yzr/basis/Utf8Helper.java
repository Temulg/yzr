/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.basis;

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

	public static long encodedBitLength(
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

	public static long encodeCodepointLE(int cp_, int encLen) {
		long cp = cp_;
		switch (encLen) {
		case 8:
			return cp;
		case 16:
			return ((cp >>> 6) | 0xc0)
				| (((cp & 0x3f) | 0x80) << 8);
		case 24:
			return ((cp >>> 12) | 0xe0)
				| ((((cp >>> 6) & 0x3f) | 0x80) << 8)
				| (((cp & 0x3f) | 0x80) << 16);
		case 32:
			return ((cp >>> 18) | 0xf0)
				| ((((cp >>> 12) & 0x3f) | 0x80) << 8)
				| ((((cp >>> 6) & 0x3f) | 0x80) << 16)
				| (((cp & 0x3f) | 0x80) << 24);
		default:
			throw new IllegalStateException(String.format(
				"code point %0x is outside of supported range",
				cp
			));
		}
	}

	public static long encodeCodepointBE(int cp_, int encLen) {
		long cp = cp_;
		switch (encLen) {
		case 8:
			return cp << 56;
		case 16:
			return (((cp & 0x3f) | 0x80)
				| (((cp << 2) & 0x1f00) | 0xc000)) << 48;
		case 24:
			return (((cp & 0x3f) | 0x80)
				| (((cp << 2) & 0x3f00) | 0x8000)
				| (((cp << 4) & 0xf0000) | 0xe00000)) << 40;
		case 32:
			return (((cp & 0x3f) | 0x80)
				| (((cp << 2) & 0x3f00) | 0x8000)
				| (((cp << 4) & 0x3f0000) | 0x800000)
				| (((cp << 6) & 0x7000000) | 0xf0000000)) << 32;
		default:
			throw new IllegalStateException(String.format(
				"code point %08x is outside of supported range",
				cp
			));
		}
	}

	public static int codepointBits(byte b) {
		int rv = UTF8_LEAD_TO_BITS[b & 0xff];
		if (rv < 0)
			throw new IllegalStateException(String.format(
				"invalid UTF-8 lead byte %02x", b
			));
		return rv;
	}

	public static int decodeCodepointLE(long w, int decLen) {
		switch (decLen) {
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

	public static int decodeCodepointBE(long w, int decLen) {
		switch (decLen) {
		case 8:
			return (int)(w >>> 56);
		case 16:
			return (int)(
				((w >>> 48) & 0x3f) | ((w >>> 50) & 0x7c0)
			);
		case 24:
			return (int)(
				((w >>> 40) & 0x3f)
				| ((w >>> 42) & 0xfc0)
				| ((w >>> 44) & 0xf000)
			);
		case 32:
			return (int)(
				((w >>> 32) & 0x3f)
				| ((w >>> 34) & 0xfc0)
				| ((w >>> 36) & 0x3f000)
				| ((w >>> 38) & 0x1c0000)
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
		8, 8, 8, 8, 8, 8, 8, 8
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
