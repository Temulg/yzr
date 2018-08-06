/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package yzr.basis;

public class ByteHelper {
	private ByteHelper() {
	}

	public static int onesCount(byte b) {
		return BYTE_PARAMS[b & 0xff] & 0xf;
	}

	public static int trailingZeros(byte b) {
		return (BYTE_PARAMS[b & 0xff] >>> 4) & 0xf;
	}

	public static int leadingZeros(byte b) {
		return (BYTE_PARAMS[b & 0xff] >>> 8) & 0xf;
	}

	public static int trailingNthOne(byte b, int count) {
		int pos = 0;

		switch (count) {
		case 7:
			return (b == 0xff) ? 7 : -1;
		case 6:
			if (onesCount(b) > 6) {
				return ((b & 0x7f) == 0x7f) ? 6 : 7;
			} else
				return -1;
		case 5:
			pos = trailingZeros(b);
			if (pos > 2)
				return -1;
			b ^= 1 << pos;
		case 4:
			pos = trailingZeros(b);
			if (pos > 3)
				return -1;
			b ^= 1 << pos;
		case 3:
			pos = trailingZeros(b);
			if (pos > 4)
				return -1;
			b ^= 1 << pos;
		case 2:
			pos = trailingZeros(b);
			if (pos > 5)
				return -1;
			b ^= 1 << pos;
		case 1:
			pos = trailingZeros(b);
			if (pos > 6)
				return -1;
			b ^= 1 << pos;
		case 0:
			pos = trailingZeros(b);
			return pos < 8 ? pos : -1;
		default:
			return -1;
		}
	}

	private static char[] BYTE_PARAMS = new char[] {
		(char)0x0880, (char)0x0701, (char)0x0611, (char)0x0602,// 0x00
		(char)0x0521, (char)0x0502, (char)0x0512, (char)0x0503,
		(char)0x0431, (char)0x0402, (char)0x0412, (char)0x0403,
		(char)0x0422, (char)0x0403, (char)0x0413, (char)0x0404,
		(char)0x0341, (char)0x0302, (char)0x0312, (char)0x0303, // 0x10
		(char)0x0322, (char)0x0303, (char)0x0313, (char)0x0304,
		(char)0x0332, (char)0x0303, (char)0x0313, (char)0x0304,
		(char)0x0323, (char)0x0304, (char)0x0314, (char)0x0305,
		(char)0x0251, (char)0x0202, (char)0x0212, (char)0x0203, // 0x20
		(char)0x0222, (char)0x0203, (char)0x0213, (char)0x0204,
		(char)0x0232, (char)0x0203, (char)0x0213, (char)0x0204,
		(char)0x0223, (char)0x0204, (char)0x0214, (char)0x0205,
		(char)0x0242, (char)0x0203, (char)0x0213, (char)0x0204, // 0x30
		(char)0x0223, (char)0x0204, (char)0x0214, (char)0x0205,
		(char)0x0233, (char)0x0204, (char)0x0214, (char)0x0205,
		(char)0x0224, (char)0x0205, (char)0x0215, (char)0x0206,
		(char)0x0161, (char)0x0102, (char)0x0112, (char)0x0103, // 0x40
		(char)0x0122, (char)0x0103, (char)0x0113, (char)0x0104,
		(char)0x0132, (char)0x0103, (char)0x0113, (char)0x0104,
		(char)0x0123, (char)0x0104, (char)0x0114, (char)0x0105,
		(char)0x0142, (char)0x0103, (char)0x0113, (char)0x0104, // 0x50
		(char)0x0123, (char)0x0104, (char)0x0114, (char)0x0105,
		(char)0x0133, (char)0x0104, (char)0x0114, (char)0x0105,
		(char)0x0124, (char)0x0105, (char)0x0115, (char)0x0106,
		(char)0x0152, (char)0x0103, (char)0x0113, (char)0x0104, // 0x60
		(char)0x0123, (char)0x0104, (char)0x0114, (char)0x0105,
		(char)0x0133, (char)0x0104, (char)0x0114, (char)0x0105,
		(char)0x0124, (char)0x0105, (char)0x0115, (char)0x0106,
		(char)0x0143, (char)0x0104, (char)0x0114, (char)0x0105, // 0x70
		(char)0x0124, (char)0x0105, (char)0x0115, (char)0x0106,
		(char)0x0134, (char)0x0105, (char)0x0115, (char)0x0106,
		(char)0x0125, (char)0x0106, (char)0x0116, (char)0x0107,
		(char)0x0071, (char)0x0002, (char)0x0012, (char)0x0003, // 0x80
		(char)0x0022, (char)0x0003, (char)0x0013, (char)0x0004,
		(char)0x0032, (char)0x0003, (char)0x0013, (char)0x0004,
		(char)0x0023, (char)0x0004, (char)0x0014, (char)0x0005,
		(char)0x0042, (char)0x0003, (char)0x0013, (char)0x0004, // 0x90
		(char)0x0023, (char)0x0004, (char)0x0014, (char)0x0005,
		(char)0x0033, (char)0x0004, (char)0x0014, (char)0x0005,
		(char)0x0024, (char)0x0005, (char)0x0015, (char)0x0006,
		(char)0x0052, (char)0x0003, (char)0x0013, (char)0x0004, // 0xa0
		(char)0x0023, (char)0x0004, (char)0x0014, (char)0x0005,
		(char)0x0033, (char)0x0004, (char)0x0014, (char)0x0005,
		(char)0x0024, (char)0x0005, (char)0x0015, (char)0x0006,
		(char)0x0043, (char)0x0004, (char)0x0014, (char)0x0005, // 0xb0
		(char)0x0024, (char)0x0005, (char)0x0015, (char)0x0006,
		(char)0x0034, (char)0x0005, (char)0x0015, (char)0x0006,
		(char)0x0025, (char)0x0006, (char)0x0016, (char)0x0007,
		(char)0x0062, (char)0x0003, (char)0x0013, (char)0x0004, // 0xc0
		(char)0x0023, (char)0x0004, (char)0x0014, (char)0x0005,
		(char)0x0033, (char)0x0004, (char)0x0014, (char)0x0005,
		(char)0x0024, (char)0x0005, (char)0x0015, (char)0x0006,
		(char)0x0043, (char)0x0004, (char)0x0014, (char)0x0005, // 0xd0
		(char)0x0024, (char)0x0005, (char)0x0015, (char)0x0006,
		(char)0x0034, (char)0x0005, (char)0x0015, (char)0x0006,
		(char)0x0025, (char)0x0006, (char)0x0016, (char)0x0007,
		(char)0x0053, (char)0x0004, (char)0x0014, (char)0x0005, // 0xe0
		(char)0x0024, (char)0x0005, (char)0x0015, (char)0x0006,
		(char)0x0034, (char)0x0005, (char)0x0015, (char)0x0006,
		(char)0x0025, (char)0x0006, (char)0x0016, (char)0x0007,
		(char)0x0044, (char)0x0005, (char)0x0015, (char)0x0006, // 0xf0
		(char)0x0025, (char)0x0006, (char)0x0016, (char)0x0007,
		(char)0x0035, (char)0x0006, (char)0x0016, (char)0x0007,
		(char)0x0026, (char)0x0007, (char)0x0017, (char)0x0008
	};
}
