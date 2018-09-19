/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

#include "app_state.hpp"

namespace yzr {

namespace bootstrap {

void forEachItem(void *userData, void (*cons)(
	void *userData, jbyte const *data, jsize compSize, jsize size
));

}

void AppState::loadBootstrap(JNIEnv *env, int argc, char **argv) {
	jni::ClassLoader::JClassType clCls(env);
	auto cl(jni::ClassLoader::getSystemClassLoader(clCls));

	{
		typedef std::pair<
			jni::ClassLoader &, jni::Inflater &
		> CbClosureType;

		jni::Inflater::JClassType infCls(env);
		jni::Inflater inf(infCls);
		CbClosureType pack(cl, inf);

		bootstrap::forEachItem(&pack, [](
			void *userData, jbyte const *data, jsize compSize,
			jsize size
		) {
			auto pack(reinterpret_cast<CbClosureType *>(
				userData
			));
			auto &cl(pack->first);
			auto &inf(pack->second);
			auto env(inf.cls.env);

			jni::ByteArray bIn(
				data, compSize, env
			), bOut(size, env);

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

	yb.start();
}

}
