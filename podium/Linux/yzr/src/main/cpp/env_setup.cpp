/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

#include "app_state.hpp"
#include "path_utils.hpp"

#include <vector>
#include <pwd.h>

namespace yzr {
namespace {

void yzr_dir(app_env &env, std::vector<char> &buf) {
	ssize_t sz(0);

	while (true) {
		sz = readlink("/proc/self/exe", buf.data(), buf.size());
		if (sz < 0)
			throw error(
				"error reading /proc/self/exe: %s",
				strerror(errno)
			);

		if (sz < buf.size()) {
			buf[sz] = 0;
			break;
		} else
			buf.resize(buf.size() << 1);
	}

	ssize_t elem_pos(0);
	std::pair<size_t, size_t> elems[4];

	detail::for_each_substring(buf, '/', [&elems, &elem_pos, &buf](
		auto first, auto last
	) {
		auto pos(first - buf.begin());
		auto sz(last - first);

		elems[elem_pos & 3] = std::make_pair(pos, sz);
		elem_pos++;
	});

	auto good_layout(elem_pos > 1);
	good_layout = good_layout && (elems[(elem_pos - 2) & 3].second == 3);
	good_layout = good_layout && (0 == strncmp(
		buf.data() + elems[(elem_pos - 2) & 3].first, "bin", 3
	));

	if (good_layout) {
		if (elem_pos > 2)
			env.yzr_dir.assign(
				buf.data(),
				elems[(elem_pos - 3) & 3].first
				+ elems[(elem_pos - 3) & 3].second
			);
		else
			env.yzr_dir = env.work_dir;
	} else
		throw error("%s: invalid yzr directory layout", buf.data());
}

void work_dir(app_env &env, std::vector<char> &buf) {
	while (true) {
		auto wd(getcwd(buf.data(), buf.size()));
		if (wd) {
			env.work_dir.assign(wd);
			break;
		} else {
			if (errno != ERANGE)
				throw error(
					"error getting currend working dir: "
					"%s", strerror(errno)
				);

			buf.resize(buf.size() << 1);
		}
	}
}

void build_dir(app_env &env, std::vector<char> &buf) {
	if (!env.build_dir.empty()) {
		env.build_dir = fs::absolute(
			env.build_dir, env.work_dir, buf
		);
		return;
	}

	std::string bd(env.work_dir);

	while (!bd.empty()) {
		auto ybd(bd);
		ybd.append("/.yzr");

		if (fs::is_directory(ybd)) {
			env.build_dir.assign(ybd);
			break;
		}

		fs::strip_leaf(bd);
	}
}

void user_home_dir(app_env &env, std::vector<char> &buf) {
	auto pw(getpwuid(geteuid()));
	if (!pw)
		throw error("invalid user data for uid %d", getuid());

	env.user_name.assign(pw->pw_name);

	auto ev(getenv("HOME"));
	if (ev) {
		env.user_home_dir.assign(ev);
		if (fs::is_canonical(env.user_home_dir))
			return;
	}

	env.user_home_dir.assign(pw->pw_dir);
}

void user_data_dir(app_env &env, std::vector<char> &buf) {
	auto ev(getenv("XDG_DATA_HOME"));
	if (ev) {
		env.user_data_dir.assign(ev);
		if (!fs::is_canonical(env.user_data_dir))
			env.user_data_dir.clear();
	}

	if (env.user_data_dir.empty()) {
		env.user_data_dir = env.user_home_dir;
		env.user_data_dir.append("/.local/share/yzr");
	} else
		env.user_data_dir.append("/yzr");

	fs::mkdirs(env.user_data_dir);
}

void user_config_dir(app_env &env, std::vector<char> &buf) {
	auto ev(getenv("XDG_CONFIG_HOME"));
	if (ev) {
		env.user_config_dir.assign(ev);
		if (!fs::is_canonical(env.user_config_dir))
			env.user_config_dir.clear();
	}

	if (env.user_config_dir.empty()) {
		env.user_config_dir = env.user_home_dir;
		env.user_config_dir.append("/.config/yzr");
	} else
		env.user_config_dir.append("/yzr");

	fs::mkdirs(env.user_config_dir);
}

void user_runtime_dir(app_env &env, std::vector<char> &buf) {
	auto ev(getenv("XDG_RUNTIME_DIR"));
	if (ev) {
		env.user_runtime_dir.assign(ev);
		if (fs::is_canonical(env.user_runtime_dir)) {
			env.user_runtime_dir.append("/yzr");
			fs::mkdirs(env.user_runtime_dir);
		} else
			env.user_runtime_dir.clear();
	}
}

void user_runtime_dir_alt(app_env &env, std::vector<char> &buf) {
	if (!env.user_runtime_dir.empty())
		return;

	auto ev(getenv("TMPDIR"));
	if (ev) {
		env.user_runtime_dir.assign(ev);
		if (!fs::is_canonical(env.user_runtime_dir))
			env.user_runtime_dir.clear();
	}

#if defined(P_tmpdir)
	if (env.user_runtime_dir.empty()) {
		env.user_runtime_dir.assign(P_tmpdir);
		if (!fs::is_canonical(env.user_runtime_dir))
			env.user_runtime_dir.clear();
	}
#endif

	if (env.user_runtime_dir.empty())
		env.user_runtime_dir.assign("/tmp");

	env.user_runtime_dir.append("/yzr-");
	env.user_runtime_dir.append(env.user_name);

	fs::mkdirs(env.user_runtime_dir);
}

constexpr std::array env_configurators = {
	yzr_dir,
	work_dir,
	build_dir,
	user_home_dir,
	user_data_dir,
	user_config_dir,
	user_runtime_dir,
	user_runtime_dir_alt,
};

std::string yzr_java_prop(app_env const &env) {
	return env.yzr_dir + "/conf/java.properties";
}

std::string user_java_prop(app_env const &env) {
	return env.user_config_dir + "/java.properties";
}

std::string build_java_prop(app_env const &env) {
	if (!env.build_dir.empty())
		return env.build_dir + "/java.properties";
	else
		return std::string{};
}

constexpr std::array java_property_sources = {
	build_java_prop, user_java_prop, yzr_java_prop
};

}

app_env &app_env::setup() {
	std::vector<char> buf(128);

	for (auto cfg: env_configurators) {
		cfg(*this, buf);
		buf.clear();
		buf.resize(buf.capacity());
	}

	for (auto psrc: java_property_sources) {
		auto p(psrc(*this));
		if (fs::is_regular(p)) {
			java_prop_set.emplace_back(p, properties {});
			java_prop_set.back().second.maybe_append_file(p, buf);
		}
	}

	return *this;
}

}
