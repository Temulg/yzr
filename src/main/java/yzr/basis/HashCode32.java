/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package yzr.basis;

import java.util.PrimitiveIterator;

public class HashCode32 {
	private HashCode32() {
	}

	public static int of(int v) {
		int h = XXHASH_SEED + XXHASH_PRIME32_4 + 4;

		h += v * XXHASH_PRIME32_2;
		h = rotl32(h, 17) * XXHASH_PRIME32_3;

		return xxHashAvalanche(h);
	}

	public static int of(int... v) {
		int h = 0;

		switch (v.length) {
		case 0:
			h = XXHASH_SEED + XXHASH_PRIME32_4;
			break;
		case 1:
			h = ofTail(
				XXHASH_SEED + XXHASH_PRIME32_4 + 4,
				v[0]
			);
			break;
		case 2:
			h = ofTail(
				XXHASH_SEED + XXHASH_PRIME32_4 + 8,
				v[0], v[1]
			);
			break;
		case 3:
			h = ofTail(
				XXHASH_SEED + XXHASH_PRIME32_4 + 12,
				v[0], v[1], v[2]
			);
			break;
		default:
			return of(new PrimitiveIterator.OfInt() {
				@Override
				public boolean hasNext() {
					return pos < v.length;
				}

				@Override
				public int nextInt() {
					return v[pos++];
				}

				private int pos = 0;
			});
		}

		return xxHashAvalanche(h);
	}

	public static int of(PrimitiveIterator.OfInt iter) {
		int pos = 0;
		int v0 = 0, v1 = 0, v2 = 0, v3 = 0;

		while (iter.hasNext()) {
			switch (pos) {
			case 0:
				v0 = iter.nextInt();
				break;
			case 1:
				v1 = iter.nextInt();
				break;
			case 2:
				v2 = iter.nextInt();
				break;
			case 3:
				v3 = iter.nextInt();
				return ofBlock(v0, v1, v2, v3, iter);
			}

			pos++;
		}

		int h = XXHASH_SEED + XXHASH_PRIME32_4 + (pos << 2);

		switch (pos) {
		case 3:
			h = ofTail(h, v0, v1, v2);
			break;
		case 2:
			h = ofTail(h, v0, v1);
			break;
		case 1:
			h = ofTail(h, v0);
			break;
		}

		return xxHashAvalanche(h);
	}

	private static int ofBlock(
		int v0, int v1, int v2, int v3, PrimitiveIterator.OfInt iter
	) {
		int pos = 4;
		int h0 = XXHASH_SEED + XXHASH_PRIME32_0 + XXHASH_PRIME32_1;
		int h1 = XXHASH_SEED + XXHASH_PRIME32_1;
		int h2 = XXHASH_SEED + 0;
		int h3 = XXHASH_SEED - XXHASH_PRIME32_0;

		h0 = xxHashRound(h0, v0);
		h1 = xxHashRound(h1, v1);
		h2 = xxHashRound(h2, v2);
		h3 = xxHashRound(h3, v3);

		while (iter.hasNext()) {
			switch (pos & 3) {
			case 0:
				v0 = iter.nextInt();
				break;
			case 1:
				v1 = iter.nextInt();
				break;
			case 2:
				v2 = iter.nextInt();
				break;
			case 3:
				v3 = iter.nextInt();
				h0 = xxHashRound(h0, v0);
				h1 = xxHashRound(h1, v1);
				h2 = xxHashRound(h2, v2);
				h3 = xxHashRound(h3, v3);
				break;
			}
			pos++;
		}

		int h = rotl32(h0, 1) + rotl32(h1, 7) + rotl32(h2, 12)
			+ rotl32(h3, 18);

		h += pos << 2;
		switch (pos & 3) {
		case 1:
			h = ofTail(h, v0);
			break;
		case 2:
			h = ofTail(h, v0, v1);
			break;
		case 3:
			h = ofTail(h, v0, v1, v2);
			break;
		}

		return xxHashAvalanche(h);
	}

	private static int ofTail(int h, int v0) {
		h += v0 * XXHASH_PRIME32_2;
		return rotl32(h, 17) * XXHASH_PRIME32_3;
	}

	private static int ofTail(int h, int v0, int v1) {
		h += v0 * XXHASH_PRIME32_2;
		h = rotl32(h, 17) * XXHASH_PRIME32_3;

		h += v1 * XXHASH_PRIME32_2;
		return rotl32(h, 17) * XXHASH_PRIME32_3;
	}

