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

import au.gov.aims.layers2svg.Utils;
import au.gov.aims.sld.geom.GeoShape;
import au.gov.aims.sld.geom.GeoShapeGroup;
import au.gov.aims.sld.geom.Layer;
import org.jfree.graphics2d.svg.SVGHints;
import org.jfree.graphics2d.svg.SVGUnits;
import org.jfree.graphics2d.svg.SVGUtils;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.RenderingHints.Key;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.io.File;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This wrapper can write as SVG, PNG, in screen, etc.
 * It's using either Graphics2D from Java or from SVGGraphics2D.
 *
 * SVGGraphics2D has some flaw:
 *   - No fill and stroke. When fill / draw is called, the SVG is created with duplicated shapes making the file
 *     twice as big.
 */
public class VectorRasterGraphics2D extends Graphics2D {
	private static final Logger LOGGER = Logger.getLogger(VectorRasterGraphics2D.class.getSimpleName());
	private static final String DEFAULT_LAYER_NAME = "Unnamed";

	// Classic Java Graphics2D objects, to generate images such as PNG.
	private BufferedImage g2dImage;
	private Graphics2D g2d;

	// SVG libraries which implements Graphics2D, to generate editable vector image.
	private SVGGraphics2D svgG2d;
	private StringBuilder svgSb;

	// Define the writing area.
	// This is used to remove elements which are not displayed, making smaller vector images.
	private double margin;
	private Rectangle2D drawingArea;

	private boolean crop;

	private String currentLayerName = null;
	private int layerCounter = 0;

	public VectorRasterGraphics2D(int width, int height, double margin) {
		this(width, height, null, new StringBuilder());
		this.margin = margin;
		this.drawingArea = new Rectangle2D.Double(
			this.margin, this.margin,
			width-this.margin-this.margin,
			height-this.margin-this.margin);
	}

	public VectorRasterGraphics2D(int width, int height, SVGUnits units) {
		this(width, height, units, new StringBuilder());
	}

	private VectorRasterGraphics2D(int width, int height, SVGUnits units, StringBuilder sb) {
		this.crop = true;
		this.svgSb = sb;

		// For image rendering
		this.g2dImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		this.g2d = this.g2dImage.createGraphics();

		// For SVG rendering
		this.svgG2d = new SVGGraphics2D(width, height, units, this.svgSb);
	}

	private VectorRasterGraphics2D(VectorRasterGraphics2D parent) {
		this.crop = parent.crop;

		if (parent.g2d != null) {
			this.g2d = (Graphics2D)parent.g2d.create();
		}
		if (parent.svgG2d != null) {
			this.svgG2d = (SVGGraphics2D)parent.svgG2d.create();
		}
	}

	public void disableVectorGeneration() {
		this.disposeVector();
	}
	public boolean isVectorGenerationEnabled() {
		return this.svgG2d != null;
	}

	public void disableRasterGeneration() {
		this.disposeRaster();
	}
	public boolean isRasterGenerationEnabled() {
		return this.g2d != null;
	}

	// Open a layer if there is none.
	// This is used to ensure everything is drawn in a layer.
	private void checkLayer() {
		if (this.currentLayerName == null) {
			this.createLayer(null);
		}
	}

	public void createLayer(String layerName) {
		if (layerName == null || layerName.isEmpty()) {
			layerName = VectorRasterGraphics2D.DEFAULT_LAYER_NAME;
		}

		if (!layerName.equals(this.currentLayerName)) {
			this.closeLayer();
			this.layerCounter++;
			this.currentLayerName = layerName;
			if (this.svgG2d != null) {
				// SVG Group with multiple properties for layer support in different software.
				this.svgSb.append("<g " +
							"style=\"display:inline\" " +
							"inkscape:groupmode=\"layer\" " +
							"id=\"layer").append(this.layerCounter).append("\" " +
							"inkscape:label=\"").append(layerName).append("\"><title>")
						.append(this.currentLayerName)
						.append(" (").append(this.layerCounter).append(")")
						.append("</title>");
			}
		}
	}

	private void closeLayer() {
		if (this.currentLayerName != null) {
			if (this.svgG2d != null) {
				this.svgSb.append("</g>");
			}
			this.currentLayerName = null;
		}
	}

	public void setCrop(boolean crop) {
		this.crop = crop;
	}

	public boolean isCrop() {
		return this.crop;
	}

	/**
	 * Crop shape to drawing area, if it's too large.
	 *
	 * NOTE: Clip create a mask which "hide" part of the image. It doesn't cut the shapes (which causes issue with SVG).
	 *   g2d.clipRect(0+1000, 0+1000, width-2000, height-2000);
	 */
	private Shape cropShape(Shape shape) {
		if (!this.crop) {
			return shape;
		}

		Shape croppedShape = null;

		// Shape is completely outside the drawing area - do not render
		if (shape.intersects(this.drawingArea)) {
			boolean isOpenPath = (shape instanceof Path2D) && !Utils.isClosed(shape);

			// Check if the shape goes outside the drawing area.
			// If it does, crop it.
			if (!this.drawingArea.contains(shape.getBounds())) {
				if (isOpenPath) {
					croppedShape = Utils.cropOpenShape(shape, this.drawingArea);
				} else {
					croppedShape = new Area(shape);
					((Area)croppedShape).intersect(new Area(this.drawingArea));
				}
			} else {
				croppedShape = shape;
			}
		}

		return croppedShape;
	}

