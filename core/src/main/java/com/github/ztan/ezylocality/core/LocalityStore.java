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
import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static java.util.Optional.ofNullable;

/**
 * A simple Java utility for postcode lookup, based on data exported from
 * geonames.org. Each <code>LocalityStore</code> contain postal geo data of a
 * specific country.
 *
 * @author ztan
 */
public class LocalityStore {

	private static final Logger log = Logger.getLogger(LocalityStore.class.getName());

	private static final List<String> COLUMNS = Arrays.asList("country_code", "postal_code", "place_name",
			"admin_name1", "admin_code1", "admin_name2", "admin_code2", "admin_name3", "admin_code3", "latitude",
			"longitude", "accuracy");

	private static final List<String> COLUMNS_DEF =
			Arrays.asList("country_code VARCHAR(2)", "postal_code VARCHAR(15)", "place_name VARCHAR(150)",
					"admin_name1 VARCHAR(150)", "admin_code1 VARCHAR(15)", "admin_name2 VARCHAR(150)", "admin_code2 VARCHAR(15)",
					"admin_name3 VARCHAR(150)", "admin_code3 VARCHAR(15)", "latitude FLOAT",
					"longitude FLOAT", "accuracy FLOAT");

	private static final List<String> SEARCH_COLUMNS = Arrays.asList("postal_code", "place_name", "admin_name1",
			"admin_code1", "admin_name2", "admin_code2", "admin_name3", "admin_code3");

	private static final List<String> RANK_COLUMNS = Arrays.asList("postal_code", "place_name", "admin_name1",
			"admin_name2", "admin_name3");

	private static final String CSV_COLUMNS = COLUMNS.stream().map(c -> "'" + c.toUpperCase() + "'")
			.collect(Collectors.joining("||CHAR(9)||"));
	private static final String SELECT_COLUMNS = String.join(", ", COLUMNS);
	private static final String SELECT_COLUMNS_DEF = String.join(", ", COLUMNS_DEF);

	private static final List<String> ALL_SUPPORTED_COUNTRIES = Arrays.asList("AD", "AR", "AS", "AT", "AU", "AX", "BD",
			"BE", "BG", "BM", "BR", "BY", "CA", "CH", "CO", "CR", "CZ", "DE", "DK", "DO", "DZ", "ES", "FI", "FM", "FO",
			"FR", "GB", "GF", "GG", "GL", "GP", "GT", "GU", "HR", "HU", "IE", "IM", "IN", "IS", "IT", "JE", "JP", "LI",
			"LK", "LT", "LU", "LV", "MC", "MD", "MH", "MK", "MP", "MQ", "MT", "MX", "MY", "NC", "NL", "NO", "NZ", "PH",
			"PK", "PL", "PM", "PR", "PT", "PW", "RE", "RO", "RU", "SE", "SI", "SJ", "SK", "SM", "TH", "TR", "UA", "US",
			"UY", "VA", "VI", "WF", "YT", "ZA");

	private final String csvFile;
	private final String selectAllStatement;
	private final String selectCountStatement;
	private String tableName;

	/**
	 * Constructs a locality store.
	 *
	 * @param countryCode    an ISO 3166-1 country code
	 * @param createDataFile whether to create a h2 in memory table to back all queries.
	 */
	public LocalityStore(String countryCode, boolean createDataFile) {
		assertDriver();


		this.csvFile = getCsvFilePath(countryCode);
		String _tableName = null;
		if (createDataFile) {
			try {
				_tableName = "COUNTRY_" + countryCode.toUpperCase() + System.currentTimeMillis();
				Connection conn = getInMemoryConnection();
				final PreparedStatement statement =
						conn.prepareStatement("CREATE TABLE " + _tableName + " (" + SELECT_COLUMNS_DEF + ") " +
								"AS " + getSelectSql(SELECT_COLUMNS));
				statement.execute();
			} catch (SQLException ex) {
				log.log(Level.SEVERE, "Cannot create data table.", ex);
				_tableName = null;
			}
		}
		this.tableName = _tableName;
		this.selectAllStatement = getSelectSql(SELECT_COLUMNS);
		this.selectCountStatement = getSelectSql(" COUNT(*) ");
	}

	/**
	 * Constructs a locality store.
	 *
	 * @param countryCode an ISO 3166-1 country code
	 */
	public LocalityStore(String countryCode) {
		this(countryCode, false);
	}

