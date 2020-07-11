package com.github.ztan.ezylocality.au;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import com.github.ztan.ezylocality.core.LocalityStore;

public class AULocalityStoreTest {

	private static final Random rand = new Random(System.currentTimeMillis());

	@RepeatedTest(value = 20, name = "{displayName} {currentRepetition} of {totalRepetitions}")
	void testRepeatedAccess() {
		LocalityStore auLocalities = new LocalityStore("AU");

		String input = "5" + rand.nextInt(1000);
		List<Map<String, String>> results = assertTimeout(Duration.ofMillis(150),
				() -> auLocalities.search(input).limit(10).collect(Collectors.toList()));
		System.out.println(" ==== search for post code " + input + " ====");
		System.out.println(" " + results.size() + " matched.");
	}

	@Test
	void testGeneralAccess() {
		LocalityStore auLocalities = new LocalityStore("AU");
		List<Map<String, String>> results = auLocalities.search("5000").limit(10).collect(Collectors.toList());
		System.out.println(" ==== " + 5000 + " ====");
		results.forEach(System.out::println);
		Map<String, String> item = results.iterator().next();
		assertEquals("Adelaide", item.get("place_name"));
		assertEquals("SA", item.get("admin_code1"));

	}

	@Test
	void testRanked() {

		LocalityStore auLocalities = new LocalityStore("AU");

		List<Map<String, String>> results = auLocalities.search("adelaide", true)
				.sorted(Comparator.comparing(m -> m.get("rank"))).limit(20).collect(Collectors.toList());

		System.out.println(" ==== adelaide ====");
		results.forEach(System.out::println);
		Map<String, String> item = results.iterator().next();
		assertEquals("Adelaide Lead", item.get("place_name"));
	}
}
