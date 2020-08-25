package com.github.ztan.ezylocality.au;

import com.github.ztan.ezylocality.core.LocalityStore;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;

public class AULocalityStoreTest {

	private static final Random rand = new Random(System.currentTimeMillis());

	private static final ThreadLocal<LocalityStore> localitiesTL = ThreadLocal.withInitial(() -> new LocalityStore("AU"));

	@RepeatedTest(value = 20, name = "{displayName} {currentRepetition} of {totalRepetitions}")
	void testRepeatedAccess() {
		final LocalityStore store = localitiesTL.get();

		String input = "5" + rand.nextInt(1000);
		List<Map<String, String>> results = assertTimeout(Duration.ofMillis(350),
				() -> {
					try (final Stream<Map<String, String>> stream = store.search(input)) {
						return stream.limit(10).collect(Collectors.toList());
					}
				});
		System.out.println(" ==== search for post code " + input + " ====");
		System.out.println(" " + results.size() + " matched.");
	}

	@Test
	void testGeneralAccess() {
		LocalityStore auLocalities = localitiesTL.get();
		try (final Stream<Map<String, String>> stream = auLocalities.search("5000")) {
			List<Map<String, String>> results = stream.limit(10).collect(Collectors.toList());
			System.out.println(" ==== " + 5000 + " ====");
			results.forEach(System.out::println);
			Map<String, String> item = results.iterator().next();
			assertEquals("Adelaide Bc", item.get("place_name"));
			assertEquals("SA", item.get("admin_code1"));
		}
	}

	@Test
	void testRanked() {

		LocalityStore auLocalities = localitiesTL.get();

		try (final Stream<Map<String, String>> stream = auLocalities.search("adelaide", true)) {
			List<Map<String, String>> results = stream
					.sorted(Comparator.comparing(m -> m.get("rank"))).limit(20).collect(Collectors.toList());

			System.out.println(" ==== adelaide ====");
			results.forEach(System.out::println);
			Map<String, String> item = results.iterator().next();
			assertEquals("Adelaide Lead", item.get("place_name"));
		}
	}
}
