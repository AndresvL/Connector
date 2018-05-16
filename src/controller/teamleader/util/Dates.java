package controller.teamleader.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

/**
 * A Utility Class for formatting <tt>Unix Timestamps</tt> and
 * <tt>Date Strings</tt>.
 */
public abstract class Dates {
	
	/** Default format. */
	public static final DateFormat DATE_TIME_M = new SimpleDateFormat("dd-MM-yyyy HH:mm");
	
	public static final DateFormat DATE_TIME = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
	
	public static final DateFormat DATE = new SimpleDateFormat("dd-MM-yyyy");
	
	public static final DateFormat TIME = new SimpleDateFormat("HH:mm:ss");
	
	public static final DateFormat DATE_TIME_M_INV = new SimpleDateFormat("yyyy-MM-dd HH:mm");
	
	public static final DateFormat DATE_TIME_INV = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
	public static final DateFormat DATE_INV = new SimpleDateFormat("yyyy-MM-dd");
	
	/**
	 * Converts a <tt>Date String</tt> of the from format to a
	 * <tt>Date String</tt> of the to format.
	 *
	 * @param date
	 *            The <tt>Date String</tt> to convert.
	 * @param from
	 *            The format the <tt>Date String</tt> is currently in.
	 * @param to
	 *            The format the <tt>Date String</tt> has to be converted to.
	 * @return A <tt>Date String</tt> of the to format.
	 */
	public static String convert(String date, DateFormat from, DateFormat to) {
		try {
			return to.format(from.parse(date));
		} catch (ParseException e) {
			throw new RuntimeException(e);
		}
	}
	
	/**
	 * Converts a <tt>Date String</tt> of the default format to a
	 * <tt>Unix Timestamp</tt> (in seconds).
	 *
	 * @param date
	 *            The <tt>Date String</tt> to convert.
	 * @return A <tt>Unix Timestamp</tt> in seconds.
	 */
	public static long toTimestamp(String date) {
		return toTimestamp(date, DATE_TIME_M);
	}
	
	/**
	 * Converts a <tt>Date String</tt> of the provided format to a
	 * <tt>Unix Timestamp</tt> (in seconds).
	 *
	 * @param date
	 *            The <tt>Date String</tt> to convert.
	 * @param format
	 *            The format to use.
	 * @return A <tt>Unix Timestamps</tt> in seconds, or 0 if parsing failed.
	 */
	public static long toTimestamp(String date, DateFormat format) {
		try {
			return format.parse(date).toInstant().getEpochSecond();
		} catch (ParseException e) {
			return 0;
		}
	}
	
	/**
	 * Converts a <tt>Unix Timestamp</tt> (in seconds) to a <tt>Date String</tt>
	 * of the provided format.
	 *
	 * @param timestamp
	 *            The <tt>Unix Timestamp</tt> in seconds to convert.
	 * @return A <tt>Date String</tt> of the default format.
	 */
	public static String toDate(long timestamp) {
		return toDate(timestamp, DATE_TIME_M);
	}
	
	/**
	 * Converts a <tt>Unix Timestamp</tt> (in seconds) to a <tt>Date String</tt>
	 * of the provided format.
	 *
	 * @param timestamp
	 *            The <tt>Unix Timestamp</tt> in seconds to convert.
	 * @param format
	 *            The format to use.
	 * @return A <tt>Date String</tt> of the provided format.
	 */
	public static String toDate(long timestamp, DateFormat format) {
		return format.format(Date.from(Instant.ofEpochSecond(timestamp)));
	}
	
	// =======================================================================//
	
	/**
	 * Gets the current <tt>Unix Timestamp</tt> (in seconds).
	 *
	 * @return A <tt>Unix Timestamps</tt> in seconds.
	 */
	public static long getCurrentTimestamp() {
		return Instant.now().getEpochSecond();
	}
	
	/**
	 * Gets the current <tt>Date String</tt> of the default format.
	 *
	 * @return A <tt>Date String</tt> of the default format.
	 */
	public static String getCurrentDate() {
		return DATE_TIME_M.format(Date.from(Instant.now()));
	}
	
	/**
	 * Gets the current <tt>Date String</tt> of the provided format.
	 *
	 * @param format
	 *            The format to use.
	 * @return A <tt>Date String</tt> of the provided format.
	 */
	public static String getCurrentDate(DateFormat format) {
		return format.format(Date.from(Instant.now()));
	}
	
	// =======================================================================//
	
	/**
	 * Rounds seconds to the nearest minute indicated by the rounding parameter.
	 *
	 * @param seconds
	 *            Seconds to round.
	 * @param rounding
	 *            The amount of minutes to round to.
	 * @return Seconds that have been rounded.
	 */
	public static long roundS(long seconds, int rounding) {
		long minutes = seconds / 60;
		long offset = minutes % rounding;
		return (minutes - ((offset / 2D) < rounding ? offset : offset - rounding)) * 60;
	}
	
	/**
	 * Rounds minutes to the nearest minute indicated by the rounding parameter.
	 *
	 * @param minutes
	 *            Minutes to round.
	 * @param rounding
	 *            The amount of minutes to round to.
	 * @return Minutes that have been rounded.
	 */
	public static long roundM(long minutes, int rounding) {
		long offset = minutes % rounding;
		return minutes - ((offset / 2D) < rounding ? offset : offset - rounding);
	}
}