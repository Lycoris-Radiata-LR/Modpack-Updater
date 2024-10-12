package net.lycorissystem.modpack_updater.file_io;

import net.lycorissystem.modpack_updater.utils.UpdaterFileUtils;
import net.lycorissystem.modpack_updater.utils.IOUtil;
import net.lycorissystem.modpack_updater.utils.LoggingUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.function.Consumer;
import java.util.logging.Level;

/**
 * 用于下载文件的线程
 */
public class DownloaderThread implements Runnable{
	
	
	String url;
	String checksumURL;
	File downloadFile;
	Consumer<DownloadProgressInfo> updateProgressCallback;
	Consumer<Boolean> doneCallback;
	
	/**
	 * 创建一个下载线程
	 * @param URL 下载地址
	 * @param checksumURL MD5地址（应为文本文件）
	 * @param downloadFile 下载到的文件
	 * @param updateProgressCallback 进度更新回调
	 * @param doneCallback 完成回调
	 */
	public DownloaderThread(String URL, String checksumURL, File downloadFile, Consumer<DownloadProgressInfo> updateProgressCallback, Consumer<Boolean> doneCallback) {
		this.url = URL;
		this.checksumURL = checksumURL;
		this.downloadFile = downloadFile;
		this.updateProgressCallback = updateProgressCallback;
		this.doneCallback = doneCallback;
	}
	
	@Override
	public void run() {
		try {
			if(download(4096)) {
				if(checksumURL == null || verifyChecksum()) {
					doneCallback.accept(true);
				}else {
					doneCallback.accept(false);
				}
			}else {
				doneCallback.accept(false);
			}
		} catch (IOException e) {
			LoggingUtils.getLogger().log(Level.SEVERE, "下载线程出错", e);
			doneCallback.accept(false);
		}
	}
	
	@SuppressWarnings("SameParameterValue")
	private boolean download(int bufferSize) throws IOException {
		LoggingUtils.getLogger().info(String.format("正在从 %s 下载到 %s， 缓冲区大小 %d", url, downloadFile.toString(), bufferSize));
		try (BufferedOutputStream bos = new BufferedOutputStream(Files.newOutputStream(downloadFile.toPath()))) {
			
			IOUtil.downloadFile(url, bos, (downloaded, total) -> {
				DownloadProgressInfo info = new DownloadProgressInfo();
				info.downloaded = downloaded;
				info.total = total;
				updateProgressCallback.accept(info);
			}, bufferSize);
			
		} catch (IOException e) {
			LoggingUtils.getLogger().log(Level.SEVERE,
			String.format("从 %s 下载到 %s 失败", url, downloadFile),
			e);
			return false;
		}
		return true;
		
	}
	
	private boolean verifyChecksum() {
		try{
			String expectedChecksum = IOUtil.downloadText(checksumURL).trim();
			LoggingUtils.getLogger().finer(String.format("从 %s 下载到的MD5为 %s", checksumURL, expectedChecksum));
			String actualChecksum = UpdaterFileUtils.getChecksum(downloadFile);
			LoggingUtils.getLogger().finer(String.format("计算 %s 的MD5为 %s", downloadFile, actualChecksum));
			return expectedChecksum.equalsIgnoreCase(actualChecksum);
		}catch (IOException e){
			LoggingUtils.getLogger().log(Level.SEVERE, String.format("校验 %s 的MD5失败", downloadFile), e);
			return false;
		}
	}
	
	public static class DownloadProgressInfo{
		public int total;
		public int downloaded;
	}
}
