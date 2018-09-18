/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

#if !defined(HPP_E65780A7B5A829DF6D3C5A0F20B6713D)
#define HPP_E65780A7B5A829DF6D3C5A0F20B6713D

#include <jni.h>

namespace yzr {

struct AppState {
	jint createJvm(JavaVM **jvm);

	void obtainClassLoader();
	void loadBootstrap(int argc, char **argv);
	void configureBootstrap(int argc, char **argv);
	void startBootstrap();
	bool callByteStringSetter(jmethodID h, char const *s);

	JNIEnv *env;
	jobject classLoader;
	jclass appClass;
	jobject appObj;
};

}
#endif
