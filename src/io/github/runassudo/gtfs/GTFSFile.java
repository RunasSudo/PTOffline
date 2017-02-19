/*
    PTOffline: An offline GTFS/public transport app for Android
    Copyright © 2017  RunasSudo

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

import java.io.IOException;

/**
 * Created by runassudo on 19/02/17.
 */

public abstract class GTFSFile {
	public abstract void iterateThroughContents(IterateThroughContentsCallback callback) throws IOException;

	public interface IterateThroughContentsCallback {
		void call(GTFSCSV gtfsCsv) throws IOException;
	}
}
