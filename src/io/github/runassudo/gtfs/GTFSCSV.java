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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import io.github.runassudo.ptoffline.pte.PtvProvider;
import io.github.runassudo.ptoffline.pte.dto.Location;
import io.github.runassudo.ptoffline.pte.dto.LocationType;
import io.github.runassudo.ptoffline.pte.dto.Point;
import io.github.runassudo.ptoffline.utils.TransportrUtils;

/**
 * Created by runassudo on 19/02/17.
 */

public abstract class GTFSCSV {
	String name;
	public List<String> fieldsReversed;
	HashMap<String, Integer> fields = new HashMap<>();

	GTFSCSV(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public abstract BufferedReader getBufferedReader() throws IOException;

	public void iterateThroughEntries(IterateThroughEntriesCallback callback) throws IOException {
		BufferedReader reader = getBufferedReader();

		String line = reader.readLine();
		// Handle BOM
		if (line.startsWith("\ufeff")) {
			line = line.substring(1);
		}
		fieldsReversed = Arrays.asList(line.split(","));

		while((line = reader.readLine()) != null) {
			String[] fields = line.split(",");
			callback.call(new GTFSEntry(this, fields));
		}

		reader.close();
	}

	public int getFieldColumn(String field) {
		if (fields.containsKey(field)) {
			return fields.get(field);
		}
		int column = fieldsReversed.indexOf(field);
		fields.put(field, column);
		return column;
	}

	public interface IterateThroughEntriesCallback {
		void call(GTFSEntry gtfsEntry) throws IOException;
	}
}
