/*    Transportr
 *    Copyright (C) 2013 - 2016 Torsten Grote
 *
 *    This program is Free Software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as
 *    published by the Free Software Foundation, either version 3 of the
 *    License, or (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.github.runassudo.ptoffline;

import java.util.ArrayList;
import java.util.List;

import io.github.runassudo.ptoffline.pte.dto.Trip;

// this hack seems to be necessary because RecyclerView has no way to save view states for its items
public class ListTrip {
	public Trip trip;
	public boolean expanded;

	public ListTrip(Trip trip) {
		this.trip = trip;

		// don't expand trips initially
		this.expanded = false;
	}

	public static List<ListTrip> getList(List<Trip> trip_list) {
		List<ListTrip> new_list = new ArrayList<>();

		for(Trip trip : trip_list) {
			new_list.add(new ListTrip(trip));
		}

		return new_list;
	}

	// used when checking list.contains(trip)
	// ignores expand state
	public boolean equals(Object o) {
		if(o == this) {
			return true;
		}
		if(!(o instanceof ListTrip)) {
			return false;
		}
		final ListTrip other = (ListTrip) o;

		return trip.equals(other.trip);
	}
}