	private static int ofTail(int h, int v0, int v1, int v2) {
		h += v0 * XXHASH_PRIME32_2;
		h = rotl32(h, 17) * XXHASH_PRIME32_3;

		h += v1 * XXHASH_PRIME32_2;
		h = rotl32(h, 17) * XXHASH_PRIME32_3;

		h += v2 * XXHASH_PRIME32_2;
		return rotl32(h, 17) * XXHASH_PRIME32_3;
	}

	public static int of(long v) {
		int h = XXHASH_SEED + XXHASH_PRIME32_4 + 8;

		h += (int)v * XXHASH_PRIME32_2;
		h = rotl32(h, 17) * XXHASH_PRIME32_3;

		h += (int)(v >>> 32) * XXHASH_PRIME32_2;
		h = rotl32(h, 17) * XXHASH_PRIME32_3;

		return xxHashAvalanche(h);
	}

	public static int of(long... v) {
		int h = 0;

		switch (v.length) {
		case 0:
			h = XXHASH_SEED + XXHASH_PRIME32_4;
			break;
		case 1:
			h = ofTail(
				XXHASH_SEED + XXHASH_PRIME32_4 + 8,
				(int)v[0], (int)(v[0] >>> 32)
			);
			break;
		default:
			return of(new PrimitiveIterator.OfLong() {
				@Override
				public boolean hasNext() {
					return pos < v.length;
				}

				@Override
				public long nextLong() {
					return v[pos++];
				}

				private int pos = 0;
			});
		}

		return xxHashAvalanche(h);
	}

	public static int of(PrimitiveIterator.OfLong iter) {
		int pos = 0;
		long v0 = 0, v1 = 0;

		while (iter.hasNext()) {
			switch (pos) {
			case 0:
				v0 = iter.nextLong();
				break;
			case 1:
				v1 = iter.nextLong();
				return ofBlock(v0, v1, iter);
			}

			pos++;
		}

		int h = XXHASH_SEED + XXHASH_PRIME32_4 + (pos << 3);

		if (pos == 1)
			h = ofTail(h, (int)v0, (int)(v0 >>> 32));

		return xxHashAvalanche(h);
	}

	private static int ofBlock(
		long v0, long v1, PrimitiveIterator.OfLong iter
	) {
		int pos = 2;
		int h0 = XXHASH_SEED + XXHASH_PRIME32_0 + XXHASH_PRIME32_1;
		int h1 = XXHASH_SEED + XXHASH_PRIME32_1;
		int h2 = XXHASH_SEED + 0;
		int h3 = XXHASH_SEED - XXHASH_PRIME32_0;

		h0 = xxHashRound(h0, (int)v0);
		h1 = xxHashRound(h1, (int)(v0 >>> 32));
		h2 = xxHashRound(h2, (int)v1);
		h3 = xxHashRound(h3, (int)(v1 >>> 32));

		while (iter.hasNext()) {
			if ((pos & 1) == 0) {
				v0 = iter.nextLong();
			} else {
				v1 = iter.nextLong();
				h0 = xxHashRound(h0, (int)v0);
				h1 = xxHashRound(h1, (int)(v0 >>> 32));
				h2 = xxHashRound(h2, (int)v1);
				h3 = xxHashRound(h3, (int)(v1 >>> 32));
			}

			pos++;
		}

		int h = rotl32(h0, 1) + rotl32(h1, 7) + rotl32(h2, 12)
			+ rotl32(h3, 18);

		h += pos << 3;
		if ((pos & 1) != 0)
			h = ofTail(h, (int)v0, (int)(v0 >>> 32));

		return xxHashAvalanche(h);
	}

	private static int xxHashRound(int h, int v) {
		h += v * XXHASH_PRIME32_1;
		h = rotl32(h, 13);
		return h * XXHASH_PRIME32_0;
	}

	private static int xxHashAvalanche(int h) {
		h ^= h >>> 15;
		h *= XXHASH_PRIME32_1;
		h ^= h >>> 13;
		h *= XXHASH_PRIME32_2;
		h ^= h >>> 16;
		return h;
	}

	public static int rotl32(int v, int r) {
		return (v << r) | (v >>> (32 - r));
	}

	private static int XXHASH_SEED = 0;
	private static final int XXHASH_PRIME32_0 = 0x9e3779b1;
	private static final int XXHASH_PRIME32_1 = 0x85ebca77;
	private static final int XXHASH_PRIME32_2 = 0xc2b2ae3d;
	private static final int XXHASH_PRIME32_3 = 0x27d4eb2f;
	private static final int XXHASH_PRIME32_4 = 0x165667b1;
}
