/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 as publi-
 * shed by the Free Software Foundation.
 */

#include "bootstrap.hpp"
#include <stdio.h>

jint app_state::create_jvm(JavaVM **jvm) {
	/*
	JavaVMOption options[] = {{
		.optionString = "-Djava.class.path=/usr/lib/java"
	}};
	*/
	JavaVMInitArgs vm_args = {
		.version = JNI_VERSION_10,
		.nOptions = 0,
		.options = NULL,//options,
		.ignoreUnrecognized = JNI_FALSE
	};

	return JNI_CreateJavaVM(
		jvm, reinterpret_cast<void **>(&env), &vm_args
	);
}

void app_state::obtain_class_loader() {
	jclass cls = env->FindClass("java/lang/ClassLoader");
	if (!cls)
		return;

	jmethodID getter = env->GetStaticMethodID(
		cls, "getSystemClassLoader", "()Ljava/lang/ClassLoader;"
	);
	if (!getter)
		return;

	class_loader = env->CallStaticObjectMethod(cls, getter);
}

int main(int argc, char **argv) {
	JavaVM *jvm;
	app_state app;

	jint rc = app.create_jvm(&jvm);
	if (rc < 0) {
		fprintf(stderr, "Error initializing JVM (%x)\n", rc);
		return rc;
	}

	app.obtain_class_loader();

	//load_bootstrap(&app, argc, argv);

	rc = jvm->DestroyJavaVM();
	if (rc < 0) {
		fprintf(stderr, "Error destroying JVM (%x)\n", rc);
		return rc;
	}

	return 0;
}
