/*
 * J2ME Loader
 * Copyright (C) 2015-2016 Nickolay Savchenko
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

package ua.naiksoftware.j2meloader;

import java.util.Comparator;

public class AlphabeticComparator<T extends SortItem> implements Comparator<T> {

	public int compare(T p1, T p2) {
		return p1.getSortField().compareToIgnoreCase(p2.getSortField());
	}
}
