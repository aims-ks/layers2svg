package au.gov.aims.layers2svg.graphics;

public enum GeoGraphicsFormat {
	SVG("image/svg+xml", "svg"),
	PNG("image/png", "png"),
	GIF("image/gif", "gif"),
	JPG("image/jpeg", "jpg");

	private String mimetype;
	private String extension;

	public String getMimeType() {
		return this.mimetype;
	}

	public String getExtension() {
		return this.extension;
	}

	GeoGraphicsFormat(String mimetype, String extension) {
		this.mimetype = mimetype;
		this.extension = extension;
	}
}
