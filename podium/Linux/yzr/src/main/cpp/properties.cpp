/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

#include "error.hpp"
#include "properties.hpp"

#include <fcntl.h>
#include <unistd.h>

namespace yzr {
namespace {

bool is_space(char ch) {
	switch (ch) {
	case ' ':
	case '\t':
	case '\f':
		return true;
	default:
		return false;
	}
}

bool is_line_sep(char ch) {
	switch (ch) {
	case '\n':
	case '\r':
		return true;
	default:
		return false;
	}
}

bool is_key_sep(char ch) {
	switch (ch) {
	case ':':
	case '=':
		return true;
	default:
		return false;
	}
}

bool is_any_space(char ch) {
	switch (ch) {
	case ' ':
	case '\t':
	case '\f':
	case '\n':
	case '\r':
		return true;
	default:
		return false;
	}
}

bool is_comment(char ch) {
	switch (ch) {
	case '#':
	case '!':
		return true;
	default:
		return false;
	}
}

bool append_xdigit(int &dst, char dig) {
	if (dig >= '0' && dig <= '9') {
		dst = (dst << 4) | (dig - '0');
		return true;
	} else if (dig >= 'A' && dig <= 'F') {
		dst = (dst << 4) | ((dig - 'A') + 10);
		return true;
	} else if (dig >= 'a' && dig <= 'f') {
		dst = (dst << 4) | ((dig - 'a') + 10);
		return true;
	} else
		return false;
}

void append_utf8(std::string &s, uint32_t cp) {
	if (cp < 0x80)
		s.push_back(cp);
	else if (cp < 0x800) {
		s.push_back(0xc0 | (cp >> 6));
		s.push_back(0x80 | (cp & 0x3f));
	} else if (cp < 0x10000) {
		s.push_back(0xe0 | (cp >> 12));
		s.push_back(0x80 | ((cp >> 6) & 0x3f));
		s.push_back(0x80 | (cp & 0x3f));
	} else
		throw error("codepoint 0x%x out of supported range", cp);
}

}

void properties::maybe_append_file(
	std::string const &p, std::vector<char> &buf
) {
	enum {
		INITIAL_WS,
		COMMENT,
		KEY_CHAR,
		KEY_SEP,
		KEY_SSEP,
		VALUE_CHAR,
		ESCAPE,
		ESCAPE_R,
		ESCAPE_N,
		UNI_DIGIT,
		ESCAPE_END,
	} state(INITIAL_WS), p_state(KEY_CHAR);

	auto fd(open(p.c_str(), O_RDONLY));
	if (fd < 0)
		return;

	if (buf.capacity() < 2048)
		buf.reserve(2048);

	std::string key;
	std::string value;
	int uni_digit_count(0), uni_value(0);

	while (true) {
		buf.resize(buf.capacity());
		auto sz(read(fd, buf.data(), buf.size()));
		if (sz <= 0)
			break;

		buf.resize(sz);

		for (auto ch: buf) {
retry:
			switch (state) {
			case INITIAL_WS:
				if (is_any_space(ch))
					break;

				if (is_comment(ch)) {
					state = COMMENT;
					break;
				}

				if (ch != '\\') {
					key.push_back(ch);
					state = KEY_CHAR;
				} else {
					p_state = KEY_CHAR;
					state = ESCAPE;
				}

				break;
			case COMMENT:
				if (is_line_sep(ch))
					state = INITIAL_WS;
				break;
			case KEY_CHAR:
				if (is_space(ch)) {
					state = KEY_SEP;
					break;
				}

				if (is_key_sep(ch)) {
					state = KEY_SSEP;
					break;
				}

				if (is_line_sep(ch)) {
					values.emplace(std::make_pair(
						key_type(key), value
					));
					key.clear();
					state = INITIAL_WS;
					break;
				}

				if (ch != '\\')
					key.push_back(ch);
				else {
					p_state = state;
					state = ESCAPE;
				}
				break;
			case KEY_SEP:
				if (is_space(ch))
					break;

				if (is_key_sep(ch)) {
					state = KEY_SSEP;
					break;
				}

				if (is_line_sep(ch)) {
					values.emplace(std::make_pair(
						key_type(key), value
					));
					key.clear();
					state = INITIAL_WS;
					break;
				}

				if (ch != '\\') {
					value.push_back(ch);
					state = VALUE_CHAR;
				} else {
					p_state = VALUE_CHAR;
					state = ESCAPE;
				}

				break;
			case KEY_SSEP:
				if (is_space(ch))
					break;

				if (is_line_sep(ch)) {
					values.emplace(std::make_pair(
						key_type(key), value
					));
					key.clear();
					state = INITIAL_WS;
					break;
				}

				if (ch != '\\') {
					value.push_back(ch);
					state = VALUE_CHAR;
				} else {
					p_state = VALUE_CHAR;
					state = ESCAPE;
				}

				break;
			case VALUE_CHAR:
				if (is_line_sep(ch)) {
					values.emplace(std::make_pair(
						key_type(key), value
					));
					key.clear();
					value.clear();
					state = INITIAL_WS;
					break;
				}

				if (ch != '\\')
					value.push_back(ch);
				else {
					p_state = state;
					state = ESCAPE;
				}

				break;
			case ESCAPE:
				switch (ch) {
				case '\n':
					state = ESCAPE_N;
					break;
				case '\r':
					state = ESCAPE_R;
					break;
				case 'f':
					uni_value = '\f';
					state = ESCAPE_END;
					break;
				case 'n':
					uni_value = '\n';
					state = ESCAPE_END;
					break;
				case 'r':
					uni_value = '\r';
					state = ESCAPE_END;
					break;
				case 't':
					uni_value = '\t';
					state = ESCAPE_END;
					break;
				case 'u':
					uni_value = 0;
					state = UNI_DIGIT;
					break;
				default:
					uni_value = ch;
				}
				break;
			case ESCAPE_R:
				switch (ch) {
				case '\n':
					state = ESCAPE_N;
					break;
				case '\r':
					uni_value = -1;
					state = ESCAPE_END;
					break;
				default:
					if (is_space(ch))
						state = ESCAPE_N;
					else {
						uni_value = ch;
						state = ESCAPE_END;
					}
				}
				break;
			case ESCAPE_N:
				if (is_space(ch))
					break;

				if (is_line_sep(ch)) {
					uni_value = -1;
					state = ESCAPE_END;
					break;
				}

				uni_value = ch;
				state = ESCAPE_END;
				break;
			case UNI_DIGIT:
				if (append_xdigit(uni_value, ch)) {
					uni_digit_count++;
					if (uni_digit_count == 4) {
						uni_digit_count = 0;
						state = ESCAPE_END;
						break;
					}
				} else {
					if (!uni_digit_count)
						uni_value = 'u';

					uni_digit_count = 0;
					state = ESCAPE_END;
					[[fallthrough]];
				}
			case ESCAPE_END:
				if (uni_value >= 0) {
					state = p_state;
					if (state == KEY_CHAR)
						append_utf8(key, uni_value);
					else if (state == VALUE_CHAR)
						append_utf8(value, uni_value);
				} else {
					state = INITIAL_WS;
					if (!key.empty()) {
						values.emplace(std::make_pair(
							key_type(key), value
						));
						key.clear();
						value.clear();
					}
				}
				goto retry;
			}
		}
	}

	close(fd);

	if (!key.empty())
		values.emplace(std::make_pair(key_type(key), value));
}

}
