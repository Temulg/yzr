/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 as publi-
 * shed by the Free Software Foundation.
 */

#if !defined(H_E65780A7B5A829DF6D3C5A0F20B6713D)
#define H_E65780A7B5A829DF6D3C5A0F20B6713D

#include <jni.h>

struct ClassData {
	jbyte const *data;
	jsize size;
	jsize compSize;
};

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

#endif
