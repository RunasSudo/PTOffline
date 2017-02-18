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

package de.grobox.liberario.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import de.grobox.liberario.FavLocation;
import de.grobox.liberario.Preferences;
import de.grobox.liberario.R;
import de.grobox.liberario.RecentTrip;
import de.grobox.liberario.WrapLocation;
import de.grobox.liberario.adapters.LocationAdapter;
import de.grobox.liberario.data.RecentsDB;
import de.grobox.liberario.tasks.AsyncQueryTripsTask;
import de.grobox.liberario.pte.dto.Location;
import de.grobox.liberario.pte.dto.Product;
import de.grobox.liberario.pte.dto.QueryTripsResult;

public class AmbiguousLocationActivity extends TransportrActivity implements AsyncQueryTripsTask.TripHandler {
	private Location from, via, to;
	private Date date;
	private Boolean departure;
	private ArrayList<Product> products;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_ambiguous_location);

		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		if(toolbar != null) {
			toolbar.setSubtitle(Preferences.getTransportNetwork(this).getName());
			setSupportActionBar(toolbar);

			ActionBar actionBar = getSupportActionBar();
			if(actionBar != null) actionBar.setDisplayHomeAsUpEnabled(true);
		}

		Intent intent = getIntent();
		QueryTripsResult trips = (QueryTripsResult) intent.getSerializableExtra("de.grobox.liberario.pte.dto.QueryTripsResult");
		from = (Location) intent.getSerializableExtra("de.grobox.liberario.pte.dto.Trip.from");
		via = (Location) intent.getSerializableExtra("de.grobox.liberario.pte.dto.Trip.via");
		to = (Location) intent.getSerializableExtra("de.grobox.liberario.pte.dto.Trip.to");
		date = (Date) intent.getSerializableExtra("de.grobox.liberario.pte.dto.Trip.date");
		departure = intent.getBooleanExtra("de.grobox.liberario.pte.dto.Trip.departure", true);
		products = (ArrayList<Product>) intent.getSerializableExtra("de.grobox.liberario.pte.dto.Trip.products");

		final ViewHolder ui = new ViewHolder(findViewById(R.id.layout));

		// From
		if(trips.ambiguousFrom != null) {
			LocationAdapter loca = new LocationAdapter(this, trips.ambiguousFrom);
			loca.setSort(FavLocation.LOC_TYPE.FROM);
			ui.fromSpinner.setAdapter(loca);
			if(trips.ambiguousFrom.size() == 1) ui.fromSpinner.setEnabled(false);
		}
		else {
			List<Location> list = new ArrayList<>();
			list.add(from);
			LocationAdapter loca = new LocationAdapter(this, list);
			loca.setSort(FavLocation.LOC_TYPE.FROM);
			ui.fromSpinner.setAdapter(loca);
			ui.fromSpinner.setEnabled(false);
		}

		// Via
		if(via == null) {
			ui.viaText.setVisibility(View.GONE);
			ui.viaSpinner.setVisibility(View.GONE);
		} else if (trips.ambiguousVia != null) {
			LocationAdapter loca = new LocationAdapter(this, trips.ambiguousVia);
			loca.setSort(FavLocation.LOC_TYPE.VIA);
			ui.viaSpinner.setAdapter(loca);
			if(trips.ambiguousVia.size() == 1) ui.viaSpinner.setEnabled(false);
		} else {
			List<Location> list = new ArrayList<>();
			list.add(via);
			LocationAdapter loca = new LocationAdapter(this, list);
			loca.setSort(FavLocation.LOC_TYPE.VIA);
			ui.viaSpinner.setAdapter(loca);
			ui.viaSpinner.setEnabled(false);
		}

		// To
		if(trips.ambiguousTo != null) {
			LocationAdapter loca = new LocationAdapter(this, trips.ambiguousTo);
			loca.setSort(FavLocation.LOC_TYPE.TO);
			ui.toSpinner.setAdapter(loca);
			if(trips.ambiguousTo.size() == 1) ui.toSpinner.setEnabled(false);
		}
		else {
			List<Location> list = new ArrayList<>();
			list.add(to);
			LocationAdapter loca = new LocationAdapter(this, list);
			loca.setSort(FavLocation.LOC_TYPE.TO);
			ui.toSpinner.setAdapter(loca);
			ui.toSpinner.setEnabled(false);
		}

		// Search Button
		ui.button.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				from = ((WrapLocation) ui.fromSpinner.getSelectedItem()).getLocation();
				if(ui.viaSpinner.getSelectedItem() != null) via = ((WrapLocation) ui.viaSpinner.getSelectedItem()).getLocation();
				to = ((WrapLocation) ui.toSpinner.getSelectedItem()).getLocation();

				// remember location and trip
				RecentsDB.updateFavLocation(getApplicationContext(), from, FavLocation.LOC_TYPE.FROM);
				if(via != null) RecentsDB.updateFavLocation(getApplicationContext(), via, FavLocation.LOC_TYPE.VIA);
				RecentsDB.updateFavLocation(getApplicationContext(), to, FavLocation.LOC_TYPE.TO);
				RecentsDB.updateRecentTrip(getApplicationContext(), new RecentTrip(from, via, to));

				AsyncQueryTripsTask query_trips = new AsyncQueryTripsTask(v.getContext(), AmbiguousLocationActivity.this);

				query_trips.setFrom(from);
				query_trips.setVia(via);
				query_trips.setTo(to);
				query_trips.setDate(date);
				query_trips.setDeparture(departure);
				query_trips.setProducts(new HashSet<>(products));

				query_trips.execute();
			}
		});
	}

	@Override
	public void onTripRetrieved(QueryTripsResult result) {
		if(result.status == QueryTripsResult.Status.OK && result.trips != null && result.trips.size() > 0) {
			Log.d(getClass().getSimpleName(), result.toString());

			Intent intent = new Intent(this, TripsActivity.class);
			intent.putExtra("de.grobox.liberario.pte.dto.QueryTripsResult", result);
			fillIntent(intent);
			startActivity(intent);
		}
		else if(result.status == QueryTripsResult.Status.AMBIGUOUS) {
			Log.d(getClass().getSimpleName(), "QueryTripsResult is AMBIGUOUS");

			Intent intent = new Intent(this, AmbiguousLocationActivity.class);
			intent.putExtra("de.grobox.liberario.pte.dto.QueryTripsResult", result);
			fillIntent(intent);
			startActivity(intent);
		}
		else {
			Toast.makeText(this, getResources().getString(R.string.error_no_trips_found), Toast.LENGTH_LONG).show();
		}
	}

	@Override
	public void onTripRetrievalError(String error) { }

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
		switch (item.getItemId()) {
			case android.R.id.home:
				onBackPressed();

				return true;
			default:
				return super.onOptionsItemSelected(item);
		}
	}

	private void fillIntent(Intent intent) {
		intent.putExtra("de.grobox.liberario.pte.dto.Trip.from", from);
		intent.putExtra("de.grobox.liberario.pte.dto.Trip.via", via);
		intent.putExtra("de.grobox.liberario.pte.dto.Trip.to", to);
		intent.putExtra("de.grobox.liberario.pte.dto.Trip.date", date);
		intent.putExtra("de.grobox.liberario.pte.dto.Trip.departure", departure);
		intent.putExtra("de.grobox.liberario.pte.dto.Trip.products", products);
	}

	private static class ViewHolder {
		Spinner fromSpinner;
		View viaText;
		Spinner viaSpinner;
		Spinner toSpinner;
		Button button;

		ViewHolder(View v) {
			fromSpinner = (Spinner) v.findViewById(R.id.fromSpinner);
			viaText = v.findViewById(R.id.viaView);
			viaSpinner = (Spinner) v.findViewById(R.id.viaSpinner);
			toSpinner = (Spinner) v.findViewById(R.id.toSpinner);
			button = (Button) v.findViewById(R.id.button);
		}
	}

}
