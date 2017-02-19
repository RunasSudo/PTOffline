/*
 * Copyright 2013, 2017 the original author or authors.
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

package io.github.runassudo.ptoffline;

import java.lang.ref.Reference;
import java.lang.ref.SoftReference;

import io.github.runassudo.ptoffline.pte.*;

/**
 * @author Andreas Schildbach
 * @author RunasSudo
 */
public final class NetworkProviderFactory
{
	private static Reference<PtvProvider> ptvProviderRef;

	public static synchronized NetworkProvider provider(final NetworkId networkId)
	{
		if (networkId.equals(NetworkId.PTV))
		{
			if(ptvProviderRef != null) {
				final PtvProvider provider = ptvProviderRef.get();
				if(provider != null)
					return provider;
			}

			final PtvProvider provider = new PtvProvider();
			ptvProviderRef = new SoftReference<>(provider);
			return provider;
		}
		else
		{
			throw new IllegalArgumentException(networkId.name());
		}
	}
}
