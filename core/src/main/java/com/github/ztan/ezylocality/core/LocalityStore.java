package com.github.ztan.ezylocality.core;

import java.net.URL;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * A simple Java utility for postcode lookup, based on data exported from
 * geonames.org. Each <code>LocalityStore</code> contain postal geo data of a
 * specific country.
 * 
 * @author ztan
 *
 */
public class LocalityStore {

	private static final Logger log = Logger.getLogger(LocalityStore.class.getName());

	private static final List<String> COLUMNS = Arrays.asList("country_code", "postal_code", "place_name",
			"admin_name1", "admin_code1", "admin_name2", "admin_code2", "admin_name3", "admin_code3", "latitude",
			"longitude", "accuracy");

	private static final List<String> SEARCH_COLUMNS = Arrays.asList("postal_code", "place_name", "admin_name1",
			"admin_code1", "admin_name2", "admin_code2", "admin_name3", "admin_code3");

	private static final String CSV_COLUMNS = COLUMNS.stream().map(c -> "'" + c.toUpperCase() + "'")
			.collect(Collectors.joining("||CHAR(9)||"));
	private static final String SELECT_COLUMNS = COLUMNS.stream().collect(Collectors.joining(", "));

	private static final List<String> ALL_SUPPORTED_COUNTRIES = Arrays.asList("AD", "AR", "AS", "AT", "AU", "AX", "BD",
			"BE", "BG", "BM", "BR", "BY", "CA", "CH", "CO", "CR", "CZ", "DE", "DK", "DO", "DZ", "ES", "FI", "FM", "FO",
			"FR", "GB", "GF", "GG", "GL", "GP", "GT", "GU", "HR", "HU", "IE", "IM", "IN", "IS", "IT", "JE", "JP", "LI",
			"LK", "LT", "LU", "LV", "MC", "MD", "MH", "MK", "MP", "MQ", "MT", "MX", "MY", "NC", "NL", "NO", "NZ", "PH",
			"PK", "PL", "PM", "PR", "PT", "PW", "RE", "RO", "RU", "SE", "SI", "SJ", "SK", "SM", "TH", "TR", "UA", "US",
			"UY", "VA", "VI", "WF", "YT", "ZA");

	private final String csvFile;
	private final String selectAllStatement;
	private final String selectCountStatement;

	private static boolean assertDriver() {
		try {
			Class.forName("org.h2.Driver");
		} catch (ClassNotFoundException e) {
			log.warning("H2 driver not found. Locality store disabled");
			return false;
		}
		return true;
	}

	private static String getCsvFilePath(String code) {
		URL csvResource = getDataResource(code);
		if (csvResource == null) {
			log.warning("Zip package for country '" + code
					+ "' has not been installed. Make sure you have artifact 'ezy-locality-" + code.toLowerCase()
					+ "' listed as a Maven dependency.");
			return null;
		}
		return csvResource.toExternalForm();
	}

	private static URL getDataResource(String code) {
		List<String> paths = Arrays.asList(LocalityStore.class.getPackage().getName().split("\\."));
		String parentPath = paths.subList(0, paths.size() - 1).stream().collect(Collectors.joining("/"));

		return LocalityStore.class.getResource("/" + parentPath + "/countries/" + code + ".txt");
	}

	/**
	 * Gets the supported countries. In order to load the dataset for a certain
	 * country, add the package {@code ezy-locality-<country code> } to the
	 * classpath.
	 * 
	 * @return a <code>Set</code> instance of country codes
	 */
	public static Set<String> supportedCountries() {
		return ALL_SUPPORTED_COUNTRIES.stream().filter(c -> getDataResource(c) != null).collect(Collectors.toSet());
	}

	private String getSelectSql(String columns) {
		if (this.csvFile == null) {
			return null;
		}
		return "SELECT " + columns + " FROM CSVREAD('" + csvFile + "', " + CSV_COLUMNS
				+ ", 'fieldSeparator=' || CHAR(9)) ";
	}

	/**
	 * Constructs a locality store.
	 * 
	 * @param countryCode
	 *            an ISO 3166-1 country code
	 */
	public LocalityStore(String countryCode) {
		assertDriver();

		this.csvFile = getCsvFilePath(countryCode);
		this.selectAllStatement = getSelectSql(SELECT_COLUMNS);
		this.selectCountStatement = getSelectSql(" COUNT(*) ");
	}

	private static String getStringFromResultSet(ResultSet rs, String columnName) {
		try {
			return Optional.ofNullable(rs.getString(columnName)).orElse("");
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Searches for a given term (<code>text</code>) in the country data. Note: this
	 * is not fulltext search and the order of the results are based on their
	 * appearances in the data file.
	 * 
	 * @param text
	 *            the search term
	 * @return a <code>Stream</code> instance backed by a query result set. Each
	 *         element of the stream is a map of key-value pairs of the matched
	 *         results.
	 */
	public Stream<Map<String, String>> search(final String text) {
		if (this.selectAllStatement == null) {
			return Stream.empty();
		}
		UncheckedCloseable close = null;
		try {
			Connection conn = getInMemoryConnection();
			close = UncheckedCloseable.wrap(conn);
			PreparedStatement stmt = conn.prepareStatement(
					this.selectAllStatement + " WHERE " + getColumnMatchingClause(SEARCH_COLUMNS, text));

			close = close.nest(stmt);
			ResultSet resultSet = stmt.executeQuery();
			close = close.nest(resultSet);

			return StreamSupport.stream(
					new Spliterators.AbstractSpliterator<Map<String, String>>(Long.MAX_VALUE, Spliterator.ORDERED) {
						@Override
						public boolean tryAdvance(Consumer<? super Map<String, String>> action) {
							try {
								if (!resultSet.next())
									return false;
								action.accept(COLUMNS.stream()
										.collect(Collectors.toMap(c -> c, c -> getStringFromResultSet(resultSet, c),
												(a, b) -> b, () -> new LinkedHashMap<>())));
								return true;
							} catch (SQLException ex) {
								throw new RuntimeException(ex);
							}
						}
					}, false).onClose(close);

		} catch (SQLException sqlEx) {
			if (close != null) {
				try {
					close.close();
				} catch (Exception ex) {
					sqlEx.addSuppressed(ex);
				}
			}
			log.log(Level.SEVERE, "Failed to execute query. ", sqlEx);
		}
		return Stream.empty();
	}

	/**
	 * Counts the total occurrences of the search term.
	 * 
	 * @param text
	 *            the search term
	 * @return an integer
	 */
	public int count(final String text) {
		if (this.selectCountStatement == null) {
			return 0;
		}

		try (Connection conn = getInMemoryConnection();
				PreparedStatement query = conn.prepareStatement(
						this.selectCountStatement + " WHERE " + getColumnMatchingClause(SEARCH_COLUMNS, text))) {
			final ResultSet resultSet = query.executeQuery();
			resultSet.next();
			return resultSet.getInt(1);

		} catch (SQLException e) {
			log.log(Level.SEVERE, "Failed to execute query. ", e);
			return 0;
		}

	}

	private String getColumnMatchingClause(List<String> columns, String text) {
		String key = ("%" + text + "%").toLowerCase();
		return columns.stream().map(c -> "LOWER(" + c + ") like '" + key + "'").collect(Collectors.joining(" OR "));
	}

	private Connection getInMemoryConnection() throws SQLException {
		return DriverManager.getConnection("jdbc:h2:mem:test", "sa", "sa");
	}
}