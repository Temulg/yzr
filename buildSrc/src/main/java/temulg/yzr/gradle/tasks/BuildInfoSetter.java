/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.gradle.tasks;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

import org.gradle.api.Task;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

public class BuildInfoSetter extends DefaultTask {
	public BuildInfoSetter() throws IOException {
		getOutputs().upToDateWhen(BuildInfoSetter::upToDate);
		gitData = GitData.fromGit(
			getProject().getRootDir().toPath()
		);
	}

	@OutputDirectory
	DirectoryProperty getDestDir() {
		return destinationDirectory;
	}

	private static boolean upToDate(Task task_) {
		try {
			var task = (BuildInfoSetter)task_;
			var biFile = task.getBuildInfoPath();

			if (!Files.isRegularFile(biFile))
				return false;

			var gd = GitData.fromBuildInfo(biFile);
			return gd.equals(task.gitData);
		} catch (IOException e) {
			throw new GradleException(
				"Error accessing native build info file", e
			);
		}
	}

	private Path getBuildInfoPath() {
		return destinationDirectory.getAsFile().get().toPath().resolve(
			"build_info.hpp"
		);
	}

	@TaskAction
	public void process() throws IOException {
		var biFile = getBuildInfoPath();

		try (var out = Files.newBufferedWriter(biFile)) {
			appendHeader(out);

			out.write(
				"\tstatic constexpr char const "
				+ "*build_ref_name = \""
			);
			out.write(gitData.refName);
			out.write("\";");
			out.newLine();

			out.write(
				"\tstatic constexpr char const "
				+ "*build_ref_id = \""
			);
			out.write(gitData.refId);
			out.write("\";");
			out.newLine();

			appendFooter(out);
		}
	}

	private void appendHeader(BufferedWriter out) throws IOException {
		var rnd  = ThreadLocalRandom.current();
		var guard = (
			Long.toHexString(rnd.nextLong()) + 
			Long.toHexString(rnd.nextLong())
		).toUpperCase();

		out.write("#if !defined(HPP_");
		out.write(guard);
		out.write(')');
		out.newLine();
		out.write("#define HPP_");
		out.write(guard);
		out.newLine();
		out.newLine();
		out.write("namespace yzr {");
		out.newLine();
		out.newLine();
		out.write("template <typename _D = void>");
		out.newLine();
		out.write("struct build_info {");
		out.newLine();
	}

	private void appendFooter(BufferedWriter out) throws IOException {
		out.write("};");
		out.newLine();
		out.newLine();
		out.write('}');
		out.newLine();
		out.write("#endif");
		out.newLine();
	}

	private static class GitData {
		static GitData fromGit(Path wd) throws IOException {
			var head = new String(
				Files.readAllBytes(wd.resolve(".git/HEAD")),
				StandardCharsets.UTF_8
			);
			var m = GIT_HEAD_REF.matcher(head);
			var refName = m.matches() ? m.group(1) : "";

			var refId = new String(
				Files.readAllBytes(
					wd.resolve(".git/" + refName)
				), StandardCharsets.UTF_8
			).trim();

			return new GitData(refName, refId);
		}

		static GitData fromBuildInfo(Path biFile) throws IOException {
			var bi = new String(
				Files.readAllBytes(biFile),
				StandardCharsets.UTF_8
			);

			String refName = "", refId = "";

			var m0 = BI_REF_NAME.matcher(bi);
			if (m0.find()) {
				refName = m0.group(1);

				var m1 = BI_REF_ID.matcher(bi);
				if (m1.find(m0.end(1)))
					refId = m1.group(1);
			}
			return new GitData(refName, refId);
		}

		private GitData(String refName_, String refId_) {
			refName = refName_;
			refId = refId_;
		}

		@Override
		public boolean equals(Object other_) {
			if (other_ == this)
				return true;

			if (other_ instanceof GitData) {
				var other = (GitData)other_;
				return refName.equals(
					other.refName
				) && refId.equals(other.refId);
			} else
				return false;
		}

		final String refName;
		final String refId;
	}

	private static final Pattern GIT_HEAD_REF = Pattern.compile(
		"^ref:\\s+(.+)\\s*$"
	);
	private static final Pattern BI_REF_NAME = Pattern.compile(
		"build_ref_name = \"(.+)\""
	);
	private static final Pattern BI_REF_ID = Pattern.compile(
		"build_ref_id = \"(.+)\""
	);

	private final DirectoryProperty destinationDirectory
	= getProject().getObjects().directoryProperty();
	private final GitData gitData;
}
