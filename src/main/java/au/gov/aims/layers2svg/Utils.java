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

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Utils {
	/**
	 * Read a UTF-8 file into a String.
	 * @param file
	 * @return
	 * @throws IOException
	 */
	public static String readFile(File file) throws IOException {
		return Utils.readFile(file, StandardCharsets.UTF_8, false);
	}

	public static String readFile(File file, boolean ignoreComments) throws IOException {
		return Utils.readFile(file, StandardCharsets.UTF_8, ignoreComments);
	}

	public static String readFile(File file, Charset encoding) throws IOException {
		return Utils.readFile(file, encoding, false);
	}

	public static String readFile(File file, Charset encoding, boolean ignoreComments) throws IOException {
		if (file == null || !file.exists() || !file.canRead()) {
			return null;
		}

		InputStream inputStream = new FileInputStream(file);

		String content;
		try {
			content = Utils.readFile(inputStream, encoding, ignoreComments);
		} finally {
			inputStream.close();
		}

		return content;
	}

	public static String readFile(InputStream inputStream) throws IOException {
		return Utils.readFile(inputStream, StandardCharsets.UTF_8, false);
	}

	public static String readFile(InputStream inputStream, boolean ignoreComments) throws IOException {
		return Utils.readFile(inputStream, StandardCharsets.UTF_8, ignoreComments);
	}

	/**
	 * Read a file into a String.
	 * See: http://stackoverflow.com/questions/326390/how-to-create-a-java-string-from-the-contents-of-a-file#326440
	 * @param inputStream
	 * @param encoding
	 * @return
	 * @throws IOException
	 */
	public static String readFile(InputStream inputStream, Charset encoding, boolean ignoreComments) throws IOException {
		if (inputStream == null) {
			return null;
		}

		StringBuilder sb = new StringBuilder();
		BufferedReader bufferedReader = null;

		try {
			bufferedReader = new BufferedReader(new InputStreamReader(inputStream, encoding));

			String line;
			while ((line = bufferedReader.readLine()) != null) {
				if (!ignoreComments || !line.trim().startsWith("//")) {
					sb.append(line).append("\n");
				}
			}
		} finally {
			if (bufferedReader != null) {
				bufferedReader.close();
			}
		}
		return sb.toString();
	}

	public static boolean isClosed(Shape shape) {
		PathIterator iterator = shape.getPathIterator(null);
		while (!iterator.isDone()) {
			double[] coords = new double[6];
			int type = iterator.currentSegment(coords);
			if (type == PathIterator.SEG_CLOSE) {
				return true;
			}
			iterator.next();
		}
		return false;
	}

	public static Shape openShape(Shape shape) {
		Path2D openShape = new Path2D.Double();

		PathIterator iterator = shape.getPathIterator(null);
		while (!iterator.isDone()) {
			double[] coords = new double[6];
			int type = iterator.currentSegment(coords);

			switch (type) {
				case PathIterator.SEG_MOVETO:
					openShape.moveTo(coords[0], coords[1]);
					break;
				case PathIterator.SEG_LINETO:
					openShape.lineTo(coords[0], coords[1]);
					break;
				case PathIterator.SEG_QUADTO:
					openShape.quadTo(coords[0], coords[1], coords[2], coords[3]);
					break;
				case PathIterator.SEG_CUBICTO:
					openShape.curveTo(coords[0], coords[1], coords[2], coords[3], coords[4], coords[5]);
					break;
				case PathIterator.SEG_CLOSE:
					// Do not close!
					break;
				default:
					break;
			}

			iterator.next();
		}

		return openShape;
	}

	public static Shape cropOpenShape(Shape shape, Rectangle2D croppingArea) {
		// TODO Implement this!
		return shape;
	}

	public static BufferedImage removeAlphaChannel(BufferedImage rgbaImage) {
		int width = rgbaImage.getWidth();
		int height = rgbaImage.getHeight();

		BufferedImage rgbImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		int[] rgb = rgbaImage.getRGB(0, 0, width, height, null, 0, width);
		rgbImage.setRGB(0, 0, width, height, rgb, 0, width);

		return rgbImage;
	}
}
