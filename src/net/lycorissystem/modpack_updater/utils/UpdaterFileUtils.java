package net.lycorissystem.modpack_updater.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.lycorissystem.modpack_updater.AppExecutionFlow;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class UpdaterFileUtils {
	
	/**
	 * 获取文件的MD5校验和
	 * @param file 文件
	 * @return MD5校验和，以十六进制字符串表示
	 */
	public static String getChecksum(File file){
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			try {
				md.update(Files.readAllBytes(file.toPath()));
			} catch (Exception e) {
				LoggingUtils.getLogger().log(Level.SEVERE, String.format("读取文件 %s 出错", file), e);
				return null;
			}
			byte[] digest = md.digest();
			StringBuffer sb = new StringBuffer();
			for (byte b : digest) {
				sb.append(String.format("%02x", b & 0xff));
			}
			String checksum = sb.toString();
			LoggingUtils.getLogger().finest(String.format("文件 %s 的MD5校验和为 %s", file.getName(), checksum));
			return checksum;
		} catch (NoSuchAlgorithmException e) {
			LoggingUtils.getLogger().log(Level.SEVERE, "MD5算法不可用", e);
			return null;
		}
	}
	
	public static Pair<Boolean, String> initFiles(){
		//初始化工作目录
		File dir = getWorkDir();
		if (!dir.exists()){
			if(!dir.mkdirs()){
				LoggingUtils.getLogger().severe("无法创建工作目录");
				return Pair.of(false, "无法创建工作目录");
			}
		}
		return Pair.of(true, null);
	}
	
	public static File getWorkDir(){
		File dir = new File("./更新器工作目录");
		return dir;
	}
	
	public static void clearWorkDir(){
		File dir = getWorkDir();
		if(dir.exists()){
			try {
				Files.walk(dir.toPath())
						.sorted(java.util.Comparator.reverseOrder())
						.map(Path::toFile)
						.forEach(File::delete);
			} catch (IOException e) {
				LoggingUtils.getLogger().log(Level.SEVERE, "清空工作目录出错", e);
			}
		}
		dir.deleteOnExit();
	}
	
	public static JsonNode readJsonFile(File file){
		try {
			return new ObjectMapper().readTree(file);
		} catch (Exception e) {
			LoggingUtils.getLogger().log(Level.SEVERE, String.format("读取JSON文件 %s 出错", file), e);
			return null;
		}
	}
	
	public static File findActualMinecraftDir(){
		if(AppExecutionFlow.isServer){
			File dir = new File(".");
			if(new File(dir, "mods").isDirectory() && new File(dir, "config").isDirectory() && new File(dir, "server.properties").isFile()){
				return dir;
			}
			LoggingUtils.getLogger().severe("未找到服务器端目录");
			return null;
		}
		File dir = new File("./.minecraft");
		if(!dir.isDirectory()){
			LoggingUtils.getLogger().severe("未找到.minecraft目录");
			return null;
		}
		//假如这个文件夹包含mods和config文件夹，则认为是.minecraft文件夹
		if(new File(dir, "mods").isDirectory() && new File(dir, "config").isDirectory()){
			return dir;
		}
		//否则，尝试寻找versions文件夹中第一个包含mods和config文件夹的文件夹
		File versions = new File(dir, "versions");
		if(!versions.isDirectory()){
			LoggingUtils.getLogger().severe("未找到.minecraft/versions目录");
			return null;
		}
		File[] versionDirs = versions.listFiles();
		if(versionDirs == null){
			LoggingUtils.getLogger().severe("未找到.minecraft/versions目录");
			return null;
		}
		for(File versionDir : versionDirs){
			if(new File(versionDir, "mods").isDirectory() && new File(versionDir, "config").isDirectory()){
				return versionDir;
			}
		}
		LoggingUtils.getLogger().severe("未找到包含mods和config文件夹的版本文件夹");
		return null;
	}
	
	public static void unzipDir(ZipFile zipFile, String entryName, File destDir, BiConsumer<Integer, Integer> progressCallback) throws IOException {
		ZipEntry entry = zipFile.getEntry(entryName);
		if (entry == null || !entry.isDirectory()) {
			throw new IOException(String.format("压缩文件 %s 中的 %s 不是文件夹", zipFile.getName(), entryName));
		}
		
		// 遍历压缩文件中的所有条目，并解压需要的目录
		List<? extends ZipEntry> entries = zipFile.stream().filter(e -> e.getName().startsWith(entryName + "/")).collect(Collectors.toList());
		for (int i = 0; i < entries.size(); i++) {
			progressCallback.accept(i, entries.size());
			ZipEntry subEntry = entries.get(i);
			Path subDestPath = subEntry.getName().substring(entryName.length() + 1).isEmpty() ?
					destDir.toPath() : destDir.toPath().resolve(subEntry.getName().substring(entryName.length() + 1));
			System.out.println(subDestPath);
			
			// 确保路径在目标目录中
			if (!subDestPath.startsWith(destDir.toPath())) {
				throw new IOException(String.format("禁止解压到指定路径之外！出错的文件: %s", subDestPath));
			}
			
			if (subEntry.isDirectory()) {
				LoggingUtils.getLogger().finest(String.format("创建目录: %s", subDestPath));
				Files.createDirectories(subDestPath);
			} else {
				// 确保父目录存在
				Files.createDirectories(subDestPath.getParent());
				// 使用 REPLACE_EXISTING 覆盖同名文件
				LoggingUtils.getLogger().finest(String.format("解压文件: %s", subDestPath));
				Files.copy(zipFile.getInputStream(subEntry), subDestPath, StandardCopyOption.REPLACE_EXISTING);
			}
		}
	}
	
	public static String readTextFile(File file){
		try {
			return String.join("\n", Files.readAllLines(file.toPath()));
		} catch (IOException e) {
			LoggingUtils.getLogger().log(Level.SEVERE, String.format("读取文本文件 %s 出错", file), e);
			return null;
		}
	}
}