	public void render(GeoGraphicsFormat format, File outputFile) throws IOException {
		this.closeLayer();
		if (outputFile == null) {
			throw new IllegalArgumentException("Output file can't be null.");
		}

		if (GeoGraphicsFormat.SVG.equals(format)) {
			if (this.svgG2d != null) {
				String svgElement = this.svgG2d.getSVGElement();
				SVGUtils.writeToSVG(outputFile, svgElement, false); // boolean: zip
			} else {
				throw new IllegalStateException("Vector generation is disabled.");
			}

		} else if (GeoGraphicsFormat.PNG.equals(format)) {
			if (this.g2dImage != null) {
				ImageIO.write(this.g2dImage, "png", outputFile);
			} else {
				throw new IllegalStateException("Raster generation is disabled.");
			}

		} else if (GeoGraphicsFormat.GIF.equals(format)) {
			if (this.g2dImage != null) {
				ImageIO.write(this.g2dImage, "gif", outputFile);
			} else {
				throw new IllegalStateException("Raster generation is disabled.");
			}

		} else if (GeoGraphicsFormat.JPG.equals(format)) {
			if (this.g2dImage != null) {
				// Setting the compression level is not trivial.
				// I will add support for that when required.
				//   http://stackoverflow.com/questions/17108234/setting-jpg-compression-level-with-imageio-in-java
				BufferedImage rgbImage = Utils.removeAlphaChannel(this.g2dImage);
				ImageIO.write(rgbImage, "jpg", outputFile);
				rgbImage.flush();
				rgbImage.getGraphics().dispose();
			} else {
				throw new IllegalStateException("Raster generation is disabled.");
			}

		} else {
			throw new IllegalArgumentException("Unsupported format '" + format.getMimeType() + "'.");
		}
	}

	public void render(Graphics2D screen) throws IOException {
		this.closeLayer();
		if (this.g2dImage != null) {
			screen.drawImage(this.g2dImage, null, 0, 0);
		} else {
			throw new IllegalStateException("Raster generation is disabled.");
		}
	}

	/**
	 * Fill a shape using current paint and draw the stroke using strokePaint.
	 * This method is not thread safe, it temporarily alter the current paint.
	 * Inspired from org.jfree.graphics2d.svg.SVGGraphics2D
	 */
	public void fillAndStroke(Shape shape, Paint strokePaint) {
		shape = this.cropShape(shape);
		if (shape != null) {
			this.checkLayer();
			if (this.g2d != null) {
				fillAndStrokeFallback(this.g2d, shape, strokePaint);
			}
			if (this.svgG2d != null) {
				// if the current stroke is not a BasicStroke then it is handled as
				// a special case
				if (!(this.svgG2d.getStroke() instanceof BasicStroke)) {
					fillAndStrokeFallback(this.svgG2d, shape, strokePaint);
					return;
				}

				if (shape instanceof Line2D) {
					fillAndStrokeFallback(this.svgG2d, shape, strokePaint);

				} else if (shape instanceof Rectangle2D) {
					Rectangle2D r = (Rectangle2D) shape;
					this.svgSb.append("<rect ");
					this.svgG2d.appendOptionalElementIDFromHint(this.svgSb);
					this.svgSb.append("x=\"").append(this.svgG2d.geomDP(r.getX()))
							.append("\" y=\"").append(this.svgG2d.geomDP(r.getY()))
							.append("\" width=\"").append(this.svgG2d.geomDP(r.getWidth()))
							.append("\" height=\"").append(this.svgG2d.geomDP(r.getHeight()))
							.append("\" ");
					this.svgSb.append("style=\"").append(this.getSVGStrokeStyle(strokePaint))
							.append("; ").append(this.svgG2d.getSVGFillStyle()).append("\" ");
					this.svgSb.append("transform=\"").append(this.svgG2d.getSVGTransform(
							this.svgG2d.getTransform())).append("\" ");
					this.svgSb.append(this.svgG2d.getClipPathRef());
					this.svgSb.append("/>");

				} else if (shape instanceof Ellipse2D) {
					Ellipse2D e = (Ellipse2D) shape;
					this.svgSb.append("<ellipse ");
					this.svgG2d.appendOptionalElementIDFromHint(this.svgSb);
					this.svgSb.append("cx=\"").append(this.svgG2d.geomDP(e.getCenterX()))
							.append("\" cy=\"").append(this.svgG2d.geomDP(e.getCenterY()))
							.append("\" rx=\"").append(this.svgG2d.geomDP(e.getWidth() / 2.0))
							.append("\" ry=\"").append(this.svgG2d.geomDP(e.getHeight() / 2.0))
							.append("\" ");
					this.svgSb.append("style=\"").append(this.getSVGStrokeStyle(strokePaint))
							.append("; ").append(this.svgG2d.getSVGFillStyle()).append("\" ");
					this.svgSb.append("transform=\"").append(this.svgG2d.getSVGTransform(
							this.svgG2d.getTransform())).append("\" ");
					this.svgSb.append(this.svgG2d.getClipPathRef());
					this.svgSb.append("/>");

				} else {
					Path2D path = (shape instanceof Path2D) ?
						(Path2D) shape :
						new GeneralPath(shape);

					this.svgSb.append("<g ");
					this.svgG2d.appendOptionalElementIDFromHint(this.svgSb);
					this.svgSb.append("style=\"").append(this.getSVGStrokeStyle(strokePaint))
							.append("; ").append(this.svgG2d.getSVGFillStyle()).append("\" ");
					this.svgSb.append("transform=\"").append(this.svgG2d.getSVGTransform(
							this.svgG2d.getTransform())).append("\" ");
					this.svgSb.append(this.svgG2d.getClipPathRef());
					this.svgSb.append(">");
					this.svgSb.append("<path ").append(this.svgG2d.getSVGPathData(path)).append("/>");
					this.svgSb.append("</g>");
				}
			}
		}
	}

