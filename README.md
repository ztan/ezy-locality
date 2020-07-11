# ezy-locality
This project packages postal areas information downloaded from [GeoNames](http://download.geonames.org/export/zip/) into a simple Java utility, which is suited for small projects to implement postcode search functionality without needing to rely on the web services provided by geonames.org.

# Installation via Maven

Add the following dependency to your projects pom.xml
```XML
<dependency>
	<groupId>com.github.ztan.ezy-locality</groupId>
	<artifactId>ezy-locality-core</artifactId>
	<version>0.1.6</version>
</dependency>

```
This will make the utility `LocalityStore` available to your project. The utility is built on top of [H2 JDBC](http://www.h2database.com/html/main.html) driver, therefore you should also make sure the driver package is on the project's classpath. If one of the build artifacts of your project is a `WAR` archive, the driver package should be bundled automatically in `WEB-INF/lib` folder by maven-war-plugin.

You will also need to list at least one of the country specific packages in the Maven dependencies, e.g.
```XML
<dependency>
	<groupId>com.github.ztan.ezy-locality</groupId>
	<artifactId>ezy-locality-au</artifactId>
	<version>0.1.6</version>
</dependency>
```

Alternatively, you can add one of the 'shaded' collections in the dependencies, e.g.
```XML
<dependency>
	<groupId>com.github.ztan.ezy-locality</groupId>
	<artifactId>ezy-locality-anglosphere</artifactId>
	<version>0.1.6</version>
</dependency>

```
The package `ezy-locality-anglosphere` has postcode data of 5 countries, namely AU, CA, GB, NZ, and US, while 'ezy-locality-all-countries' contains the data for all countries.

# Getting started

The following snippet shows how to use the search utility.

```Java
// The static method ` LocalityStore.supportedCountries` lists all the countries (codes) with the corresponding
// country specific packages loaded in the classpath.
Set<String> supportedCountries = LocalityStore.supportedCountries();
System.out.println(supportedCountries);

// constructs a 'locality store' of the country 'US'
LocalityStore usLocalities = new LocalityStore("US");
// a try block ensures the underlying data resources backing the returned `Stream` instance will be released even
// when an exception occurs.
try (Stream<Map<String, String>> stream = usLocalities.search("Shrev")) {
	stream
		.limit(10) // sets the maximum number of iterations over the result set
		.forEach(System.out::println);
}
 ```

# Performance
The search function provided is not fulltext search, and the underlying data sources are `Tab Delimited Files` downloaded from http://download.geonames.org/export/zip/. If a data file is over 1MB in size and the target matches are near the end of the file, it may take a little bit over 50 milliseconds for the first 10 results to return. Potentially in the future this project may look to improve the performance metrics by converting the TDFs into H2 native data files, and also by enabling the fulltext search on those data files.

# Releases

GeoNames: [http://www.geonames.org/] periodically exports their postal code data to http://download.geonames.org/export/zip/, therefore in order to keep the locality data up to date this project will aim to release a new build every time the data dump is refreshed.

# Licenses

This utility is released under MIT license. Please refer to the [LICENSE](LICENSE) file for more information.
The postal code data files from [GeoNames](http://www.geonames.org/) are released under [Creative Commons Attribution 3.0 License]: https://creativecommons.org/licenses/by/3.0/. No modification has been done to the data files by this project.
