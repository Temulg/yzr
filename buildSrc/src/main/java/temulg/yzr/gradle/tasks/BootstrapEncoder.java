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
import java.util.zip.Deflater;
import java.util.zip.DeflaterInputStream;

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
	DirectoryProperty getDestDir() {
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
			"bootstrap_data.cpp"
		);

		try (var out = Files.newBufferedWriter(outFile)) {
			appendHeader(out);

			source.getAsFileTree().visit(f -> {
				if (f.isDirectory())
					return;

				if (!f.getName().endsWith(".class"))
					return;

				appendElement(out, f);
			});

			out.write("\tnullptr, 0, 0");
			out.newLine();
			out.write("}};");
			out.newLine();

			appendFooter(out);
		}
		deflater.end();
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
		var in = new DeflaterInputStream(f, deflater);
		var data = in.readAllBytes();
		deflater.reset();

		out.write("\t(jbyte const *)");
		out.newLine();
		out.write("\t\"");

		int linePos = 0;
		int cpLen;
		int cpVal;

		for (byte b: data) {
			cpVal = ((int)b) & 0xff;

			if (cpVal >= 0x20 && cpVal < 0x7f) {
				switch (cpVal) {
				case '"':
				case '\\':
					cpLen = 2;
					break;
				default:
					cpLen = 1;
				}
			} else if (cpVal >= 0x07 && cpVal < 0x0e) {
				cpVal = C_ESCAPE_CHARS[cpVal - 7];
				cpLen = 2;
			} else {
				cpLen = 4;
			}

			if ((linePos + cpLen) > BINARY_BLOB_LINE_LEN) {
				out.write('"');
				out.newLine();
				out.write("\t\"");
				linePos = 0;
			}

			switch (cpLen) {
			case 1:
				out.write(cpVal);
				linePos++;
				break;
			case 2:
				out.write('\\');
				out.write(cpVal);
				linePos += 2;
				break;
			case 4:
				out.write(String.format("\\%03o", cpVal));
				linePos += 4;
			}
		}
		out.write("\",");
		out.newLine();
		out.write("\t");
		out.write(Integer.toString(data.length));
		out.write(", ");
		out.write(Long.toString(size));
		out.newLine();
		out.write("}, {");
		out.newLine();
	}

	private void appendHeader(BufferedWriter out) throws IOException {
		out.write("#include <jni.h>");
		out.newLine();
		out.newLine();
		out.write("namespace yzr { namespace bootstrap {");
		out.newLine();
		out.newLine();
		out.write("constexpr static struct item {");
		out.newLine();
		out.write("\tjbyte const *data;");
		out.newLine();
		out.write("\tjsize compSize;");
		out.newLine();
		out.write("\tjsize size;");
		out.newLine();
		out.write("} items[] = {{");
		out.newLine();
	}

	private void appendFooter(BufferedWriter out) throws IOException {
		out.newLine();
		out.write("void forEachItem(void *userData, void (*cons)(");
		out.newLine();
		out.write(
			"\tvoid *userData, jbyte const *data, "
			+ "jsize compSize, jsize size"
		);
		out.newLine();
		out.write(")) {");
		out.newLine();
		out.write("\tfor (auto const *it(items); it->data; ++it)");
		out.newLine();
		out.write(
			"\t\tcons(userData, it->data, it->compSize, "
			+ "it->size);"
		);
		out.newLine();
		out.write("}");
		out.newLine();
		out.newLine();
		out.write("}}");
		out.newLine();
	}

	static final int BINARY_BLOB_LINE_LEN = 68;
	static final char[] C_ESCAPE_CHARS = new char[] {
		'a', 'b', 't', 'n', 'v', 'f', 'r'
	};

	private final DirectoryProperty destinationDirectory;
	private final ConfigurableFileCollection source;
	private final Deflater deflater = new Deflater(9);
}