	public void fillAndStroke(List<Layer> layers) {
		if (layers != null && !layers.isEmpty()) {
			for (Layer layer : layers) {
				this.fillAndStroke(layer);
			}
		}
	}

	public void fillAndStroke(Layer layer) {
		String layerName = layer.getName();
		this.createLayer(layerName);

		List<GeoShape> labels = new ArrayList<GeoShape>();
		for (GeoShapeGroup group : layer.getShapeGroups()) {
			labels.addAll(this.fillAndStrokeFeatures(group));
		}

		this.fillLabelsText(labels, layerName + "_text");

		this.closeLayer();
	}

	public void fillAndStroke(GeoShapeGroup group) {
		List<GeoShape> labels = this.fillAndStrokeFeatures(group);
		this.fillLabelsText(labels, group.getName() + "_text");
	}

	private List<GeoShape> fillAndStrokeFeatures(GeoShapeGroup group) {
		List<GeoShape> labels = new ArrayList<GeoShape>();
		if (group != null && !group.isEmpty()) {
			if (this.svgG2d != null) {
				// Shape Group
				this.svgSb.append("<g id=\"").append(group.getName()).append("\">");
			}

			for (GeoShapeGroup geoShapeGroup : group.getGeoShapeGroups()) {
				labels.addAll(this.fillAndStrokeFeatures(geoShapeGroup));
			}

			for (GeoShape geoShape : group.getGeoShapes()) {
				this.fillAndStroke(geoShape);

				String label = geoShape.getLabel();
				if (label != null && !label.isEmpty()) {
					labels.add(geoShape);
				}
			}

			if (this.svgG2d != null) {
				// EO Shape Group
				this.svgSb.append("</g>");
			}
		}

		return labels;
	}

	public void fillAndStroke(GeoShape geoShape) {
		Paint oldPaint = this.getPaint();
		Stroke oldStroke = this.getStroke();

		Paint shapeFillPaint = geoShape.getFillPaint();
		Paint shapeStrokePaint = geoShape.getStrokePaint();
		Stroke shapeStroke = geoShape.getStroke();

		if (shapeFillPaint != null && shapeStrokePaint != null && shapeStroke != null) {
			Object rawShape = geoShape.getShape();
			if (rawShape instanceof Shape) {
				this.setPaint(shapeFillPaint);
				this.setStroke(shapeStroke);

				this.fillAndStroke((Shape)rawShape, shapeStrokePaint);

				this.setPaint(oldPaint);
				this.setStroke(oldStroke);
			}
		} else {
			if (shapeFillPaint != null) {
				this.fill(geoShape);
			}
			if (shapeStrokePaint != null && shapeStroke != null) {
				this.draw(geoShape);
			}
		}
	}

	public void fillLabelsText(List<GeoShape> labels, String groupName) {
		if (labels != null && !labels.isEmpty()) {
			if (this.svgG2d != null) {
				// Text Group
				this.svgSb.append("<g id=\"").append(groupName).append("\">");
			}

			for (GeoShape label : labels) {
				this.fillLabelText(label);
			}

			if (this.svgG2d != null) {
				// EO Text Group
				this.svgSb.append("</g>");
			}
		}
	}

	public void fillLabelText(GeoShape label) {
		if (this.g2d != null) {
			this.fillLabelText(this.g2d, label);
		}
		if (this.svgG2d != null) {
			this.fillLabelText(this.svgG2d, label);
		}
	}

