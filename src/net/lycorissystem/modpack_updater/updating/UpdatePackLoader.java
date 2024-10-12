package net.lycorissystem.modpack_updater.updating;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import net.lycorissystem.modpack_updater.utils.LoggingUtils;
import net.lycorissystem.modpack_updater.versioning.VersionInfo;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

public class UpdatePackLoader {
	
	public static Pair<Optional<UpdatePack>, String> loadPack(File file){
		if (!file.exists()){
			LoggingUtils.getLogger().severe(String.format("更新包文件 %s 不存在", file));
			return Pair.of(Optional.empty(), "更新包文件不存在");
		}
		try (ZipFile zipFile = new ZipFile(file)){
			ZipEntry entry = zipFile.getEntry("update_pack.json");
			if (entry == null){
				LoggingUtils.getLogger().severe(String.format("更新包文件 %s 中没有 update_pack.json", file));
				return Pair.of(Optional.empty(), "更新包文件中没有 update_pack.json");
			}
			UpdatePack pack = loadPacks(file, zipFile, entry);
			return Pair.of(Optional.of(pack), null);
		} catch (Exception e) {
			LoggingUtils.getLogger().log(java.util.logging.Level.SEVERE, String.format("读取更新包文件 %s 出错", file), e);
			return Pair.of(Optional.empty(), "读取更新包文件出错");
		}
		
	}
	
	private static UpdatePack loadPacks(File origFile, ZipFile file, ZipEntry packManifest) throws IOException {
		JsonNode packManifestJson = new ObjectMapper().readTree(file.getInputStream(packManifest));
		String version = packManifestJson.get("version").asText();
		String clientPack = packManifestJson.has("client") ? packManifestJson.get("client").asText() : null;
		String serverPack = packManifestJson.has("server") ? packManifestJson.get("server").asText() : null;
		String commonPack = packManifestJson.has("shared") ? packManifestJson.get("shared").asText() : null;
		LoggingUtils.getLogger().info(String.format("客户端更新包: %s, 服务端更新包: %s, 共用更新包: %s", clientPack, serverPack, commonPack));
		SidedUpdatePack client = null;
		SidedUpdatePack server = null;
		SidedUpdatePack common = null;
		if (clientPack != null){
			client = loadSidedPack(origFile, file, clientPack);
		}
		if (serverPack != null){
			server = loadSidedPack(origFile, file, serverPack);
		}
		if (commonPack != null){
			common = loadSidedPack(origFile, file, commonPack);
		}
		return new UpdatePack(version, common, client, server);
		
	}
	private static SidedUpdatePack loadSidedPack(File origFile, ZipFile file, String packName) throws IOException {
		ZipEntry entry = file.getEntry(packName+"/");
		LoggingUtils.getLogger().info(String.format("读取更新包文件 %s 中的 %s", file.getName(), packName));
		if(!entry.isDirectory()){
			LoggingUtils.getLogger().severe(String.format("更新包文件 %s 中的 %s 不是文件夹", file.getName(), packName));
			throw new ZipException(String.format("更新包文件 %s 中的 %s 不是文件夹", file.getName(), packName));
		}
		LoggingUtils.getLogger().info(entry.getName());
	    ZipEntry manifest = file.getEntry(packName + "/manifest.json");
		if(manifest == null){
			LoggingUtils.getLogger().severe(String.format("更新包文件 %s 中的 %s 没有 manifest.json", file.getName(), packName));
			throw new ZipException(String.format("更新包文件 %s 中的 %s 没有 manifest.json", file.getName(), packName));
		}
		JsonNode manifestJson = new ObjectMapper().readTree(file.getInputStream(manifest));
		String toAddFiles = manifestJson.get("to_add_files").asText();
		ArrayList<FileToRemove> toRemoveFiles = new ArrayList<>();
		for(JsonNode node : manifestJson.get("to_remove")){
			String folder = node.get("folder").asText();
			String fileName = node.get("file_name").asText();
			String md5 = node.get("md5").asText();
			toRemoveFiles.add(new FileToRemove(folder, md5, fileName));
		}
		return new SidedUpdatePack(origFile, packName, toRemoveFiles, toAddFiles);
	}
}
