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

void for_each_item(std::function<
	void (jbyte const *data, jsize comp_size, jsize size)
> cons);

}

void app_state::load_bootstrap(app_env &ae) {
	jni::ClassLoader::JClassType clCls(env);
	auto cl(jni::ClassLoader::getSystemClassLoader(clCls));

	jni::Inflater::JClassType infCls(env);
	jni::Inflater inf(infCls);

	bootstrap::for_each_item([&cl, &inf] (
		jbyte const *data, jsize compSize, jsize size
	) {
		auto env(inf.cls.env);

		jni::ByteArray bIn(data, compSize, env);
		jni::ByteArray bOut(size, env);

		inf.setInput(bIn);
		inf.inflate(bOut);
		inf.reset();

		auto outBytes(env->GetByteArrayElements(bOut, nullptr));
		env->DefineClass(nullptr, cl, outBytes, size);
		env->ReleaseByteArrayElements(
			bOut, outBytes, JNI_ABORT
		);
	});

	jni::System::JClassType sysCls(env);
	if (!ae.java_prop_set.empty()) {
		auto iter(ae.java_prop_set.begin());
		auto props(std::move(iter->second));
		for (++iter; iter != ae.java_prop_set.end(); ++iter)
			props.values.merge(iter->second.values);

		ae.java_prop_set.clear();

		for (auto const &item: props.values)
			jni::System::setProperty(
				sysCls,
				jni::String(item.first, env),
				jni::String(item.second, env)
			);

	}
}

int app_state::prepare_and_start(int argc, char **argv) {
	jni::YzrBootstrap::JClassType ybCls(env);
	jni::YzrBootstrap yb(ybCls);

	{
		jni::String::JClassType stringCls(env);
		jni::StringArray sa(stringCls, argc);

		for (int pos(0); pos < argc; ++pos)
			sa.set(pos, jni::String(argv[pos], env));

		yb.prepare(sa);
	}

	yb.start();
	return 0;
}

}
