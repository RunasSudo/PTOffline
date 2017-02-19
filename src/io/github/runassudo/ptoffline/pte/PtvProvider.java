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

package io.github.runassudo.ptoffline.pte;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.EnumSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.annotation.Nullable;

import io.github.runassudo.ptoffline.TransportrApplication;
import io.github.runassudo.ptoffline.pte.dto.Location;
import io.github.runassudo.ptoffline.pte.dto.LocationType;
import io.github.runassudo.ptoffline.pte.dto.NearbyLocationsResult;
import io.github.runassudo.ptoffline.pte.dto.Point;
import io.github.runassudo.ptoffline.pte.dto.Product;
import io.github.runassudo.ptoffline.pte.dto.QueryDeparturesResult;
import io.github.runassudo.ptoffline.pte.dto.QueryTripsContext;
import io.github.runassudo.ptoffline.pte.dto.QueryTripsResult;
import io.github.runassudo.ptoffline.pte.dto.ResultHeader;
import io.github.runassudo.ptoffline.pte.dto.Style;
import io.github.runassudo.ptoffline.pte.dto.SuggestLocationsResult;
import io.github.runassudo.ptoffline.pte.dto.SuggestedLocation;
import io.github.runassudo.ptoffline.utils.TransportrUtils;
import io.github.runassudo.gtfs.GTFSCollection;

/**
 * @author RunasSudo
 */
public class PtvProvider extends AbstractNetworkProvider
{
	public PtvProvider()
	{
		super(NetworkId.PTV);

		setTimeZone("Australia/Melbourne");
	}

	protected boolean hasCapability(Capability capability) {
		switch (capability) {
			case NEARBY_LOCATIONS:
				return true;
			default:
				return false;
		}
	}

	class WorkingNearbyLocation {
		Location location;
		double distance;

		WorkingNearbyLocation(Location location, double distance) {
			this.location = location;
			this.distance = distance;
		}
	}

	public NearbyLocationsResult queryNearbyLocations(EnumSet<LocationType> types, Location location, int maxDistance,
	                                           int maxLocations) throws IOException {
		final int actualMaxDistance;
		if (maxDistance <= 0) {
			actualMaxDistance = 50000;
		} else {
			actualMaxDistance = maxDistance;
		}

		// TreeSet is sorted, so we can keep only the "maxLocations" closest in memory
		TreeSet<WorkingNearbyLocation> locations = new TreeSet<WorkingNearbyLocation>(new Comparator<WorkingNearbyLocation>() {
			public int compare(WorkingNearbyLocation l1, WorkingNearbyLocation l2) {
				return (int) (l1.distance - l2.distance);
			}
		});

		GTFSCollection gtfsCollection = new GTFSCollection(new File("/sdcard/gtfs.zip"));
		gtfsCollection.iterateThroughContents("stops.txt", gtfsCsv -> {
			gtfsCsv.iterateThroughEntries(gtfsEntry -> {
				double lat = Double.parseDouble(gtfsEntry.getField("stop_lat"));
				double lon = Double.parseDouble(gtfsEntry.getField("stop_lon"));
				Point point = Point.fromDouble(lat, lon);
				Location stopLocation = Location.coord(point);
				double distance = TransportrUtils.computeDistance(location, stopLocation);
				if(distance <= actualMaxDistance) {
					String id = gtfsEntry.getField("stop_id");
					String name = gtfsEntry.getField("stop_name");
					Location stop = new Location(LocationType.STATION, id, point, null, name);
					if (locations.size() < maxLocations) {
						locations.add(new WorkingNearbyLocation(stop, distance));
					} else {
						WorkingNearbyLocation currentMax = locations.last();
						if (distance < currentMax.distance) {
							locations.pollLast();
							locations.add(new WorkingNearbyLocation(stop, distance));
						}
					}
				}
			});
		});

		ArrayList<Location> locationsList = new ArrayList<Location>();
		for (WorkingNearbyLocation workingLocation : locations) {
			locationsList.add(workingLocation.location);
		}

		final ResultHeader resultHeader = new ResultHeader(network, "PTOffline");
		return new NearbyLocationsResult(resultHeader, locationsList);
	}

	public QueryDeparturesResult queryDepartures(String stationId, @Nullable Date time, int maxDepartures, boolean equivs)
			throws IOException {
		// NYI
		throw new RuntimeException("Heya, queryDepartures!");
	}

	public SuggestLocationsResult suggestLocations(CharSequence constraint) throws IOException {
		final ResultHeader resultHeader = new ResultHeader(network, "PTOffline");
		return new SuggestLocationsResult(resultHeader, new ArrayList<SuggestedLocation>());
	}

	public Set<Product> defaultProducts() {
		// NYI
		throw new RuntimeException("Heya, defaultProducts!");
	}

	public QueryTripsResult queryTrips(Location from, @Nullable Location via, Location to, Date date, boolean dep,
	                            @Nullable Set<Product> products, @Nullable Optimize optimize, @Nullable WalkSpeed walkSpeed,
	                            @Nullable Accessibility accessibility, @Nullable Set<Option> options) throws IOException {
		// NYI
		throw new RuntimeException("Heya, queryTrips!");
	}

	public QueryTripsResult queryMoreTrips(QueryTripsContext context, boolean later) throws IOException {
		// NYI
		throw new RuntimeException("Heya, queryMoreTrips!");
	}

	public Style lineStyle(@Nullable String network, @Nullable Product product, @Nullable String label) {
		// NYI
		throw new RuntimeException("Heya, lineStyle!");
	}

	public Point[] getArea() throws IOException {
		return new Point[] { Point.fromDouble(-37.5043, 144.5326), Point.fromDouble(-37.5043, 145.4433), Point.fromDouble(-38.1684, 145.4433), Point.fromDouble(-38.1684, 144.5326) };
	}
}
