/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

#include "bootstrap.hpp"
#include <stdio.h>

namespace yzr {

jint AppState::createJvm(JavaVM **jvm) {
	/*
	JavaVMOption options[] = {{
		.optionString = "-Djava.class.path=/usr/lib/java"
	}};
	*/
	JavaVMInitArgs vmArgs = {
		.version = JNI_VERSION_10,
		.nOptions = 0,
		.options = NULL,//options,
		.ignoreUnrecognized = JNI_FALSE
	};

	return JNI_CreateJavaVM(
		jvm, reinterpret_cast<void **>(&env), &vmArgs
	);
}

void AppState::obtainClassLoader() {
	jclass cls = env->FindClass("java/lang/ClassLoader");
	if (!cls)
		return;

	jmethodID getter = env->GetStaticMethodID(
		cls, "getSystemClassLoader", "()Ljava/lang/ClassLoader;"
	);
	if (!getter)
		return;

	classLoader = env->CallStaticObjectMethod(cls, getter);
}

}

int main(int argc, char **argv) {
	JavaVM *jvm;
	yzr::AppState app;

	jint rc = app.createJvm(&jvm);
	if (rc < 0) {
		fprintf(stderr, "Error initializing JVM (%x)\n", rc);
		return rc;
	}

	app.obtainClassLoader();

	app.loadBootstrap(argc, argv);

	rc = jvm->DestroyJavaVM();
	if (rc < 0) {
		fprintf(stderr, "Error destroying JVM (%x)\n", rc);
		return rc;
	}

	return 0;
}
