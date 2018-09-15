/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

#include "bootstrap.hpp"
#include <string.h>

static char const MAIN_BOOTSTRAP_CLASS[] = "temulg/yzr/podium/Bootstrap";

extern struct ClassData BOOTSTRAP[];

void AppState::startBootstrap() {
	jmethodID h = env->GetMethodID(appClass, "start", "()V");
	if (!h)
		return;

	env->CallObjectMethod(appObj, h);
}

bool AppState::callByteStringSetter(jmethodID h, char const *s) {
	size_t sz = strlen(s);	
	jbyteArray b = env->NewByteArray(sz);
	if (!b)
		return false;

 	env->SetByteArrayRegion(b, 0, sz, (jbyte *)s);
	if (env->ExceptionOccurred())
		return false;

	env->CallVoidMethod(appObj, h, b);
	env->DeleteLocalRef(b);
	return true;
}

void AppState::configureBootstrap(int argc, char **argv) {
	if (!argc) {
		startBootstrap();
		return;
	}

	int pos = 0;

	jmethodID h = env->GetMethodID(appClass, "setProgramName", "([B)V");

	if (h) {
		if (!callByteStringSetter(h, argv[pos++]))
			return;
	}

	h = env->GetMethodID(appClass, "appendArgument", "([B)V");

	if (h) {
		while (pos < argc) {
			if (!callByteStringSetter(h, argv[pos++]))
				return;
		}
	}

	startBootstrap();
}

void AppState::loadBootstrap(int argc, char **argv) {
	if (!classLoader)
		return;

	for(auto cd = &BOOTSTRAP[0]; cd->data; cd++) {
		env->DefineClass(nullptr, classLoader, cd->data, cd->size);
	}

	appClass = env->FindClass(MAIN_BOOTSTRAP_CLASS);
	if (!appClass)
		return;

	jmethodID ctor = env->GetMethodID(appClass, "<init>", "()V");
	if (!ctor)
		return;

	appObj = env->NewObject(appClass, ctor);
	configureBootstrap(argc, argv);
}
