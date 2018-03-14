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

static jobject convert_argv(JNIEnv *env, int argc, char **argv) {
	jclass cls = (*env)->FindClass(env, "java/lang/String");
	if (!cls)
		return NULL;

	jobject args = (*env)->NewObjectArray(env, argc, cls, NULL);
	if (!args)
		return NULL;

	for (int pos = 0; pos < argc; pos++) {
		jobject val = (*env)->NewStringUTF(env, argv[pos]);

		(*env)->SetObjectArrayElement(
			env, args, pos, val
		);
	}

	return args;
}

void load_bootstrap(JNIEnv *env, jobject class_loader, jobject args);

int main(int argc, char **argv) {
	JavaVM *jvm;
	JNIEnv *env;

	jint rc = create_jvm(&jvm, &env);
	if (rc < 0) {
		fprintf(stderr, "Error initializing JVM (%x)\n", rc);
		return -1;
	}

	jobject loader = obtain_class_loader(env);

	load_bootstrap(env, loader, convert_argv(env, argc, argv));

	rc = (*jvm)->DestroyJavaVM(jvm);
	if (rc < 0) {
		fprintf(stderr, "Error destroying JVM (%x)\n", rc);
	}

	return 0;
}
