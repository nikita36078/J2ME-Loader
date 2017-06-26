package ua.naiksoftware.j2meloader;

import java.util.Comparator;

/*
 * @author Naik
 */
public class AlphabeticComparator<T extends SortItem> implements Comparator<T> {

	public int compare(T p1, T p2) {
		return p1.getSortField().compareToIgnoreCase(p2.getSortField());
	}
}
