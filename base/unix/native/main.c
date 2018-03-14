/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 as publi-
 * shed by the Free Software Foundation.
 */

#include <jni.h>
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

static jobject obtain_class_loader(JNIEnv *env) {
	jclass cls = (*env)->FindClass(env, "java/lang/ClassLoader");
	if (!cls)
		return NULL;

	jmethodID getter = (*env)->GetStaticMethodID(
		env, cls, "getSystemClassLoader", "()Ljava/lang/ClassLoader;"
	);
	if (!getter)
		return NULL;

	return (*env)->CallStaticObjectMethod(env, cls, getter);
}

void load_bootstrap(JNIEnv *env, jobject class_loader);

int main(int argc, char **argv) {
	JavaVM *jvm;
	JNIEnv *env;

	jint rc = create_jvm(&jvm, &env);
	if (rc < 0) {
		fprintf(stderr, "Error initializing JVM (%x)\n", rc);
		return -1;
	}

	jobject loader = obtain_class_loader(env);

	load_bootstrap(env, loader);

	rc = (*jvm)->DestroyJavaVM(jvm);
	if (rc < 0) {
		fprintf(stderr, "Error destroying JVM (%x)\n", rc);
	}

	return 0;
}
