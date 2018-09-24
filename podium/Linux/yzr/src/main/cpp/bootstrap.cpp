/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

#include "app_state.hpp"
#include "jni_adapter.hpp"
#include <functional>

namespace yzr {

namespace bootstrap {

void forEachItem(std::function<
	void (jbyte const *data, jsize compSize, jsize size)
> cons);

}

void AppState::loadBootstrap(AppEnv const &appEnv) {
	jni::ClassLoader::JClassType clCls(env);
	auto cl(jni::ClassLoader::getSystemClassLoader(clCls));

	{
		jni::Inflater::JClassType infCls(env);
		jni::Inflater inf(infCls);

		bootstrap::forEachItem([&cl, &inf] (
			jbyte const *data, jsize compSize, jsize size
		) {
			auto env(inf.cls.env);

			jni::ByteArray bIn(data, compSize, env);
			jni::ByteArray bOut(size, env);

			inf.setInput(bIn);
			inf.inflate(bOut);
			inf.reset();

			auto outBytes(
				env->GetByteArrayElements(bOut, nullptr)
			);
			env->DefineClass(nullptr, cl, outBytes, size);
			env->ReleaseByteArrayElements(
				bOut, outBytes, JNI_ABORT
			);
		});
	}

	jni::YzrBootstrap::JClassType ybCls(env);
	jni::YzrBootstrap yb(ybCls);
/*
	if (!argc) {
		yb.start();
		return;
	}

	int pos = 0;
	{
		jni::ByteArray b(argv[pos++], env);
		yb.setProgramName(b);
	}

	while (pos < argc) {
		jni::ByteArray b(argv[pos++], env);
		yb.appendArgument(b);
	}
*/
	yb.start();
}

}
