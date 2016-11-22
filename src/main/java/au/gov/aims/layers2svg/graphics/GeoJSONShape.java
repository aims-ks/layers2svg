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
import au.gov.aims.sld.geom.GeoShapeGroup;
import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GeoJSON Specs:
 *   http://geojson.org/geojson-spec.html
 */
public class GeoJSONShape extends GeoShapeGroup {
	private static final Logger LOGGER = Logger.getLogger(GeoJSONShape.class.getSimpleName());

	private JSONObject geoJson;

	/**
	 * geoJson = {
	 *   "type": "FeatureCollection",
	 *   "features": [ ... ]
	 * }
	 */
	public GeoJSONShape(JSONObject geoJson, String name) {
		super(name);
		this.geoJson = geoJson;
	}

	public void parse() {
		if (this.geoJson == null) {
			LOGGER.log(Level.SEVERE, "GeoJSON is null.");
		} else {
			String type = this.geoJson.optString("type", null);
			if (type == null) {
				LOGGER.log(Level.SEVERE, "GeoJSON has no type defined.");
			} else {
				// "Point", "MultiPoint", "LineString", "MultiLineString", "Polygon", "MultiPolygon", "GeometryCollection", "Feature", or "FeatureCollection"
				if ("FeatureCollection".equals(type)) {
					JSONArray featureCollection = this.geoJson.optJSONArray("features");
					if (featureCollection == null) {
						LOGGER.log(Level.SEVERE, "GeoJSON contains no feature.");
					} else {
						this.parseFeatureCollection(featureCollection);
					}
				} else {
					LOGGER.log(Level.SEVERE, "Unsupported GeoJSON type '" + type + "'.");
				}
			}
		}
	}


	/**
	 * featureCollection = [
	 *   { ... },
	 *   { ... }
	 * ]
	 */
	private void parseFeatureCollection(JSONArray featureCollection) {
		if (featureCollection != null) {
			for (int i=0; i<featureCollection.length(); i++) {
				JSONObject feature = featureCollection.optJSONObject(i);
				if (feature != null) {
					this.parseFeature(feature);
				}
			}
		}
	}

	/**
	 * feature = {
	 *   "type": "Feature",
	 *   "properties": { ... },
	 *   "geometry": { ... }
	 * }
	 */
	private void parseFeature(JSONObject feature) {
		String type = feature.optString("type", null);

		if (type == null) {
			LOGGER.log(Level.SEVERE, "Feature has no type defined.");
		} else {
			if ("Feature".equals(type)) {
				// "properties" contains a list of "Key: Value" pairs (metadata).
				JSONObject jsonProperties = feature.optJSONObject("properties");
				Map<String, PropertyValue> properties = this.parseProperties(jsonProperties);

				JSONObject jsonGeometry = feature.optJSONObject("geometry");
				if (jsonGeometry == null) {
					LOGGER.log(Level.SEVERE, "GeoJSON contains empty geometry.");
				} else {
					this.parseGeometry(jsonGeometry, properties);
				}
			} else {
				LOGGER.log(Level.SEVERE, "Unsupported feature type '" + type + "'.");
			}
		}
	}

	private Map<String, PropertyValue> parseProperties(JSONObject jsonProperties) {
		Map<String, PropertyValue> properties = new HashMap<String, PropertyValue>();
		for (String key : jsonProperties.keySet()) {
			Object value = jsonProperties.opt(key);
			if (value != null) {
				properties.put(key, new PropertyValue(value));
			}
		}
		return properties;
	}

	/**
	 * geometry = {
	 *   "type": "LineString",
	 *   "coordinates": [ [ longitude, latitude ], ... ]
	 * }
	 * OR
	 * geometry = {
	 *   "type": "MultiLineString",
	 *   "coordinates": [ [ [ longitude, latitude ], ... ], ... ]
	 * }
	 * OR
	 * geometry = {
	 *   "type": "Polygon",
	 *   "coordinates": [ [ [ longitude, latitude ], ... ], ... ]
	 * }
	 * OR
	 * geometry = {
	 *   "type": "MultiPolygon",
	 *   "coordinates": [ [ [ [ longitude, latitude ], ... ], ... ], ... ]
	 * }
	 * OR
	 * geometry = {
	 *   "type": "Point",
	 *   "coordinates": [ longitude, latitude ]
	 * }
	 */
	private void parseGeometry(JSONObject geometry, Map<String, PropertyValue> properties) {
		String type = geometry.optString("type", null);

		if (type == null) {
			LOGGER.log(Level.SEVERE, "Geometry has no type defined.");
		} else {
			// "Point", "MultiPoint", "LineString", "MultiLineString", "Polygon", "MultiPolygon", or "GeometryCollection"
			if ("LineString".equals(type)) {
				JSONArray coordinates = geometry.optJSONArray("coordinates");
				if (coordinates == null) {
					LOGGER.log(Level.SEVERE, "GeoJSON contains empty LineString coordinates.");
				} else {
					this.parseLineString(coordinates, properties);
				}
			} else if ("MultiLineString".equals(type)) {
				JSONArray multiLineString = geometry.optJSONArray("coordinates");
				if (multiLineString == null) {
					LOGGER.log(Level.SEVERE, "GeoJSON contains empty MultiLineString coordinates.");
				} else {
					for (int i=0; i<multiLineString.length(); i++) {
						JSONArray coordinates = multiLineString.optJSONArray(i);
						if (coordinates == null) {
							LOGGER.log(Level.SEVERE, "GeoJSON contains empty MultiLineString coordinates.");
						} else {
							this.parseLineString(coordinates, properties);
						}
					}
				}
			} else if ("Polygon".equals(type)) {
				JSONArray coordinates = geometry.optJSONArray("coordinates");
				if (coordinates == null) {
					LOGGER.log(Level.SEVERE, "GeoJSON contains empty Polygon coordinates.");
				} else {
					this.parsePolygon(coordinates, properties);
				}
			} else if ("MultiPolygon".equals(type)) {
				JSONArray multiPolygon = geometry.optJSONArray("coordinates");
				if (multiPolygon == null) {
					LOGGER.log(Level.SEVERE, "GeoJSON contains empty MultiPolygon coordinates.");
				} else {
					for (int i=0; i<multiPolygon.length(); i++) {
						JSONArray coordinates = multiPolygon.optJSONArray(i);
						if (coordinates == null) {
							LOGGER.log(Level.SEVERE, "GeoJSON contains empty MultiPolygon coordinates.");
						} else {
							this.parsePolygon(coordinates, properties);
						}
					}
				}
			} else if ("Point".equals(type)) {
				JSONArray point = geometry.optJSONArray("coordinates");
				if (point == null) {
					LOGGER.log(Level.SEVERE, "GeoJSON contains empty Point coordinates.");
				} else {
					this.parsePoint(point, properties);
				}

			} else {
				LOGGER.log(Level.SEVERE, "Unsupported feature type '" + type + "'.");
			}
		}
	}

