package net.lycorissystem.modpack_updater.updating;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.lycorissystem.modpack_updater.utils.UpdaterFileUtils;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@SuppressWarnings("resource")
public class UpdatePacker {
	/*
	 * 打包目录格式
	 * - client:客户端文件
	 * - server:服务端文件
	 * - shared:双端共用文件
	 *
	 * 每个文件夹包括：
	 * - 子文件夹"delete"，包含需要删除的文件
	 * - 子文件夹"add"，包含需要添加的文件
	 */
	
	public static void runPacker(String dir, String ver) throws IOException {
		File workdir = new File("update_output");
		if (!workdir.exists()) {
			workdir.mkdirs();
		}
		//总清单文件
		File packManifest = new File(workdir + "/update_pack.json");
		ObjectNode node = new ObjectMapper().createObjectNode();
		node.put("version", ver);
		
		
		if (new File(dir + "/client").exists()) {
			node.put("client", "client");
			packSidedPack(new File(dir), workdir, "client");
		}
		if (new File(dir + "/server").exists()) {
			node.put("server", "server");
			packSidedPack(new File(dir), workdir, "server");
		}
		if (new File(dir + "/shared").exists()) {
			node.put("shared", "shared");
			packSidedPack(new File(dir), workdir, "shared");
		}
		
		try {
			new ObjectMapper().writeValue(packManifest, node);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		//打包
		File zipFile = new File("update_" + ver + ".zip");
		zipDirectory(workdir.getAbsolutePath(), zipFile.getAbsolutePath());
		
		//生成MD5
		String md5 = UpdaterFileUtils.getChecksum(zipFile);
		File md5File = new File("update_" + ver + ".zip.md5");
		try (PrintWriter writer = new PrintWriter(md5File)) {
			writer.write(md5);
		}
		
	}
	
	private static void packSidedPack(File dir, File outputDir, String side) throws IOException {
		File packDir = new File(dir, side);
		System.out.println("Pack dir: " + packDir);
		if (!packDir.exists()) {
			return;
		}
		File packOutputDir = new File(outputDir, side);
		if (!packOutputDir.exists()) {
			packOutputDir.mkdirs();
		}
		File manifest = new File(packOutputDir, "manifest.json");
		System.out.println("manifest: " + manifest);
		ObjectNode node = new ObjectMapper().createObjectNode();
		ArrayNode toRemove = node.putArray("to_remove");
		//列出需要删除的文件
		//格式：
		//		{
		//			"folder": "mods",
		//			"file_name": "examplemod",
		//			"md5": "1a2b3c4d"
		//		}
		File deleteDir = new File(packDir, "delete");
		Files.walkFileTree(deleteDir.toPath(), new FileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				return FileVisitResult.CONTINUE;
			}
			
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				String folder = deleteDir.toPath().relativize(file.getParent()).toString();
				String md5 = UpdaterFileUtils.getChecksum(file.toFile());
				ObjectNode fileNode = new ObjectMapper().createObjectNode();
				fileNode.put("folder", folder);
				fileNode.put("file_name", file.getFileName().toString());
				fileNode.put("md5", md5);
				toRemove.add(fileNode);
				return FileVisitResult.CONTINUE;
			}
			
			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}
			
			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				return FileVisitResult.CONTINUE;
			}
		});
		
		
		//复制要添加的整个文件树
		File addDir = new File(packDir, "add");
		File targetDir = new File(packOutputDir, "files");
		if (addDir.exists()) {
			FileUtils.copyDirectory(addDir, targetDir);
		}
		node.put("to_add_files", "files");
		
		//写入清单
		System.out.println(node);
		new ObjectMapper().writeValue(manifest, (JsonNode) node);
	}
	
	
	public static void zipDirectory(String sourceDirPath, String zipFilePath) throws IOException {
		Path sourceDir = Paths.get(sourceDirPath);
		try (ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFilePath))) {
			Files.walk(sourceDir).forEach(path -> {
				// Convert path to ZIP format with forward slashes
				String zipEntryName = sourceDir.relativize(path).toString().replace("\\", "/");
				try {
					if (Files.isDirectory(path)) {
						// Add a trailing slash for directory entries
						zipOut.putNextEntry(new ZipEntry(zipEntryName + "/"));
						zipOut.closeEntry();
					} else {
						zipOut.putNextEntry(new ZipEntry(zipEntryName));
						Files.copy(path, zipOut);
						zipOut.closeEntry();
					}
				} catch (IOException e) {
					System.err.println("Error while zipping: " + e.getMessage());
				}
			});
		}
	}

	
	
	
}
