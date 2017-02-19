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

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.util.Locale;

import io.github.runassudo.ptoffline.Preferences;
import io.github.runassudo.ptoffline.R;

public class TransportrActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		useLanguage(this);

		// Use current theme
		if(Preferences.darkThemeEnabled(this)) {
			setTheme(R.style.AppTheme);
		} else {
			setTheme(R.style.AppTheme_Light);
		}

		super.onCreate(savedInstanceState);
	}

	public static void useLanguage(Context context) {
		String lang = Preferences.getLanguage(context);
		if(!lang.equals(context.getString(R.string.pref_language_value_default))) {
			Locale locale;
			if(lang.contains("_")) {
				String[] lang_array = lang.split("_");
				locale = new Locale(lang_array[0], lang_array[1]);
			} else {
				locale = new Locale(lang);
			}
			Locale.setDefault(locale);
			Configuration config = context.getResources().getConfiguration();
			config.locale = locale;
			context.getResources().updateConfiguration(config, context.getResources().getDisplayMetrics());
		} else {
			// use default language
			context.getResources().updateConfiguration(Resources.getSystem().getConfiguration(), context.getResources().getDisplayMetrics());
		}
	}

	public void runOnThread(final Runnable task) {
		new Thread(task).start();
	}

}
