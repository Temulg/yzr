/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

#include "error.hpp"
#include "app_state.hpp"
#include <cstring>

namespace {

enum struct arg_type : int {
	INVALID,
	LONG,
	SHORT,
	ANY
};

arg_type arg_to_type(char const *arg) {
	if (arg[0] != '-')
		return arg[0] ? arg_type::ANY : arg_type::INVALID;

	if (arg[1] != '-')
		return arg[1] ? arg_type::SHORT : arg_type::ANY;

	return arg[2] ? arg_type::LONG : arg_type::ANY;
}

bool match_arg(
	char const *arg, arg_type at, char const *shopt, char const *lopt
) {
	switch (at) {
	case arg_type::LONG:
		return strcmp(arg, lopt) ? false : true;
	case arg_type::SHORT:
		return strcmp(arg, shopt) ? false : true;
	default:
		return false;
	}
}

constexpr char usage_string[] =
	"Usage: yzr [-b | --build-dir <path>] <command> [<args>]\n";

}

namespace yzr {

error::error(char const *format, ...) {
	va_list args;
	va_start(args, format);
	if (0 >= vasprintf(&msg, format, args))
		msg = nullptr;
	va_end(args);
}

usage_error::usage_error(char const *format, ...) {
	va_list args;
	va_start(args, format);
	if (0 >= vasprintf(&msg, format, args))
		msg = nullptr;
	va_end(args);
}

app_env &app_env::inspect_cmd_args(int argc, char **argv) {
	int pos(1);

	while (pos < argc) {
		auto a(argv[pos]);
		auto at(arg_to_type(a));
		char const *b;

		switch (at) {
		case arg_type::INVALID:
			throw usage_error("invalid argument %s", a);
		case arg_type::LONG:
			b = a + 2;
			break;
		case arg_type::SHORT:
			b = a + 1;
			break;
		case arg_type::ANY:
			return *this;
		}

		if (match_arg(b, at, "b", "build-dir")) {
			if (argc == ++pos)
				throw usage_error(
					"no directory given for %s", a
				);

			build_dir = argv[pos++];
			continue;
		}

		pos++;
	}

	return *this;
}

}

int main(int argc, char **argv) {
	yzr::app_state app{};

	try {
		yzr::app_env ae;

		ae.inspect_cmd_args(
			argc, argv
		).setup();

		app.locate_jvm(ae);
		app.load_bootstrap(ae);
	} catch (yzr::usage_error const &e) {
		fputs(e.what(), stderr);
		fputc('\n', stderr);
		fputs(usage_string, stderr);
		return -1;
	} catch (std::exception const &e) {
		fputs(e.what(), stderr);
		fputc('\n', stderr);
		return -2;
	}

	return app.prepare_and_start(argc, argv);
}