	private void fillLabelText(Graphics2D g2d, GeoShape label) {
		String labelText = label.getLabel();

		if (labelText != null && !labelText.isEmpty()) {
			// X, Y = Coordinate of the upper left corner.
			Object rawShape = label.getShape();
			if (rawShape instanceof Point2D) {
				Point2D anchor = (Point2D)rawShape;

				// Do not render the text if its anchor point is not in the drawing area (not perfect but will do)
				if (this.drawingArea.contains(anchor)) {
					Paint oldPaint = this.getPaint();
					Font oldFont = this.getFont();

					this.setPaint(label.getFillPaint());
					this.setFont(label.getFont());
					g2d.drawString(labelText, (float)anchor.getX(), (float)anchor.getY());

					this.setPaint(oldPaint);
					this.setFont(oldFont);
				}
			} else {
				LOGGER.log(Level.WARNING, "Requested to render the label '" + labelText + "' without an anchor. The label was ignored.");
			}
		}
	}


	private void fillAndStrokeFallback(Graphics2D g2d, Shape s, Paint strokePaint) {
		g2d.fill(s);

		// Change to stroke colour
		Paint oldPaint = g2d.getPaint();

		g2d.setPaint(strokePaint);
		g2d.draw(s);

		// Revert to background colour
		g2d.setPaint(oldPaint);
	}

	private String getSVGStrokeStyle(Paint strokePaint) {
		if (strokePaint == null) {
			return this.svgG2d.strokeStyle();
		}
		Paint oldPaint = this.svgG2d.getPaint();

		this.svgG2d.setPaint(strokePaint);
		String strokeStyle = this.svgG2d.strokeStyle();

		this.svgG2d.setPaint(oldPaint);

		return strokeStyle;
	}

	public void draw(GeoShape geoShape) {
		Paint oldPaint = this.getPaint();
		Stroke oldStroke = this.getStroke();

		Paint shapeStrokePaint = geoShape.getStrokePaint();
		Stroke shapeStroke = geoShape.getStroke();

		if (shapeStrokePaint != null && shapeStroke != null) {
			Object rawShape = geoShape.getShape();
			if (rawShape instanceof Shape) {
				this.setPaint(shapeStrokePaint);
				this.setStroke(shapeStroke);

				this.draw((Shape)rawShape);

				this.setPaint(oldPaint);
				this.setStroke(oldStroke);
			}
		}
	}

	@Override
	public void draw(Shape shape) {
		this.checkLayer();
		shape = this.cropShape(shape);
		if (shape != null) {
			if (this.g2d != null) {
				this.g2d.draw(shape);
			}
			if (this.svgG2d != null) {
				this.svgG2d.draw(shape);
			}
		}
	}

