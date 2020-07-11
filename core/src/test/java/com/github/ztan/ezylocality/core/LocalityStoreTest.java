package com.github.ztan.ezylocality.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

public class LocalityStoreTest {

	@Test
	void testSampleZipCodes() {
		LocalityStore store = new LocalityStore("SAMPLE");
		List<Map<String, String>> results = store.search("6112", true).limit(100).collect(Collectors.toList());

		results.forEach(System.out::println);

		assertEquals(12, results.size());

		int count = store.count("6111");

		assertEquals(4, count);

	}

	@Test
	void testMissintCountryCode() {
		LocalityStore store = new LocalityStore("FOO");
		List<Map<String, String>> restults = store.search("bar").limit(1).collect(Collectors.toList());
		assertEquals(0, restults.size());

		int count = store.count("text");

		assertEquals(0, count);
	}

	@Test
	void testSupportedCountries() {
		Set<String> supportedCountries = LocalityStore.supportedCountries();
		assertTrue(supportedCountries.isEmpty());
	}
}
