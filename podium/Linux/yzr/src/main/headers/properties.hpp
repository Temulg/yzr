/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

#if !defined(HPP_DBF6F64A1D02C583701EE43033871E08)
#define HPP_DBF6F64A1D02C583701EE43033871E08

#include <string>
#include <vector>
#include <unordered_map>

namespace yzr {

struct properties {
	struct key_hash;
	struct key_equals;

	struct key_type {
		key_type(std::string const &name_)
		: name(name_), hash(std::hash<std::string>{}(name_)) {}

		friend struct properties::key_hash;
		friend struct properties::key_equals;

		operator std::string() const {
			return name;
		}

		char const *data() const {
			return name.data();
		}

	private:
		std::string name;
		size_t hash;
	};

	typedef std::string value_type;

	bool empty() const {
		return values.empty();
	}

	std::pair<std::string, bool> get(std::string const &key) const {
		auto iter(values.find(key_type(key)));
		if (iter != values.end())
			return std::make_pair(iter->second, true);
		else
			return std::make_pair(std::string {}, false);
	}

	void maybe_append_file(std::string const &p, std::vector<char> &buf);

	struct key_hash {
		size_t operator()(key_type const &key) const {
			return key.hash;
		}
	};

	struct key_equals {
		bool operator()(
			key_type const &l, key_type const &r
		) const {
			return l.name == r.name;
		}
	};

	std::unordered_map<key_type, value_type, key_hash, key_equals> values;
};

}
#endif
