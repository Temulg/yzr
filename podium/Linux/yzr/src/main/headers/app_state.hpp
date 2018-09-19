/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

#if !defined(HPP_E65780A7B5A829DF6D3C5A0F20B6713D)
#define HPP_E65780A7B5A829DF6D3C5A0F20B6713D

#include "jni_adapter.hpp"

namespace yzr {

struct AppState {
	static void loadBootstrap(JNIEnv *env, int argc, char **argv);

	jint createJvm(JavaVM **jvm);

	JNIEnv *env;
};

}
#endif
