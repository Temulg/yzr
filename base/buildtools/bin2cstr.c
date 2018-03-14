/*
 * Convert binary data into a properly escaped C string.
 *
 * Copyright (C) 2010 Alex Dubov <oakad@yahoo.com>
 *
 * This program is free software; you can redistribute  it and/or modify it
 * under  the  terms of  the GNU General Public License version 3 as publi-
 * shed by the Free Software Foundation.
 */

#include <stdio.h>

int c2s(char *s_out, int c_in)
{
	const char nc[] = {'a', 'b', 't', 'n', 'v', 'f', 'r'};

	if (c_in >= 7 && c_in < 14) {
		s_out[0] = '\\';
		s_out[1] = nc[c_in - 7];
		s_out[2] = 0;
		return 2;
	} else if (c_in >= 32 && c_in < 34) {
		s_out[0] = c_in;
		s_out[1] = 0;
		return 1;
	} else if (c_in == 34 || c_in == 92) {
		s_out[0] = '\\';
		s_out[1] = c_in;
		s_out[2] = 0;
		return 2;
	} else if ((c_in >= 35 && c_in < 92) || (c_in >= 93 && c_in < 127)) {
		s_out[0] = c_in;
		s_out[1] = 0;
		return 1;
	} else {
		s_out[0] = '\\';
		s_out[1] = (c_in >> 6) | 48;
		s_out[2] = ((c_in >> 3) & 7) | 48;
		s_out[3] = (c_in & 7) | 48;
		s_out[4] = 0;
		return 4;
	}
}

int main(int argc, char **argv)
{
	int c, l_cnt = 0, c_cnt;
	size_t b_cnt = 0;
	char c_out[5];

	while (EOF != (c = fgetc(stdin))) {
		if (l_cnt == 0) {
			fputc('\"', stdout);
			l_cnt++;
		}

		c_cnt = c2s(c_out, c);
		if ((l_cnt + c_cnt) > 79) {
			ungetc(c, stdin);
			fputs("\"\n", stdout);
			l_cnt = 0;
		} else {
			fputs(c_out, stdout);
			l_cnt += c_cnt;
			b_cnt++;
		}
	}
	if (l_cnt)
		fputs("\"\n", stdout);

	if (b_cnt)
		printf(", %lu", b_cnt);

	return 0;
}
