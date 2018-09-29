/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

#if !defined(HPP_802B610699B6226F1B91C0369D83C902)
#define HPP_802B610699B6226F1B91C0369D83C902

#include <iterator>

namespace yzr::detail {

template <typename StringType, typename Consumer>
void for_each_substring(
	StringType const &s, typename StringType::value_type sep,
	Consumer &&cons
) {
	auto pos(std::begin(s));
	decltype(pos) b(pos);
	int mode(0);

	for (; pos != std::end(s); ++pos) {
		auto ch(*pos);

		if (!ch)
			break;

		switch (mode) {
		case 0:
			mode = ch != sep ? 2 : 1;
			break;
		case 1:
			if (ch != sep) {
				b = pos;
				mode = 2;
			}
			break;
		case 2:
			if (ch == sep) {
				cons(b, pos);
				mode = 1;
			}
			break;
		}
	}

	if (mode == 2)
		cons(b, pos);
}

}
#endif
