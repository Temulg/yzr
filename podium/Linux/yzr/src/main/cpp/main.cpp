/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

#include "app_state.hpp"
#include <cstring>

namespace {

enum struct ArgType : int {
	INVALID,
	LONG,
	SHORT,
	ANY
};

ArgType argType(char const *arg) {
	if (arg[0] != '-')
		return arg[0] ? ArgType::ANY : ArgType::INVALID;

	if (arg[1] != '-')
		return arg[1] ? ArgType::SHORT : ArgType::ANY;

	return arg[2] ? ArgType::LONG : ArgType::ANY;
}

bool matchArg(
	char const *arg, ArgType at, char const *shopt, char const *lopt
) {
	switch (at) {
	case ArgType::LONG:
		return strcmp(arg, lopt) ? false : true;
	case ArgType::SHORT:
		return strcmp(arg, shopt) ? false : true;
	default:
		return false;
	}
}

}

namespace yzr {

Error::Error(char const *format, ...) {
	va_list args;
	va_start(args, format);
	if (0 >= vasprintf(&msg, format, args))
		msg = nullptr;
	va_end(args);
}

UsageError::UsageError(char const *format, ...) {
	va_list args;
	va_start(args, format);
	if (0 >= vasprintf(&msg, format, args))
		msg = nullptr;
	va_end(args);
}

static constexpr char usageString[] =
	"Usage: yzr [-b | --build-dir <path>] <command> [<args>]\n";

bool AppEnv::inspectCmdArgs(int argc, char **argv) {
	int pos(1);

	while (pos < argc) {
		auto a(argv[pos]);
		auto at(argType(a));
		char const *b;

		switch (at) {
		case ArgType::INVALID:
			return false;
		case ArgType::LONG:
			b = a + 2;
			break;
		case ArgType::SHORT:
			b = a + 1;
			break;
		case ArgType::ANY:
			return true;
		}

		if (matchArg(b, at, "b", "build-dir")) {
			if (argc == ++pos)
				throw UsageError(
					"no directory given for %s", a
				);

			buildDir = argv[pos++];
			continue;
		}

		pos++;
	}

	return true;
}


}

int main(int argc, char **argv) {
	yzr::AppState app{};

	try {
		yzr::AppEnv env;

		if (!env.inspectCmdArgs(argc, argv)) {
			
			return -1;
		}

		env.setup();
	} catch (yzr::UsageError const &e) {
		fputs(e.what(), stderr);
		fputc('\n', stderr);
		fputs(yzr::usageString, stderr);
		return -1;
	} catch (std::exception const &e) {
		fputs(e.what(), stderr);
		fputc('\n', stderr);
		return -2;
	}

	return 0;
}
