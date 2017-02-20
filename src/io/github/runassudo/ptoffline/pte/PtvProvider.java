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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nullable;

import io.github.runassudo.gtfs.FlatGTFSCSV;
import io.github.runassudo.gtfs.GTFSCollection;
import io.github.runassudo.gtfs.GTFSEntry;
import io.github.runassudo.gtfs.GTFSFile;
import io.github.runassudo.ptoffline.pte.dto.Departure;
import io.github.runassudo.ptoffline.pte.dto.Line;
import io.github.runassudo.ptoffline.pte.dto.Location;
import io.github.runassudo.ptoffline.pte.dto.LocationType;
import io.github.runassudo.ptoffline.pte.dto.NearbyLocationsResult;
import io.github.runassudo.ptoffline.pte.dto.Point;
import io.github.runassudo.ptoffline.pte.dto.Product;
import io.github.runassudo.ptoffline.pte.dto.QueryDeparturesResult;
import io.github.runassudo.ptoffline.pte.dto.QueryTripsContext;
import io.github.runassudo.ptoffline.pte.dto.QueryTripsResult;
import io.github.runassudo.ptoffline.pte.dto.ResultHeader;
import io.github.runassudo.ptoffline.pte.dto.StationDepartures;
import io.github.runassudo.ptoffline.pte.dto.Stop;
import io.github.runassudo.ptoffline.pte.dto.Style;
import io.github.runassudo.ptoffline.pte.dto.SuggestLocationsResult;
import io.github.runassudo.ptoffline.pte.dto.SuggestedLocation;
import io.github.runassudo.ptoffline.pte.dto.Trip;
import io.github.runassudo.ptoffline.utils.TransportrUtils;

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
			case SUGGEST_LOCATIONS:
			case NEARBY_LOCATIONS:
			case DEPARTURES:
			case TRIPS:
				return true;
			default:
				return false;
		}
	}

	GTFSCollection getGTFSCollection() {
		return new GTFSCollection(new File("/sdcard/gtfs.zip"));
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
		TreeSet<WorkingNearbyLocation> locations = new TreeSet<>((l1, l2) -> (int) (l1.distance - l2.distance));

		GTFSCollection gtfsCollection = getGTFSCollection();
		gtfsCollection.iterateThroughContents("stops.txt", gtfsCsv -> {
			final boolean[] done = new boolean[] { false }; // DODGY!
			gtfsCsv.iterateThroughEntries(gtfsEntry -> {
				if (done[0]) {
					return;
				}

				double lat = Double.parseDouble(gtfsEntry.getField("stop_lat"));
				double lon = Double.parseDouble(gtfsEntry.getField("stop_lon"));
				Point point = Point.fromDouble(lat, lon);
				Location stopLocation = Location.coord(point);
				double distance = TransportrUtils.computeDistance(location, stopLocation);
				if(distance <= actualMaxDistance) {
					String id = gtfsEntry.getField("stop_id");
					String name = gtfsEntry.getField("stop_name");
					Location stop = new Location(LocationType.STATION, id, point, null, name);
					if(locations.size() < maxLocations) {
						locations.add(new WorkingNearbyLocation(stop, distance));
						if (maxLocations == 1 && location.type != LocationType.COORD && name.equals(location.name)) {
							done[0] = true;
						}
					} else {
						WorkingNearbyLocation currentMax = locations.last();
						if(distance < currentMax.distance) {
							locations.pollLast();
							locations.add(new WorkingNearbyLocation(stop, distance));
							if (maxLocations == 1 && location.type != LocationType.COORD && name.equals(location.name)) {
								done[0] = true;
							}
						}
					}
				}
			});
		});

		ArrayList<Location> locationsList = new ArrayList<>();
		for (WorkingNearbyLocation workingLocation : locations) {
			locationsList.add(workingLocation.location);
		}

		final ResultHeader resultHeader = new ResultHeader(network, "PTOffline");
		return new NearbyLocationsResult(resultHeader, locationsList);
	}

	final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
	final static SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");

	class ServiceCalendarData {
		boolean[] weekdays; // Mon to Sun
		Calendar startDate;
		Calendar endDate;

		ArrayList<String> datesAdded = new ArrayList<>();
		ArrayList<String> datesRemoved = new ArrayList<>();

		Calendar nextDate;

		public ServiceCalendarData(boolean[] weekdays, Date startDate, Date endDate) {
			this.weekdays = weekdays;
			this.startDate = Calendar.getInstance();
			this.startDate.setTime(startDate);
			this.endDate = Calendar.getInstance();
			this.endDate.setTime(endDate);
			this.endDate.add(Calendar.DAY_OF_MONTH, 1); // Since we use .before with this date, but want <= behaviour
		}

		boolean isDateOkay(Calendar date) {
			String dateString = dateFormat.format(date.getTime());
			if (datesAdded.contains(dateString)) {
				return true;
			}
			int weekday = (date.get(Calendar.DAY_OF_WEEK) - 2) % 7; // sunday = 1, saturday = 7 in Java
			weekday = (weekday + 7) % 7; // Ensure positive
			if (weekdays[weekday] && date.after(startDate) && !datesRemoved.contains(dateString)) {
				return true;
			}
			return false;
		}

		public void precomputeNextDate(Date after) {
			Calendar date = Calendar.getInstance();
			date.setTime(after);
			if (date.before(startDate)) {
				date = startDate;
			}
			// Account for >= startDate
			date.set(Calendar.HOUR_OF_DAY, 23);
			date.set(Calendar.MINUTE, 59);
			date.set(Calendar.SECOND, 59);
			date.set(Calendar.MILLISECOND, 0);

			while (date.before(endDate)) {
				if (isDateOkay(date)) {
					nextDate = date;
					return;
				}
				date.add(Calendar.DAY_OF_MONTH, 1);
			}
		}

		public Date nextDate(Date after, Date timeOfDay) {
			if (nextDate == null) {
				return null;
			}

			Calendar timeOfDayCal = Calendar.getInstance();
			timeOfDayCal.setTime(timeOfDay);
			Calendar afterCal = Calendar.getInstance();
			afterCal.setTime(after);

			Calendar date = (Calendar) nextDate.clone();
			date.set(Calendar.HOUR_OF_DAY, timeOfDayCal.get(Calendar.HOUR_OF_DAY));
			date.set(Calendar.MINUTE, timeOfDayCal.get(Calendar.MINUTE));
			date.set(Calendar.SECOND, timeOfDayCal.get(Calendar.SECOND));
			date.set(Calendar.MILLISECOND, 0);

			if (date.after(afterCal)) {
				return date.getTime();
			} else {
				while(date.before(endDate)) {
					date.add(Calendar.DAY_OF_MONTH, 1);
					if(isDateOkay(date)) {
						return date.getTime();
					}
				}
				return null;
			}
		}
	}

	class WorkingTripDepartureRoute {
		GTFSFile gtfsFile;

		String trip_id;
		String departure_time;

		String service_id;
		ServiceCalendarData serviceCalendarData;

		String route_id;
		Line line;
		String trip_headsign;

		WorkingTripDepartureRoute(String trip_id, String departure_time, GTFSFile gtfsFile) {
			this.trip_id = trip_id;
			this.departure_time = departure_time;
			this.gtfsFile = gtfsFile;
		}
	}

	public QueryDeparturesResult queryDepartures(String stationId, @Nullable Date time, int maxDepartures, boolean equivs)
			throws IOException {
		// TODO: Implement equivs (from PTE; Transportr doesn't seem to use it)

		if (time == null) {
			time = new Date();
		}

		// Get stop for stationId
		// Also identify which file stationId is in
		// Otherwise it will be *very* slow looping through all of them
		System.out.println("Getting stop");
		final Location[] station_ = new Location[] { null }; // DODGY!
		final HashSet<GTFSFile> gtfsFiles = new HashSet<>();
		GTFSCollection gtfsCollection = getGTFSCollection();
		gtfsCollection.iterateThroughMembers(gtfsFile -> {
			gtfsFile.iterateThroughContents(gtfsCsv -> {
				if (gtfsCsv.getName().equals("stops.txt")) {
					gtfsCsv.iterateThroughEntries(gtfsEntry -> {
						if(station_[0] != null) {
							return;
						}

						String id = gtfsEntry.getField("stop_id");
						if(id.equals(stationId)) {
							// Cache this file
							gtfsFiles.add(gtfsFile.toFlatFile());
							//gtfsFiles.add(gtfsFile);

							String name = gtfsEntry.getField("stop_name");
							double lat = Double.parseDouble(gtfsEntry.getField("stop_lat"));
							double lon = Double.parseDouble(gtfsEntry.getField("stop_lon"));
							Point point = Point.fromDouble(lat, lon);
							Location station = new Location(LocationType.STATION, id, point, null, name);
							station_[0] = station;
						}
					});
				}
			});
		});
		Location station = station_[0];
		System.out.println("Got stop " + station.id);

		HashMap<GTFSFile, HashMap<String, ServiceCalendarData>> calendarDataMap = new HashMap<>();

		// Get service calendar data
		System.out.println("Getting calendar data");
		for (GTFSFile gtfsFile : gtfsFiles) {
			HashMap<String, ServiceCalendarData> thisFileCalendarDataMap = new HashMap<>();
			gtfsFile.iterateThroughContents("calendar.txt", gtfsCsv -> {
				gtfsCsv.iterateThroughEntries(gtfsEntry -> {
					String service_id = gtfsEntry.getField("service_id");
					boolean[] weekdays = new boolean[] {
							gtfsEntry.getField("monday").equals("1"),
							gtfsEntry.getField("tuesday").equals("1"),
							gtfsEntry.getField("wednesday").equals("1"),
							gtfsEntry.getField("thursday").equals("1"),
							gtfsEntry.getField("friday").equals("1"),
							gtfsEntry.getField("saturday").equals("1"),
							gtfsEntry.getField("sunday").equals("1"),
					};
					try {
						thisFileCalendarDataMap.put(service_id, new ServiceCalendarData(weekdays, dateFormat.parse(gtfsEntry.getField("start_date")), dateFormat.parse(gtfsEntry.getField("end_date"))));
					} catch(ParseException ex) {
						ex.printStackTrace();
						throw new IOException("Error parsing date format");
					}
				});
			});
			calendarDataMap.put(gtfsFile, thisFileCalendarDataMap);
		}
		System.out.println("Getting calendar exceptions");
		for (GTFSFile gtfsFile : gtfsFiles) {
			HashMap<String, ServiceCalendarData> thisFileCalendarDataMap = calendarDataMap.get(gtfsFile);
			gtfsFile.iterateThroughContents("calendar_dates.txt", gtfsCsv -> {
				gtfsCsv.iterateThroughEntries(gtfsEntry -> {
					String service_id = gtfsEntry.getField("service_id");
					String exception_type = gtfsEntry.getField("exception_type");
					if(exception_type.equals("1")) {
						thisFileCalendarDataMap.get(service_id).datesAdded.add(gtfsEntry.getField("date"));
					} else if(exception_type.equals("2")) {
						thisFileCalendarDataMap.get(service_id).datesRemoved.add(gtfsEntry.getField("date"));
					} else {
						throw new IOException("Unknown exception_type " + exception_type);
					}
				});
			});
			for (Map.Entry<String, ServiceCalendarData> entry : thisFileCalendarDataMap.entrySet()) {
				System.out.println("Precomputing time for " + entry.getKey());
				entry.getValue().precomputeNextDate(time);
			}
		}

		StationDepartures stationDepartures = new StationDepartures(station, new ArrayList<>(), null);

		// Identify trips with this stop
		System.out.println("Finding trips in " + gtfsFiles.size() + " files");
		ArrayList<WorkingTripDepartureRoute> trips = new ArrayList<>();
		ArrayList<String> tripIds = new ArrayList<>();
		for (GTFSFile gtfsFile : gtfsFiles) {
			gtfsFile.iterateThroughContents("stop_times.txt", gtfsCsv -> {
				File file = ((FlatGTFSCSV) gtfsCsv).file;

				// Get data
				BufferedReader reader = gtfsCsv.getBufferedReader();
				String line = reader.readLine();
				// Handle BOM
				if (line.startsWith("\ufeff")) {
					line = line.substring(1);
				}
				gtfsCsv.fieldsReversed = Arrays.asList(line.split(","));
				reader.close();

				// UNLEASH THE SUPER DODGY HACK!
				// BufferedReader is waaaay too slow for the whole file (approx. 15-20 seconds for buses)
				Process proc = new ProcessBuilder("grep", stationId, file.getPath()).start();
				reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				while((line = reader.readLine()) != null) {
					String[] fields = line.split(",");
					GTFSEntry gtfsEntry = new GTFSEntry(gtfsCsv, fields);
					String stop_id = gtfsEntry.getField("stop_id");
					if(stop_id.equals(stationId)) {
						String trip_id = gtfsEntry.getField("trip_id");
						trips.add(new WorkingTripDepartureRoute(trip_id, gtfsEntry.getField("departure_time"), gtfsFile));
						tripIds.add(trip_id);
					}
				}
			});
		}
		System.out.println("Found " + trips.size() + " trips");

		// Get correspoding routes
		System.out.println("Getting routes");
		HashSet<String> routeIds = new HashSet<>();
		for (GTFSFile gtfsFile : gtfsFiles) {
			HashMap<String, ServiceCalendarData> thisFileCalendarDataMap = calendarDataMap.get(gtfsFile);
			gtfsFile.iterateThroughContents("trips.txt", gtfsCsv -> {
				gtfsCsv.iterateThroughEntries(gtfsEntry -> {
					String trip_id = gtfsEntry.getField("trip_id");
					if(tripIds.contains(trip_id)) {
						for(WorkingTripDepartureRoute workingTripDepartureRoute : trips) {
							if(workingTripDepartureRoute.gtfsFile != gtfsFile) {
								continue;
							}
							if(trip_id.equals(workingTripDepartureRoute.trip_id)) {
								String route_id = gtfsEntry.getField("route_id");
								workingTripDepartureRoute.route_id = route_id;
								String service_id = gtfsEntry.getField("service_id");
								workingTripDepartureRoute.service_id = service_id;
								routeIds.add(route_id);
								workingTripDepartureRoute.serviceCalendarData = thisFileCalendarDataMap.get(service_id);
								workingTripDepartureRoute.trip_headsign = gtfsEntry.getField("trip_headsign");
							}
						}
					}
				});
			});
		}

		// Get details for route
		System.out.println("Getting route details");
		for (GTFSFile gtfsFile : gtfsFiles) {
			gtfsFile.iterateThroughContents("routes.txt", gtfsCsv -> {
				gtfsCsv.iterateThroughEntries(gtfsEntry -> {
					String route_id = gtfsEntry.getField("route_id");
					if (routeIds.contains(route_id)) {
						for(WorkingTripDepartureRoute workingTripDepartureRoute : trips) {
							if(workingTripDepartureRoute.gtfsFile != gtfsFile) {
								continue;
							}
							if(route_id.equals(workingTripDepartureRoute.route_id)) {
								workingTripDepartureRoute.line = new Line(route_id, null, null, gtfsEntry.getField("route_short_name"), gtfsEntry.getField("route_long_name"), null);
							}
						}
					}
				});
			});
		}

		System.out.println("Building departures");
		for (WorkingTripDepartureRoute wtdr : trips) {
			try {
				Date departureTime = wtdr.serviceCalendarData.nextDate(time, timeFormat.parse(wtdr.departure_time));
				if(departureTime != null) {
					stationDepartures.departures.add(new Departure(
							departureTime,
							null,
							wtdr.line,
							null,
							new Location(LocationType.STATION, wtdr.trip_id + "_destination", null, wtdr.trip_headsign),
							null,
							null
					));
				}
			} catch (ParseException ex) {
				ex.printStackTrace();
				throw new IOException("Error parsing time format");
			}
		}

		QueryDeparturesResult qdr = new QueryDeparturesResult(new ResultHeader(network, "PTOffline"));
		qdr.stationDepartures.add(stationDepartures);
		return qdr;
	}

	public SuggestLocationsResult suggestLocations(CharSequence constraint) throws IOException {
		ArrayList<SuggestedLocation> locations = new ArrayList<>();

		GTFSCollection gtfsCollection = getGTFSCollection();
		gtfsCollection.iterateThroughContents("stops.txt", gtfsCsv -> {
			gtfsCsv.iterateThroughEntries(gtfsEntry -> {
				String name = gtfsEntry.getField("stop_name");
				if(name.toLowerCase().contains(constraint.toString().toLowerCase())) {
					String id = gtfsEntry.getField("stop_id");
					double lat = Double.parseDouble(gtfsEntry.getField("stop_lat"));
					double lon = Double.parseDouble(gtfsEntry.getField("stop_lon"));
					Point point = Point.fromDouble(lat, lon);
					locations.add(new SuggestedLocation(new Location(LocationType.STATION, id, point, null, name), name.toLowerCase().startsWith(constraint.toString().toLowerCase()) ? 1 : 0));
				}
			});
		});

		final ResultHeader resultHeader = new ResultHeader(network, "PTOffline");
		return new SuggestLocationsResult(resultHeader, locations);
	}

	public Set<Product> defaultProducts() {
		// NYI
		throw new RuntimeException("Heya, defaultProducts!");
	}

	class WorkingTrip {
		String trip_headsign;

		String route_id;
		Line line;

		String service_id;
		ServiceCalendarData serviceCalendarData;

		ArrayList<WorkingTripStop> stops = new ArrayList<>();

		public WorkingTrip(String trip_headsign, String route_id, String service_id) {
			this.trip_headsign = trip_headsign;
			this.route_id = route_id;
			this.service_id = service_id;
		}
	}

	class WorkingTripStop {
		String arrival_time;
		String depatrure_time;

		String stop_id;
		String name;
		double lat;
		double lon;

		public WorkingTripStop(String arrival_time, String depatrure_time, String stop_id) {
			this.arrival_time = arrival_time;
			this.depatrure_time = depatrure_time;
			this.stop_id = stop_id;
		}

		public Location toLocation() {
			return new Location(LocationType.STATION, stop_id, Point.fromDouble(lat, lon), null, name);
		}

		public Stop toStop(WorkingTrip trip, Date date) throws ParseException {
			Date firstArrivalTime = trip.serviceCalendarData.nextDate(date, timeFormat.parse(arrival_time));
			Date firstDepartureTime = trip.serviceCalendarData.nextDate(date, timeFormat.parse(depatrure_time));
			return new Stop(toLocation(), firstArrivalTime, null, null, null, false, firstDepartureTime, null, null, null, false);
		}
	}

	public QueryTripsResult queryTrips(Location from, @Nullable Location via, Location to, Date date, boolean dep,
	                            @Nullable Set<Product> products, @Nullable Optimize optimize, @Nullable WalkSpeed walkSpeed,
	                            @Nullable Accessibility accessibility, @Nullable Set<Option> options) throws IOException {
		if (to.id.endsWith("_destination")) {
			// A simple trip query
			System.out.println(to.id);
			String trip_id = to.id.substring(0, to.id.length() - 12);
			System.out.println(trip_id);

			// Identify trip and files
			System.out.println("Finding trip");
			final WorkingTrip[] trip_ = new WorkingTrip[] { null }; // DODGY!
			final GTFSFile[] gtfsFile_ = new GTFSFile[] { null };
			GTFSCollection gtfsCollection = getGTFSCollection();
			gtfsCollection.iterateThroughMembers(gtfsFile -> {
				gtfsFile.iterateThroughContents(gtfsCsv -> {
					if (gtfsCsv.getName().equals("trips.txt")) {
						gtfsCsv.iterateThroughEntries(gtfsEntry -> {
							if(trip_[0] != null) {
								return;
							}

							String id = gtfsEntry.getField("trip_id");
							if(id.equals(trip_id)) {
								// Cache this file
								gtfsFile_[0] = gtfsFile.toFlatFile();
								trip_[0] = new WorkingTrip(gtfsEntry.getField("trip_headsign"), gtfsEntry.getField("route_id"), gtfsEntry.getField("service_id"));
							}
						});
					}
				});
			});
			WorkingTrip trip = trip_[0];
			GTFSFile gtfsFile = gtfsFile_[0];

			// Get service calendar data
			System.out.println("Getting calendar data");
			gtfsFile.iterateThroughContents("calendar.txt", gtfsCsv -> {
				gtfsCsv.iterateThroughEntries(gtfsEntry -> {
					String service_id = gtfsEntry.getField("service_id");
					if (service_id.equals(trip.service_id)) {
						boolean[] weekdays = new boolean[] {
								gtfsEntry.getField("monday").equals("1"),
								gtfsEntry.getField("tuesday").equals("1"),
								gtfsEntry.getField("wednesday").equals("1"),
								gtfsEntry.getField("thursday").equals("1"),
								gtfsEntry.getField("friday").equals("1"),
								gtfsEntry.getField("saturday").equals("1"),
								gtfsEntry.getField("sunday").equals("1"),
						};
						try {
							trip.serviceCalendarData = new ServiceCalendarData(weekdays, dateFormat.parse(gtfsEntry.getField("start_date")), dateFormat.parse(gtfsEntry.getField("end_date")));
						} catch(ParseException ex) {
							ex.printStackTrace();
							throw new IOException("Error parsing date format");
						}
					}
				});
			});
			System.out.println("Getting calendar exceptions");
			gtfsFile.iterateThroughContents("calendar_dates.txt", gtfsCsv -> {
				gtfsCsv.iterateThroughEntries(gtfsEntry -> {
					String service_id = gtfsEntry.getField("service_id");
					String exception_type = gtfsEntry.getField("exception_type");
					if(exception_type.equals("1")) {
						trip.serviceCalendarData.datesAdded.add(gtfsEntry.getField("date"));
					} else if(exception_type.equals("2")) {
						trip.serviceCalendarData.datesRemoved.add(gtfsEntry.getField("date"));
					} else {
						throw new IOException("Unknown exception_type " + exception_type);
					}
				});
			});

			System.out.println("Precomputing time");
			trip.serviceCalendarData.precomputeNextDate(date);

			// Get route data
			System.out.println("Getting route data");
			gtfsFile.iterateThroughContents("routes.txt", gtfsCsv -> {
				gtfsCsv.iterateThroughEntries(gtfsEntry -> {
					String route_id = gtfsEntry.getField("route_id");
					if (route_id.equals(trip.route_id)) {
						trip.line = new Line(route_id, null, null, gtfsEntry.getField("route_short_name"), gtfsEntry.getField("route_long_name"), null);
					}
				});
			});

			// Get stops
			System.out.println("Getting stops");
			HashSet<String> stopIds = new HashSet<>();
			int[] start_index_ = new int[] { -1 };
			gtfsFile.iterateThroughContents("stop_times.txt", gtfsCsv -> {
				File file = ((FlatGTFSCSV) gtfsCsv).file;

				// Get data
				BufferedReader reader = gtfsCsv.getBufferedReader();
				String line = reader.readLine();
				// Handle BOM
				if (line.startsWith("\ufeff")) {
					line = line.substring(1);
				}
				gtfsCsv.fieldsReversed = Arrays.asList(line.split(","));
				reader.close();

				// SUPER DODGY HACK
				Process proc = new ProcessBuilder("grep", trip_id, file.getPath()).start();
				reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
				while((line = reader.readLine()) != null) {
					String[] fields = line.split(",");
					GTFSEntry gtfsEntry = new GTFSEntry(gtfsCsv, fields);
					String trip_id1 = gtfsEntry.getField("trip_id");
					if(trip_id1.equals(trip_id)) {
						String stop_id = gtfsEntry.getField("stop_id");
						int index = Integer.parseInt(gtfsEntry.getField("stop_sequence")) - 1;
						trip.stops.add(index, new WorkingTripStop(gtfsEntry.getField("arrival_time"), gtfsEntry.getField("departure_time"), stop_id));
						stopIds.add(stop_id);
						if (stop_id.equals(from.id)) {
							start_index_[0] = index;
						}
					}
				}
			});
			int start_index = start_index_[0];

			// Process stop data
			System.out.println("Getting stop data");
			gtfsFile.iterateThroughContents("stops.txt", gtfsCsv -> {
				gtfsCsv.iterateThroughEntries(gtfsEntry -> {
					String stop_id = gtfsEntry.getField("stop_id");
					if (stopIds.contains(stop_id)) {
						for(WorkingTripStop wts : trip.stops) {
							if(stop_id.equals(wts.stop_id)) {
								wts.name = gtfsEntry.getField("stop_name");
								wts.lat = Double.parseDouble(gtfsEntry.getField("stop_lat"));
								wts.lon = Double.parseDouble(gtfsEntry.getField("stop_lon"));
							}
						}
					}
				});
			});

			// Build trip info
			System.out.println("Buildling trip info");
			try {
				WorkingTripStop firstStop = trip.stops.get(start_index);
				Location firstLocation = new Location(LocationType.STATION, firstStop.stop_id, Point.fromDouble(firstStop.lat, firstStop.lon), null, firstStop.name);
				WorkingTripStop lastStop = trip.stops.get(trip.stops.size() - 1);
				Location lastLocation = new Location(LocationType.STATION, lastStop.stop_id, Point.fromDouble(lastStop.lat, lastStop.lon), null, lastStop.name);

				ArrayList<Stop> intermediateStops = new ArrayList<>();
				for (int i = start_index + 1; i < trip.stops.size() - 1; i++) {
					intermediateStops.add(trip.stops.get(i).toStop(trip, date));
				}

				Trip.Public leg = new Trip.Public(trip.line,
						lastStop.toLocation(),
						firstStop.toStop(trip, date),
						lastStop.toStop(trip, date),
						intermediateStops,
						null,
						null
				);

				Trip pteTrip = new Trip(trip_id, firstLocation, lastLocation, Arrays.asList(leg), null, null, null);

				final ResultHeader resultHeader = new ResultHeader(network, "PTOffline");
				return new QueryTripsResult(resultHeader, null, from, via, to, new PtvContext(), Arrays.asList(pteTrip));
			} catch (ParseException ex) {
				ex.printStackTrace();
				throw new IOException("Error parsing date format");
			}
		} else {
			// NYI
			throw new RuntimeException("Heya, queryTrips!");
		}
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

class PtvContext implements QueryTripsContext {
	public boolean canQueryLater() { return false; }
	public boolean canQueryEarlier() { return false; }
}