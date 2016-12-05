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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class TestUtils {
	private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

	public static void copyResourceToDisk(String resource, File file) throws IOException {
		InputStream inputStream = null;
		OutputStream outputStream = null;
		try {
			File folder = file.getParentFile();
			if (!folder.exists()) {
				folder.mkdirs();
			}

			inputStream = TestUtils.class.getClassLoader().getResourceAsStream(resource);
			outputStream = new FileOutputStream(file);
			TestUtils.copy(inputStream, outputStream);
		} finally {
			if (inputStream != null) {
				inputStream.close();
			}
			if (outputStream != null) {
				outputStream.close();
			}
		}
	}

	// Inspired from IOUtils (I didn't wanted to bring a whole library for a single method)
	public static long copy(InputStream input, OutputStream output)
			throws IOException {

		byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
		long count = 0;
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;
		}
		return count;
	}
}