	private static void assertDriver() {
		try {
			Class.forName("org.h2.Driver");
		} catch (ClassNotFoundException e) {
			log.warning("H2 driver not found. Locality store disabled");
		}
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

	private static URL _getDataResource(String code) {
		List<String> paths = Arrays.asList(LocalityStore.class.getPackage().getName().split("\\."));
		String parentPath = String.join("/", paths.subList(0, paths.size() - 1));

		return LocalityStore.class.getResource("/" + parentPath + "/countries/" + code + ".txt");
	}

	private static URL getDataResource(String code) {
		if ("GB".equals(code)) {
			return ofNullable(_getDataResource("GB_full")).orElse(_getDataResource(code));
		} else {
			return _getDataResource(code);
		}
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

	@Override
	protected void finalize() throws Throwable {
		dispose();
		super.finalize();
	}

	public void dispose() {
		if (this.tableName != null) {
			try {
				getInMemoryConnection().prepareStatement("DROP TABLE " + this.tableName).execute();
				this.tableName = null;
			} catch (SQLException ex) {
				log.log(Level.SEVERE, "Cannot delete data table.", ex);
			}
		}
	}

	private String getSelectSql(String columns) {
		if (this.csvFile == null) {
			return null;
		}

		if (this.tableName == null) {
			return "SELECT " + columns + " FROM CSVREAD('" + csvFile + "', " + CSV_COLUMNS
					+ ", 'fieldSeparator=' || CHAR(9)) ";
		} else {
			return "SELECT " + columns + " FROM " + this.tableName;

		}
	}

	private static String getStringFromResultSet(ResultSet rs, String columnName) {
		try {
			return ofNullable(rs.getString(columnName)).orElse("");
		} catch (SQLException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Searches for a given term (<code>text</code>) in the country data. Note: this
	 * is not fulltext search and the order of the results are based on their
	 * appearances in the data file.
	 *
	 * @param text the search term
	 * @return a <code>Stream</code> instance backed by a query result set. Each
	 * element of the stream is a map of key-value pairs of the matched
	 * results.
	 */
	public Stream<Map<String, String>> search(final String text) {
		return search(text, false);
	}

	/**
	 * Searches for a given term (<code>text</code>) in the country data. Note: this
	 * is not fulltext search and the order of the results are based on their
	 * appearances in the data file.
	 *
	 * @param text the search term
	 * @param rank whether to calculate lexical similarity value in the range [0,1], and store it as 'rank'
	 * @return a <code>Stream</code> instance backed by a query result set. Each
	 * element of the stream is a map of key-value pairs of the matched
	 * results.
	 */
	public Stream<Map<String, String>> search(final String text, boolean rank) {
		if (this.selectAllStatement == null) {
			return Stream.empty();
		}
		UncheckedCloseable close = null;
		try {
			Connection conn = getInMemoryConnection();
			close = UncheckedCloseable.wrap(conn);
			PreparedStatement stmt = conn.prepareStatement(
					this.selectAllStatement + " WHERE " + getColumnMatchingClause(text));

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
								Map<String, String> item = COLUMNS.stream()
										.collect(Collectors.toMap(c -> c, c -> getStringFromResultSet(resultSet, c),
												(a, b) -> b, LinkedHashMap::new));
								if (rank) {
									rankResult(text, item);
								}
								action.accept(item);
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
	 * @param text the search term
	 * @return an integer
	 */
	public int count(final String text) {
		if (this.selectCountStatement == null) {
			return 0;
		}

		try (Connection conn = getInMemoryConnection();
			 PreparedStatement query = conn.prepareStatement(
					 this.selectCountStatement + " WHERE " + getColumnMatchingClause(text))) {
			final ResultSet resultSet = query.executeQuery();
			resultSet.next();
			return resultSet.getInt(1);

		} catch (SQLException e) {
			log.log(Level.SEVERE, "Failed to execute query. ", e);
			return 0;
		}

	}

	private String getColumnMatchingClause(String text) {
		String key = ("%" + text + "%").toLowerCase();
		return LocalityStore.SEARCH_COLUMNS.stream().map(c -> "LOWER(" + c + ") like '" + key + "'").collect(Collectors.joining(" OR "));
	}

	private Connection getInMemoryConnection() throws SQLException {
		return DriverManager.getConnection("jdbc:h2:mem:test", "sa", "sa");
	}

	private void rankResult(String searchText, Map<String, String> result) {
		String value = RANK_COLUMNS.stream().map(result::get).collect(Collectors.joining(" "));
		double rank = 1 - CompareUtils.calculateSimilarity(searchText, value);
		result.put("rank", String.valueOf(rank));
	}
}