package com.github.ztan.ezylocality.gb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import com.github.ztan.ezylocality.core.LocalityStore;

public class GBLocalityStoreTest {

	private static final Random rand = new Random(System.currentTimeMillis());

	@RepeatedTest(value = 20, name = "{displayName} {currentRepetition} of {totalRepetitions}")
	void testRepeatedAccess() {
		LocalityStore auLocalities = new LocalityStore("GB");

		String input = "1160" + String.valueOf(rand.nextInt(10));
		List<Map<String, String>> results = assertTimeout(Duration.ofMillis(150),
				() -> auLocalities.search(input).limit(10).collect(Collectors.toList()));
		System.out.println(" ==== search for post code " + input + " ====");
		System.out.println(" " + results.size() + " matched.");
	}

	@Test
	void testGeneralAccess() {
		LocalityStore auLocalities = new LocalityStore("GB");
		List<Map<String, String>> results = auLocalities.search("LONGDON").limit(10).collect(Collectors.toList());
		System.out.println(" ==== LONGDON ====");
		results.forEach(System.out::println);
		Map<String, String> item = results.iterator().next();
		assertEquals("Longdon upon Tern", item.get("place_name"));
		assertEquals("ENG", item.get("admin_code1"));
	}
}
