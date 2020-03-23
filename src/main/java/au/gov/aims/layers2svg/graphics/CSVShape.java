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

import au.gov.aims.sld.PropertyValue;
import au.gov.aims.sld.SldUtils;
import au.gov.aims.sld.geom.GeoShapeGroup;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVStrategy;

import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * This class represent the shapes defined in a CSV file.
 * It's a "GeoShapeGroup" instead of a "Layer" to add the possibility
 * of adding multiple CSV into a single layer.
 *
 * GeoJSON Specs:
 *   http://geojson.org/geojson-spec.html
 */
public class CSVShape extends GeoShapeGroup {
	private static final Logger LOGGER = Logger.getLogger(CSVShape.class.getSimpleName());

	private static final String CSV_COMMENT_PREFIX = "#";

	private static final CSVStrategy EXCEL_STRATEGY = new CSVStrategy(
			',', '"', CSVStrategy.COMMENTS_DISABLED, CSVStrategy.ESCAPE_DISABLED,
			true, true, false, true);
	// Common CSV used by OpenOffice and LibreOffice
	private static final CSVStrategy OOFFICE_STRATEGY = new CSVStrategy(
			';', '"', CSVStrategy.COMMENTS_DISABLED, CSVStrategy.ESCAPE_DISABLED,
			true, true, false, true);

	private static final List<CSVStrategy> CSV_STRATEGIES = new ArrayList<CSVStrategy>();
	static {
		CSV_STRATEGIES.add(CSVStrategy.DEFAULT_STRATEGY);
		CSV_STRATEGIES.add(CSVShape.OOFFICE_STRATEGY);
		CSV_STRATEGIES.add(CSVShape.EXCEL_STRATEGY);
		CSV_STRATEGIES.add(CSVStrategy.EXCEL_STRATEGY);
		CSV_STRATEGIES.add(CSVStrategy.TDF_STRATEGY);
	}

	private File csvFile;
	private String longitudeColumnName;
	private String latitudeColumnName;

	/**
	 * geoJson = {
	 *   "type": "FeatureCollection",
	 *   "features": [ ... ]
	 * }
	 */
	public CSVShape(File csvFile, String name, String longitudeColumnName, String latitudeColumnName) {
		super(name);
		this.csvFile = csvFile;
		this.longitudeColumnName = longitudeColumnName;
		this.latitudeColumnName = latitudeColumnName;
	}

	public void parse() throws IOException {
		if (this.csvFile == null) {
			LOGGER.log(Level.SEVERE, "CSV file is null");
			return;
		}

		if (this.longitudeColumnName == null || this.latitudeColumnName == null) {
			LOGGER.log(Level.SEVERE, "Longitude / Latitude column name are mandatory.");
			return;
		}

		String lonColName = this.longitudeColumnName;
		String latColName = this.latitudeColumnName;

		Reader reader = null;

		try {
			CSVParser csvParser = null;
			Map<String, Integer> header = null;
			// Verify if we can read the file header using one of the CSV Strategy
			// (the CSV file can use different characters for separator / string delimiter)
			for (CSVStrategy csvStrategy : CSV_STRATEGIES) {
				reader = new FileReader(this.csvFile);
				csvParser = new CSVParser(reader, csvStrategy);

				String[] headerArr = csvParser.getLine();
				header = new HashMap<String, Integer>();
				if (headerArr != null) {
					for (int i=0; i<headerArr.length; i++) {
						String label = headerArr[i];
						if (label != null) {
							label = label.trim();
							if (!label.isEmpty()) {
								// Fix for messed-up CSV file (MS Notepad add invisible chars that confuse the parser)
								String stringDelimiter = "" + csvStrategy.getEncapsulator();
								if (label.contains(stringDelimiter)) {
									String quotedStringDelimiter = Pattern.quote(stringDelimiter);
									label = label.replaceAll("^.*" + quotedStringDelimiter +"(.+)" + quotedStringDelimiter + ".*$", "$1");
								}
								header.put(label, i);
							}
						}
					}
				}

				if (!header.containsKey(lonColName) || !header.containsKey(latColName)) {
					// The CSV Strategy is no good, try an other one...
					try {
						reader.close();
						reader = null;
					} catch(Exception ex) {
						LOGGER.log(Level.WARNING, "Can not close the CSV file reader", ex);
					}
				} else {
					// The CSV Strategy is good, let's parse that file!
					break;
				}
			}

			if (header == null || !header.containsKey(lonColName) || !header.containsKey(latColName)) {
				throw new IOException("Invalid CSV file");
			} else {
				// Read each line of the CSV, one by one
				String[] line;
				int lonIndex = header.get(lonColName);
				int latIndex = header.get(latColName);
				while ((line = csvParser.getLine()) != null) {
					// Ignore commented lines and line that do not have a filename
					String longitudeStr = line[lonIndex];
					String latitudeStr = line[latIndex];
					if (!line[0].startsWith(CSV_COMMENT_PREFIX) &&
							longitudeStr != null && !longitudeStr.isEmpty() &&
							latitudeStr != null && !latitudeStr.isEmpty()) {

						this.parsePoint(header, line, lonIndex, latIndex);
					}
				}
			}

		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch(Exception ex) {
					LOGGER.log(Level.WARNING, "Can not close the CSV file reader", ex);
				}
			}
		}
	}

	private void parsePoint(Map<String, Integer> header, String[] line, int lonIndex, int latIndex) {
		String longitudeStr = line[lonIndex];
		String latitudeStr = line[latIndex];

		double longitude = Double.parseDouble(longitudeStr);
		double latitude = Double.parseDouble(latitudeStr);

		Point2D.Double point = new Point2D.Double(longitude, latitude);

		Map<String, PropertyValue> properties = new HashMap<String, PropertyValue>();;
		for (Map.Entry<String, Integer> headerEntry : header.entrySet()) {
			String propertyName = headerEntry.getKey();
			String valueStr = line[headerEntry.getValue()];

			PropertyValue propertyValue;
			if (SldUtils.isNumeric(valueStr)) {
				propertyValue = new PropertyValue(Double.parseDouble(valueStr));
			} else {
				propertyValue = new PropertyValue(valueStr);
			}

			properties.put(propertyName, propertyValue);
		}

		this.add(point, properties);
	}
}
