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
import au.gov.aims.sld.geom.GeoShapeGroup;
import au.gov.aims.sld.geom.Layer;
import org.json.JSONObject;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class DEPRECATED_TestHole {
	public static void main(String ... args) throws IOException {
		System.out.println("GENERATING SVG");

		int width = 800;
		int height = 600;

		String svgFilename = "/tmp/testHole.svg";
		String pngFilename = "/tmp/testHole.png";

		VectorRasterGraphics2D g2d = new VectorRasterGraphics2D(width, height, 0);

		// Antialiasing text
		g2d.setRenderingHint(
			RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		// Rounded strokes
		BasicStroke mainlandStroke = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		g2d.setStroke(mainlandStroke);

		// Set the background colour
		g2d.setBackground(new Color(200, 220, 220));



		InputStream mainlandStream = DEPRECATED_Poc.class.getClassLoader().getResourceAsStream("layers/polyHole.geojson");
		String mainlandString = Utils.readFile(mainlandStream);
		JSONObject mainlandJson = new JSONObject(mainlandString);
		mainlandStream.close();

		GeoShapeGroup mainland = new GeoJSONShape(mainlandJson, "mainland");
		Layer mainlandLayer = new Layer("Polygon with holes");
		mainlandLayer.add(mainland);


		AffineTransform transform = new AffineTransform();
		transform.concatenate(AffineTransform.getScaleInstance(30, -30));
		transform.concatenate(AffineTransform.getTranslateInstance(-45, -5));
		Layer mainlandScaled = mainlandLayer.createTransformedLayer(transform);

		g2d.setPaint(new Color(150, 150, 150));
//		g2d.setStrokePaint(new Color(0, 0, 0));
		g2d.fillAndStroke(mainlandScaled);



		// Save as SVG file
		g2d.render(GeoGraphicsFormat.SVG, new File(svgFilename));
		System.out.println("GENERATION DONE: " + svgFilename);

		// Save as PNG file
		g2d.render(GeoGraphicsFormat.PNG, new File(pngFilename));
		System.out.println("GENERATION DONE: " + pngFilename);

		g2d.dispose();
	}
}
