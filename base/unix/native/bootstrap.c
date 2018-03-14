/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 as publi-
 * shed by the Free Software Foundation.
 */

#include <jni.h>
#include <stdio.h>

static char const main_bootstrap_class[] = "yzr/base/UnixBootstrap";

struct class_data {
	char const *name;
	jbyte const *data;
	jsize data_size;
};

static struct class_data bootstrap[] = {{
	main_bootstrap_class,
	#include "UnixBootstrap_data.h"
}, {
	NULL, NULL, 0
}};

void load_bootstrap(JNIEnv *env, jobject class_loader, jobject args) {
	jclass main_class = NULL;

	for(struct class_data *cd = &bootstrap[0]; cd->name; cd++) {
		jclass cls = (*env)->DefineClass(
			env, cd->name, class_loader, cd->data, cd->data_size
		);
		if (cd->name == main_bootstrap_class)
			main_class = cls;
	}

	jmethodID main = (*env)->GetStaticMethodID(
		env, main_class, "main", "([Ljava/lang/String;)V"
	);

	(*env)->CallStaticVoidMethod(env, main_class, main, args);
}
