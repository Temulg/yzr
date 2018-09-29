/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

#if !defined(HPP_ABBDA72939DC9589E8A9C7DA00FEAE71)
#define HPP_ABBDA72939DC9589E8A9C7DA00FEAE71

#include "error.hpp"
#include "string_utils.hpp"
#include <cstring>
#include <fcntl.h>
#include <unistd.h>
#include <sys/stat.h>

namespace yzr::fs {

template <typename StringType>
bool is_canonical(StringType const &p) {
	enum {
		INITIAL,
		SEP,
		DOT,
		DOTDOT,
		ANY
	} state(INITIAL);

	for (auto ch: p) {
		switch (state) {
		case INITIAL:
			if (ch != '/')
				return false;
			state = SEP;
			break;
		case SEP:
			if (ch == '/')
				return false;
			else if (ch == '.')
				state = DOT;
			else
				state = ANY;
			break;
		case DOT:
			if (ch == '/')
				return false;
			else if (ch == '.')
				state = DOTDOT;
			else
				state = ANY;
			break;
		case DOTDOT:
			if (ch == '/')
				return false;
			else
				state = ANY;
			break;
		case ANY:
			if (ch == '/')
				state = SEP;
			break;
		}
	}
	return (state == ANY) || (state == SEP);
}

template <typename StringType>
bool is_directory(StringType const &p) {
	struct stat sb{};

	auto rc(lstat(p.data(), &sb));
	if (rc < 0)
		return false;

	return S_ISDIR(sb.st_mode);
}

template <typename StringType>
bool is_regular(StringType const &p) {
	struct stat sb{};

	auto rc(lstat(p.data(), &sb));
	if (rc < 0)
		return false;

	return S_ISREG(sb.st_mode);
}

template <typename StringType>
void strip_leaf(StringType &p) {
	enum {
		INITIAL,
		SEP_LAST,
		ANY,
		SEP_FIRST
	} state(INITIAL);

	while (!p.empty()) {
		auto ch(p.back());
		switch (state) {
		case INITIAL:
			state = ch != '/' ? ANY : SEP_LAST;
			break;
		case SEP_LAST:
			if (ch != '/')
				state = ANY;

			break;
		case ANY:
			if (ch == '/')
				state = SEP_FIRST;
			break;
		case SEP_FIRST:
			if (ch != '/')
				return;
			break;
		}
		p.pop_back();
	}
}

template <typename StringType, typename BufferType>
StringType absolute(
	StringType const &p, StringType const &wd, BufferType &buf
) {
	if (p.empty())
		throw error("non empty path must be specified");

	if (is_canonical(p))
		return p;

	buf.clear();
	auto pos(p.begin());
	if (p.front() != '/') {
		if (wd.empty())
			throw error(
				"non empty working directory be specified"
			);

		std::copy(wd.begin(), wd.end(), std::back_inserter(buf));
	} else
		++pos;

	enum {
		SEP,
		DOT,
		DOTDOT,
		ANY
	} state(SEP);

	for (; pos != p.end(); ++pos) {
		auto ch(*pos);

		switch (state) {
		case SEP:
			if (ch == '.')
				state = DOT;
			else if (ch != '/') {
				state = ANY;
				buf.push_back('/');
				buf.push_back(ch);
			}
			break;
		case DOT:
			if (ch == '/')
				state = SEP;
			else if (ch == '.')
				state = DOTDOT;
			else {
				state = ANY;
				buf.push_back('/');
				buf.push_back('.');
				buf.push_back(ch);
			}
			break;
		case DOTDOT:
			if (ch == '/') {
				strip_leaf(buf);
				state = SEP;
			} else {
				state = ANY;
				buf.push_back('/');
				buf.push_back('.');
				buf.push_back('.');
				buf.push_back(ch);
			}
			break;
		case ANY:
			if (ch == '/')
				state = SEP;
			else
				buf.push_back(ch);
		}
	}
	if (state == DOTDOT)
		strip_leaf(buf);

	if (buf.empty())
		return StringType("/", 1);

	buf.push_back(0);

	struct stat sb{};

	auto rc(lstat(buf.data(), &sb));
	if (rc < 0)
		throw error(
			"error accessing %s: %s", buf.data(),
			strerror(errno)
		);

	buf.pop_back();
	StringType next_wd(buf.begin(), buf.end());
	if (!S_ISLNK(sb.st_mode))
		return next_wd;

	buf.resize(buf.capacity());

	if (sb.st_size > buf.size())
		buf.resize(sb.st_size);

	ssize_t sz(0);

	while (true) {
		sz = readlink(next_wd.data(), buf.data(), buf.size());
		if (sz <= 0)
			throw error(
				"error reading %s: %s",
				next_wd.data(), strerror(errno)
			);

		if (sz < buf.size())
			break;
		else
			buf.resize(buf.size() << 1);
	}

	StringType next_p(buf.begin(), buf.begin() + sz);
	strip_leaf(next_wd);

	return absolute(next_p, next_wd, buf);
}

template <typename StringType>
void mkdirs(StringType const &p) {
	if (is_directory(p))
		return;

	auto fd(open("/", O_PATH));
	if (fd < 0)
		throw error("error accessing /: %s", strerror(errno));

	detail::for_each_substring(p, '/', [&fd, p](auto first, auto last) {
		StringType sp(first, last);
		if (0 > mkdirat(fd, sp.data(), 0755)) {
			if (errno != EEXIST) {
				sp.assign(p.begin(), last);
				auto errno_(errno);
				close(fd);
				throw error(
					"error creating directory %s: %s",
					sp.data(), strerror(errno_)
				);
			}
		}
		auto nfd(openat(fd, sp.data(), O_PATH));
		auto errno_(errno);
		close(fd);

		if (nfd < 0) {
			sp.assign(p.begin(), last);
			throw error(
				"error accessing %s: %s",
				sp.data(), strerror(errno_)
			);
		}
		fd = nfd;
	});
	close(fd);
}

}
#endif
