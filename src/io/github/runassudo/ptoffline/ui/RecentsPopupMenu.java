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

package io.github.runassudo.ptoffline.ui;

import android.content.Context;
import android.view.MenuItem;
import android.view.View;

import io.github.runassudo.ptoffline.RecentTrip;
import io.github.runassudo.ptoffline.R;
import io.github.runassudo.ptoffline.data.RecentsDB;
import io.github.runassudo.ptoffline.utils.TransportrUtils;

public class RecentsPopupMenu extends BasePopupMenu {

	private RecentTrip trip;
	private FavouriteRemovedListener removedListener = null;

	public RecentsPopupMenu(Context context, View anchor, RecentTrip trip) {
		super(context, anchor);

		this.trip = trip;
		this.getMenuInflater().inflate(R.menu.recent_trip_actions, getMenu());

		TransportrUtils.setFavState(context, getMenu().findItem(R.id.action_mark_favourite), trip.isFavourite(), false);

		showIcons();
	}

	public OnMenuItemClickListener getOnMenuItemClickListener() {
		return new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				// handle presses on menu items
				switch(item.getItemId()) {
					// Swap Locations
					case R.id.action_swap_locations:
						TransportrUtils.findDirections(context, trip.getTo(), trip.getVia(), trip.getFrom());

						return true;
					// Preset Locations
					case R.id.action_set_locations:
						TransportrUtils.presetDirections(context, trip.getFrom(), trip.getVia(), trip.getTo());

						return true;
					case R.id.action_mark_favourite:
						RecentsDB.toggleFavouriteTrip(context, trip);
						trip.setFavourite(!trip.isFavourite());

						TransportrUtils.setFavState(context, item, trip.isFavourite(), false);

						if(removedListener != null) {
							removedListener.onFavouriteRemoved();
						}
						return true;
					default:
						return false;
				}
			}
		};
	}

	public void setRemovedListener(FavouriteRemovedListener l) {
		this.removedListener = l;
	}

	public static abstract class FavouriteRemovedListener {
		public abstract void onFavouriteRemoved();
	}

}
