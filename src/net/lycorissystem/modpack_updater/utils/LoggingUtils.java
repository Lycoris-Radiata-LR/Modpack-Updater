package net.lycorissystem.modpack_updater.utils;

import java.io.IOException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.logging.*;

public class LoggingUtils {
	static Logger logger = null;
	static Level level = Level.INFO;
	
	public static void initLogger() {
		logger = Logger.getLogger("Modpack Updater");
		logger.setLevel(Level.FINEST);
		
		//控制台输出
		StreamHandler handler = new StreamHandler(System.out, new SimpleFormatter()){
			@Override
			public synchronized void publish(LogRecord record) {
				super.publish(record);
				flush();
			}
		};
		handler.setLevel(level);
		logger.addHandler(handler);
		
		//文件输出
		try {
			FileHandler fileHandler = new FileHandler("updater.log", false);
			fileHandler.setLevel(Level.FINEST);
			fileHandler.setFormatter(new XMLFormatter());
			logger.addHandler(fileHandler);
		} catch (IOException e) {
			logger.log(Level.WARNING, "无法创建日志文件 updater.log", e);
		}
	}
	
	public static Logger getLogger() {
		if (logger == null) {
			initLogger();
		}
		return logger;
	}
}