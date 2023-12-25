package Zeze.Services.Log4jQuery;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import Zeze.Util.KV;
import Zeze.Util.OutLong;
import Zeze.Util.Random;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import Zeze.Util.OutInt;
import java.util.concurrent.Future;

/**
 * 日志文件集合，能搜索当前存在的所有日志。
 * 静态管理所有Log4jFile。Index。监控rotate。
 */
public class Log4jFileManager {
	public static class Log4jFile {
		public volatile File file;
		public volatile LogIndex index;

		public Log4jFile(File file, LogIndex index) {
			this.file = file;
			this.index = index;
		}

		public static Log4jFile of(File file, LogIndex index) {
			return new Log4jFile(file, index);
		}
	}

	private final ArrayList<Log4jFile> files = new ArrayList<>();
	private static final Logger logger = LogManager.getLogger();
	private final FileCreateDetector fileCreateDetector;
	private final String logFileBegin;
	private final String logFileEnd;
	private final String logDir;
	private final String logDatePattern;
	private final String charsetName;
	private final Future<?> buildIndexTimer;

	public Log4jFileManager(@NotNull String logActive,
							@NotNull String logDir,
							@NotNull String datePattern,
							@NotNull String charsetName) throws Exception {
		var fulls = logActive.split("\\.");
		this.logFileBegin = fulls[0];
		this.logFileEnd = fulls.length > 1 ? fulls[1] : "";
		this.logDir = logDir;
		this.logDatePattern = datePattern;
		this.charsetName = charsetName;

		this.fileCreateDetector = new FileCreateDetector(logDir, this::onFileCreated);

		loadRotates(logDir);
		var active = new File(logDir, logActive);
		if (active.exists()) {
			// 警告，如果启动的瞬间发生了log4j rotate，由于原子性没有保证，可能会创建多余的Log4jFile，
			// 搜索的时候忽略文件不存在的错误？
			// 暂时先不处理！
			files.add(Log4jFile.of(active, loadIndex(active,logActive + ".index")));
		}
		var period = 300_000L;
		buildIndexTimer = Task.scheduleUnsafe(Random.getInstance().nextLong(period), period, this::buildIndex);
		removeOldLinkFiles();
	}

	public Log4jFileSession seek(long time, OutInt out) throws IOException {
		for (var i = files.size() - 1; i >= 0; --i) {
			var file = files.get(i);
			if (time >= file.index.getBeginTime()) {
				out.value = i;
				var logFileSession = new Log4jFileSession(file.file, file.index, charsetName);
				logFileSession.seek(time);
				return logFileSession;
			}
		}
		return null;
	}

	public String getCurrentLogFileName() {
		return logFileBegin + "." + logFileEnd;
	}

	public String getCurrentIndexFileName() {
		return getCurrentLogFileName() + ".index";
	}

	public String getLogDir() {
		return logDir;
	}

	public int testFileName(String fileName, OutLong out) {
		if (fileName.equals(getCurrentLogFileName()))
			return 0; // 当前日志文件

		if (fileName.startsWith(logFileBegin) && fileName.endsWith(logFileEnd)) {
			// rotate log file name = logFileBegin + logDatePattern + '.' + logFileEnd;
			// logDatePattern默认是 .yyyy-MM-dd
			var datePatternPart = fileName.substring(logFileBegin.length(), fileName.length() - logFileEnd.length() - 1);
			var formatter = new SimpleDateFormat(logDatePattern);
			try {
				var date = formatter.parse(datePatternPart);
				if (null != out)
					out.value = date.getTime();
				return 1; // 是rotate出来的日志文件。
			} catch (ParseException e) {
				// skip and continue
			}
		}
		return -1; // 其他。
	}

	private synchronized void onFileCreated(Path path) {
		try {
			var fileName = path.toFile().getName();
			var type = testFileName(fileName, null);
			switch (type) {
			case 0: // current log file created
				var currentLogFileName = getCurrentLogFileName();
				if (fileName.equals(currentLogFileName)
					&& (files.isEmpty() || !files.get(files.size() - 1).file.getName().equals(currentLogFileName))) {
					var logFile = new File(logDir, fileName);
					files.add(Log4jFile.of(logFile, loadIndex(logFile, getCurrentIndexFileName())));
				}
				break;

			case 1: // rotate target
				if (files.isEmpty())
					return;

				var last = files.get(files.size() - 1);
				if (last.file.getName().equals(getCurrentLogFileName())) {
					// rename index file
					var indexFile = Path.of(logDir, getCurrentIndexFileName()).toFile();
					if (indexFile.exists()) {
						if (!indexFile.renameTo(new File(logDir, fileName + ".index")))
							logger.error("rename error. " + indexFile);
					}
					// 修改file指向新的logFile。index保持不变。
					last.file = new File(logDir, fileName);
				}
				break;
			}
		} catch (Exception ex) {
			logger.error("", ex);
		}
	}

