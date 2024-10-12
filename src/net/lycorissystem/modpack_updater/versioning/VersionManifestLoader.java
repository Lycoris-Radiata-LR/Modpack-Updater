package net.lycorissystem.modpack_updater.versioning;

import com.fasterxml.jackson.databind.JsonNode;
import net.lycorissystem.modpack_updater.file_io.DownloaderThread;
import net.lycorissystem.modpack_updater.utils.UpdaterFileUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@SuppressWarnings("GrazieInspection") // 不检查语法错误
public class VersionManifestLoader {
	/*
	 * 版本清单格式（JSON）：
	 * {
	 *   //版本列表
	 *   "versions": [
	 *     {
	 * 	     "version_name": "1.0.0",//版本名称
	 * 	     "next_version_name": "1.0.1",//下一个版本名称（最新版本或EOL版本不填）
	 * 	     "is_latest": true,//是否为最新版本（不填默认为false）
	 * 	     "is_previous": false,//是否为最新版本的上个版本（不填默认为false）
	 * 	     "is_end_of_life": false,//是否为终止支持版本（不填默认为false，如true则提示重新下载，不进行更新）
	 * 	     "package_url": "http://example.com/1.0.0.zip",//包地址
	 * 	     "checksum_url": "http://example.com/1.0.0.md5",//校验和地址
	 * 	     "version_deploy_time": { //版本部署时间
	 *           "year": 2021,
	 * 		     "month": 1,
	 * 		     "day": 1,
	 * 		     "hour": 4,
	 * 		     "minute": 0,
	 * 		     "second": 0,
	 *           ”timezone": "Asia/Shanghai"
	 *     },
	 *   ],
	 *   //客户端没有版本号时的默认版本
	 *   "default_version": "1.0.0",
	 * }
	 */
	
	private static VersionInfo parseVersionInfo(JsonNode json) {
		String versionName = json.get("version_name").asText();
		String nextVersionName = json.has("next_version_name") ? json.get("next_version_name").asText() : null;
		boolean isLatest = json.has("is_latest") && json.get("is_latest").asBoolean();
		boolean isPrevious = json.has("is_previous") && json.get("is_previous").asBoolean();
		boolean isEndOfLife = json.has("is_end_of_life") && json.get("is_end_of_life").asBoolean();
		String packageURL = json.get("package_url").asText();
		String checksumURL = json.get("checksum_url").asText();
		long versionDeployTime = ZonedDateTime.of(
			json.get("version_deploy_time").get("year").asInt(),
			json.get("version_deploy_time").get("month").asInt(),
			json.get("version_deploy_time").get("day").asInt(),
			json.get("version_deploy_time").get("hour").asInt(),
			json.get("version_deploy_time").get("minute").asInt(),
			json.get("version_deploy_time").get("second").asInt(),
			0,
			ZoneId.of(json.get("version_deploy_time").get("timezone").asText())
		).toInstant().toEpochMilli();
		
		return new VersionInfo(versionName, nextVersionName, isLatest, isPrevious, isEndOfLife, packageURL, checksumURL, versionDeployTime);
	}
	
	private static VersionManifest parseVersionManifest(JsonNode json) {
		VersionManifest manifest = new VersionManifest(json.get("default_version").asText());
		for (JsonNode version : json.get("versions")) {
			manifest.addVersion(parseVersionInfo(version));
		}
		return manifest;
	}
	
	public static Pair<Optional<VersionManifest>, String> getAndProcessManifest(String manifestURL, BiConsumer<Integer, Integer> progressCallback, Consumer<String> statusCallback) {
		File workDir = UpdaterFileUtils.getWorkDir();
		File manifestFile = new File(workDir, "version_manifest.json");
		if(manifestFile.exists()){
			if(!manifestFile.delete()){
				return Pair.of(Optional.empty(), "无法删除旧的清单文件");
			}
		}
		AtomicBoolean success = new AtomicBoolean(false);
		statusCallback.accept("正在下载版本清单...");
		DownloaderThread downloader = new DownloaderThread(manifestURL, null, manifestFile,
		info -> {
			progressCallback.accept(info.downloaded, info.total);
		},
		s -> {
			if(s){
				statusCallback.accept("版本清单下载成功");
				success.set(true);
			}else{
				statusCallback.accept("版本清单下载失败");
			}
		});
		
		Thread downloaderThread = new Thread(downloader);
		downloaderThread.start();
		try {
			downloaderThread.join();
		} catch (InterruptedException e) {
			return Pair.of(Optional.empty(), "下载线程被中断");
		}
		
		if(!manifestFile.exists()){
			return Pair.of(Optional.empty(), "版本清单文件不存在，或未能下载成功");
		}
		if(!success.get()){
			return Pair.of(Optional.empty(), "版本清单下载失败");
		}
		
		JsonNode manifestJson = UpdaterFileUtils.readJsonFile(manifestFile);
		if(manifestJson == null){
			return Pair.of(Optional.empty(), "无法解析版本清单文件");
		}
		
		VersionManifest manifest = parseVersionManifest(manifestJson);
		return Pair.of(Optional.of(manifest), null);
	}
	
	public static String getManifestURL(){
		//从Jar内置资源中读取版本清单地址
		try {
			InputStream is = VersionManifestLoader.class.getResource("/version_manifest_url.txt").openStream();
			byte[] buffer = new byte[4096];
			int bytesRead = is.read(buffer);
			is.close();
			return new String(buffer, 0, bytesRead).trim();
		} catch (IOException e) {
			return null;
		}
	}
}
