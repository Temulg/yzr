/*
 * Convert binary data into a properly escaped C string.
 *
 * Copyright (C) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * This program is free software; you can redistribute  it and/or modify it
 * under  the  terms of  the GNU General Public License version 3 as publi-
 * shed by the Free Software Foundation.
 */

#include <stdio.h>
#include <unistd.h>

size_t encode_byte(char *line, size_t line_pos, char ch, char next_ch) {
	const char nc[] = {'a', 'b', 't', 'n', 'v', 'f', 'r'};

	if (ch >= 0x7 && ch < 0xe) {
		line[line_pos++] = '\\';
		line[line_pos++] = nc[ch - 7];
	} else if (ch == ' ' || ch == '!') {
		line[line_pos++] = ch;
	} else if (ch == '\"' || ch == '\\') {
		line[line_pos++] = '\\';
		line[line_pos++] = ch;
	} else if ((ch >= '#' && ch < '\\') || (ch >= ']' && ch < 0x7f)) {
		line[line_pos++] = ch;
	} else {
		line[line_pos++] = '\\';

		char d[] = {
			(ch >> 6) & 3, (ch >> 3) & 7, ch & 7
		};

		if (d[0] || (next_ch >= '0' && next_ch < '8')) {
			line[line_pos++] = d[0] | '0';
			line[line_pos++] = d[1] | '0';
		} else if (d[1])
			line[line_pos++] = d[1] | '0';

		line[line_pos++] = d[2] | '0';
	}
	return line_pos;
}

static char buffer_in[4096];
static char line_buffer_out[80];

int main(int argc, char **argv)
{
	size_t line_pos = 0, byte_count = 0;
	ssize_t write_count = 0;

	ssize_t count = read(
		STDIN_FILENO, buffer_in, sizeof(buffer_in)
	);

	ssize_t in_pos = 0;
	char char_pair[3];
	if (count > 2) {
		char_pair[0] = buffer_in[in_pos++];
		char_pair[1] = buffer_in[in_pos++];
		char_pair[2] = 1;
	} else if (count == 1) {
		char_pair[0] = buffer_in[in_pos++];
		char_pair[2] = 0;
	} else if (count <= 0) {
		write_count += write(STDIN_FILENO, "\"\", 0", 5);
		return 0;
	}

	line_buffer_out[0] = '\"';
	line_pos = 1;

	while (1) {
		if (char_pair[2]) {
			line_pos = encode_byte(
				line_buffer_out, line_pos, char_pair[0],
				char_pair[1]
			);
			byte_count++;

			if ((sizeof(line_buffer_out) - line_pos) <= 6) {
				line_buffer_out[line_pos++] = '\"';
				line_buffer_out[line_pos++] = '\n';
				write_count += write(
					STDOUT_FILENO, line_buffer_out,
					line_pos
				);
				line_pos = 1;
				line_buffer_out[0] = '\"';
			}

			char_pair[0] = char_pair[1];

			if (in_pos < count)
				char_pair[1] = buffer_in[in_pos++];
			else
				char_pair[2] = 0;
		} else {
			count = read(
				STDIN_FILENO, buffer_in, sizeof(buffer_in)
			);
			if (count <= 0)
				break;

			in_pos = 0;
			char_pair[1] = buffer_in[in_pos++];
			char_pair[2] = 1;
		}
	}

	if ((sizeof(line_buffer_out) - line_pos) <= 6) {
		line_buffer_out[line_pos++] = '\"';
		line_buffer_out[line_pos++] = '\n';
		write_count += write(
			STDOUT_FILENO, line_buffer_out, line_pos
		);
		line_pos = 1;
		line_buffer_out[0] = '\"';
	}

	line_pos = encode_byte(line_buffer_out, line_pos, char_pair[0], 0);
	byte_count++;

	if ((sizeof(line_buffer_out) - line_pos) > 12) {
		line_buffer_out[line_pos++] = '\"';
		line_buffer_out[line_pos++] = ',';
		line_buffer_out[line_pos++] = ' ';
		line_pos += snprintf(
			line_buffer_out + line_pos, 
			sizeof(line_buffer_out) - line_pos,
			"%lu", byte_count
		);
		line_buffer_out[line_pos++] = '\n';
		write_count += write(
			STDOUT_FILENO, line_buffer_out, line_pos
		);
	} else {
		line_buffer_out[line_pos++] = '\"';
		line_buffer_out[line_pos++] = ',';
		line_buffer_out[line_pos++] = '\n';
		write_count += write(
			STDOUT_FILENO, line_buffer_out, line_pos
		);

		line_pos = snprintf(
			line_buffer_out, sizeof(line_buffer_out),
			"%lu", byte_count
		);
		line_buffer_out[line_pos++] = '\n';
		write_count += write(
			STDOUT_FILENO, line_buffer_out, line_pos
		);
	}

	return 0;
}
