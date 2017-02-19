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

package io.github.runassudo.ptoffline.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;

import io.github.runassudo.ptoffline.Preferences;
import io.github.runassudo.ptoffline.R;
import io.github.runassudo.ptoffline.TransportNetwork;
import io.github.runassudo.ptoffline.adapters.TripAdapter;
import io.github.runassudo.ptoffline.tasks.AsyncQueryTripsTask;
import io.github.runassudo.ptoffline.utils.DateUtils;
import io.github.runassudo.ptoffline.utils.TransportrUtils;
import io.github.runassudo.ptoffline.pte.dto.Location;
import io.github.runassudo.ptoffline.pte.dto.Product;
import io.github.runassudo.ptoffline.pte.dto.QueryTripsResult;
import io.github.runassudo.ptoffline.pte.dto.Trip;

public class TripDetailActivity extends TransportrActivity implements AsyncQueryTripsTask.TripHandler {

	private Trip trip;
	private TripAdapter.BaseTripHolder ui;
	private Menu mMenu;
	private Location from;
	private Location to;
	private ArrayList<Product> products;
	private boolean showLineName;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_trip_details);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

		TransportNetwork network = Preferences.getTransportNetwork(this);
		showLineName = network != null && network.hasGoodLineNames();

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		if(toolbar != null) {
			if(network != null) toolbar.setSubtitle(network.getName());
			setSupportActionBar(toolbar);

			ActionBar actionBar = getSupportActionBar();
			if(actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);
		}

		final Intent intent = getIntent();
		trip = (Trip) intent.getSerializableExtra("io.github.runassudo.ptoffline.pte.dto.Trip");
		ui = new TripAdapter.BaseTripHolder(findViewById(R.id.cardView), trip.legs.size());

		// retrieve trip data from intent that is not stored properly in trip object
		from = (Location) intent.getSerializableExtra("io.github.runassudo.ptoffline.pte.dto.Trip.from");
		to = (Location) intent.getSerializableExtra("io.github.runassudo.ptoffline.pte.dto.Trip.to");
		products = (ArrayList<Product>) intent.getSerializableExtra("io.github.runassudo.ptoffline.pte.dto.Trip.products");

		setHeader();
		setTrip(trip);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.trip_details, menu);
		mMenu = menu;

		TransportrUtils.fixToolbarIcon(this, menu.findItem(R.id.action_reload));
		TransportrUtils.fixToolbarIcon(this, menu.findItem(R.id.action_show_on_map));
		TransportrUtils.fixToolbarIcon(this, menu.findItem(R.id.action_share));
		TransportrUtils.fixToolbarIcon(this, menu.findItem(R.id.action_calendar));

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();

				return true;
			case R.id.action_reload:
				reload();

				return true;
			case R.id.action_show_on_map:
				TransportrUtils.showTripOnMap(this, trip);

				return true;
			case R.id.action_share:
				TransportrUtils.share(this, trip);

				return true;
			case R.id.action_calendar:
				TransportrUtils.intoCalendar(this, trip);

				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	@Override
	public void onTripRetrieved(QueryTripsResult result) {
		if(result.status == QueryTripsResult.Status.OK && result.trips != null) {
			Log.d(getClass().getSimpleName(), result.toString());

			Log.d("TEST", "OLD TRIP: " + trip.toString());
			for(Trip.Leg leg : trip.legs) {
				Log.d("TEST", "OLD TRIP: " + leg.toString());
			}
			Log.d("TEST", "OLD TRIP: ------------------------------------");

			for(Trip new_trip : result.trips) {
				Log.d("TEST", "NEW TRIP: " + new_trip.toString());
				for(Trip.Leg leg : new_trip.legs) {
					Log.d("TEST", "NEW TRIP: " + leg.toString());
				}

				if(isTheSameTrip(trip, new_trip)) {
					setTrip(new_trip);
					Log.d("TEST", "SAME TRIP!!!!!!");
					onReloadComplete();
					return;
				}

				Log.d("TEST", "NEW TRIP: ------------------------------------");
			}
		}
		Toast.makeText(this, R.string.error_trip_refresh_failed, Toast.LENGTH_LONG).show();
		onReloadComplete();
	}

	@Override
	public void onTripRetrievalError(String error) {
		onReloadComplete();
	}

	private void setHeader() {
		((TextView) findViewById(R.id.departureView)).setText(TransportrUtils.getLocName(trip.from));
		((TextView) findViewById(R.id.arrivalView)).setText(TransportrUtils.getLocName(trip.to));
		((TextView) findViewById(R.id.durationView)).setText(DateUtils.getDuration(trip.getDuration()));
		((TextView) findViewById(R.id.dateView)).setText(DateUtils.getDate(this, trip.getFirstDepartureTime()));
	}

	private void setTrip(Trip trip) {
		this.trip = trip;
		ViewCompat.setTransitionName(ui.card, trip.getId());

		int i = 0;
		for(final Trip.Leg leg : trip.legs) {
			TripAdapter.bindLeg(this, ui.legs.get(i), leg, true, showLineName);
			i += 1;
		}

		// hide last divider
		ui.legs.get(trip.legs.size() - 1).divider.setVisibility(View.GONE);
	}

	private void reload() {
		if(mMenu != null) {
			mMenu.findItem(R.id.action_reload).setActionView(R.layout.actionbar_progress_actionview);
		}
		AsyncQueryTripsTask task = new AsyncQueryTripsTask(this, this);

		// use a new date slightly earlier to avoid missing the right trip
		Date new_date = new Date();
		new_date.setTime(trip.getFirstDepartureTime().getTime() - 5000);

		task.setFrom(from);
		task.setTo(to);
		task.setDate(new_date);
		task.setDeparture(true);
		task.setProducts(new HashSet<>(products));

		task.execute();
	}

	private void onReloadComplete() {
		if(mMenu != null) {
			mMenu.findItem(R.id.action_reload).setActionView(null);
		}
	}

	private boolean isTheSameTrip(Trip old_trip, Trip new_trip) {
		if(old_trip == null || new_trip == null || new_trip.legs == null) return false;

		// check number of changes and legs
		if(old_trip.numChanges.equals(new_trip.numChanges) && old_trip.legs.size() == new_trip.legs.size()) {
			// check departure times
			if(old_trip.legs.get(0) instanceof Trip.Public && new_trip.legs.get(0) instanceof Trip.Public) {
				Trip.Public old_start = (Trip.Public) old_trip.legs.get(0);
				Trip.Public new_start = (Trip.Public) new_trip.legs.get(0);
				if(old_start.departureStop.plannedDepartureTime != null && old_start.departureStop.plannedDepartureTime.equals(new_start.departureStop.plannedDepartureTime)) {
					// basic line check
					if(old_start.line.label != null && new_start.line.label != null) {
						if(old_start.line.label.equals(new_start.line.label)) {
							return true;
						}
					} else {
						return true;
					}
				}
			}
			else if(old_trip.legs.get(0) instanceof Trip.Individual && new_trip.legs.get(0) instanceof Trip.Individual) {
				Trip.Individual old_start = (Trip.Individual) old_trip.legs.get(0);
				Trip.Individual new_start = (Trip.Individual) new_trip.legs.get(0);
				if(old_start.departureTime.equals(new_start.departureTime)) {
					return true;
				}
			}
		}

		return false;
	}

}
