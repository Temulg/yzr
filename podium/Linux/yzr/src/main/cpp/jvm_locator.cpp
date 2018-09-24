/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

#include "app_state.hpp"
#include <array>
#include <dlfcn.h>

namespace yzr {
namespace detail {

void DlCloser::operator()(void *handle) const {
	dlclose(handle);
}

void JVMCloser::operator()(JavaVM *jvm) const {
	jvm->DestroyJavaVM();
}

}

namespace {

struct JvmLocator {
	virtual ~JvmLocator() {}

	virtual bool hasNext() = 0;
	virtual detail::DlHandle &&next() = 0;
};

typedef std::unique_ptr<JvmLocator> JvmLocatorPtr;
typedef JvmLocatorPtr (*JvmLocatorFactory)();

struct JvmLocatorEnv : JvmLocator {
	static JvmLocatorPtr make() {
		return JvmLocatorPtr(new JvmLocatorEnv());
	}

	bool hasNext() override;

	detail::DlHandle &&next() override {
		return std::move(handle);
	}

private:
	bool done;
	detail::DlHandle handle;
};

static constexpr std::array jvmLocators = {
	&JvmLocatorEnv::make
};

static constexpr jint desiredJvmVersion = JNI_VERSION_10;

static constexpr std::array jvmVariants = {
	"lib/server/libjvm.so",
	"lib/client/libjvm.so",
	"lib/minimal/libjvm.so"
};

}

void AppState::locateJvm() {
	/*
	JavaVMOption options[] = {{
		.optionString = "-Djava.class.path=/usr/lib/java"
	}};
	*/
	JavaVMInitArgs vmArgs {
		.version = desiredJvmVersion,
		.nOptions = 0,
		.options = nullptr,
		.ignoreUnrecognized = JNI_FALSE
	};

	for (auto lf: jvmLocators) {
		auto loc(lf());
		while (loc->hasNext()) {
			auto handle(loc->next());

			void *initArgs(dlsym(
				handle.get(), "JNI_GetDefaultJavaVMInitArgs"
			));

			 if (!initArgs)
				continue;

			auto rc(reinterpret_cast<
				jint (*)(JavaVMInitArgs *)
			>(initArgs)(&vmArgs));

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
			)>(create)(&jvm_, &env_, &vmArgs);

			if (!rc) {
				libJvmHandle = std::move(handle);
				jvm.reset(jvm_);
				env = env_;
				return;
			}
		}
	}
}

namespace {

static detail::DlHandle openLibJvm(std::string const &jvmPath_) {
	detail::DlHandle rv;
	std::error_code ec;
/*
	auto jvmPath(fs::system_complete(jvmPath_, ec));
	if (ec)
		return std::move(rv);

	if (!fs::is_directory(jvmPath, ec))
		return std::move(rv);

	for (auto relp: jvmVariants) {
		auto varp(jvmPath / fs::path(relp));

		if (fs::is_regular_file(varp, ec)) {
			rv.reset(dlopen(varp.c_str(), RTLD_NOW | RTLD_GLOBAL));
			if (rv)
				break;
		}
	}
*/
	return rv;
}

bool JvmLocatorEnv::hasNext() {
	if (done)
		return false;

	done = true;

	auto jhVar(getenv("JAVA_HOME"));
	if (!jhVar)
		return false;

	handle = openLibJvm(jhVar);
	return handle ? true : false;
}

}
}
