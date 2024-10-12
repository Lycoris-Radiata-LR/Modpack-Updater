package net.lycorissystem.modpack_updater.updating;

import net.lycorissystem.modpack_updater.utils.UpdaterFileUtils;
import net.lycorissystem.modpack_updater.utils.LoggingUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.zip.ZipFile;

public class SidedUpdatePack {
	File file;
	String side;
	ArrayList<FileToRemove> filesToRemove;
	String folder;
	
	public SidedUpdatePack(File file, String side, ArrayList<FileToRemove> filesToRemove, String folder) {
		this.file = file;
		this.side = side;
		this.filesToRemove = filesToRemove;
		this.folder = folder;
	}
	
	public ArrayList<FileToRemove> getFilesToRemove() {
		return filesToRemove;
	}
	
	public String getFolder() {
		return folder;
	}
	
	@Override
	public String toString() {
		return "SidedUpdatePack{" +
		       "side='" + side + '\'' +
		       ", filesToRemove=" + filesToRemove.toString() +
		       ", folder='" + folder + '\'' +
		       '}';
	}
	
	public void apply(File minecraftDir, BiConsumer<Integer, Integer> progressCallback, Consumer<String> statusCallback) throws IOException {
		statusCallback.accept("正在删除旧文件");
		for (int i = 0; i < filesToRemove.size(); i++) {
			progressCallback.accept(i, filesToRemove.size());
			FileToRemove fileToRemove = filesToRemove.get(i);
			Optional<File> file = fileToRemove.findFile(minecraftDir);
			if (file.isPresent()) {
				LoggingUtils.getLogger().info("删除文件: " + file.get().getAbsolutePath());
				if (!file.get().delete()) {
					LoggingUtils.getLogger().warning("删除文件失败: " + file.get().getAbsolutePath());
					throw new IOException("删除文件失败: " + file.get().getAbsolutePath());
				}
			}
		}
		statusCallback.accept("正在解压新文件");
		UpdaterFileUtils.unzipDir(new ZipFile(this.file), side + "/" + folder, minecraftDir, progressCallback);
		
	}
}
