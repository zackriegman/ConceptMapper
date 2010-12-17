package org.argmap.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/*
 * Simple class that keeps track of a number of time periods, merging them when they overlap, 
 * and providing a method to test whether a given date is within any of the time periods.
 */
public class TimePeriods {

	private static class Marker {
		public Marker(Date date, boolean start) {
			this.date = date;
			this.start = start;
		}

		public final Date date;
		public final boolean start;
	}

	private List<Marker> periods;

	public TimePeriods() {
		periods = new ArrayList<Marker>();
	}

	private TimePeriods(List<Marker> list) {
		periods = list;
	}

	public void addPeriod(Date start, Date end) {
		if (!start.before(end)) {
			throw new RuntimeException("start no before end");
		}

		int i;

		/* place the start marker if necessary */
		boolean looseStart = true;
		for (i = 0; i < periods.size(); i++) {
			Marker marker = periods.get(i);
			if (start.after(marker.date)) {
				/*
				 * do nothing and keep traveling down the line
				 */
				continue;
			} else if (start.before(marker.date)) {
				/*
				 * only need to lay down a new marker when we are not already in
				 * a marked period; i.e. when marker is a 'start'. If the marker
				 * is an 'end', that means we are in a marked period because we
				 * passed the 'start' marker but not its corresponding 'end'
				 * marker.
				 */
				if (marker.start) {
					periods.add(i, new Marker(start, true));
					/*
					 * increment the index so it refers to the same Marker it
					 * refered to before the add()
					 */
					i++;
				}
				looseStart = false;
				break;
			} else if (start.equals(marker.date)) {
				/*
				 * if the marker is an end, remove it (thereby merging previous
				 * period with the new period)
				 */
				if (!marker.start) {
					periods.remove(i);
				} else {
					/*
					 * we want i to refer to the next unexamined marker if we've
					 * removed an end marker (as above) then we haven't looked
					 * at the new marker that has slid into the end markers
					 * place, so we don't need to increment i. However if we
					 * leave an equal start marker we need to increment i, just
					 * as if we had added a marker as we do above with start is
					 * before a start marker.
					 */
					i++;
				}
				/*
				 * if it's not an end, there is already an equivalent start
				 * marker and we don't need to do anything
				 */

				/*
				 * in either case start is handled so set looseStart to false
				 * and break
				 */
				looseStart = false;
				break;
			}
		}

		/*
		 * this just handles the case where we've come to the end of the list
		 * and still haven't placed a start marker
		 */
		if (looseStart) {
			periods.add(i, new Marker(start, true));
			i++;
		}

		/* delete unneeded markers and place end marker if necessary */
		boolean looseEnd = true;
		while (i < periods.size()) {
			Marker marker = periods.get(i);
			if (end.before(marker.date)) {
				/*
				 * only need to lay down a new marker when we are not already in
				 * a marked period; i.e. when marker is a 'start'. If the marker
				 * is an 'end', that means we are in a marked period because we
				 * passed the 'start' marker but not its corresponding 'end'
				 * marker.
				 */
				if (marker.start) {
					periods.add(i, new Marker(end, false));
				}
				looseEnd = false;
				break;
			} else if (end.after(marker.date)) {
				periods.remove(i);
			} else if (end.equals(marker.date)) {
				/*
				 * if the equal marker is a start marker then we merge periods
				 * by deleting the marker
				 */
				if (marker.start) {
					periods.remove(i);
				}

				looseEnd = false;
				break;
			}
		}

		/*
		 * if we got to the end of the list without either placing an end marker
		 * or finding an end marker to merge with
		 */
		if (looseEnd) {
			periods.add(i, new Marker(end, false));
		}
	}

	/*
	 * to test a date for inclusion walk down line until date is before marker.
	 * If marker is start, that means the date is not within a period, because
	 * it comes after the previous end and before the marked start, therefore
	 * return false. If marker is end, that means date is within a period
	 * because it comes after the previous start and before the marked end,
	 * therefore return true.
	 */
	public boolean covers(Date date) {
		for (Marker marker : periods) {
			if (date.before(marker.date)) {
				if (marker.start) {
					return false;
				} else {
					return true;
				}
			}
		}
		return false;
	}

