/*
 *  Copyright (C) 2016 Australian Institute of Marine Science
 *
 *  Contact: Gael Lafond <g.lafond@aims.org.au>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package au.gov.aims.layers2svg.graphics;

import au.gov.aims.layers2svg.TestUtils;
import au.gov.aims.sld.PropertyValue;
import au.gov.aims.sld.geom.GeoShape;
import au.gov.aims.sld.geom.GeoShapeGroup;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class CSVShapeTest {
	private static final Logger LOGGER = Logger.getLogger(CSVShapeTest.class.getSimpleName());
	private static final String LAYER_CSV_FILE = "layers/World_NE_10m-cities_V3_Ranked.csv";

	private static final double EPSILON = 0.000000000001;

	@BeforeClass
	public static void init() throws IOException {
		// Copy the CSV file to the tmp folder.
		File csvFile = new File("/tmp/" + LAYER_CSV_FILE);
		TestUtils.copyResourceToDisk(LAYER_CSV_FILE, csvFile);
	}

	@AfterClass
	public static void cleanup() throws IOException {
		// Delete CSV file
		File csvFile = new File("/tmp/" + LAYER_CSV_FILE);
		csvFile.delete();
	}

	@Test
	public void testCSVParser() throws Exception {
		File csvFile = new File("/tmp/" + LAYER_CSV_FILE);
		CSVShape csvShape = new CSVShape(csvFile, "Cities", "LONGITUDE", "LATITUDE");

		csvShape.parse();

		List<GeoShapeGroup> shapeGroups = csvShape.getGeoShapeGroups();
		Assert.assertTrue("Some shape groups were found in the CSV Shape.", shapeGroups == null || shapeGroups.isEmpty());

		List<GeoShape> shapes = csvShape.getGeoShapes();
		Assert.assertTrue("No shape were found in the CSV Shape.", shapes != null && !shapes.isEmpty());

		Map<String, Boolean> foundMap = new HashMap<String, Boolean>();
		for (GeoShape shape : shapes) {
			// It would take too long to validate the whole file.
			// This test will check for the first city in the file, the last one and some random sample.
			Map<String, PropertyValue> properties = shape.getProperties();
			Assert.assertNotNull("A shape has no property." + properties);

			PropertyValue name = properties.get("NAME");
			Assert.assertNotNull("A shape has no name." + name);

			String nameStr = name.getStringValue();

			if ("Cooktown".equals(nameStr)) {
				// Row 2 (first row of data)
				foundMap.put(nameStr, true);
				this.validateProperties(properties,
					145.2515, -15.46903,
					10,
					"Australia", "AUS", "AU",
					"Queensland", "Australia/Brisbane",
					2339, 2339
				);

			} else if ("Cachoeira do Sul".equals(nameStr)) {
				// Row 1502
				foundMap.put(nameStr, true);
				this.validateProperties(properties,
					-52.9099851904, -30.0299641736,
					8,
					"Brazil", "BRA", "BR",
					"Rio Grande do Sul", "America/Sao_Paulo",
					49048, 74694
				);

			} else if ("Arauca".equals(nameStr)) {
				// Row 3528
				foundMap.put(nameStr, true);
				this.validateProperties(properties,
					-70.7616345639, 7.09066408175,
					7,
					"Colombia", "COL", "CO",
					"Arauca", "America/Bogota",
					23797, 69264
				);

			} else if ("Krishnanagar".equals(nameStr)) {
				// Row 5380
				foundMap.put(nameStr, true);
				this.validateProperties(properties,
					88.5300378, 23.3803416127,
					6,
					"India", "IND", "IN",
					"West Bengal", "Asia/Kolkata",
					145926, 145926
				);

			} else if ("Turangi".equals(nameStr)) {
				// Row 7345 (last row)
				foundMap.put(nameStr, true);
				this.validateProperties(properties,
					175.81424143638, -38.98888813665,
					8,
					"New Zealand", "NZL", "NZ",
					"Waikato", "Pacific/Auckland",
					3240, 3240
				);
			}
		}

		if (!foundMap.containsKey("Cooktown")) {
			Assert.fail("Cooktown was not found.");
		}
		if (!foundMap.containsKey("Cachoeira do Sul")) {
			Assert.fail("Cachoeira do Sul was not found.");
		}
		if (!foundMap.containsKey("Arauca")) {
			Assert.fail("Arauca was not found.");
		}
		if (!foundMap.containsKey("Krishnanagar")) {
			Assert.fail("Krishnanagar was not found.");
		}
		if (!foundMap.containsKey("Turangi")) {
			Assert.fail("Turangi was not found.");
		}
	}

	private void validateProperties(
			Map<String, PropertyValue> properties,
			double expectedLongitude,    // LONGITUDE
			double expectedLatitude,     // LATITUDE
			int expectedScaleRank,       // SCALERANK
			String expectedCountry,      // SOV0NAME and ADM0NAME
			String expectedCountryCode3, // SOV_A3 and ADM0_A3
			String expectedCountryCode2, // ISO_A2
			String expectedProvince,     // ADM1NAME
			String expectedTimezone,     // TIMEZONE
			int expectedPopMin,          // POP_MIN
			int expectedPopMax           // POP_MAX
	) {

		PropertyValue name = properties.get("NAME");
		Assert.assertNotNull("NAME is null", name);

		String nameStr = name.getStringValue();

		PropertyValue longitude = properties.get("LONGITUDE");
		Assert.assertNotNull("LONGITUDE is null for '" + nameStr + "'", longitude);
		Assert.assertEquals("Wrong LONGITUDE for '" + nameStr + "'", longitude.getDoubleValue(), expectedLongitude, EPSILON);

		PropertyValue latitude = properties.get("LATITUDE");
		Assert.assertNotNull("LATITUDE is null for '" + nameStr + "'", latitude);
		Assert.assertEquals("Wrong LATITUDE for '" + nameStr + "'", latitude.getDoubleValue(), expectedLatitude, EPSILON);

		PropertyValue scaleRank = properties.get("SCALERANK");
		Assert.assertNotNull("SCALERANK is null for '" + nameStr + "'", scaleRank);
		Assert.assertEquals("Wrong SCALERANK for '" + nameStr + "'", scaleRank.getDoubleValue().intValue(), expectedScaleRank);


		PropertyValue country = properties.get("SOV0NAME");
		Assert.assertNotNull("SOV0NAME is null for '" + nameStr + "'", country);
		Assert.assertEquals("Wrong SOV0NAME for '" + nameStr + "'", country.getStringValue(), expectedCountry);

		PropertyValue country2 = properties.get("ADM0NAME");
		Assert.assertNotNull("ADM0NAME is null for '" + nameStr + "'", country2);
		Assert.assertEquals("Wrong ADM0NAME for '" + nameStr + "'", country2.getStringValue(), expectedCountry);


		PropertyValue countryCode3 = properties.get("SOV_A3");
		Assert.assertNotNull("SOV_A3 is null for '" + nameStr + "'", countryCode3);
		Assert.assertEquals("Wrong SOV_A3 for '" + nameStr + "'", countryCode3.getStringValue(), expectedCountryCode3);

		PropertyValue countryCode3_2 = properties.get("ADM0_A3");
		Assert.assertNotNull("ADM0_A3 is null for '" + nameStr + "'", countryCode3_2);
		Assert.assertEquals("Wrong ADM0_A3 for '" + nameStr + "'", countryCode3_2.getStringValue(), expectedCountryCode3);

		PropertyValue countryCode2 = properties.get("ISO_A2");
		Assert.assertNotNull("ISO_A2 is null for '" + nameStr + "'", countryCode2);
		Assert.assertEquals("Wrong ISO_A2 for '" + nameStr + "'", countryCode2.getStringValue(), expectedCountryCode2);


		PropertyValue province = properties.get("ADM1NAME");
		Assert.assertNotNull("ADM1NAME is null for '" + nameStr + "'", province);
		Assert.assertEquals("Wrong ADM1NAME for '" + nameStr + "'", province.getStringValue(), expectedProvince);


		PropertyValue timezone = properties.get("TIMEZONE");
		Assert.assertNotNull("TIMEZONE is null for '" + nameStr + "'", timezone);
		Assert.assertEquals("Wrong TIMEZONE for '" + nameStr + "'", timezone.getStringValue(), expectedTimezone);


		PropertyValue popMin = properties.get("POP_MIN");
		Assert.assertNotNull("POP_MIN is null for '" + nameStr + "'", popMin);
		Assert.assertEquals("Wrong POP_MIN for '" + nameStr + "'", popMin.getDoubleValue().intValue(), expectedPopMin);

		PropertyValue popMax = properties.get("POP_MAX");
		Assert.assertNotNull("POP_MAX is null for '" + nameStr + "'", popMax);
		Assert.assertEquals("Wrong POP_MAX for '" + nameStr + "'", popMax.getDoubleValue().intValue(), expectedPopMax);
	}
}
