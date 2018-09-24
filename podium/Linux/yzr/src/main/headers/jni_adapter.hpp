/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

#if !defined(HPP_2EDD3BEB1E72E1C276F19EFE674B566E)
#define HPP_2EDD3BEB1E72E1C276F19EFE674B566E

#include <jni.h>
#include <array>
#include <cstring>

namespace yzr { namespace jni {

struct MethodInfo {
	char const *name;
	char const *signature;
};

namespace class_def {

template <typename _D = void>
struct ClassLoader {
	static constexpr char const *name = "java/lang/ClassLoader";

	static constexpr std::array<MethodInfo, 0> methods = {};

	static constexpr std::array<MethodInfo, 1> staticMethods = {
		{"getSystemClassLoader", "()Ljava/lang/ClassLoader;"}
	};
};

template <typename _D = void>
struct Inflater {
	static constexpr char const *name = "java/util/zip/Inflater";

	static constexpr std::array<MethodInfo, 5> methods = {{
		{"<init>", "()V"},
		{"setInput", "([B)V"},
		{"inflate", "([B)I"},
		{"reset", "()V"},
		{"end", "()V"}
	}};

	static constexpr std::array<MethodInfo, 0> staticMethods = {};
};

template <typename _D>
constexpr std::array<MethodInfo, 5> Inflater<_D>::methods;

template <typename _D = void>
struct YzrBootstrap {
	static constexpr char const *name = "temulg/yzr/podium/Bootstrap";

	static constexpr std::array<MethodInfo, 4> methods = {{
		{"<init>", "()V"},
		{"setProgramName", "([B)V"},
		{"appendArgument", "([B)V"},
		{"start", "()V"}
	}};

	static constexpr std::array<MethodInfo, 0> staticMethods = {};
};

template <typename _D>
constexpr std::array<MethodInfo, 4> YzrBootstrap<_D>::methods;

}

template <typename ClassDef>
struct JClass {
	JClass(JNIEnv *env_)
	: env(env_), cls(env->FindClass(ClassDef::name)) {
		auto pos(0);
		for (auto const &m: ClassDef::methods) {
			auto jm(env->GetMethodID(cls, m.name, m.signature));
			methods[pos++] = jm;
		}

		pos = 0;
		for (auto const &m: ClassDef::staticMethods) {
			auto jm(env->GetStaticMethodID(cls, m.name, m.signature));
			staticMethods[pos++] = jm;
		}
	}

	~JClass() {
		env->DeleteLocalRef(cls);
	}

	JNIEnv *env;
	jclass cls;
	std::array<jmethodID, ClassDef::methods.size()> methods;
	std::array<jmethodID, ClassDef::staticMethods.size()> staticMethods;

};

struct ByteArray {
	ByteArray(jsize size_, JNIEnv *env_)
	: env(env_), obj(env->NewByteArray(size_)), size(size_) {
	}

	ByteArray(char const *str, JNIEnv *env_)
	: ByteArray(strlen(str), env_) {
		env->SetByteArrayRegion(
			obj, 0, size, reinterpret_cast<jbyte const *>(str)
		);
	}

	ByteArray(jbyte const *data, jsize size_, JNIEnv *env_)
	: ByteArray(size_, env_) {
		env->SetByteArrayRegion(obj, 0, size, data);
	}

	~ByteArray() {
		env->DeleteLocalRef(obj);
	}

	operator jbyteArray() {
		return obj;
	}

	JNIEnv *env;
	jbyteArray obj;
	jsize size;
};

struct ClassLoader {
	typedef JClass<class_def::ClassLoader<>> JClassType;

	static ClassLoader getSystemClassLoader(JClassType const &cls) {
		auto obj(cls.env->CallStaticObjectMethod(
			cls.cls, cls.staticMethods[0]
		));
		return ClassLoader(cls, obj);
	}

	operator jobject() {
		return obj;
	}

	JClassType const &cls;
	jobject obj;

private:
	ClassLoader(JClassType const &cls_, jobject obj_)
	: cls(cls_), obj(obj_) {
	}
};

struct YzrBootstrap {
	typedef JClass<class_def::YzrBootstrap<>> JClassType;

	YzrBootstrap(JClassType const &cls_)
	: cls(cls_), obj(cls.env->NewObject(cls.cls, cls.methods[0])) {
	}

	void setProgramName(jbyteArray src) {
		cls.env->CallVoidMethod(obj, cls.methods[1], src);
	}

	void appendArgument(jbyteArray src) {
		cls.env->CallVoidMethod(obj, cls.methods[2], src);
	}

	void start() {
		cls.env->CallVoidMethod(obj, cls.methods[3]);
	}

	~YzrBootstrap() {
		cls.env->DeleteLocalRef(obj);
	}

	JClassType const &cls;
	jobject obj;
};

struct Inflater {
	typedef JClass<class_def::Inflater<>> JClassType;

	Inflater(JClassType const &cls_)
	: cls(cls_), obj(cls.env->NewObject(cls.cls, cls.methods[0])) {
	}

	void setInput(jbyteArray src) {
		cls.env->CallVoidMethod(obj, cls.methods[1], src);
	}

	jint inflate(jbyteArray dst) {
		return cls.env->CallIntMethod(obj, cls.methods[2], dst);
	}

	void reset() {
		cls.env->CallVoidMethod(obj, cls.methods[3]);
	}

	~Inflater() {
		cls.env->CallVoidMethod(obj, cls.methods[4]);
		cls.env->DeleteLocalRef(obj);
	}

	JClassType const &cls;
	jobject obj;
};

}}

#endif