	@Override
	public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
		this.checkLayer();
		boolean result = false;
		if (this.g2d != null) {
			result = this.g2d.drawImage(img, xform, obs) || result;
		}
		if (this.svgG2d != null) {
			result = this.svgG2d.drawImage(img, xform, obs) || result;
		}
		return result;
	}

	@Override
	public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
		this.checkLayer();
		if (this.g2d != null) {
			this.g2d.drawImage(img, op, x, y);
		}
		if (this.svgG2d != null) {
			this.svgG2d.drawImage(img, op, x, y);
		}
	}

	@Override
	public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
		this.checkLayer();
		if (this.g2d != null) {
			this.g2d.drawRenderedImage(img, xform);
		}
		if (this.svgG2d != null) {
			this.svgG2d.drawRenderedImage(img, xform);
		}
	}

	@Override
	public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
		this.checkLayer();
		if (this.g2d != null) {
			this.g2d.drawRenderableImage(img, xform);
		}
		if (this.svgG2d != null) {
			this.svgG2d.drawRenderableImage(img, xform);
		}
	}

	public void drawString(String str, int x, int y, TextAlignment alignment) {
		this.drawString(str, (float)x, (float)y, alignment);
	}

	@Override
	public void drawString(String str, int x, int y) {
		this.checkLayer();
		if (this.g2d != null) {
			this.g2d.drawString(str, x, y);
		}
		if (this.svgG2d != null) {
			this.svgG2d.drawString(str, x, y);
		}
	}

	public void drawString(String str, float x, float y, TextAlignment alignment) {
		if (TextAlignment.LEFT.equals(alignment)) {
			// LEFT
			this.drawString(str, x, y);
		} else {
			if (TextAlignment.RIGHT.equals(alignment)) {
				// RIGHT
				if (this.g2d != null) {
					int strWidth = this.getFontMetrics().stringWidth(str);
					this.g2d.drawString(str, x-strWidth, y);
				}
				if (this.svgG2d != null) {
					this.drawSVGString(str, x, y, alignment);
				}
			} else {
				// CENTRE
				if (this.g2d != null) {
					int strWidth = this.getFontMetrics().stringWidth(str);
					this.g2d.drawString(str, x-(strWidth / 2), y);
				}
				if (this.svgG2d != null) {
					this.drawSVGString(str, x, y, alignment);
				}
			}
		}
	}

	private void drawSVGString(String str, float x, float y, TextAlignment alignment) {
		if (!SVGHints.VALUE_DRAW_STRING_TYPE_VECTOR.equals(
				this.getRenderingHint(SVGHints.KEY_DRAW_STRING_TYPE))) {

			String alignStyle = "";
			if (TextAlignment.RIGHT.equals(alignment)) {
				alignStyle = "text-align:end;text-anchor:end;";
			} else if (TextAlignment.CENTRE.equals(alignment)) {
				alignStyle = "text-align:center;text-anchor:middle;";
			}

			this.svgSb.append("<g ");
			this.svgG2d.appendOptionalElementIDFromHint(this.svgSb);
			this.svgSb.append("transform=\"").append(this.svgG2d.getSVGTransform(
					this.svgG2d.getTransform())).append("\">");
			this.svgSb.append("<text x=\"").append(this.svgG2d.geomDP(x))
					.append("\" y=\"").append(this.svgG2d.geomDP(y))
					.append("\"");
			this.svgSb.append(" style=\"")
					.append(this.svgG2d.getSVGFontStyle())
					.append(alignStyle)
					.append("\"");
			Object hintValue = getRenderingHint(SVGHints.KEY_TEXT_RENDERING);
			if (hintValue != null) {
				String textRenderValue = hintValue.toString();
				this.svgSb.append(" text-rendering=\"").append(textRenderValue)
						.append("\"");
			}
			this.svgSb.append(" ").append(this.svgG2d.getClipPathRef());
			this.svgSb.append(">");
			this.svgSb.append(SVGUtils.escapeForXML(str)).append("</text>");
			this.svgSb.append("</g>");
		} else {
			AttributedString as = new AttributedString(str,
					this.getFont().getAttributes());
			drawString(as.getIterator(), x, y);
		}
	}

	@Override
	public void drawString(String str, float x, float y) {
		this.checkLayer();
		if (this.g2d != null) {
			this.g2d.drawString(str, x, y);
		}
		if (this.svgG2d != null) {
			this.svgG2d.drawString(str, x, y);
		}
	}

	@Override
	public void drawString(AttributedCharacterIterator iterator, int x, int y) {
		this.checkLayer();
		if (this.g2d != null) {
			this.g2d.drawString(iterator, x, y);
		}
		if (this.svgG2d != null) {
			this.svgG2d.drawString(iterator, x, y);
		}
	}

	@Override
	public boolean drawImage(Image img, int x, int y, ImageObserver observer) {
		this.checkLayer();
		boolean result = false;
		if (this.g2d != null) {
			result = this.g2d.drawImage(img, x, y, observer) || result;
		}
		if (this.svgG2d != null) {
			result = this.svgG2d.drawImage(img, x, y, observer) || result;
		}
		return result;
	}

	@Override
	public boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
		this.checkLayer();
		boolean result = false;
		if (this.g2d != null) {
			result = this.g2d.drawImage(img, x, y, width, height, observer) || result;
		}
		if (this.svgG2d != null) {
			result = this.svgG2d.drawImage(img, x, y, width, height, observer) || result;
		}
		return result;
	}

	@Override
	public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
		this.checkLayer();
		boolean result = false;
		if (this.g2d != null) {
			result = this.g2d.drawImage(img, x, y, bgcolor, observer) || result;
		}
		if (this.svgG2d != null) {
			result = this.svgG2d.drawImage(img, x, y, bgcolor, observer) || result;
		}
		return result;
	}

	@Override
	public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
		this.checkLayer();
		boolean result = false;
		if (this.g2d != null) {
			result = this.g2d.drawImage(img, x, y, width, height, bgcolor, observer) || result;
		}
		if (this.svgG2d != null) {
			result = this.svgG2d.drawImage(img, x, y, width, height, bgcolor, observer) || result;
		}
		return result;
	}

	@Override
	public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, ImageObserver observer) {
		this.checkLayer();
		boolean result = false;
		if (this.g2d != null) {
			result = this.g2d.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer) || result;
		}
		if (this.svgG2d != null) {
			result = this.svgG2d.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, observer) || result;
		}
		return result;
	}

	@Override
	public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2, Color bgcolor, ImageObserver observer) {
		this.checkLayer();
		boolean result = false;
		if (this.g2d != null) {
			result = this.g2d.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer) || result;
		}
		if (this.svgG2d != null) {
			result = this.svgG2d.drawImage(img, dx1, dy1, dx2, dy2, sx1, sy1, sx2, sy2, bgcolor, observer) || result;
		}
		return result;
	}

	@Override
	public void dispose() {
		this.disposeRaster();
		this.disposeVector();
	}

	private void disposeRaster() {
		if (this.g2dImage != null) {
			this.g2dImage.flush();
			this.g2dImage.getGraphics().dispose();
			this.g2dImage = null;
		}
		if (this.g2d != null) {
			this.g2d.dispose();
			this.g2d = null;
		}
	}

	private void disposeVector() {
		if (this.svgG2d != null) {
			this.svgG2d.dispose();
			this.svgG2d = null;
		}
	}

	@Override
	public void drawString(AttributedCharacterIterator iterator, float x, float y) {
		this.checkLayer();
		if (this.g2d != null) {
			this.g2d.drawString(iterator, x, y);
		}
		if (this.svgG2d != null) {
			this.svgG2d.drawString(iterator, x, y);
		}
	}

	@Override
	public void drawGlyphVector(GlyphVector g, float x, float y) {
		this.checkLayer();
		if (this.g2d != null) {
			this.g2d.drawGlyphVector(g, x, y);
		}
		if (this.svgG2d != null) {
			this.svgG2d.drawGlyphVector(g, x, y);
		}
	}

	public void fill(GeoShape geoShape) {
		Paint oldPaint = this.getPaint();

		Paint shapeFillPaint = geoShape.getFillPaint();

		if (shapeFillPaint != null) {
			Object rawShape = geoShape.getShape();
			if (rawShape instanceof Shape) {
				this.setPaint(shapeFillPaint);
				this.fill((Shape)rawShape);
				this.setPaint(oldPaint);
			}
		}
	}

	@Override
	public void fill(Shape shape) {
		this.checkLayer();
		shape = this.cropShape(shape);
		if (shape != null) {
			if (this.g2d != null) {
				this.g2d.fill(shape);
			}
			if (this.svgG2d != null) {
				this.svgG2d.fill(shape);
			}
		}
	}

	@Override
	public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
		if (this.g2d != null) {
			return this.g2d.hit(rect, s, onStroke);
		}
		if (this.svgG2d != null) {
			return this.svgG2d.hit(rect, s, onStroke);
		}
		return false;
	}

	@Override
	public GraphicsConfiguration getDeviceConfiguration() {
		if (this.g2d != null) {
			return this.g2d.getDeviceConfiguration();
		}
		if (this.svgG2d != null) {
			return this.svgG2d.getDeviceConfiguration();
		}
		return null;
	}

	@Override
	public void setComposite(Composite comp) {
		if (this.g2d != null) {
			this.g2d.setComposite(comp);
		}
		if (this.svgG2d != null) {
			this.svgG2d.setComposite(comp);
		}
	}

	@Override
	public void setPaint(Paint paint) {
		if (this.g2d != null) {
			this.g2d.setPaint(paint);
		}
		if (this.svgG2d != null) {
			this.svgG2d.setPaint(paint);
		}
	}

	@Override
	public void setStroke(Stroke s) {
		if (this.g2d != null) {
			this.g2d.setStroke(s);
		}
		if (this.svgG2d != null) {
			this.svgG2d.setStroke(s);
		}
	}

	@Override
	public void setRenderingHint(Key hintKey, Object hintValue) {
		if (this.g2d != null) {
			this.g2d.setRenderingHint(hintKey, hintValue);
		}
		if (this.svgG2d != null) {
			this.svgG2d.setRenderingHint(hintKey, hintValue);
		}
	}

	@Override
	public Object getRenderingHint(Key hintKey) {
		if (this.g2d != null) {
			return this.g2d.getRenderingHint(hintKey);
		}
		if (this.svgG2d != null) {
			return this.svgG2d.getRenderingHint(hintKey);
		}
		return null;
	}

	@Override
	public void setRenderingHints(Map<?, ?> hints) {
		if (this.g2d != null) {
			this.g2d.setRenderingHints(hints);
		}
		if (this.svgG2d != null) {
			this.svgG2d.setRenderingHints(hints);
		}
	}

	@Override
	public void addRenderingHints(Map<?, ?> hints) {
		if (this.g2d != null) {
			this.g2d.addRenderingHints(hints);
		}
		if (this.svgG2d != null) {
			this.svgG2d.addRenderingHints(hints);
		}
	}

	@Override
	public RenderingHints getRenderingHints() {
		if (this.g2d != null) {
			return this.g2d.getRenderingHints();
		}
		if (this.svgG2d != null) {
			return this.svgG2d.getRenderingHints();
		}
		return null;
	}

	@Override
	public Graphics create() {
		return new VectorRasterGraphics2D(this);
	}

	@Override
	public void translate(int x, int y) {
		if (this.g2d != null) {
			this.g2d.translate(x, y);
		}
		if (this.svgG2d != null) {
			this.svgG2d.translate(x, y);
		}
	}

	@Override
	public Color getColor() {
		if (this.g2d != null) {
			return this.g2d.getColor();
		}
		if (this.svgG2d != null) {
			return this.svgG2d.getColor();
		}
		return null;
	}

	@Override
	public void setColor(Color c) {
		if (this.g2d != null) {
			this.g2d.setColor(c);
		}
		if (this.svgG2d != null) {
			this.svgG2d.setColor(c);
		}
	}

	@Override
	public void setPaintMode() {
		if (this.g2d != null) {
			this.g2d.setPaintMode();
		}
		if (this.svgG2d != null) {
			this.svgG2d.setPaintMode();
		}
	}

	@Override
	public void setXORMode(Color c1) {
		if (this.g2d != null) {
			this.g2d.setXORMode(c1);
		}
		if (this.svgG2d != null) {
			this.svgG2d.setXORMode(c1);
		}
	}

	@Override
	public Font getFont() {
		if (this.g2d != null) {
			return this.g2d.getFont();
		}
		if (this.svgG2d != null) {
			return this.svgG2d.getFont();
		}
		return null;
	}

	@Override
	public void setFont(Font font) {
		if (this.g2d != null) {
			this.g2d.setFont(font);
		}
		if (this.svgG2d != null) {
			this.svgG2d.setFont(font);
		}
	}

	@Override
	public FontMetrics getFontMetrics(Font f) {
		if (this.g2d != null) {
			return this.g2d.getFontMetrics(f);
		}
		if (this.svgG2d != null) {
			return this.svgG2d.getFontMetrics(f);
		}
		return null;
	}

	@Override
	public Rectangle getClipBounds() {
		if (this.g2d != null) {
			return this.g2d.getClipBounds();
		}
		if (this.svgG2d != null) {
			return this.svgG2d.getClipBounds();
		}
		return null;
	}

	@Override
	public void clipRect(int x, int y, int width, int height) {
		if (this.g2d != null) {
			this.g2d.clipRect(x, y, width, height);
		}
		if (this.svgG2d != null) {
			this.svgG2d.clipRect(x, y, width, height);
		}
	}

	@Override
	public void setClip(int x, int y, int width, int height) {
		if (this.g2d != null) {
			this.g2d.setClip(x, y, width, height);
		}
		if (this.svgG2d != null) {
			this.svgG2d.setClip(x, y, width, height);
		}
	}

	@Override
	public Shape getClip() {
		if (this.g2d != null) {
			return this.g2d.getClip();
		}
		if (this.svgG2d != null) {
			return this.svgG2d.getClip();
		}
		return null;
	}

	@Override
	public void setClip(Shape clip) {
		if (this.g2d != null) {
			this.g2d.setClip(clip);
		}
		if (this.svgG2d != null) {
			this.svgG2d.setClip(clip);
		}
	}

	@Override
	public void copyArea(int x, int y, int width, int height, int dx, int dy) {
		this.checkLayer();
		if (this.g2d != null) {
			this.g2d.copyArea(x, y, width, height, dx, dy);
		}
		if (this.svgG2d != null) {
			this.svgG2d.copyArea(x, y, width, height, dx, dy);
		}
	}

	@Override
	public void drawLine(int x1, int y1, int x2, int y2) {
		this.checkLayer();
		if (this.g2d != null) {
			this.g2d.drawLine(x1, y1, x2, y2);
		}
		if (this.svgG2d != null) {
			this.svgG2d.drawLine(x1, y1, x2, y2);
		}
	}

	@Override
	public void fillRect(int x, int y, int width, int height) {
		this.checkLayer();
		if (this.g2d != null) {
			this.g2d.fillRect(x, y, width, height);
		}
		if (this.svgG2d != null) {
			this.svgG2d.fillRect(x, y, width, height);
		}
	}

	@Override
	public void clearRect(int x, int y, int width, int height) {
		this.checkLayer();
		if (this.g2d != null) {
			this.g2d.clearRect(x, y, width, height);
		}
		if (this.svgG2d != null) {
			this.svgG2d.clearRect(x, y, width, height);
		}
	}

	@Override
	public void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		this.checkLayer();
		if (this.g2d != null) {
			this.g2d.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
		}
		if (this.svgG2d != null) {
			this.svgG2d.drawRoundRect(x, y, width, height, arcWidth, arcHeight);
		}
	}

	@Override
	public void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
		this.checkLayer();
		if (this.g2d != null) {
			this.g2d.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
		}
		if (this.svgG2d != null) {
			this.svgG2d.fillRoundRect(x, y, width, height, arcWidth, arcHeight);
		}
	}

	@Override
	public void drawOval(int x, int y, int width, int height) {
		this.checkLayer();
		if (this.g2d != null) {
			this.g2d.drawOval(x, y, width, height);
		}
		if (this.svgG2d != null) {
			this.svgG2d.drawOval(x, y, width, height);
		}
	}

	@Override
	public void fillOval(int x, int y, int width, int height) {
		this.checkLayer();
		if (this.g2d != null) {
			this.g2d.fillOval(x, y, width, height);
		}
		if (this.svgG2d != null) {
			this.svgG2d.fillOval(x, y, width, height);
		}
	}

	@Override
	public void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		this.checkLayer();
		if (this.g2d != null) {
			this.g2d.drawArc(x, y, width, height, startAngle, arcAngle);
		}
		if (this.svgG2d != null) {
			this.svgG2d.drawArc(x, y, width, height, startAngle, arcAngle);
		}
	}

	@Override
	public void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
		this.checkLayer();
		if (this.g2d != null) {
			this.g2d.fillArc(x, y, width, height, startAngle, arcAngle);
		}
		if (this.svgG2d != null) {
			this.svgG2d.fillArc(x, y, width, height, startAngle, arcAngle);
		}
	}

	@Override
	public void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
		this.checkLayer();
		if (this.g2d != null) {
			this.g2d.drawPolyline(xPoints, yPoints, nPoints);
		}
		if (this.svgG2d != null) {
			this.svgG2d.drawPolyline(xPoints, yPoints, nPoints);
		}
	}

	@Override
	public void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
		this.checkLayer();
		if (this.g2d != null) {
			this.g2d.drawPolygon(xPoints, yPoints, nPoints);
		}
		if (this.svgG2d != null) {
			this.svgG2d.drawPolygon(xPoints, yPoints, nPoints);
		}
	}

	@Override
	public void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
		this.checkLayer();
		if (this.g2d != null) {
			this.g2d.fillPolygon(xPoints, yPoints, nPoints);
		}
		if (this.svgG2d != null) {
			this.svgG2d.fillPolygon(xPoints, yPoints, nPoints);
		}
	}

	@Override
	public void translate(double tx, double ty) {
		if (this.g2d != null) {
			this.g2d.translate(tx, ty);
		}
		if (this.svgG2d != null) {
			this.svgG2d.translate(tx, ty);
		}
	}

	@Override
	public void rotate(double theta) {
		if (this.g2d != null) {
			this.g2d.rotate(theta);
		}
		if (this.svgG2d != null) {
			this.svgG2d.rotate(theta);
		}
	}

	@Override
	public void rotate(double theta, double x, double y) {
		if (this.g2d != null) {
			this.g2d.rotate(theta, x, y);
		}
		if (this.svgG2d != null) {
			this.svgG2d.rotate(theta, x, y);
		}
	}

	@Override
	public void scale(double sx, double sy) {
		if (this.g2d != null) {
			this.g2d.scale(sx, sy);
		}
		if (this.svgG2d != null) {
			this.svgG2d.scale(sx, sy);
		}
	}

	@Override
	public void shear(double shx, double shy) {
		if (this.g2d != null) {
			this.g2d.shear(shx, shy);
		}
		if (this.svgG2d != null) {
			this.svgG2d.shear(shx, shy);
		}
	}

	@Override
	public void transform(AffineTransform tx) {
		if (this.g2d != null) {
			this.g2d.transform(tx);
		}
		if (this.svgG2d != null) {
			this.svgG2d.transform(tx);
		}
	}

	@Override
	public void setTransform(AffineTransform tx) {
		if (this.g2d != null) {
			this.g2d.setTransform(tx);
		}
		if (this.svgG2d != null) {
			this.svgG2d.setTransform(tx);
		}
	}

	@Override
	public AffineTransform getTransform() {
		if (this.g2d != null) {
			return this.g2d.getTransform();
		}
		if (this.svgG2d != null) {
			return this.svgG2d.getTransform();
		}
		return null;
	}

	@Override
	public Paint getPaint() {
		if (this.g2d != null) {
			return this.g2d.getPaint();
		}
		if (this.svgG2d != null) {
			return this.svgG2d.getPaint();
		}
		return null;
	}

	@Override
	public Composite getComposite() {
		if (this.g2d != null) {
			return this.g2d.getComposite();
		}
		if (this.svgG2d != null) {
			return this.svgG2d.getComposite();
		}
		return null;
	}

	@Override
	public void setBackground(Color color) {
		this.checkLayer();
		if (this.g2d != null) {
			this.g2d.setBackground(color);
		}
		if (this.svgG2d != null) {
			this.svgG2d.setBackground(color);
		}
	}

	@Override
	public Color getBackground() {
		if (this.g2d != null) {
			return this.g2d.getBackground();
		}
		if (this.svgG2d != null) {
			return this.svgG2d.getBackground();
		}
		return null;
	}

	@Override
	public Stroke getStroke() {
		if (this.g2d != null) {
			return this.g2d.getStroke();
		}
		if (this.svgG2d != null) {
			return this.svgG2d.getStroke();
		}
		return null;
	}

	@Override
	public void clip(Shape s) {
		if (this.g2d != null) {
			this.g2d.clip(s);
		}
		if (this.svgG2d != null) {
			this.svgG2d.clip(s);
		}
	}

	@Override
	public FontRenderContext getFontRenderContext() {
		if (this.g2d != null) {
			return this.g2d.getFontRenderContext();
		}
		if (this.svgG2d != null) {
			return this.svgG2d.getFontRenderContext();
		}
		return null;
	}
}
