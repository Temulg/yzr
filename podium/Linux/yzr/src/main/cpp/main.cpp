/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

#include "app_state.hpp"
#include "cli11/app.hpp"
#include "cli11/formatter.hpp"

int main(int argc, char **argv) {
	yzr::AppState app{};

	cli_11::App appArgs{"Yzr builder tool"};

	try {
		appArgs.parse((argc), (argv));
	} catch(cli_11::ParseError const &e) {
		return appArgs.exit(e);
	}

	return appArgs.exit(cli_11::Success{});
}
