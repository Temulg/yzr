/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

#include "app_state.hpp"
#include "string_utils.hpp"

#include <vector>
#include <cstring>
#include <unistd.h>

namespace yzr {
namespace {

void workDir(AppEnv &env, std::vector<char> &buf) {
	while (true) {
		auto wd(getcwd(buf.data(), buf.size()));
		if (wd) {
			env.workDir.assign(wd);
			break;
		} else {
			if (errno != ERANGE)
				throw Error(
					"error getting currend working dir: "
					"%s", strerror(errno)
				);

			buf.resize(buf.size() << 1);
		}
	}
}

void yzrDir(AppEnv &env, std::vector<char> &buf) {
	ssize_t sz(0);

	while (true) {
		sz = readlink("/proc/self/exe", buf.data(), buf.size());
		if (sz < 0)
			throw Error(
				"error reading /proc/self/exe: %s",
				strerror(errno)
			);

		if (sz < buf.size()) {
			buf[sz] = 0;
			break;
		} else
			buf.resize(buf.size() << 1);
	}

	ssize_t elemPos(0);
	std::pair<size_t, size_t> elems[4];

	detail::forEachSubstring(buf, '/', [&elems, &elemPos, &buf](
		auto first, auto last
	) {
		auto pos(first - buf.begin());
		auto sz(last - first);

		elems[elemPos & 3] = std::make_pair(pos, sz);
		elemPos++;
	});

	auto goodLayout(elemPos > 1);
	goodLayout = goodLayout && (elems[(elemPos - 2) & 3].second == 3);
	goodLayout = goodLayout && (0 == strncmp(
		buf.data() + elems[(elemPos - 2) & 3].first, "bin", 3
	));

	if (goodLayout) {
		if (elemPos > 2)
			env.yzrDir.assign(
				buf.data(),
				elems[(elemPos - 3) & 3].first
				+ elems[(elemPos - 3) & 3].second
			);
		else
			env.yzrDir = env.workDir;
	} else
		throw Error("%s: invalid yzr directory layout", buf.data());
}

void buildDir(AppEnv &env, std::vector<char> &buf) {
	if (!env.buildDir.empty())
		return;
/*
	auto bd(env.workDir);

	while (!bd.empty()) {
		auto ybd(bd / ".yzr");

		if (fs::is_directory(ybd)) {
			env.buildDir = ybd;
			break;
		}

		bd = bd.parent_path();
	}
*/
}

static constexpr std::array envConfigurators = {
	workDir,
	yzrDir,
	buildDir
};

}

void AppEnv::setup() {
	std::vector<char> buf(128);

	for (auto cfg: envConfigurators) {
		cfg(*this, buf);
	}
}

}
