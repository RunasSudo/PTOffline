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

import java.io.IOException;
import java.util.Date;
import java.util.EnumSet;
import java.util.Set;

import javax.annotation.Nullable;

import de.grobox.liberario.pte.dto.Location;
import de.grobox.liberario.pte.dto.LocationType;
import de.grobox.liberario.pte.dto.NearbyLocationsResult;
import de.grobox.liberario.pte.dto.Point;
import de.grobox.liberario.pte.dto.Product;
import de.grobox.liberario.pte.dto.QueryDeparturesResult;
import de.grobox.liberario.pte.dto.QueryTripsContext;
import de.grobox.liberario.pte.dto.QueryTripsResult;
import de.grobox.liberario.pte.dto.Style;
import de.grobox.liberario.pte.dto.SuggestLocationsResult;

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
		// NYI
		return false;
	}

	public NearbyLocationsResult queryNearbyLocations(EnumSet<LocationType> types, Location location, int maxDistance,
	                                           int maxLocations) throws IOException {
		// NYI
		return null;
	}

	public QueryDeparturesResult queryDepartures(String stationId, @Nullable Date time, int maxDepartures, boolean equivs)
			throws IOException {
		// NYI
		return null;
	}

	public SuggestLocationsResult suggestLocations(CharSequence constraint) throws IOException {
		// NYI
		return null;
	}

	public Set<Product> defaultProducts() {
		// NYI
		return null;
	}

	public QueryTripsResult queryTrips(Location from, @Nullable Location via, Location to, Date date, boolean dep,
	                            @Nullable Set<Product> products, @Nullable Optimize optimize, @Nullable WalkSpeed walkSpeed,
	                            @Nullable Accessibility accessibility, @Nullable Set<Option> options) throws IOException {
		// NYI
		return null;
	}

	public QueryTripsResult queryMoreTrips(QueryTripsContext context, boolean later) throws IOException {
		// NYI
		return null;
	}

	public Style lineStyle(@Nullable String network, @Nullable Product product, @Nullable String label) {
		// NYI
		return null;
	}

	public Point[] getArea() throws IOException {
		return null;
	}
}
