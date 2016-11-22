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
import au.gov.aims.sld.geom.Layer;
import org.json.JSONObject;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class DEPRECATED_TestCatchments {
	public static void main(String ... args) throws IOException {
		System.out.println("GENERATING SVG");

		int width = 800;
		int height = 600;

		String svgFilename = "/tmp/testCatchments.svg";
		String pngFilename = "/tmp/testCatchments.png";

		VectorRasterGraphics2D g2d = new VectorRasterGraphics2D(width, height, 200);

		// Antialiasing text
		g2d.setRenderingHint(
			RenderingHints.KEY_TEXT_ANTIALIASING,
			RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

		// Rounded strokes
		g2d.setStroke(new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		// Set the background colour
		// NOTE: g2d.setBackground do not work
		//   g2d.setBackground(new Color(200, 220, 220));
		g2d.setPaint(new Color(200, 220, 220));
		g2d.fillRect(0, 0, width, height);

		g2d.setFont(g2d.getFont().deriveFont(Font.PLAIN, 70));



		/**
		 * Drawing
		 */
		InputStream catchmentsStream = DEPRECATED_Poc.class.getClassLoader().getResourceAsStream("layers/GBR_e-Atlas-GBRMPA_GBRMP-bounds_Ocean-bounds.geojson");
		String catchmentsString = Utils.readFile(catchmentsStream);
		JSONObject catchmentsJson = new JSONObject(catchmentsString);
		catchmentsStream.close();

		GeoJSONShape catchments = new GeoJSONShape(catchmentsJson, "Catchments");
		catchments.parse();
		Layer catchmentsLayer = new Layer("Catchments");
		catchmentsLayer.add(catchments);


		AffineTransform transform = new AffineTransform();
		transform.concatenate(AffineTransform.getScaleInstance(30, -30));
		transform.concatenate(AffineTransform.getTranslateInstance(-135, 10));

		Layer catchmentsScaled = catchmentsLayer.createTransformedLayer(transform);

		g2d.setPaint(new Color(220, 0, 0));
		g2d.fillAndStroke(catchmentsScaled);


		// Save as SVG file
		g2d.render(GeoGraphicsFormat.SVG, new File(svgFilename));
		System.out.println("GENERATION DONE: " + svgFilename);

		// Save as PNG file
		g2d.render(GeoGraphicsFormat.PNG, new File(pngFilename));
		System.out.println("GENERATION DONE: " + pngFilename);

		g2d.dispose();
	}
}
