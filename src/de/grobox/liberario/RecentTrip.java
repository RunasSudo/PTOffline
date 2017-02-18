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

package de.grobox.liberario;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import de.grobox.liberario.utils.TransportrUtils;
import de.grobox.liberario.pte.dto.Location;

public class RecentTrip implements Serializable, Comparable<RecentTrip> {

	private static final long serialVersionUID = 1690558255337614838L;

	private Location from;
	private Location to;
	private Location via;
	private int count;
	private Date last_used;
	private boolean is_favourite;

	public RecentTrip(Location from, Location via, Location to) {
		this(from, via, to, false);
	}

	public RecentTrip(Location from, Location via, Location to, boolean is_favourite) {
		this(from, via, to, 1, null, is_favourite);
	}

	@SuppressLint("SimpleDateFormat")
	public RecentTrip(Location from, Location via, Location to, int count, String last_used, boolean is_favourite) {
		this.from = from;
		this.via = via;
		this.to = to;
		this.count = count;

		try {
			this.last_used = last_used == null ? new Date() : (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).parse(last_used);
		} catch(ParseException e) {
			e.printStackTrace();
			this.last_used = new Date();
		}

		this.is_favourite = is_favourite;
	}

	public boolean isFavourite() {
		return is_favourite;
	}

	public void setFavourite(boolean is_favourite) {
		this.is_favourite = is_favourite;
	}

	public Location getFrom() {
		return from;
	}

	public Location getVia() {
		return via;
	}

	public Location getTo() {
		return to;
	}

	public int getCount() {
		return count;
	}

	@Override
	public boolean equals(Object o)	{
		if(o == this) {
			return true;
		}

		if(o instanceof RecentTrip) {
			RecentTrip t = (RecentTrip) o;
			if(getFrom().equals(t.getFrom()) &&
					getTo().equals(t.getTo()) &&
					((getVia() == null && t.getVia() == null) || ( getVia() != null && getVia().equals(t.getVia())))
			) {
				return true;
			}
		}
		return false;
	}

	@Override
	public int compareTo(@NonNull RecentTrip other) {
		return other.getCount() - getCount();
	}

	@Override
	public String toString() {
		String via = TransportrUtils.getLocName(getTo());
		String viaStr = via.equals("") ? via : " → " + via;
		return TransportrUtils.getLocName(getFrom()) + viaStr + " → " + TransportrUtils.getLocName(getTo()) + " (" + Integer.toString(getCount()) + ")";
	}

}
