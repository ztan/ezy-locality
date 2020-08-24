package com.github.ztan.ezylocality.gb;

import com.github.ztan.ezylocality.core.LocalityStore;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;

public class GBFullLocalityStoreTest {

	private static final Random rand = new Random(System.currentTimeMillis());
	private static final ThreadLocal<LocalityStore> localitiesTL = ThreadLocal.withInitial(() -> new LocalityStore("GB", false));

	@RepeatedTest(value = 10, name = "{displayName} {currentRepetition} of {totalRepetitions}")
	void testRepeatedAccess() {
		LocalityStore store = localitiesTL.get();

		String input = "1160" + String.valueOf(rand.nextInt(10));
		List<Map<String, String>> results = assertTimeout(Duration.ofMillis(4000),
				() -> store.search(input).limit(10).collect(Collectors.toList()));
		System.out.println(" ==== search for post code " + input + " ====");
		System.out.println(" " + results.size() + " matched.");
	}

	@Test
	void testGeneralAccess() {
		LocalityStore gbLocalities = localitiesTL.get();
		List<Map<String, String>> results = gbLocalities.search("LONGDON").limit(10).collect(Collectors.toList());
		System.out.println(" ==== LONGDON ====");
		results.forEach(System.out::println);
		Map<String, String> item = results.iterator().next();
		assertEquals("Longdon", item.get("place_name"));
		assertEquals("ENG", item.get("admin_code1"));
	}
}
