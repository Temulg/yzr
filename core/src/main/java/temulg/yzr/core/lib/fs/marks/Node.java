/*
 * Copyright (c) 2018-2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.core.lib.fs.marks;

import java.nio.file.FileSystems;
import java.nio.file.Path;

import temulg.yzr.core.Entity;
import temulg.yzr.core.Mark;

public class Node extends Entity implements Mark {
	public static Node of(String path) {
		return new Node(FileSystems.getDefault().getPath(path));
	}

	private Node(Path path_) {
		path = path_;
	}

	private final Path path;
}
