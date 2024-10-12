package net.lycorissystem.modpack_updater.versioning;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.lycorissystem.modpack_updater.utils.UpdaterFileUtils;
import net.lycorissystem.modpack_updater.utils.LoggingUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class UpdatePathSolver {
	
	private static String getCurrentVersionNumberInner(){
		File minecraftDir = UpdaterFileUtils.findActualMinecraftDir();
		File versionFile = new File(minecraftDir, "modpack_version");
		if(versionFile.exists()) {
			try{
				return new ObjectMapper().readTree(versionFile).get("version").asText();
			} catch (IOException e) {
				LoggingUtils.getLogger().warning("读取版本文件出错");
				return null;
			}
		}
		return null;
	}
	
	public static void writeCurrentVersionNumber(String version){
		File minecraftDir = UpdaterFileUtils.findActualMinecraftDir();
		File versionFile = new File(minecraftDir, "modpack_version");
		try {
			JsonNode node = new ObjectMapper().createObjectNode().put("version", version);
			new ObjectMapper().writeValue(versionFile, node);
		} catch (IOException e) {
			LoggingUtils.getLogger().warning("写入版本文件出错");
		}
	}
	
	public static String getCurrentVersionNumber(VersionManifest manifest) {
		String version = getCurrentVersionNumberInner();
		if (version == null) {
			return manifest.defaultVersion;
		}
		return version;
	}
	
	/**
	 * 计算从当前版本到最新版本的更新路径
	 * @param manifest 版本清单
	 * @param currentVersion 当前版本
	 * @return 更新路径
	 */
	public static UpdatePath solveUpdatePath(VersionManifest manifest, String currentVersion) {
		ArrayList<VersionInfo> path = new ArrayList<>();
		VersionInfo current = manifest.versions.get(currentVersion);
		if(current == null) {
			return new UpdatePath(null, false, false, "当前版本号不存在");
		}
		if(current.isLatest) {
			return new UpdatePath(true);
		}
		path.add(current);
		VersionInfo next = manifest.versions.get(current.nextVersionName);
		while(next != null) {
			LoggingUtils.getLogger().finer(String.format("当前版本 %s, latest %s，eol：%s，isPrevious：%s，下一个版本：%s", current.versionName, current.isLatest, current.isEndOfLife, current.isPrevious, next.versionName));
			//如果下一个版本的部署时间在当前时间之后，则返回路径
			if(next.getVersionDeployTime() > System.currentTimeMillis()) {
				if(current.isPrevious()) {
					//如果当前版本是最新版本的前一个版本，且最新版本还未发布，那么当前版本实际上是最新版本
					return new UpdatePath(path.toArray(new VersionInfo[0]), true, false, "已找到最新版本");
				}
				return new UpdatePath(path.toArray(new VersionInfo[0]), false, false, "下一个版本尚未发布，当前可用的最后一个版本不是最新版本的前一版本");
			}
			//如果下一个版本已经部署，则继续
			path.add(next);
			if(next.isLatest) {
				//如果下一个版本是最新版本，则返回路径
				return new UpdatePath(path.toArray(new VersionInfo[0]), true, false, "已找到最新版本");
			}
			if(next.isEndOfLife) {
				return new UpdatePath(path.toArray(new VersionInfo[0]), true, true, "版本已停止支持");
			}
			current = next;
			next = manifest.versions.get(next.nextVersionName);
		}
		return new UpdatePath(path.toArray(new VersionInfo[0]), false, false, "查找了所有版本，但未找到最新版本");
	}
	
	public static class UpdatePath{
		VersionInfo[] path;
		boolean success;
		boolean isEndOfLife;
		boolean noUpdate;
		String message;
		
		public UpdatePath(VersionInfo[] path, boolean success, boolean isEndOfLife, String message) {
			this.path = path;
			this.success = success;
			this.isEndOfLife = isEndOfLife;
			this.message = message;
			this.noUpdate = false;
		}
		
		public UpdatePath(boolean noUpdate) {
			this.path = new VersionInfo[]{};
			this.success = true;
			this.isEndOfLife = false;
			this.noUpdate = noUpdate;
			this.message = "当前版本已是最新版本";
		}
		
		public VersionInfo[] getPath() {
			return path;
		}
		
		public boolean isSuccess() {
			return success;
		}
		
		public boolean isEndOfLife() {
			return isEndOfLife;
		}
		
		public String getMessage() {
			return message;
		}
		
		public boolean isNoUpdate() {
			return noUpdate;
		}
	}
	
	public static String formatUpdatePath(UpdatePath path) {
		if(path.isNoUpdate()) {
			return "当前版本已是最新版本";
		}
		if(path.isEndOfLife()) {
			return "当前版本已停止支持，请重新下载完整包";
		}
		if(path.isSuccess()) {
			StringBuilder sb = new StringBuilder();
			sb.append("当前：").append(path.getPath()[0].getVersionName()).append("\n");
			sb.append("最新：").append(path.getPath()[path.getPath().length - 1].getVersionName()).append("\n");
			sb.append("更新路径：\n");
			for (VersionInfo versionInfo : path.getPath()) {
				sb.append(versionInfo.versionName).append(" -> ");
			}
			return sb.substring(0, sb.length() - 4);
		}
		return "更新路径计算失败：" + path.getMessage() + "，请重试或报告这个问题";
	}
}
