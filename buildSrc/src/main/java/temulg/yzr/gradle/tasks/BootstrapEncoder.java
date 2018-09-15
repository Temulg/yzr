/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.gradle.tasks;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileTreeElement;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;

public class BootstrapEncoder extends DefaultTask {
	public BootstrapEncoder() {
		source = getProject().files();
		destinationDirectory = newOutputDirectory();
	}

	@OutputDirectory
	DirectoryProperty getHeadersDir() {
		return destinationDirectory;
	}

	@InputFiles
	public ConfigurableFileCollection getSource() {
		return source;
	}

	public void source(Object source_) {
		source.from(source_);
	}

	@TaskAction
	public void process() throws IOException {
		var outFile = destinationDirectory.getAsFile().get().toPath().resolve(
			"bootstrap_data.hpp"
		);

		try (var out = Files.newBufferedWriter(outFile)) {
			out.write('{');
			out.newLine();

			source.getAsFileTree().visit(f -> {
				if (f.isDirectory())
					return;

				if (!f.getName().endsWith(".class"))
					return;

				appendElement(out, f);
			});

			out.write("\tnullptr, 0, 0");
			out.newLine();
			out.write('}');
			out.newLine();
		}
	}

	private void appendElement(BufferedWriter out, FileTreeElement f) {
		try (var in = f.open()) {
			appendClass(out, in, f.getSize());
		} catch(IOException ex) {
			throw new GradleException("File error", ex);
		}
	}

	private void appendClass(
		BufferedWriter out, InputStream f, long size
	) throws IOException {
		out.write("\t.data = {");
		out.newLine();
		out.write("\t},");
		out.newLine();
		out.write(String.format("\t.size = %d,", size));
		out.newLine();
		out.write(String.format("\t.compSize = %d", 0));
		out.newLine();
		out.write("}, {");
		out.newLine();
	}

	private final DirectoryProperty destinationDirectory;
	private final ConfigurableFileCollection source;
}
