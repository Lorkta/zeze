package Log4jQuery;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Collection;

public class Log4jLog {
	private final long time;
	private volatile String log;
	private final StringBuilder lines = new StringBuilder();

	public Log4jLog(long time, String line) {
		this.time = time;
		this.lines.append(line);
	}

	public void addLine(String line) {
		this.lines.append("\n").append(line);
	}

	public long getTime() {
		return time;
	}

	public String getLog() {
		var tmp = log;
		if (null != tmp)
			return tmp;

		synchronized (this) {
			log = lines.toString();
			return log;
		}
	}

	public boolean containsAll(Collection<String> words) {
		var log = getLog();
		for (var word : words) {
			if (!log.contains(word))
				return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return getLog();
	}

	public static volatile String LogTimeFormat = "yy-MM-dd HH:mm:ss.SSS";

	public static long parseTime(String strTime) {
		var parsePosition = new ParsePosition(0);
		var simpleDateFormat = new SimpleDateFormat(LogTimeFormat);
		var date = simpleDateFormat.parse(strTime, parsePosition);
		if (null == date || parsePosition.getErrorIndex() >= 0)
			throw new IllegalArgumentException("invalid time format '" + strTime + "' at " + parsePosition.getErrorIndex());
		return date.getTime();
	}

	public static Log4jLog tryParse(String line) {
		var dayOffset = line.indexOf(' ');
		if (dayOffset > 0) {
			var timeOffset = line.indexOf(' ', dayOffset + 1);
			if (timeOffset > 0) {
				var strTime = line.substring(0, timeOffset);
				var parsePosition = new ParsePosition(0);
				var simpleDateFormat = new SimpleDateFormat(LogTimeFormat);
				var date = simpleDateFormat.parse(strTime, parsePosition);
				if (null != date && parsePosition.getErrorIndex() == -1) {
					return new Log4jLog(date.getTime(), line);
				}
			}
		}
		return null;
	}
}
