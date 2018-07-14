/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package yzr.basis;

import java.util.function.IntConsumer;

public class Utf8Helper {
	private Utf8Helper() {
	}

	public static int encodedLength(int cp) {
		final int lz = Integer.numberOfLeadingZeros(cp) - 11;
		if (lz < 0)
			throw new IllegalArgumentException(String.format(
				"code point %0x is outside of supported range",
				cp
			));

		return CODEPOINT_BITS_TO_LENGTH[lz];
	}

	public static int encodedLength(
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
					seqLength += encodedLength(ch);
				}
			} else {
				if (Character.isLowSurrogate(ch)) {
					seqLength += encodedLength(
						Character.toCodePoint(hs, ch)
					);
					isSimple = true;
				} else {
					seqLength += encodedLength(hs);
					if (Character.isHighSurrogate(ch)) {
						hs = ch;
					} else {
						seqLength += encodedLength(
							ch
						);
						isSimple = true;
					}
				}
			}
		}

		if (!isSimple)
			seqLength += encodedLength(hs);

		return seqLength;
	}

	public static int encode(
		byte[] dst, int dstOffset, CharSequence src,
		int srcOffset, int srcLength
	) {
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
					dstOffset += encodeCodePoint(
						dst, dstOffset, ch
					);
				}
			} else {
				if (Character.isLowSurrogate(ch)) {
					dstOffset += encodeCodePoint(
						dst, dstOffset,
						Character.toCodePoint(hs, ch)
					);
					isSimple = true;
				} else {
					dstOffset += encodeCodePoint(
						dst, dstOffset, hs
					);
					if (Character.isHighSurrogate(ch)) {
						hs = ch;
					} else {
						dstOffset += encodeCodePoint(
							dst, dstOffset, ch
						);
						isSimple = true;
					}
				}
			}
		}

		if (!isSimple)
			dstOffset += encodeCodePoint(dst, dstOffset, hs);

		return dstOffset;
	}

	public static int encodeCodePoint(byte[] dst, int dstOffset, int cp) {
		int rv = encodedLength(cp);
		switch (rv) {
		case 1:
			dst[dstOffset] = (byte)(cp & 0x7f);
			break;
		case 2:
			dst[dstOffset] = (byte)(cp >>> 6 | 0xc0);
			dst[dstOffset + 1] = (byte)(cp & 0x3f | 0x80);
			break;
		case 3:
			dst[dstOffset] = (byte)(cp >>> 12 | 0xe0);
			dst[dstOffset + 1] = (byte)(cp >>> 6 & 0x3f | 0x80);
			dst[dstOffset + 2] = (byte)(cp & 0x3f | 0x80);
			break;
		case 4:
			dst[dstOffset] = (byte)(cp >>> 18 | 0xf0);
			dst[dstOffset + 1] = (byte)(cp >>> 12 & 0x3f | 0x80);
			dst[dstOffset + 2] = (byte)(cp >>> 6 & 0x3f | 0x80);
			dst[dstOffset + 3] = (byte)(cp & 0x3f | 0x80);
			break;
		default:
			throw new IllegalArgumentException(String.format(
				"code point %0x is outside of supported range",
				cp
			));
		}
		return rv;
	}

	public static int codepointBytes(byte b) {
		if (b >= 0)
			return 1;
		
		int rv = Integer.numberOfLeadingZeros(~(int)b) - 26;
		if (rv >= 0)
			return rv + 2;
		else
			throw new IllegalStateException(
				"invalid UTF-8 sequence"
			);
	}

	public static void decode(
		IntConsumer dst, byte[] src, int srcOffset,
		int srcLength
	) {
		int last = srcOffset + srcLength;
		int rem = 0;
		int cp = 0;

		for (; srcOffset < last; srcOffset++) {
			byte b = src[srcOffset];

			if (rem == 0) {
				rem = codepointBytes(b);
				switch (rem) {
				case 1:
					dst.accept(b);
					break;
				case 2:
					cp = b & 0x1f;
					break;
				case 3:
					cp = b & 0xf;
					break;
				case 4:
					cp = b & 7;
					break;
				default:
					throw new IllegalStateException(
						"invalid UTF-8 sequence"
					);
				}
			} else {
				if ((b & 0xc0) != 0x80)
					throw new IllegalStateException(
						"invalid UTF-8 sequence"
					);

				cp = (cp << 6) | (b & 0x3f);

				if (rem == 1)
					dst.accept(cp);
			}
			rem--;
		}

		if (rem > 0)
			throw new IllegalStateException(
				"invalid UTF-8 sequence"
			);
	}

	private static int[] CODEPOINT_BITS_TO_LENGTH = new int[] {
		4, 4, 4, 4, 4,
		3, 3, 3, 3, 3,
		2, 2, 2, 2,
		1, 1, 1, 1, 1, 1, 1
	};
}
