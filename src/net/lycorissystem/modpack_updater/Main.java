package net.lycorissystem.modpack_updater;

import net.lycorissystem.modpack_updater.updating.UpdatePacker;
import net.lycorissystem.modpack_updater.utils.UpdaterFileUtils;

import javax.swing.*;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class Main {
	
	
	@SuppressWarnings("all")
	public static void main(String[] args) {
		try {
			File lockFile = new File("app.lock");
			FileChannel channel = new FileOutputStream(lockFile).getChannel();
			
			FileLock lock = channel.tryLock();
			if (lock == null) {
				JOptionPane.showMessageDialog(null, "不能同时运行多个更新器，这会损坏minecraft客户端", "错误", JOptionPane.ERROR_MESSAGE);
				System.exit(1);
			}
			
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				try {
					lock.release();
					channel.close();
					lockFile.delete();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}));
			
			mainInner(args);
			
		} catch (Exception e) {
			System.err.println("启动错误: " + e.getMessage());
			System.exit(1);
		}
	}
	
	private static void mainInner(String[] args) {
		Runtime.getRuntime().addShutdownHook(new Thread(UpdaterFileUtils::clearWorkDir));
		
		//如果第一个参数是"packing",则对第二个参数给出的目录进行打包（而不是运行更新器）
		if (args.length > 0 && args[0].equals("packing")) {
			if (args.length < 3) {
				System.err.println("缺少参数");
				return;
			}
			String dir = args[1];
			String ver = args[2];
			try {
				UpdatePacker.runPacker(dir, ver);
			}catch (Exception e){
				e.printStackTrace();
			}
			return;
		}
		
		//如果第一个参数是"server"，则启动服务器模式
		if (args.length > 0 && args[0].equals("server")) {
			AppExecutionFlow.isServer = true;
		}
		SwingUtilities.invokeLater(AppExecutionFlow::init);
	}
	
}
