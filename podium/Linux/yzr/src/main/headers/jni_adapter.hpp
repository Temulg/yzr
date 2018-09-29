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

	static constexpr std::array<MethodInfo, 0> constructors = {};

	static constexpr std::array<MethodInfo, 0> methods = {};

	static constexpr std::array<MethodInfo, 1> staticMethods = {{
		{"getSystemClassLoader", "()Ljava/lang/ClassLoader;"}
	}};
};

template <typename _D = void>
struct System {
	static constexpr char const *name = "java/lang/System";

	static constexpr std::array<MethodInfo, 0> constructors = {};

	static constexpr std::array<MethodInfo, 0> methods = {};

	static constexpr std::array<MethodInfo, 1> staticMethods = {{
		{
			"setProperty",
			"(Ljava/lang/String;Ljava/lang/String;)"
			"Ljava/lang/String;"
		}
	}};
};

template <typename _D = void>
struct String {
	static constexpr char const *name = "java/lang/String";

	static constexpr std::array<MethodInfo, 0> constructors = {};

	static constexpr std::array<MethodInfo, 0> methods = {};

	static constexpr std::array<MethodInfo, 0> staticMethods = {};
};

template <typename _D = void>
struct Inflater {
	static constexpr char const *name = "java/util/zip/Inflater";

	static constexpr std::array<MethodInfo, 1> constructors = {{
		{"<init>", "()V"}
	}};

	static constexpr std::array<MethodInfo, 4> methods = {{
		{"setInput", "([B)V"},
		{"inflate", "([B)I"},
		{"reset", "()V"},
		{"end", "()V"}
	}};

	static constexpr std::array<MethodInfo, 0> staticMethods = {};
};

template <typename _D = void>
struct YzrBootstrap {
	static constexpr char const *name = "temulg/yzr/podium/Bootstrap";

	static constexpr std::array<MethodInfo, 1> constructors = {{
		{"<init>", "()V"}
	}};

	static constexpr std::array<MethodInfo, 2> methods = {{
		{"prepare", "([Ljava/lang/String;)V"},
		{"start", "()V"}
	}};

	static constexpr std::array<MethodInfo, 0> staticMethods = {};
};
}

template <typename ClassDef>
struct JClass {
	JClass(JNIEnv *env_)
	: env(env_), cls(env->FindClass(ClassDef::name)) {
		auto pos(0);
		for (auto const &m: ClassDef::constructors) {
			auto jm(env->GetMethodID(cls, m.name, m.signature));
			constructors[pos++] = jm;
		}

		pos = 0;
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
	std::array<jmethodID, ClassDef::constructors.size()> constructors;
	std::array<jmethodID, ClassDef::methods.size()> methods;
	std::array<jmethodID, ClassDef::staticMethods.size()> staticMethods;

};

struct String {
	typedef JClass<class_def::String<>> JClassType;

	template <typename StringType>
	String(StringType const &str, JNIEnv *env_)
	: env(env_), obj(env->NewStringUTF(str.data())) {}

	String(char *const str, JNIEnv *env_)
	: env(env_), obj(env->NewStringUTF(str)) {}

	String(jstring obj_, JNIEnv *env_)
	: env(env_), obj(obj_) {}

	~String() {
		env->DeleteLocalRef(obj);
	}

	operator jstring() {
		return obj;
	}

	JNIEnv *env;
	jstring obj;
};

struct StringArray {
	StringArray(String::JClassType const &cls_, jsize size_)
	: cls(cls_), obj(cls.env->NewObjectArray(size_, cls.cls, nullptr)) {
	}

	void set(jsize pos, jstring val) {
		cls.env->SetObjectArrayElement(obj, pos, val);
	}

	operator jobjectArray() const {
		return obj;
	}

	typename String::JClassType const &cls;
	jobjectArray obj;
	jsize size;
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


struct System {
	typedef JClass<class_def::System<>> JClassType;

	static String setProperty(
		JClassType const &cls, String const &key, String const &value
	) {
		auto obj(cls.env->CallStaticObjectMethod(
			cls.cls, cls.staticMethods[0], key.obj, value.obj
		));
		return String(static_cast<jstring>(obj), cls.env);
	}

	operator jobject() {
		return obj;
	}

	JClassType const &cls;
	jobject obj;
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
	: cls(cls_), obj(cls.env->NewObject(cls.cls, cls.constructors[0])) {
	}

	void prepare(StringArray const &sa) {
		cls.env->CallVoidMethod(obj, cls.methods[0], sa);
	}

	void start() {
		cls.env->CallVoidMethod(obj, cls.methods[1]);
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
	: cls(cls_), obj(cls.env->NewObject(cls.cls, cls.constructors[0])) {
	}

	void setInput(jbyteArray src) {
		cls.env->CallVoidMethod(obj, cls.methods[0], src);
	}

	jint inflate(jbyteArray dst) {
		return cls.env->CallIntMethod(obj, cls.methods[1], dst);
	}

	void reset() {
		cls.env->CallVoidMethod(obj, cls.methods[2]);
	}

	~Inflater() {
		cls.env->CallVoidMethod(obj, cls.methods[3]);
		cls.env->DeleteLocalRef(obj);
	}

	JClassType const &cls;
	jobject obj;
};

}}

#endif
