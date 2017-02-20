/*
    PTOffline: An offline GTFS/public transport app for Android
    Copyright © 2017  RunasSudo (Yingtong Li)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.runassudo.gtfs;

import com.google.common.hash.Hashing;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by runassudo on 19/02/17.
 */

public class ZipStreamGTFSFile extends GTFSFile {
	ZipFile parentFile;
	ZipEntry zipEntry;

	public ZipStreamGTFSFile(ZipFile parentFile, ZipEntry zipEntry) {
		this.parentFile = parentFile;
		this.zipEntry = zipEntry;
	}

	public void iterateThroughContents(IterateThroughContentsCallback callback) throws IOException {
		ZipArchiveInputStream gtfsStream = new ZipArchiveInputStream(parentFile.getInputStream(zipEntry));
		ZipArchiveEntry contentEntry;
		while ((contentEntry = gtfsStream.getNextZipEntry()) != null) {
			callback.call(new ZipStreamGTFSCSV(gtfsStream, contentEntry));
		}
	}

	public FlatGTFSFile toFlatFile() throws IOException {
		File destBase = new File(GTFSCollection.cacheDir, Hashing.sha256().hashString(zipEntry.getName(), StandardCharsets.UTF_8).toString());

		if (!destBase.exists()) {
			ZipArchiveInputStream gtfsStream = new ZipArchiveInputStream(parentFile.getInputStream(zipEntry));
			ZipArchiveEntry contentEntry;
			while((contentEntry = gtfsStream.getNextZipEntry()) != null) {
				// Copy this file to cache
				File dest = new File(destBase, contentEntry.getName());
				dest.getParentFile().mkdirs();
				FileOutputStream os = new FileOutputStream(dest);
				byte[] buf = new byte[4096];
				int len;
				while((len = gtfsStream.read(buf)) > 0) {
					os.write(buf, 0, len);
				}
				os.close();
			}
			gtfsStream.close();
		}

		return new FlatGTFSFile(destBase);
	}
}
