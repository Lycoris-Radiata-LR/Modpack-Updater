package net.lycorissystem.modpack_updater.updating;

import net.lycorissystem.modpack_updater.utils.UpdaterFileUtils;

import java.io.File;
import java.util.Optional;

public class FileToRemove {
	String folder;
	String md5;
	String supposedName;
	
	public FileToRemove(String folder, String md5, String supposedName) {
		this.folder = folder;
		this.md5 = md5;
		this.supposedName = supposedName;
	}
	
	public String getFolder() {
		return folder;
	}
	
	public String getMd5() {
		return md5;
	}
	
	public String getSupposedName() {
		return supposedName;
	}
	
	public Optional<File> findFile(File baseDir){
		File[] files = new File(baseDir, folder).listFiles();
		if(files == null) {
			return Optional.empty();
		}
		for(File file : files) {
			String md5 = UpdaterFileUtils.getChecksum(file);
			if(md5 != null && md5.equals(this.md5)) {
				return Optional.of(file);
			}
		}
		return Optional.empty();
	}
}
