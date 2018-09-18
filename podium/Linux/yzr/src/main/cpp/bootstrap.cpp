/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

#include "bootstrap.hpp"
#include <cstring>

namespace yzr {

static char const MAIN_BOOTSTRAP_CLASS[] = "temulg/yzr/podium/Bootstrap";

namespace bootstrap {

void forEachItem(void *userData, void (*cons)(
	void *userData, jbyte const *data, jsize compSize, jsize size
));

}

struct Inflater {
	Inflater(AppState &as_)
	: as(as_),
	infClass(as.env->FindClass("java/util/zip/Inflater")),
	inf(as.env->NewObject(infClass, as.env->GetMethodID(
		infClass, "<init>", "()V"
	))),
	h_setInput(as.env->GetMethodID(infClass, "setInput", "([B)V")),
	h_inflate(as.env->GetMethodID(infClass, "inflate", "([B)I")),
	h_reset(as.env->GetMethodID(infClass, "reset", "()V")) {}

	~Inflater() {
		as.env->CallVoidMethod(
			inf, as.env->GetMethodID(infClass, "end", "()V")
		);
	}

	void loadClass(jbyte const *data, jsize compSize, jsize size) {
		auto bIn(as.env->NewByteArray(compSize));
		as.env->SetByteArrayRegion(bIn, 0, compSize, data);
		as.env->CallVoidMethod(inf, h_setInput, bIn);
		as.env->DeleteLocalRef(bIn);

		auto bOut(as.env->NewByteArray(size));
		auto len(as.env->CallIntMethod(inf, h_inflate, bOut));
		printf("-- inf %p, %d\n", data, len);
		as.env->ExceptionDescribe();

		auto outBytes(as.env->GetByteArrayElements(bOut, nullptr));

		auto cls(as.env->DefineClass(
			nullptr, as.classLoader, outBytes, size
		));
		printf("-- class %p, size %ld\n", cls, size);

		as.env->ReleaseByteArrayElements(bOut, outBytes, JNI_ABORT);
		as.env->DeleteLocalRef(bOut);

		as.env->CallVoidMethod(inf, h_reset);
	}

	AppState &as;
	jclass infClass;
	jobject inf;
	jmethodID h_setInput;
	jmethodID h_inflate;
	jmethodID h_reset;
};

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

	{
		Inflater inf(*this);

		bootstrap::forEachItem(&inf, [](
			void *userData, jbyte const *data, jsize compSize,
			jsize size
		) {
			reinterpret_cast<Inflater *>(userData)->loadClass(
				data, compSize, size
			);
		});
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

}
