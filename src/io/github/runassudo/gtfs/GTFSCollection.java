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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Created by runassudo on 19/02/17.
 */

public class GTFSCollection {
	File file;

	public GTFSCollection(File file) {
		this.file = file;
	}

	public void iterateThroughMembers(IterateThroughMembersCallback callback) throws IOException {
		// Apache ZipFile requires unsupported NIO File.toPath
		ZipFile zipFile = new ZipFile(file);
		Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
		while (zipEntries.hasMoreElements()) {
			ZipEntry zipEntry = zipEntries.nextElement();
			if(zipEntry.isDirectory()) {
				continue;
			}
			// Process this GTFS zip
			callback.call(new ZipStreamGTFSFile(zipFile, zipEntry));
		}
	}

	public void iterateThroughContents(GTFSFile.IterateThroughContentsCallback callback) throws IOException {
		iterateThroughMembers(gtfsFile -> {
			gtfsFile.iterateThroughContents(callback);
		});
	}

	public void iterateThroughContents(String[] files, GTFSFile.IterateThroughContentsCallback... callbacks) throws IOException {
		List<String> filesList = Arrays.asList(files);
		iterateThroughContents(gtfsCsv -> {
			if (filesList.contains(gtfsCsv.getName())) {
				callbacks[filesList.indexOf(gtfsCsv.getName())].call(gtfsCsv);
			}
		});
	}

	public void iterateThroughContents(String file, GTFSFile.IterateThroughContentsCallback callback) throws IOException {
		iterateThroughContents(new String[] {file}, callback);
	}

	public interface IterateThroughMembersCallback {
		void call(GTFSFile gtfsFile) throws IOException;
	}
}