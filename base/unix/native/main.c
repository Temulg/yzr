/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 as publi-
 * shed by the Free Software Foundation.
 */

#include "bootstrap.h"
#include <stdio.h>

static jint create_jvm(JavaVM **jvm, JNIEnv **env) {
	/*
	JavaVMOption options[] = {{
		.optionString = "-Djava.class.path=/usr/lib/java"
	}};
	*/
	JavaVMInitArgs vm_args = {
		.version = JNI_VERSION_9,
		.nOptions = 0,
		.options = NULL,//options,
		.ignoreUnrecognized = JNI_FALSE
	};

	return JNI_CreateJavaVM(jvm, (void **)env, &vm_args);
}

static void obtain_class_loader(struct app_state *app) {
	jclass cls = (*app->env)->FindClass(
		app->env, "java/lang/ClassLoader"
	);
	if (!cls)
		return;

	jmethodID getter = (*app->env)->GetStaticMethodID(
		app->env, cls, "getSystemClassLoader",
		"()Ljava/lang/ClassLoader;"
	);
	if (!getter)
		return;

	app->class_loader = (*app->env)->CallStaticObjectMethod(
		app->env, cls, getter
	);
}

int main(int argc, char **argv) {
	JavaVM *jvm;
	struct app_state app = {0};

	jint rc = create_jvm(&jvm, &app.env);
	if (rc < 0) {
		fprintf(stderr, "Error initializing JVM (%x)\n", rc);
		return -1;
	}

	obtain_class_loader(&app);

	load_bootstrap(&app, argc, argv);

	rc = (*jvm)->DestroyJavaVM(jvm);
	if (rc < 0) {
		fprintf(stderr, "Error destroying JVM (%x)\n", rc);
	}

	return 0;
}
