/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 3 as publi-
 * shed by the Free Software Foundation.
 */

#include "bootstrap.h"
#include <string.h>

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

static void start_bootstrap(struct app_state *app) {
	jmethodID h = (*app->env)->GetMethodID(
		app->env, app->app_class, "start", "()V"
	);
	if (!h)
		return;

	(*app->env)->CallObjectMethod(app->env, app->app_obj, h);
}

static int callByteStringSetter(
	struct app_state *app, jmethodID h, char const *s
) {
	size_t sz = strlen(s);	
	jbyteArray b =(*app->env)->NewByteArray(app->env, sz);
	if (!b)
		return 0;

 	(*app->env)->SetByteArrayRegion(app->env, b, 0, sz, (jbyte *)s);
	if ((*app->env)->ExceptionOccurred(app->env))
		return 0;

	(*app->env)->CallVoidMethod(app->env, app->app_obj, h, b);
	(*app->env)->DeleteLocalRef(app->env, b);
	return 1;
}

static void configure_bootstrap(struct app_state *app, int argc, char **argv) {
	if (!argc) {
		start_bootstrap(app);
		return;
	}

	int pos = 0;

	jmethodID h = (*app->env)->GetMethodID(
		app->env, app->app_class, "setProgramName", "([B)V"
	);

	if (h) {
		if (!callByteStringSetter(app, h, argv[pos++]))
			return;
	}

	h = (*app->env)->GetMethodID(
		app->env, app->app_class, "appendArgument", "([B)V"
	);

	if (h) {
		while (pos < argc) {
			if (!callByteStringSetter(app, h, argv[pos++]))
				return;
		}
	}

	start_bootstrap(app);
}

void load_bootstrap(
	struct app_state *app, int argc, char **argv
) {
	if (!app->class_loader)
		return;

	for(struct class_data *cd = &bootstrap[0]; cd->name; cd++) {
		jclass cls = (*app->env)->DefineClass(
			app->env, cd->name, app->class_loader, cd->data,
			cd->data_size
		);
		if (cd->name == main_bootstrap_class)
			app->app_class = cls;
	}

	if (!app->app_class)
		return;

	jmethodID ctor = (*app->env)->GetMethodID(
		app->env, app->app_class, "<init>", "()V"
	);
	if (!ctor)
		return;

	app->app_obj = (*app->env)->NewObject(app->env, app->app_class, ctor);
	configure_bootstrap(app, argc, argv);
}