	public void stop() {
		buildIndexTimer.cancel(false);
		fileCreateDetector.stopAndJoin();
	}

	public boolean isEmpty() {
		return files.isEmpty();
	}

	public int size() {
		return files.size();
	}

	public Log4jFileSession get(int index) throws IOException {
		var file = files.get(index);
		return new Log4jFileSession(file.file, file.index, charsetName);
	}

	private void loadRotates(String logRotateDir) throws Exception {
		var listFiles = new File(logRotateDir).listFiles();
		var rotates = new ArrayList<KV<Long, String>>();
		if (null != listFiles) {
			for (var file : listFiles) {
				if (file.isFile()) {
					var date = new OutLong();
					if (1 == testFileName(file.getName(), date))
						rotates.add(KV.create(date.value, file.getName()));
				}
			}
			rotates.sort(Comparator.comparingLong(KV::getKey));
			for (var kv : rotates) {
				var logFile = new File(logDir, kv.getValue());
				this.files.add(Log4jFile.of(logFile, loadIndex(logFile, kv.getValue() + ".index")));
			}
		}
	}

	private void removeOldLinkFiles() {
		var linkDir = new File(logDir, "indexLinks");
		var links = linkDir.listFiles();
		var max = 0L;
		File maxFile = null;
		if (null != links) {
			for (var link : links) {
				if (link.isDirectory())
					continue;
				var linkName = link.getName();
				var linkValue = Long.parseLong(linkName);
				if (linkValue > max) {
					max = linkValue;
					maxFile = link;
				}
			}

			for (var link : links) {
				if (link != maxFile) {
					if (!link.delete())
						logger.warn("delete link error: " + link);
				}
			}
		}
	}

	private File nextLinkFile() throws IOException {
		var linkDir = new File(logDir, "indexLinks");
		Files.createDirectories(linkDir.toPath());
		var links = linkDir.listFiles();
		var max = 0L;
		if (null != links) {
			for (var link : links) {
				if (link.isDirectory())
					continue;
				var linkName = link.getName();
				var linkValue = Long.parseLong(linkName);
				if (linkValue > max)
					max = linkValue;
			}
		}
		return new File(linkDir, String.valueOf(max + 1));
	}

	private LogIndex loadIndex(File logFile, String logIndexFileName) throws Exception {
		var indexFile = new File(logDir, logIndexFileName);
		var linkFile = nextLinkFile();
		var linkPath = Files.createLink(linkFile.toPath(), indexFile.toPath());
		var index = new LogIndex(linkPath.toFile());
		return loadIndex(logFile, index);
	}

	private LogIndex loadIndex(File logFile, LogIndex index) throws Exception {
		// 索引没有建立完成的需要继续
		try (var log = new Log4jFileSession(logFile, null, charsetName)) {
			var indexes = new ArrayList<LogIndex.Record>();
			var lastIndexTime = index.getEndTime();
			var offset = index.lowerBound(lastIndexTime);
			log.seek(offset, lastIndexTime);
			while (log.hasNext()) {
				var next = log.next();
				if (next.getTime() - lastIndexTime >= 10_000) {
					// 每10s建立一条索引。
					indexes.add(LogIndex.Record.of(next.getTime(), next.getOffset()));
					if (indexes.size() >= 100) {
						index.addIndex(indexes);
						lastIndexTime = indexes.get(indexes.size() - 1).time;
						indexes.clear();
					}
				}
			}
			if (!indexes.isEmpty())
				index.addIndex(indexes);
		}
		return index;
	}

	private synchronized void buildIndex() {
		try {
			if (files.isEmpty())
				return;

			var last = files.get(files.size() - 1);
			if (!last.file.getName().equals(getCurrentLogFileName()))
				return;

			loadIndex(last.file, last.index);
		} catch (Exception ex) {
			logger.error("", ex);
		}
	}
}
