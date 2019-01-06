/*
 * Copyright (c) 2019 Alex Dubov <oakad@yahoo.com>
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package temulg.yzr.core;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.Callable;
import java.util.function.Function;

public class UncheckExceptions {
	private UncheckExceptions() {}

	public static <V> V of(final Callable<V> callable) {
		return INVOKER.invoke(callable);
	}

	@FunctionalInterface
	private interface Invoker extends Function<Callable<?>, Object> {
		<V> V invoke(final Callable<V> callable);

		@Override
		default Object apply(final Callable<?> callable) {
			return invoke(callable);
		}

		@SuppressWarnings("unused")
		static <V> V call(final Callable<V> callable) throws Exception {
			return callable.call();
		}

		static MethodType SIGNATURE = MethodType.methodType(
			Object.class, Callable.class
		);
	}

	private static final Invoker INVOKER;

	static {
		final MethodHandles.Lookup lookup = MethodHandles.lookup();

		try {
			final CallSite site = LambdaMetafactory.metafactory(
				lookup, "invoke",
				MethodType.methodType(Invoker.class),
				Invoker.SIGNATURE,
				lookup.findStatic(
					Invoker.class,
					"call",
					Invoker.SIGNATURE
				),
				Invoker.SIGNATURE
			);
			INVOKER = (Invoker)site.getTarget().invokeExact();
		} catch (final Throwable e) {
			throw new ExceptionInInitializerError(e);
		}
	}
}
