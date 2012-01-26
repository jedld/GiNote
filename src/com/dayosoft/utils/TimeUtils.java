package com.dayosoft.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public final class TimeUtils {

	public static boolean isSameDay(Date value1, Date value2) {
		SimpleDateFormat dateformat = new SimpleDateFormat("MMddyyyy");
		return (dateformat.format(value1).equalsIgnoreCase(dateformat
				.format(value2)));
	}

	public static boolean isToday(Date value) {
		SimpleDateFormat dateformat = new SimpleDateFormat("MMddyyyy");
		String datestr = dateformat.format(value);
		String today = dateformat.format(new Date());
		return (datestr.equalsIgnoreCase(today));
	}

	public static boolean isYesterday(Date value) {
		SimpleDateFormat dateformat = new SimpleDateFormat("MMddyyyy");
		String datestr = dateformat.format(value);
		Calendar cal1 = Calendar.getInstance();
		cal1.add(Calendar.DATE, -1);
		String yesterday = dateformat.format(cal1.getTime());
		return (datestr.equalsIgnoreCase(yesterday));
	}

	
	public static String computeRelativeTimeString(Date value) {
		SimpleDateFormat dateformat = new SimpleDateFormat("hh:mma MM-dd-yyyy");

		long seconds = (TimeUnit.MILLISECONDS.toSeconds(System
				.currentTimeMillis() - value.getTime()));
		long minutes = seconds / 60;
		long hours = minutes / 60;

		String timestr = "";
		if (seconds < 60) {
			timestr = seconds + "s ago";
		} else if ((minutes >= 1) && (minutes < 60)) {
			timestr = minutes + "min ago";
		} else if ((hours >= 1 && (hours < 24))) {
			timestr = hours + "h " + (minutes % 60) + "min ago ";
		} else {
			Calendar cal1 = Calendar.getInstance();
			cal1.add(Calendar.DATE, -1); // For tomorrow
			cal1.set(Calendar.HOUR_OF_DAY, 24);
			cal1.set(Calendar.MINUTE, 0);

			Calendar cal2 = Calendar.getInstance();
			cal2.add(Calendar.DATE, -2); // For tomorrow
			cal2.set(Calendar.HOUR_OF_DAY, 24);
			cal2.set(Calendar.MINUTE, 0);

			if (cal2.before(value) && cal1.after(value)) {
				timestr = "yesterday";
			}
		}
		if (!timestr.equals("")) {
			return dateformat.format(value) + " (" + timestr + ")";
		} else {
			return dateformat.format(value);
		}
	}

}
