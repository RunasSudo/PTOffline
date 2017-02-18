/*
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

import android.content.Context;
import android.support.annotation.StringRes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.grobox.liberario.pte.NetworkId;

public class TransportNetworks {

	private List<TransportNetwork> networks;
	private HashMap<String, TransportNetwork> networks_by_id;
	private HashMap<String, List<TransportNetwork>> networks_by_region;
	private Context context;

	TransportNetworks(Context context) {
		this.context = context;
		this.networks = populateNetworks();
	}

	private List<TransportNetwork> populateNetworks() {
		List<TransportNetwork> list = new ArrayList<>();
		String region;
		// Australia
		region = region(R.string.np_region_australia, "\uD83C\uDDE6\uD83C\uDDFA");

		list.add(new TransportNetwork(context, NetworkId.PTV)
				.setName(getString(R.string.np_name_ptv))
				.setDescription(getString(R.string.np_desc_ptv))
				.setRegion(region)
		);

		return list;
	}

	public List<TransportNetwork> getList() {
		return networks;
	}

	private HashMap<String, TransportNetwork> getHashMapByStringId() {
		if(networks_by_id == null) {
			networks_by_id = new HashMap<>();

			for(final TransportNetwork network : networks) {
				networks_by_id.put(network.getIdString(), network);
			}
		}
		return networks_by_id;
	}

	public HashMap<String, List<TransportNetwork>> getHashMapByRegion() {
		if(networks_by_region == null) {
			networks_by_region = new HashMap<>();

			for(final TransportNetwork network : networks) {
				if(networks_by_region.containsKey(network.getRegion())) {
					networks_by_region.get(network.getRegion()).add(network);
				} else {
					List<TransportNetwork> list = new ArrayList<>(1);
					list.add(network);

					networks_by_region.put(network.getRegion(), list);
				}
			}
		}
		return networks_by_region;
	}

	TransportNetwork getTransportNetwork(String idString) {
		return getHashMapByStringId().get(idString);
	}

	private String getString(int res) {
		return context.getString(res);
	}

	private String region(@StringRes int name, String flag) {
		return getString(name) + " " + flag;
	}
}