	/*
	 * makes a copy of this TimePeriods, by simply copying the periods list.
	 * Since markers are immutable (fields are final) a copy of the list is
	 * distinct and any changes to a TimePeriods will not affect its copy or
	 * vice versa.
	 */
	public TimePeriods copy() {
		return new TimePeriods(new ArrayList<Marker>(periods));
	}

	private static long YEAR_IN_MILLIS = 365 * 24 * 60 * 60 * 1000;

	public static Date THIRTY_YEARS_AGO = new Date(System.currentTimeMillis()
			- 30 * YEAR_IN_MILLIS);

	public static Date ONE_HUNDRED_YEARS_FROM_NOW = new Date(
			System.currentTimeMillis() + 100 * YEAR_IN_MILLIS);

	/******************************************************
	 * TEST METHODS (COMMENT THESE OUT WHEN NOT TESTING ) *
	 ******************************************************/

	// public void print() {
	// for (Marker marker : periods) {
	// System.out.println("marker -- date: " + marker.date + " "
	// + (marker.start ? "START" : "END"));
	// }
	// }
	//
	// public static void test(int testNumber, TimePeriods periods, Date
	// testDate,
	// boolean shouldBeInPeriod) {
	// if (periods.inPeriod(testDate)) {
	// if (shouldBeInPeriod) {
	// System.out.println(testNumber + ": pass");
	// } else {
	// System.out.println(testNumber + ": fail");
	// }
	// } else {
	// if (shouldBeInPeriod) {
	// System.out.println(testNumber + ": fail");
	// } else {
	// System.out.println(testNumber + ": pass");
	// }
	// }
	// }
	//
	// @SuppressWarnings("deprecation")
	// public static void main(String[] args) {
	// TimePeriods periods = new TimePeriods();
	// periods.addPeriod(new Date(80, 3, 2), new Date(80, 3, 5));
	// periods.addPeriod(new Date(80, 3, 7), new Date(80, 3, 9));
	// periods.print();
	//
	// test(1, periods, new Date(80, 3, 1), false);
	// test(2, periods, new Date(80, 3, 6), false);
	// test(3, periods, new Date(80, 3, 4), true);
	// test(4, periods, new Date(80, 3, 8), true);
	// test(5, periods, new Date(80, 3, 10), false);
	//
	// periods.addPeriod(new Date(80, 3, 2), new Date(80, 3, 9));
	// periods.print();
	//
	// test(6, periods, new Date(80, 3, 1), false);
	// test(7, periods, new Date(80, 3, 6), true);
	// test(8, periods, new Date(80, 3, 4), true);
	// test(9, periods, new Date(80, 3, 8), true);
	// test(10, periods, new Date(80, 3, 10), false);
	//
	// periods.addPeriod(new Date(80, 4, 2), new Date(80, 4, 9));
	// periods.addPeriod(new Date(80, 4, 4), new Date(80, 4, 7));
	// periods.print();
	//
	// test(11, periods, new Date(80, 4, 3), true);
	// test(12, periods, new Date(80, 4, 5), true);
	// test(13, periods, new Date(80, 4, 8), true);
	// test(14, periods, new Date(80, 4, 1), false);
	// test(15, periods, new Date(80, 4, 10), false);
	//
	// test(16, periods, new Date(80, 3, 1), false);
	// test(17, periods, new Date(80, 3, 6), true);
	// test(18, periods, new Date(80, 3, 4), true);
	// test(19, periods, new Date(80, 3, 8), true);
	//
	// periods.addPeriod(new Date(80, 5, 2), new Date(80, 5, 9));
	// periods.addPeriod(new Date(80, 5, 7), new Date(80, 5, 12));
	// periods.print();
	//
	// test(20, periods, new Date(80, 5, 1), false);
	// test(21, periods, new Date(80, 5, 3), true);
	// test(22, periods, new Date(80, 5, 8), true);
	// test(23, periods, new Date(80, 5, 11), true);
	// test(24, periods, new Date(80, 5, 13), false);
	//
	// test(25, periods, new Date(80, 4, 3), true);
	// test(26, periods, new Date(80, 4, 5), true);
	// test(27, periods, new Date(80, 4, 8), true);
	// test(28, periods, new Date(80, 4, 1), false);
	// test(29, periods, new Date(80, 4, 10), false);
	//
	// test(30, periods, new Date(80, 3, 1), false);
	// test(31, periods, new Date(80, 3, 6), true);
	// test(32, periods, new Date(80, 3, 4), true);
	// test(33, periods, new Date(80, 3, 8), true);
	//
	// periods.addPeriod(new Date(70, 6, 2), new Date(70, 6, 9));
	// periods.addPeriod(new Date(70, 6, 20), new Date(70, 6, 28));
	// periods.addPeriod(new Date(70, 6, 7), new Date(70, 6, 22));
	// periods.print();
	//
	// test(34, periods, new Date(70, 6, 1), false);
	// test(35, periods, new Date(70, 6, 3), true);
	// test(36, periods, new Date(70, 6, 21), true);
	// test(37, periods, new Date(70, 6, 15), true);
	// test(38, periods, new Date(70, 6, 8), true);
	// test(39, periods, new Date(70, 6, 21), true);
	// test(40, periods, new Date(70, 6, 29), false);
	//
	// test(44, periods, new Date(80, 5, 1), false);
	// test(45, periods, new Date(80, 5, 3), true);
	// test(46, periods, new Date(80, 5, 8), true);
	// test(47, periods, new Date(80, 5, 11), true);
	// test(48, periods, new Date(80, 5, 13), false);
	//
	// test(49, periods, new Date(80, 4, 3), true);
	// test(50, periods, new Date(80, 4, 5), true);
	// test(51, periods, new Date(80, 4, 8), true);
	// test(52, periods, new Date(80, 4, 1), false);
	// test(53, periods, new Date(80, 4, 10), false);
	//
	// test(54, periods, new Date(80, 3, 1), false);
	// test(55, periods, new Date(80, 3, 6), true);
	// test(56, periods, new Date(80, 3, 4), true);
	// test(57, periods, new Date(80, 3, 8), true);
	//
	// periods.addPeriod(new Date(71, 6, 2), new Date(71, 6, 9));
	// periods.addPeriod(new Date(71, 6, 9), new Date(71, 6, 28));
	// periods.print();
	//
	// test(58, periods, new Date(71, 6, 1), false);
	// test(59, periods, new Date(71, 6, 3), true);
	// test(60, periods, new Date(71, 6, 9), true);
	// test(61, periods, new Date(71, 6, 11), true);
	// test(62, periods, new Date(71, 6, 29), false);
	//
	// test(63, periods, new Date(70, 6, 1), false);
	// test(64, periods, new Date(70, 6, 3), true);
	// test(65, periods, new Date(70, 6, 21), true);
	// test(66, periods, new Date(70, 6, 15), true);
	// test(67, periods, new Date(70, 6, 8), true);
	// test(68, periods, new Date(70, 6, 21), true);
	// test(69, periods, new Date(70, 6, 29), false);
	//
	// test(70, periods, new Date(80, 5, 1), false);
	// test(71, periods, new Date(80, 5, 3), true);
	// test(72, periods, new Date(80, 5, 8), true);
	// test(73, periods, new Date(80, 5, 11), true);
	// test(74, periods, new Date(80, 5, 13), false);
	//
	// test(75, periods, new Date(80, 4, 3), true);
	// test(76, periods, new Date(80, 4, 5), true);
	// test(77, periods, new Date(80, 4, 8), true);
	// test(78, periods, new Date(80, 4, 1), false);
	// test(79, periods, new Date(80, 4, 10), false);
	//
	// test(80, periods, new Date(80, 3, 1), false);
	// test(81, periods, new Date(80, 3, 6), true);
	// test(82, periods, new Date(80, 3, 4), true);
	// test(83, periods, new Date(80, 3, 8), true);
	//
	// }
}