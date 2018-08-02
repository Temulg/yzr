/*
 * Copyright (c) 2018 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package yzr.basis;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

public class NomenTest {
	private String fromUtf8(Nomen n) {
		try (var os = new ByteArrayOutputStream()) {
			var ch = Channels.newChannel(os);
			n.toUtf8Channel(ch, "/");
			return os.toString(StandardCharsets.UTF_8);
		} catch (IOException ex) {
			throw new AssertionError(ex);
		}
	}

	@Test
	public void constructBasic() throws Exception {
		var n0 = Nomen.from(List.of());
		Assert.assertEquals(n0.toString(), "");
		Assert.assertEquals(fromUtf8(n0), "");
		Assert.assertEquals(n0.size(), 0);

		var n1 = Nomen.from(List.of("abcdefg"));
		Assert.assertEquals(n1.toString(), "/abcdefg");
		Assert.assertEquals(fromUtf8(n1), "/abcdefg");
		Assert.assertEquals(n1.size(), 1);

		var n2 = Nomen.from(List.of("abcdefgh"));
		Assert.assertEquals(n2.toString(), "/abcdefgh");
		Assert.assertEquals(fromUtf8(n2), "/abcdefgh");
		Assert.assertEquals(n2.size(), 1);

		var n3 = Nomen.from(List.of("abcdefg", "hijklmn"));
		Assert.assertEquals(n3.toString(), "/abcdefg/hijklmn");
		Assert.assertEquals(fromUtf8(n3), "/abcdefg/hijklmn");
		Assert.assertEquals(n3.size(), 2);

		var n4 = Nomen.from(List.of("abcdefgh", "i", "j"));
		Assert.assertEquals(n4.toString(), "/abcdefgh/i/j");
		Assert.assertEquals(fromUtf8(n4), "/abcdefgh/i/j");
		Assert.assertEquals(n4.size(), 3);
	}

	@Test(
		dataProvider = "randomStringList_1",
		dataProviderClass = StringGenerator.class
	)
	public void construct(List<CharSequence> ss) throws Exception {
		var n = Nomen.from(ss);
		var sb = new StringBuilder();
		ss.forEach(s -> {
			sb.append('/').append(s);
		});
		var s = sb.toString();

		Assert.assertEquals(n.toString(), s);
		Assert.assertEquals(fromUtf8(n), s);
		Assert.assertEquals(n.size(), ss.size());
	}

	@Test(
		dataProvider = "randomStringList_2",
		dataProviderClass = StringGenerator.class
	)
	public void cat2(
		List<CharSequence> s0, List<CharSequence> s1
	) throws Exception {
		var n0 = Nomen.from(s0);
		var n1 = Nomen.from(s1);
		var n = n0.cat(n1);

		var sb = new StringBuilder();
		s0.forEach(s -> {
			sb.append('/').append(s);
		});
		s1.forEach(s -> {
			sb.append('/').append(s);
		});
		var s = sb.toString();

		Assert.assertEquals(n.toString(), s);
		Assert.assertEquals(n.size(), s0.size() + s1.size());
	}

	@Test(
		dataProvider = "randomStringList_3",
		dataProviderClass = StringGenerator.class
	)
	public void cat3(
		List<CharSequence> s0,
		List<CharSequence> s1,
		List<CharSequence> s2
	) throws Exception {
		var n0 = Nomen.from(s0);
		var n1 = Nomen.from(s1);
		var n2 = Nomen.from(s2);
		var n = n0.cat(n1, n2);

		var sb = new StringBuilder();
		s0.forEach(s -> {
			sb.append('/').append(s);
		});
		s1.forEach(s -> {
			sb.append('/').append(s);
		});
		s2.forEach(s -> {
			sb.append('/').append(s);
		});

		Assert.assertEquals(n.toString(), sb.toString());
		Assert.assertEquals(
			n.size(), s0.size() + s1.size() + s2.size()
		);
	}

	@Test(
		dataProvider = "randomStringList_3",
		dataProviderClass = StringGenerator.class
	)
	public void prefix2(
		List<CharSequence> s0,
		List<CharSequence> s1,
		List<CharSequence> s2
	) throws Exception {
		var n0 = Nomen.from(s0);
		var n1 = Nomen.from(s1);
		var n2 = Nomen.from(s2);
		var n01 = n0.cat(n1);
		var n02 = n0.cat(n2);
		var nc1 = n01.commonPrefix(n02);
		var nc2 = n02.commonPrefix(n01);

		var sb = new StringBuilder();
		s0.forEach(s -> {
			sb.append('/').append(s);
		});

		Assert.assertEquals(nc1, n0);
		Assert.assertEquals(nc2, n0);
		Assert.assertEquals(nc1.toString(), sb.toString());
		Assert.assertEquals(nc2.toString(), sb.toString());
	}

	@Test
	public void special() throws Exception {
		System.out.println("--- special ---");
		cat3(List.of(
			"ZcrPNN2IB", "Isg6b6r95MXEP5", "L4925WnQMn73BZ",
			"SU3", "GeCleuS8Iij", "lOfEAWXCvhf", "TdCarcC",
			"kgNKNOqkTn9oZV", "AtuuBRus6S9", "4iQmRAegCRG2",
			"kecVEUourd", "c", "QliI", "HWhlgaQ"
		), List.of("YMYbWp"), List.of(
			"Gg3OD8O", "l4e", "hnBCW4DkHqLs", "ETLFwEzRxd3KI",
			"uI", "pH31Q1SYbm", "dP", "GYPf7ETldEqdHX7", "EzjuW",
			"hWxr", "yESCuR", "67", "jHiTQ4", "u4", "v"
		));

		prefix2(List.of(
			"FwYXMTgxr3aK", "yt8RsT", "J8JaMO", "cSNyK9AX4oEW1R"
		), List.of(
			"srDW6blcu7UV9G", "CX", "nc", "4nAuF",
			"O7tY6gTQq3lCC16", "APg6mJ1", "p4", "sWAvqF9vgajnj",
			"H5ZQfeCIsxY"
		), List.of("BgERz", "TeapdPW818", "dpbn")); 
	}
}
