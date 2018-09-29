/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

#include "app_state.hpp"
#include "path_utils.hpp"

#include <array>
#include <algorithm>
#include <dlfcn.h>

namespace yzr {
namespace detail {

void dl_closer::operator()(void *handle) const {
	dlclose(handle);
}

void jvm_closer::operator()(JavaVM *jvm) const {
	jvm->DestroyJavaVM();
}

}

namespace {

std::string jvm_from_property(app_env const &env, std::vector<char> &buf) {
	for (auto const &pp: env.java_prop_set) {
		auto [val, ok] = pp.second.get("temulg.yzr.java.home");
		if (ok)
			return fs::absolute(val, pp.first, buf);
	}
	return std::string {};
}

std::string jvm_from_os_env(app_env const &env, std::vector<char> &buf) {
	auto ev(getenv("JAVA_HOME"));
	if (ev)
		return fs::absolute(std::string(ev), env.work_dir, buf);
	else
		return std::string {};
}

constexpr std::array jvm_locators = {
	jvm_from_property, jvm_from_os_env
};

constexpr jint desired_jvm_version = JNI_VERSION_10;

constexpr std::array jvm_variants = {
	"/lib/server/libjvm.so",
	"/lib/client/libjvm.so",
	"/lib/minimal/libjvm.so"
};

detail::dl_handle open_libjvm(
	std::string const &jvm_path, std::vector<char> &buf
) {
	detail::dl_handle rv;

	if (jvm_path.empty())
		return rv;

	buf.clear();
	std::copy(jvm_path.begin(), jvm_path.end(), std::back_inserter(buf));
	auto prefix(buf.size());

	for (auto relp: jvm_variants) {
		std::copy_n(relp, strlen(relp), std::back_inserter(buf));
		buf.push_back(0);

		if (fs::is_regular(buf)) {
			rv.reset(dlopen(buf.data(), RTLD_NOW | RTLD_GLOBAL));
			if (rv)
				break;
		}
		buf.resize(prefix);
	}

	return rv;
}

}

void app_state::locate_jvm(app_env const &ae) {
	/*
	JavaVMOption options[] = {{
		.optionString = "-Djava.class.path=/usr/lib/java"
	}};
	*/
	JavaVMInitArgs vm_args {
		.version = desired_jvm_version,
		.nOptions = 0,
		.options = nullptr,
		.ignoreUnrecognized = JNI_FALSE
	};
	std::vector<char> buf(128);

	for (auto lf: jvm_locators) {
		auto handle(open_libjvm(lf(ae, buf), buf));
		if (!handle)
			continue;

		void *init_args(dlsym(
			handle.get(), "JNI_GetDefaultJavaVMInitArgs"
		));

		 if (!init_args)
			continue;

		auto rc(reinterpret_cast<
			jint (*)(JavaVMInitArgs *)
		>(init_args)(&vm_args));

		if (rc < 0)
			continue;

		void *create(dlsym(
			handle.get(), "JNI_CreateJavaVM"
		));

		if (!create)
			continue;

		JavaVM *jvm_;
		JNIEnv *env_;
		// extend args

		rc = reinterpret_cast<jint (*)(
			JavaVM **, JNIEnv **, JavaVMInitArgs *
		)>(create)(&jvm_, &env_, &vm_args);

		if (!rc) {
			libjvm_handle = std::move(handle);
			jvm.reset(jvm_);
			env = env_;
			return;
		}
	}

	throw usage_error(
		"unable to locate an usable Java virtual machine "
		"(needs version %d or above)", desired_jvm_version
	);
}
}
