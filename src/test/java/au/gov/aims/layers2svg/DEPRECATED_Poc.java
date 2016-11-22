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
package au.gov.aims.layers2svg;

import au.gov.aims.layers2svg.graphics.VectorRasterGraphics2D;
import au.gov.aims.layers2svg.graphics.GeoGraphicsFormat;
import au.gov.aims.layers2svg.graphics.GeoJSONShape;
import au.gov.aims.sld.geom.GeoShape;
import au.gov.aims.sld.geom.GeoShapeGroup;
import au.gov.aims.sld.geom.Layer;
import org.json.JSONObject;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class DEPRECATED_Poc {
	public static void main(String ... args) throws IOException {
		System.out.println("GENERATING SVG");

		int width = 8000;
		int height = 6000;

		String svgFilename = "/tmp/test.svg";
		String pngFilename = "/tmp/test.png";

		VectorRasterGraphics2D g2d = new VectorRasterGraphics2D(width, height, -20);


		// Antialiasing text
		g2d.setRenderingHint(
			RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		// Rounded strokes
		g2d.setStroke(new BasicStroke(5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2d.setFont(g2d.getFont().deriveFont(Font.PLAIN, 70));


		/**
		 * Drawing
		 */

		// Set the background colour
		// NOTE: g2d.setBackground do not work
		//   g2d.setBackground(new Color(200, 220, 220));
		Rectangle2D background = new Rectangle2D.Double(0, 0, width, height);
		Layer backgroundLayer = new Layer("Background");
		GeoShape backgroundGeoShape = new GeoShape(background, null);
		backgroundGeoShape.setFillPaint(new Color(200, 220, 220));

		GeoShapeGroup group = new GeoShapeGroup("background");
		group.add(backgroundGeoShape);
		backgroundLayer.add(group);



		InputStream mainlandStream = DEPRECATED_Poc.class.getClassLoader().getResourceAsStream("layers/GBR_GBRMPA_GBR-features_Mainland_300m.geojson");
		String mainlandString = Utils.readFile(mainlandStream);
		JSONObject mainlandJson = new JSONObject(mainlandString);
		mainlandStream.close();

		GeoJSONShape mainland = new GeoJSONShape(mainlandJson, "Mainland");
		mainland.parse();
		Layer mainlandLayer = new Layer("Mainland");
		mainlandLayer.add(mainland);


		InputStream reefsStream = DEPRECATED_Poc.class.getClassLoader().getResourceAsStream("layers/GBR_GBRMPA_Reefs_100m.geojson");
		String reefsString = Utils.readFile(reefsStream);
		JSONObject reefsJson = new JSONObject(reefsString);
		reefsStream.close();

		GeoJSONShape reefs = new GeoJSONShape(reefsJson, "Reefs");
		reefs.parse();
		Layer reefsLayer = new Layer("Reefs");
		reefsLayer.add(reefs);


		InputStream catchmentsStream = DEPRECATED_Poc.class.getClassLoader().getResourceAsStream("layers/AU_GA_River-basins-1997_GBR-catchments.geojson");
		String catchmentsString = Utils.readFile(catchmentsStream);
		JSONObject catchmentsJson = new JSONObject(catchmentsString);
		catchmentsStream.close();

		GeoJSONShape catchments = new GeoJSONShape(catchmentsJson, "Catchments");
		catchments.parse();
		Layer catchmentsLayer = new Layer("Catchments");
		catchmentsLayer.add(catchments);


		InputStream gbrmpaStream = DEPRECATED_Poc.class.getClassLoader().getResourceAsStream("layers/GBR_e-Atlas-GBRMPA_GBRMP-bounds_Ocean-bounds.geojson");
		String gbrmpaString = Utils.readFile(gbrmpaStream);
		JSONObject gbrmpaJson = new JSONObject(gbrmpaString);
		gbrmpaStream.close();

		GeoJSONShape gbrmpa = new GeoJSONShape(gbrmpaJson, "GBRMPA Bounds");
		gbrmpa.parse();
		Layer gbrmpaLayer = new Layer("GBRMPA Bounds");
		gbrmpaLayer.add(gbrmpa);


		InputStream citiesStream = DEPRECATED_Poc.class.getClassLoader().getResourceAsStream("layers/GBR_NERP-TE-13-1_eAtlas-NE_10m-GBR-cities.geojson");
		String citiesString = Utils.readFile(citiesStream);
		JSONObject citiesJson = new JSONObject(citiesString);
		citiesStream.close();

		GeoJSONShape cities = new GeoJSONShape(citiesJson, "Cities");
		cities.parse();
		Layer citiesLayer = new Layer("Cities");
		citiesLayer.add(cities);


		AffineTransform transform = new AffineTransform();
		transform.concatenate(AffineTransform.getScaleInstance(300, -300));
		transform.concatenate(AffineTransform.getTranslateInstance(-135, 10));

		Layer mainlandScaled = mainlandLayer.createTransformedLayer(transform);
		Layer reefsScaled = reefsLayer.createTransformedLayer(transform);
		Layer catchmentsScaled = catchmentsLayer.createTransformedLayer(transform);
		Layer gbrmpaScaled = gbrmpaLayer.createTransformedLayer(transform);
		Layer citiesScaled = citiesLayer.createTransformedLayer(transform);


		g2d.setPaint(new Color(200, 220, 220));
		g2d.fillAndStroke(backgroundLayer);

		g2d.setPaint(new Color(150, 200, 200));
//		g2d.setStrokePaint(new Color(0, 150, 150));
		g2d.fillAndStroke(reefsScaled);

		g2d.setPaint(new Color(150, 150, 150));
//		g2d.setStrokePaint(new Color(0, 0, 0));
		g2d.fillAndStroke(mainlandScaled);

		g2d.setPaint(new Color(220, 0, 0));
		g2d.fillAndStroke(catchmentsScaled);

		// Larger stroke for GBRMPA
		g2d.setStroke(new BasicStroke(20.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g2d.setPaint(new Color(0, 200, 0));
		g2d.fillAndStroke(gbrmpaScaled);

		g2d.setStroke(new BasicStroke(15.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
//		g2d.setMarkerStrokePaint(new Color(200, 0, 0));
//		g2d.setMarkerPaint(new Color(0, 200, 0));
//		g2d.setTextPaint(new Color(0, 0, 200));
		g2d.fillAndStroke(citiesScaled);


		// Save as SVG file
		g2d.render(GeoGraphicsFormat.SVG, new File(svgFilename));
		System.out.println("GENERATION DONE: " + svgFilename);

		// Save as PNG file
		g2d.render(GeoGraphicsFormat.PNG, new File(pngFilename));
		System.out.println("GENERATION DONE: " + pngFilename);

		g2d.dispose();
	}
}
