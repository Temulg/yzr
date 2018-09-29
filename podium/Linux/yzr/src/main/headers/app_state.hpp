/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

#if !defined(HPP_E65780A7B5A829DF6D3C5A0F20B6713D)
#define HPP_E65780A7B5A829DF6D3C5A0F20B6713D

#include <vector>
#include <memory>
#include "properties.hpp"
#include <jni.h>

namespace yzr {
namespace detail {

struct dl_closer {
	void operator()(void *handle) const;
};

typedef std::unique_ptr<void, dl_closer> dl_handle;

struct jvm_closer {
	void operator()(JavaVM *jvm) const;
};

typedef std::unique_ptr<JavaVM, jvm_closer> jvm_ptr;

}

struct app_env {
	app_env &inspect_cmd_args(int argc, char **argv);
	app_env &setup();

	std::string yzr_dir;
	std::string work_dir;
	std::string build_dir;

	std::string user_name;
	std::string user_home_dir;
	std::string user_data_dir;
	std::string user_config_dir;
	std::string user_runtime_dir;

	std::vector<std::pair<std::string, properties>> java_prop_set;
};

struct app_state {
	app_state(app_state const &) = delete;
	app_state& operator=(app_state const &) = delete;

	void locate_jvm(app_env const &ae);
	void load_bootstrap(app_env &ae);
	int prepare_and_start(int argc, char **argv);

	detail::dl_handle libjvm_handle;
	detail::jvm_ptr jvm;
	JNIEnv *env;
};

}
#endif
