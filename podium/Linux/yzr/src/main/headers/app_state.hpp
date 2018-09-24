/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

#if !defined(HPP_E65780A7B5A829DF6D3C5A0F20B6713D)
#define HPP_E65780A7B5A829DF6D3C5A0F20B6713D

#include <memory>
#include <experimental/filesystem>

#include <jni.h>

namespace yzr {
namespace detail {

struct DlCloser {
	void operator()(void *handle) const;
};

typedef std::unique_ptr<void, DlCloser> DlHandle;

struct JVMCloser {
	void operator()(JavaVM *jvm) const;
};

typedef std::unique_ptr<JavaVM, JVMCloser> JvmPtr;

}

struct Error : std::exception {
	Error(char const *format, ...);

	~Error() {
		free(msg);
	}

	char const *what() const noexcept override {
		return msg;
	}
protected:
	Error() {
	}

	char *msg;
};

struct UsageError : Error {
	UsageError(char const *format, ...);
};

struct AppEnv {
	bool inspectCmdArgs(int argc, char **argv);
	void setup();

	std::string yzrDir;
	std::string workDir;
	std::string buildDir;
	std::vector<std::string> configDirs;
};

struct AppState {
	AppState(AppState const &) = delete;
	AppState& operator=(AppState const &) = delete;

	void locateJvm();
	void loadBootstrap(AppEnv const &appEnv);

	detail::DlHandle libJvmHandle;
	detail::JvmPtr jvm;
	JNIEnv *env;
};

}
#endif
