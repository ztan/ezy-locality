package com.github.ztan.ezylocality.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * String compare algorithm from http://www.catalysoft.com/articles/StrikeAMatch.html
 */
final class CompareUtils {

	/**
	 * @return lexical similarity value in the range [0,1]
	 */
	static double calculateSimilarity(String text1, String text2) {
		List<String> pairs1 = wordLetterPairs(text1.toUpperCase());
		List<String> pairs2 = wordLetterPairs(text2.toUpperCase());
		int intersection = 0;
		int union = pairs1.size() + pairs2.size();
		for (Object pair1 : pairs1) {
			for (int j = 0; j < pairs2.size(); j++) {
				Object pair2 = pairs2.get(j);
				if (pair1.equals(pair2)) {
					intersection++;
					pairs2.remove(j);
					break;
				}
			}
		}

		return (2.0 * intersection) / union;
	}

	/**
	 * @return an array of adjacent letter pairs contained in the input string
	 */
	private static String[] letterPairs(String str) {

		int numPairs = str.length() - 1;
		String[] pairs = new String[numPairs];
		for (int i = 0; i < numPairs; i++) {
			pairs[i] = str.substring(i, i + 2);
		}
		return pairs;
	}

	/**
	 * @return an ArrayList of 2-character Strings.
	 */
	private static List<String> wordLetterPairs(String str) {

		List<String> allPairs = new ArrayList<>();
		// Tokenize the string and put the tokens/words into an array
		String[] words = str.split("\\s");
		// For each word
		for (String word : words) {
			// Find the pairs of characters
			String[] pairsInWord = letterPairs(word);
			allPairs.addAll(Arrays.asList(pairsInWord));
		}

		return allPairs;
	}
}
