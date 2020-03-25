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

import au.gov.aims.layers2svg.graphics.GeoGraphicsFormat;
import au.gov.aims.layers2svg.graphics.GeoJSONShape;
import au.gov.aims.layers2svg.graphics.VectorRasterGraphics2D;
import au.gov.aims.sld.SldParser;
import au.gov.aims.sld.StyleSheet;
import au.gov.aims.sld.TextAlignment;
import au.gov.aims.sld.geom.GeoShape;
import au.gov.aims.sld.geom.Layer;
import org.json.JSONObject;
import org.junit.Ignore;
import org.junit.Test;

import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RenderTest {
	private static final Logger LOGGER = Logger.getLogger(RenderTest.class.getSimpleName());

	@Test
	public void testRender() throws Exception {
		LOGGER.log(Level.INFO, "GENERATING SVG");

		int width = 800;
		int height = 600;

		AffineTransform transform = new AffineTransform();
		transform.concatenate(AffineTransform.getScaleInstance(30, -30));
		transform.concatenate(AffineTransform.getTranslateInstance(-135, 10));

		//int scale = 3000000;
		int scale = 2999999;


		/**
		 * Drawing
		 */

		// Set the background colour
		// NOTE: g2d.setBackground do not work
		//   g2d.setBackground(new Color(200, 220, 220));
/*
		Rectangle2D background = new Rectangle2D.Double(0, 0, width, height);
		Layer backgroundLayer = new Layer("Background");
		GeoShape backgroundGeoShape = new GeoShape(background, null);
		backgroundGeoShape.setFillPaint(new Color(200, 220, 220));

		GeoShapeGroup group = new GeoShapeGroup("background");
		group.add(backgroundGeoShape);
		backgroundLayer.add(group);
*/

		Rectangle2D background = new Rectangle2D.Double(0, 0, width, height);
		GeoShape backgroundGeoShape = new GeoShape(background, null);
		backgroundGeoShape.setFillPaint(new Color(200, 220, 220));



		InputStream mainlandStream = RenderTest.class.getClassLoader().getResourceAsStream("layers/GBR_GBRMPA_GBR-features_Mainland_300m.geojson");
		String mainlandString = Layers2SVGUtils.readFile(mainlandStream);
		JSONObject mainlandJson = new JSONObject(mainlandString);
		mainlandStream.close();

		GeoJSONShape mainland = new GeoJSONShape(mainlandJson, "Mainland");
		mainland.parse();
		Layer mainlandLayer = new Layer("Mainland");
		mainlandLayer.add(mainland);


		InputStream reefsStream = RenderTest.class.getClassLoader().getResourceAsStream("layers/GBR_GBRMPA_Reefs_100m.geojson");
		//InputStream reefsStream = RenderTest.class.getClassLoader().getResourceAsStream("layers/GBR_GBRMPA_Reefs_100m_filtered.geojson");
		String reefsString = Layers2SVGUtils.readFile(reefsStream);
		JSONObject reefsJson = new JSONObject(reefsString);
		reefsStream.close();

		GeoJSONShape reefs = new GeoJSONShape(reefsJson, "Reefs");
		reefs.parse();
		Layer reefsLayer = new Layer("Reefs");
		reefsLayer.add(reefs);


		InputStream catchmentsStream = RenderTest.class.getClassLoader().getResourceAsStream("layers/AU_GA_River-basins-1997_GBR-catchments.geojson");
		String catchmentsString = Layers2SVGUtils.readFile(catchmentsStream);
		JSONObject catchmentsJson = new JSONObject(catchmentsString);
		catchmentsStream.close();

		GeoJSONShape catchments = new GeoJSONShape(catchmentsJson, "Catchments");
		catchments.parse();
		Layer catchmentsLayer = new Layer("Catchments");
		catchmentsLayer.add(catchments);


		InputStream gbrmpaStream = RenderTest.class.getClassLoader().getResourceAsStream("layers/GBR_e-Atlas-GBRMPA_GBRMP-bounds_Ocean-bounds.geojson");
		String gbrmpaString = Layers2SVGUtils.readFile(gbrmpaStream);
		JSONObject gbrmpaJson = new JSONObject(gbrmpaString);
		gbrmpaStream.close();

		GeoJSONShape gbrmpa = new GeoJSONShape(gbrmpaJson, "GBRMPA Bounds");
		gbrmpa.parse();
		Layer gbrmpaLayer = new Layer("GBRMPA Bounds");
		gbrmpaLayer.add(gbrmpa);


		InputStream citiesStream = RenderTest.class.getClassLoader().getResourceAsStream("layers/GBR_NERP-TE-13-1_eAtlas-NE_10m-GBR-cities.geojson");
		//InputStream citiesStream = RenderTest.class.getClassLoader().getResourceAsStream("layers/GBR_NERP-TE-13-1_eAtlas-NE_2-GBR-cities.geojson");
		String citiesString = Layers2SVGUtils.readFile(citiesStream);
		JSONObject citiesJson = new JSONObject(citiesString);
		citiesStream.close();

		GeoJSONShape cities = new GeoJSONShape(citiesJson, "Cities");
		cities.parse();
		Layer citiesLayer = new Layer("Cities");
		citiesLayer.add(cities);


		Layer mainlandScaled = mainlandLayer.createTransformedLayer(transform);
		Layer reefsScaled = reefsLayer.createTransformedLayer(transform);
		Layer catchmentsScaled = catchmentsLayer.createTransformedLayer(transform);
		Layer gbrmpaScaled = gbrmpaLayer.createTransformedLayer(transform);
		Layer citiesScaled = citiesLayer.createTransformedLayer(transform);


		// Load style sheets (SLD)
		SldParser parser = new SldParser();
		StyleSheet riverBasins1997OutlineStyle  = this.getStyleSheet(parser, "styles/River-basins-1997_Outline.sld");
		StyleSheet gbr10mGBRCitiesStyle         = this.getStyleSheet(parser, "styles/GBR_10m-GBR-cities.sld");
		gbr10mGBRCitiesStyle.setFontSizeRatio(2.0f);

		StyleSheet gbrFeaturesStyle             = this.getStyleSheet(parser, "styles/GBR-features.sld");
		//StyleSheet gbrFeaturesOutlookStyle      = this.getStyleSheet(parser, "styles/GBR-features_Outlook_no-label.sld");
		StyleSheet gbrFeaturesOutlookStyle      = this.getStyleSheet(parser, "styles/GBR-features_Outlook.sld");
		gbrFeaturesOutlookStyle.setStrokeWidthRatio(10.0f);

		StyleSheet seltmpReefsStyle             = this.getStyleSheet(parser, "styles/SELTMP_Reefs.sld");
		//StyleSheet seltmpReefsSimplifiedStyle   = this.getStyleSheet(parser, "styles/SELTMP_Reefs_simplified.sld");
		StyleSheet polygonOutlineRedStyle       = this.getStyleSheet(parser, "styles/Polygon_Outline-Red.sld");


		// TODO TextSymbolizer => Calculate the size of the rendered text and move it where it should be.

		// Apply style to layers
		List<Layer> catchmentsStyledLayers = riverBasins1997OutlineStyle.generateStyledLayers(catchmentsScaled, scale);
		List<Layer> citiesStyledLayers     = gbr10mGBRCitiesStyle.generateStyledLayers(citiesScaled, scale);

		//List<Layer> mainlandStyledLayers   = gbrFeaturesStyle.generateStyledLayers(mainlandScaled, scale);
		List<Layer> mainlandStyledLayers   = gbrFeaturesOutlookStyle.generateStyledLayers(mainlandScaled, scale);

		List<Layer> reefsStyledLayers      = seltmpReefsStyle.generateStyledLayers(reefsScaled, scale);
		//List<Layer> reefsStyledLayers      = seltmpReefsSimplifiedStyle.generateStyledLayers(reefsScaled, scale);

		List<Layer> gbrmpaStyledLayers     = polygonOutlineRedStyle.generateStyledLayers(gbrmpaScaled, scale);



		VectorRasterGraphics2D g2d = new VectorRasterGraphics2D(width, height, -20);

		// Antialiasing text
		g2d.setRenderingHint(
			RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		// Fill background
		g2d.setPaint(new Color(200, 220, 220));
		g2d.fillRect(0, 0, width, height);

		// Draw layer using the style
//		g2d.fillAndStroke(backgroundLayer);
		g2d.fillAndStroke(reefsStyledLayers);
		g2d.fillAndStroke(mainlandStyledLayers);
		g2d.fillAndStroke(catchmentsStyledLayers);
		g2d.fillAndStroke(gbrmpaStyledLayers);
		g2d.fillAndStroke(citiesStyledLayers);

		g2d.setPaint(new Color(200, 0, 0));
		g2d.fillRect(0, 0, 10, 10);

		g2d.drawString("Test align Right", width, 20, TextAlignment.RIGHT);

		// Save
		String filename = "/tmp/test";
		this.render(g2d, GeoGraphicsFormat.SVG, filename);
		this.render(g2d, GeoGraphicsFormat.PNG, filename);
		this.render(g2d, GeoGraphicsFormat.JPG, filename);

		g2d.dispose();
	}


	/**
	 * Not implemented
	 * This feature needs to be implemented in the SLD library
	 * - au.gov.aims.sld.geom.GeoShape
	 *     Add a rotation attribute
	 * - au.gov.aims.sld.symbolizer.TextSymbolizer (and other symbolizers)
	 *     Set rotation in GeoShape
	 *  Rotate the label when render (?)
	 */
	@Test
	@Ignore
	public void testRenderRotatedLabels() throws Exception {
		LOGGER.log(Level.INFO, "GENERATING SVG");

		int width = 800;
		int height = 600;

		AffineTransform transform = new AffineTransform();
		transform.concatenate(AffineTransform.getScaleInstance(30, -30));
		transform.concatenate(AffineTransform.getTranslateInstance(-135, 10));

		int scale = 2999999;


		// Drawing

		Rectangle2D background = new Rectangle2D.Double(0, 0, width, height);
		GeoShape backgroundGeoShape = new GeoShape(background, null);
		backgroundGeoShape.setFillPaint(new Color(200, 220, 220));


		InputStream mainlandStream = RenderTest.class.getClassLoader().getResourceAsStream("layers/GBR_GBRMPA_GBR-features_Mainland_300m.geojson");
		String mainlandString = Layers2SVGUtils.readFile(mainlandStream);
		JSONObject mainlandJson = new JSONObject(mainlandString);
		mainlandStream.close();

		GeoJSONShape mainland = new GeoJSONShape(mainlandJson, "Mainland");
		mainland.parse();
		Layer mainlandLayer = new Layer("Mainland");
		mainlandLayer.add(mainland);


		InputStream citiesStream = RenderTest.class.getClassLoader().getResourceAsStream("layers/GBR_NERP-TE-13-1_eAtlas-NE_10m-GBR-cities.geojson");
		String citiesString = Layers2SVGUtils.readFile(citiesStream);
		JSONObject citiesJson = new JSONObject(citiesString);
		citiesStream.close();

		GeoJSONShape cities = new GeoJSONShape(citiesJson, "Cities");
		cities.parse();
		Layer citiesLayer = new Layer("Cities");
		citiesLayer.add(cities);


		Layer mainlandScaled = mainlandLayer.createTransformedLayer(transform);
		Layer citiesScaled = citiesLayer.createTransformedLayer(transform);


		// Load style sheets (SLD)
		SldParser parser = new SldParser();

		StyleSheet gbrFeaturesOutlookStyle      = this.getStyleSheet(parser, "styles/GBR-features_rotated.sld");
		gbrFeaturesOutlookStyle.setStrokeWidthRatio(10.0f);


		// TODO TextSymbolizer => Calculate the size of the rendered text and move it where it should be.

		// Apply style to layers
		List<Layer> mainlandStyledLayers   = gbrFeaturesOutlookStyle.generateStyledLayers(mainlandScaled, scale);


		VectorRasterGraphics2D g2d = new VectorRasterGraphics2D(width, height, -20);

		// Antialiasing text
		g2d.setRenderingHint(
			RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		// Fill background
		g2d.setPaint(new Color(200, 220, 220));
		g2d.fillRect(0, 0, width, height);

		// Draw layer using the style
		g2d.fillAndStroke(mainlandStyledLayers);

		// Save
		String filename = "/tmp/test";
		this.render(g2d, GeoGraphicsFormat.SVG, filename);
		this.render(g2d, GeoGraphicsFormat.PNG, filename);
		this.render(g2d, GeoGraphicsFormat.JPG, filename);

		g2d.dispose();
	}

	private void render(VectorRasterGraphics2D g2d, GeoGraphicsFormat format, String filename) throws IOException {
		String filePath = filename + "." + format.getExtension();
		g2d.render(format, new File(filePath));
		LOGGER.log(Level.INFO, "GENERATION DONE: " + filePath);
	}

	private StyleSheet getStyleSheet(SldParser parser, String sldPath) throws Exception {
		StyleSheet styleSheet = null;
		InputStream sldStream = null;
		try {
			sldStream = RenderTest.class.getClassLoader().getResourceAsStream(sldPath);
			styleSheet = parser.parse(sldStream);
		} finally {
			if (sldStream != null) {
				sldStream.close();
			}
		}

		return styleSheet;
	}
}
