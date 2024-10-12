package net.lycorissystem.modpack_updater.utils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class IOUtil {
	
	/**
	 * 从给定URL下载文件
	 * @param url
	 * @param to
	 * @param progressCallback
	 * @param bufferSize
	 * @throws IOException
	 */
	public static void downloadFile(String url, OutputStream to, BiConsumer<Integer, Integer> progressCallback, int bufferSize) throws IOException {
		URL downloadUrl = new URL(url);
		HttpURLConnection connection = (HttpURLConnection) downloadUrl.openConnection();
		
		LoggingUtils.logger.finest(String.format("正在从 %s 下载到 %s， 缓冲区大小 %d", url, to.toString(), bufferSize));
		
		// Get the size of the file for progress tracking
		int fileSize = connection.getContentLength();
		
		try (InputStream inputStream = connection.getInputStream()) {
			byte[] buffer = new byte[bufferSize];
			int bytesRead;
			int totalBytesRead = 0;
			
			// Read from the input stream into the buffer and write to output stream
			while ((bytesRead = inputStream.read(buffer, 0, bufferSize)) != -1) {
				
				to.write(buffer, 0, bytesRead);
				totalBytesRead += bytesRead;
				
				progressCallback.accept(totalBytesRead, fileSize > 0 ? fileSize : -1);
			}
			LoggingUtils.logger.finest(String.format("从 %s 下载了 %d 字节", url, totalBytesRead));
		} finally {
			connection.disconnect();
		}
	}
	
	/**
	 * 从给定（指向txt文件的）URL下载一段文本
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static String downloadText(String url) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			downloadFile(url, baos, (downloaded, total) -> {}, 4096);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		baos.close();
		return baos.toString(StandardCharsets.UTF_8.name());
	}
	
}
