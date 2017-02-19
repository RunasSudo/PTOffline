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
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import io.github.runassudo.ptoffline.R;
import io.github.runassudo.ptoffline.utils.TransportrUtils;
import io.github.runassudo.ptoffline.pte.dto.Line;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class LineView extends LinearLayout {

	private final ViewHolder ui;

	public LineView(Context context, AttributeSet attr) {
		super(context, attr);

		setOrientation(HORIZONTAL);

		//noinspection deprecation
		setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.line_box));

		int height = context.getResources().getDimensionPixelSize(R.dimen.line_box_height);
		setMinimumHeight(height);

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
		inflater.inflate(R.layout.line_view, this, true);
		ui = new ViewHolder(this);
	}

	public LineView(Context context) {
		this(context, null);
	}

	public void setLine(Line line) {
		if (line.product != null) {
			Drawable drawable = ContextCompat.getDrawable(getContext(), TransportrUtils.getDrawableForProduct(line.product));
			ui.product.setImageDrawable(drawable);
		} else {
			ui.product.setVisibility(GONE);
		}

		ui.label.setText(line.label);

		if (line.style != null) {
			GradientDrawable box = (GradientDrawable) ui.box.getBackground();

			if(box != null) {
				// change color and mutate before to not share state with other instances
				box.mutate();
				box.setColor(line.style.backgroundColor);
			}
			ui.label.setTextColor(line.style.foregroundColor);
			ui.product.setColorFilter(line.style.foregroundColor);
		}
	}

	public String getLabel() {
		return (String) ui.label.getText();
	}

	private static class ViewHolder {
		public ViewGroup box;
		public ImageView product;
		public TextView label;

		public ViewHolder(View view) {
			box = (ViewGroup) view;
			product = (ImageView) view.findViewById(R.id.productView);
			label = (TextView) view.findViewById(R.id.labelView);
		}
	}

}