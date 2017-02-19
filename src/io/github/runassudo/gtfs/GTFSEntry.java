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

/**
 * Created by runassudo on 19/02/17.
 */

public class GTFSEntry {
	GTFSCSV gtfsCsv;
	String[] fields;

	public GTFSEntry(GTFSCSV gtfsCsv, String[] fields) {
		this.gtfsCsv = gtfsCsv;
		this.fields = fields;
	}

	String unquote(String str) {
		// Regex replace is *very* expensive
		if (str.startsWith("\"") && str.endsWith("\"")) {
			str = str.substring(1, str.length() - 1);
		}
		return str;
	}

	public String getField(String field) {
		return unquote(fields[gtfsCsv.getFieldColumn(field)]);
	}
}