	/**
	 * jsonCoordinates = [
	 *   [ longitude, latitude ],
	 *   ...
	 * ]
	 */
	private void parseLineString(JSONArray jsonCoordinates, Map<String, PropertyValue> properties) {
		Path2D.Double line = new Path2D.Double();

		/**
		 * startPoint = [ longitude, latitude ]
		 */
		JSONArray startPoint = jsonCoordinates.optJSONArray(0);
		if (startPoint != null && startPoint.length() == 2) {
			double longitude = startPoint.optDouble(0);
			double latitude = startPoint.optDouble(1);
			line.moveTo(longitude, latitude);

			for (int pointIndex = 1; pointIndex < jsonCoordinates.length(); pointIndex++) {
				/**
				 * point = [ longitude, latitude ]
				 */
				JSONArray point = jsonCoordinates.optJSONArray(pointIndex);
				if (point != null && point.length() == 2) {
					longitude = point.optDouble(0);
					latitude = point.optDouble(1);
					line.lineTo(longitude, latitude);
				}
			}
		}

		this.add(line, properties);
	}

	/**
	 * jsonCoordinates = [
	 *   [
	 *     [ longitude, latitude ],
	 *     ...
	 *   ],
	 *   ...
	 * ]
	 */
	private void parsePolygon(JSONArray jsonCoordinates, Map<String, PropertyValue> properties) {
		Path2D.Double polygon = new Path2D.Double(Path2D.WIND_EVEN_ODD);
		/**
		 * NOTE:
		 *   GeoJSON rule: First polygon is a filled shape. Subsequent polygons are holes in the shape.
		 *   Path2D.WIND_EVEN_ODD rule: Shape which intersect create hole.
		 *   The implementation used here do not respect the GeoJSON rule, but it should be ok
		 *   since the GeoJSON has been generated therefore there should be no intersection other than holes.
		 */
		for (int polygonIndex = 0; polygonIndex < jsonCoordinates.length(); polygonIndex++) {
			/**
			 * jsonPolygon = [
			 *   [ longitude, latitude ],
			 *   ...
			 * ]
			 */
			JSONArray jsonPolygon = jsonCoordinates.optJSONArray(polygonIndex);
			if (jsonPolygon != null && jsonPolygon.length() > 1) {
				/**
				 * startPoint = [ longitude, latitude ]
				 */
				JSONArray startPoint = jsonPolygon.optJSONArray(0);
				if (startPoint != null && startPoint.length() == 2) {
					double longitude = startPoint.optDouble(0);
					double latitude = startPoint.optDouble(1);
					polygon.moveTo(longitude, latitude);

					for (int pointIndex = 1; pointIndex < jsonPolygon.length(); pointIndex++) {
						/**
						 * point = [ longitude, latitude ]
						 */
						JSONArray point = jsonPolygon.optJSONArray(pointIndex);
						if (point != null && point.length() == 2) {
							longitude = point.optDouble(0);
							latitude = point.optDouble(1);
							polygon.lineTo(longitude, latitude);
						}
					}

					polygon.closePath();
				}
			}
		}

		this.add(polygon, properties);
	}

	/**
	 * coordinates = [ longitude, latitude ]
	 */
	private void parsePoint(JSONArray coordinates, Map<String, PropertyValue> properties) {
		if (coordinates != null && coordinates.length() == 2) {
			Point2D.Double point = new Point2D.Double(
				coordinates.optDouble(0),
				coordinates.optDouble(1)
			);

			this.add(point, properties);
		}
	}
}
