/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

#if !defined(HPP_9C5B2DFA3111F370BB5112FC3CA48330)
#define HPP_9C5B2DFA3111F370BB5112FC3CA48330

#include <exception>
#include <cstdlib>

namespace yzr {

struct error : std::exception {
	[[gnu::format(printf, 2, 3)]]
	error(char const *format, ...);

	~error() {
		free(msg);
	}

	char const *what() const noexcept override {
		return msg;
	}
protected:
	error() {
	}

	char *msg;
};

struct usage_error : error {
	[[gnu::format(printf, 2, 3)]]
	usage_error(char const *format, ...);
};

}
#endif
