/*
 * Copyright 2017 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.grobox.liberario.pte;

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

import de.grobox.liberario.TransportrApplication;
import de.grobox.liberario.pte.dto.Location;
import de.grobox.liberario.pte.dto.LocationType;
import de.grobox.liberario.pte.dto.NearbyLocationsResult;
import de.grobox.liberario.pte.dto.Point;
import de.grobox.liberario.pte.dto.Product;
import de.grobox.liberario.pte.dto.QueryDeparturesResult;
import de.grobox.liberario.pte.dto.QueryTripsContext;
import de.grobox.liberario.pte.dto.QueryTripsResult;
import de.grobox.liberario.pte.dto.ResultHeader;
import de.grobox.liberario.pte.dto.Style;
import de.grobox.liberario.pte.dto.SuggestLocationsResult;
import de.grobox.liberario.pte.dto.SuggestedLocation;
import de.grobox.liberario.utils.TransportrUtils;

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

	String unquote(String str) {
		// Regex replace is *very* expensive
		if (str.startsWith("\"") && str.endsWith("\"")) {
			str = str.substring(1, str.length() - 1);
		}
		return str;
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
		if (maxDistance <= 0) {
			maxDistance = 50000;
		}

		// TreeSet is sorted, so we can keep only the "maxLocations" closest in memory
		TreeSet<WorkingNearbyLocation> locations = new TreeSet<WorkingNearbyLocation>(new Comparator<WorkingNearbyLocation>() {
			public int compare(WorkingNearbyLocation l1, WorkingNearbyLocation l2) {
				return (int) (l1.distance - l2.distance);
			}
		});

		File cacheDir = TransportrApplication.getAppContext().getCacheDir();

		// Apache ZipFile requires unsupported NIO File.toPath
		ZipFile zipFile = new ZipFile("/sdcard/gtfs.zip");
		Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
		//while (locations.size() < maxLocations && zipEntries.hasMoreElements()) {
		while (zipEntries.hasMoreElements()) {
			ZipEntry zipEntry = zipEntries.nextElement();
			if(!zipEntry.isDirectory()) {
				// Process this GTFS zip
				System.out.println(zipEntry.getName());

				ZipArchiveInputStream gtfsStream = new ZipArchiveInputStream(zipFile.getInputStream(zipEntry));
				ZipArchiveEntry stopsEntry;
				do {
					stopsEntry = gtfsStream.getNextZipEntry();
				} while (!stopsEntry.getName().equals("stops.txt"));
				BufferedReader stopsReader = new BufferedReader(new InputStreamReader(gtfsStream));

				String line = stopsReader.readLine();
				// Handle BOM
				if (line.startsWith("\ufeff")) {
					line = line.substring(1);
				}
				//System.out.println(line);
				List<String> cols = Arrays.asList(line.split(","));
				int col_stop_id = cols.indexOf("stop_id");
				int col_stop_name = cols.indexOf("stop_name");
				int col_stop_lat = cols.indexOf("stop_lat");
				int col_stop_lon = cols.indexOf("stop_lon");

				try {
					//while(locations.size() < maxLocations && (line = stopsReader.readLine()) != null) {
					while((line = stopsReader.readLine()) != null) {
						//System.out.println(line);
						String[] bits = line.split(",");
						double lat = Double.parseDouble(unquote(bits[col_stop_lat]));
						double lon = Double.parseDouble(unquote(bits[col_stop_lon]));
						Point point = Point.fromDouble(lat, lon);
						Location stopLocation = Location.coord(point);
						double distance = TransportrUtils.computeDistance(location, stopLocation);
						if(distance <= maxDistance) {
							String id = unquote(bits[col_stop_id]);
							String name = unquote(bits[col_stop_name]);
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
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				stopsReader.close();
			}
		}

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
