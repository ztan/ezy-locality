package com.github.ztan.ezylocality.anglosphere;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.github.ztan.ezylocality.core.LocalityStore;

public class AnglosphereLocalityStoreTest {

	@Test
	void testSupportedCountries() {
		Set<String> supportedCountries = LocalityStore.supportedCountries();
		assertTrue(supportedCountries.containsAll(Arrays.asList("AU", "NZ", "GB", "US", "CA")));
		
		assertEquals(5, supportedCountries.size());
	}

}
