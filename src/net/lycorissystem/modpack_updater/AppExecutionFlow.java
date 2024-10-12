package net.lycorissystem.modpack_updater;

import net.lycorissystem.modpack_updater.file_io.DownloaderThread;
import net.lycorissystem.modpack_updater.gui.UpdaterGUI;
import net.lycorissystem.modpack_updater.updating.SidedUpdatePack;
import net.lycorissystem.modpack_updater.updating.UpdatePack;
import net.lycorissystem.modpack_updater.updating.UpdatePackLoader;
import net.lycorissystem.modpack_updater.utils.UpdaterFileUtils;
import net.lycorissystem.modpack_updater.utils.LoggingUtils;
import net.lycorissystem.modpack_updater.versioning.UpdatePathSolver;
import net.lycorissystem.modpack_updater.versioning.VersionInfo;
import net.lycorissystem.modpack_updater.versioning.VersionManifest;
import net.lycorissystem.modpack_updater.versioning.VersionManifestLoader;
import org.apache.commons.lang3.tuple.Pair;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public class AppExecutionFlow {
	
	private static final int STATE_UNINITIALIZED = 0;
	private static final int STATE_INITIALIZED = 1;
	
	private static final int STATE_CHECK_MANIFEST = 2;
	private static final int STATE_CHECKING_MANIFEST = 201;
	private static final int STATE_CHECK_MANIFEST_DONE = 202;
	private static final int STATE_CHECK_MANIFEST_FAIL = 203;
	
	private static final int STATE_CHECK_DIFF = 3;
	private static final int STATE_CHECKING_LOCAL_VERSION = 301;
	private static final int STATE_CALCULATE_DIFF = 302;
	private static final int STATE_CALCULATING_DIFF = 303;
	
	private static final int STATE_ASK_UPDATE = 4;
	
	private static final int STATE_DO_DOWNLOAD = 5;
	private static final int STATE_DOWNLOADING_PACKS = 501;
	private static final int STATE_DOWNLOAD_FAIL = 502;
	private static final int STATE_DOWNLOAD_SUCCESS = 503;
	
	private static final int STATE_APPLY_UPDATE = 6;
	private static final int STATE_APPLYING_UPDATE = 601;
	
	private static final int STATE_FINALIZE_UPDATE = 7;
	
	private static final int STATE_SUCCESS_EXIT = 14;
	private static final int STATE_FAILED_EXIT = 15;
	private static final int STATE_ACTUALLY_EXIT = 16;
	private static final int WAIT_FOR_EXIT_DIALOG = 17;
	
	public static boolean isServer = false;
	
	private static UpdaterGUI gui;
	
	private static File minecraftDir = null;
	
	private static VersionManifest manifest = null;
	private static String localCurrentVersion = null;
	private static VersionInfo[] updatePath = null;
	
	private static UpdatePack[] updatePacks = null;
	private static String resultingVersion = null;
	
	
	/*
	 * 程序执行流程：
	 * 1. 初始化（显示GUI）
	 * 2. 获取版本清单
	 * 3. 计算版本差异
	 * 4. 询问用户是否更新
	 * 5. 下载和校验更新包
	 * 6. 下载失败->提示用户重新下载->如果用户选择重新下载则5，否则15
	 * 7. 下载成功->解压更新包
	 * 8. 测试更新操作，确定是否存在冲突
	 * 9. 有冲突或无法安全更新->提示用户是否继续->选择继续则10，否则15
	 * 10. 无冲突或用户选择继续->开始更新
	 * 11. 备份任何要删除的文件
	 * 12. 应用更新
	 * 13. 显示更新成功对话框
	 * 14. 退出
	 * 15. 提示更新失败
	 */
	
	private static int state = STATE_UNINITIALIZED;
	
	public static void init(){
		Pair<Boolean, String> result = UpdaterFileUtils.initFiles();
		if(!result.getLeft()){
			showErrorDialog(result.getRight());
			return;
		}
		minecraftDir = UpdaterFileUtils.findActualMinecraftDir();
		if(minecraftDir == null){
			showErrorDialog("未找到.minecraft目录");
			return;
		}
		LoggingUtils.getLogger().fine("Minecraft目录：" + minecraftDir.getAbsolutePath());
		
		setupGUI();
		setupStateMachine();
	}
	
	public static void showErrorDialog(String message){
		state = WAIT_FOR_EXIT_DIALOG;
		if(gui != null)
			gui.dispose();
		
		if(isServer){
			LoggingUtils.getLogger().severe(message);
			return;
		}
		SwingUtilities.invokeLater(() -> {
			JOptionPane.showMessageDialog(null, message, "错误", JOptionPane.ERROR_MESSAGE);
			actuallyExit();
		});
	}
	public static void showInfoDialog(String message){
		state = WAIT_FOR_EXIT_DIALOG;
		if(gui != null)
			gui.dispose();
		
		if(isServer){
			LoggingUtils.getLogger().info(message);
			return;
		}
		SwingUtilities.invokeLater(() -> {
			JOptionPane.showMessageDialog(null, message, "信息", JOptionPane.INFORMATION_MESSAGE);
			actuallyExit();
		});
	}
	
	private static void setupGUI(){
		
		try {
			UIManager.setLookAndFeel("com.sun.java.swing.plaf.motif.MotifLookAndFeel");
		}catch (Exception e){
			try{
				UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
			}catch (Exception e1){
				//ignore
			}
		}
		
		gui = new UpdaterGUI();
		if(isServer){
			gui.setTitle("SnowFantasy.net 整合包更新器[服务端模式]");
		}
		gui.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		gui.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				onExitRequest();
				e.getWindow().dispose();
			}
		});
		gui.pack();
		gui.setSize(600, 400);
		gui.setButtonExitCallback(AppExecutionFlow::onExitRequest);
		gui.setButtonYesCallback(AppExecutionFlow::buttonYesClicked);
		gui.setButtonNoCallback(AppExecutionFlow::buttonNoClicked);
		gui.setLocationRelativeTo(null);
		gui.setVisible(true);
	}
	
	private static void setupStateMachine(){
		Timer timer = new Timer(200, e -> mainLoop());
		timer.start();
		state = STATE_INITIALIZED;
	}
	
	private static void enableYesNoButtons(){
		gui.setButtonYesEnabled(true);
		gui.setButtonNoEnabled(true);
	}
	
	private static void disableYesNoButtons(){
		gui.setButtonYesEnabled(false);
		gui.setButtonNoEnabled(false);
	}
	
	@SuppressWarnings("DuplicateBranchesInSwitch")
	private static void mainLoop(){
		switch(state){
			case STATE_INITIALIZED:
				disableYesNoButtons();
				state = STATE_CHECK_MANIFEST;
				break;
			case STATE_CHECK_MANIFEST:
				disableYesNoButtons();
				state = STATE_CHECKING_MANIFEST;
				checkManifest();
				break;
			case STATE_CHECKING_MANIFEST:
				disableYesNoButtons();
				//等待
				break;
			case STATE_CHECK_MANIFEST_DONE:
				disableYesNoButtons();
				state = STATE_CHECK_DIFF;
				break;
			case STATE_CHECK_MANIFEST_FAIL:
				enableYesNoButtons();
				break;
			case STATE_CHECK_DIFF:
				disableYesNoButtons();
				state = STATE_CHECKING_LOCAL_VERSION;
				checkLocalVersion();
				break;
			case STATE_CHECKING_LOCAL_VERSION:
				disableYesNoButtons();
				//等待
				break;
			case STATE_CALCULATE_DIFF:
				disableYesNoButtons();
				state = STATE_CALCULATING_DIFF;
				checkVersionDiff();
				break;
			case STATE_CALCULATING_DIFF:
				disableYesNoButtons();
				break;
			case STATE_ASK_UPDATE:
				enableYesNoButtons();
				//等待
				break;
			case STATE_DO_DOWNLOAD:
				disableYesNoButtons();
				state = STATE_DOWNLOADING_PACKS;
				downloadUpdatePacks();
				break;
			case STATE_DOWNLOADING_PACKS:
				disableYesNoButtons();
				//等待
				break;
			case STATE_DOWNLOAD_FAIL:
				enableYesNoButtons();
				//等待
				break;
			case STATE_DOWNLOAD_SUCCESS:
				disableYesNoButtons();
				state = STATE_APPLY_UPDATE;
				break;
			case STATE_APPLY_UPDATE:
				disableYesNoButtons();
				state = STATE_APPLYING_UPDATE;
				applyUpdate();
				break;
			case STATE_APPLYING_UPDATE:
				disableYesNoButtons();
				//等待
				break;
			case STATE_FINALIZE_UPDATE:
				finalizeUpdate();
				state = STATE_SUCCESS_EXIT;
				break;
			case STATE_SUCCESS_EXIT:
				state = WAIT_FOR_EXIT_DIALOG;
				successExit();
				break;
			case STATE_FAILED_EXIT:
				state = WAIT_FOR_EXIT_DIALOG;
				failedExit();
				break;
			case STATE_ACTUALLY_EXIT:
				afterExit();
				break;
			case WAIT_FOR_EXIT_DIALOG:
				break;
		}
	}
	
	public static void buttonYesClicked(){
		LoggingUtils.getLogger().finest("已点击Yes按钮");
		switch(state){
			case STATE_CHECK_MANIFEST_FAIL:
				state = STATE_CHECK_MANIFEST;
				break;
			case STATE_ASK_UPDATE:
			case STATE_DOWNLOAD_FAIL:
				state = STATE_DO_DOWNLOAD;
				break;
		}
	}
	
	public static void buttonNoClicked(){
		LoggingUtils.getLogger().finest("已点击No按钮");
		switch(state){
			case STATE_CHECK_MANIFEST_FAIL:
			case STATE_ASK_UPDATE:
			case STATE_DOWNLOAD_FAIL:
				showErrorDialog("用户取消操作，未进行更新");
				break;
		}
	}
	
	
	
	private static void checkManifest(){
		SwingUtilities.invokeLater(() -> {
			SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
				@Override
				protected Void doInBackground() {
					try{
						checkManifestInternal();
					}catch (Exception e){
						LoggingUtils.getLogger().log(Level.SEVERE, "检查清单文件失败", e);
						showErrorDialog("检查清单文件失败：" + e.getMessage());
					}
					return null;
				}
			};
			worker.execute();
		});
	}
	private static void checkManifestInternal(){
		String manifestURL = VersionManifestLoader.getManifestURL();
		if(manifestURL == null){
			showErrorDialog("无法读取清单文件地址，此更新器jar可能未被正确配置！\n请联系管理员报告这个错误。");
			return;
		}
		Pair<Optional<VersionManifest>, String> manifestResult = VersionManifestLoader.getAndProcessManifest(
		manifestURL,
		(cur, max) -> {
			SwingUtilities.invokeLater(() -> {
				gui.setProgress1(cur);
				gui.setProgress1Max(max);
				gui.setPBar1DescText("正在获取版本清单");
				gui.setPBar1ProgressText(String.format("%d/%d", cur, max));
			});
		},
		(status) -> {
			gui.setInfoLabelText(status);
		}
		);
		LoggingUtils.getLogger().finest(String.format("版本清单: %s，错误：%s",  manifestResult.getLeft(), manifestResult.getRight()));
		if(manifestResult.getLeft().isPresent()){
			gui.setPBar1DescText("已获取版本清单");
			manifest = manifestResult.getLeft().get();
			state = STATE_CHECK_MANIFEST_DONE;
		}else {
			state = STATE_CHECK_MANIFEST_FAIL;
			gui.setInfoLabelText("无法获取版本清单");
			gui.setInfoText("错误：\n" + manifestResult.getRight());
			gui.setButtonYesText("重试");
			gui.setButtonNoText("取消");
		}
	}
	
	
	
	private static final int MAX_RETRY = 3;
	private static void downloadUpdatePacks(){
		updatePacks = new UpdatePack[updatePath.length];
		SwingUtilities.invokeLater(() -> {
			SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
				@Override
				protected Void doInBackground() {
					downloadUpdatePacksInternal();
					return null;
				}
			};
			worker.execute();
		});
	}
	private static void downloadUpdatePacksInternal(){
		for(int i = 1; i < updatePath.length; i++){ //跳过第一个版本（那是当前版本）
			
			gui.setPBar1ProgressText(String.format("%d/%d", i, updatePath.length - 1));
			gui.setPBar1DescText("正在下载更新包");
			gui.setProgress1(i);
			gui.setProgress1Max(updatePath.length);
			
			VersionInfo thisVersion = updatePath[i];
			VersionInfo lastVersion = updatePath[i-1];
			String versionName = thisVersion.getVersionName();
			File downloadFile = new File(UpdaterFileUtils.getWorkDir().getPath() + File.separator + "update_" + versionName + ".zip");
			
			if(downloadFile.exists()){
				if(!downloadFile.delete()){
					showErrorDialog("无法删除旧的更新包文件" + downloadFile);
					return;
				}
			}
			
			int retry = 0;
			AtomicBoolean success = new AtomicBoolean(false);
			while(!success.get()) {
				
				DownloaderThread downloader = buildPackDownloader(thisVersion,
				String.format("%s -> %s", lastVersion.getVersionName(), versionName),
				downloadFile, retry, success);
				downloader.run();
				if(++retry > MAX_RETRY){
					showErrorDialog("下载更新包失败");
					return;
				}
			}
			
			Pair<Optional<UpdatePack>, String> pack = UpdatePackLoader.loadPack(downloadFile);
			if(!pack.getLeft().isPresent()){
				state = STATE_DOWNLOAD_FAIL;
				gui.setInfoText("更新包" + versionName + "加载失败");
				gui.setInfoLabelText("错误：" + pack.getRight());
				gui.setButtonYesText("重试");
				gui.setButtonNoText("取消");
				return;
			}
			updatePacks[i] = pack.getLeft().get();
		}
		state = STATE_DOWNLOAD_SUCCESS;
	}
	private static DownloaderThread buildPackDownloader(VersionInfo thisVersion, String versionName, File downloadFile, int retry, AtomicBoolean success) {
		String url = thisVersion.getPackageURL();
		String checksumURL = thisVersion.getChecksumURL();
		return new DownloaderThread(
			url,
			checksumURL,
			downloadFile,
			(info) -> {
				gui.setPBar2DescText("正在下载更新包" + versionName + "(尝试" + retry+")");
				gui.setProgress2(info.downloaded);
				gui.setProgress2Max(info.total);
				gui.setPBar2ProgressText(String.format("%.3f MB/%.3f MB", info.downloaded/(1024f*1024f), info.total/(1024f*1024f)));
			},
			success::set
		);
	}
	
	private static void applyUpdate(){
		SwingUtilities.invokeLater(() -> {
			SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
				@Override
				protected Void doInBackground() {
					applyUpdateInternal();
					return null;
				}
			};
			worker.execute();
		});
	}
	private static void applyUpdateInternal(){
		for(int i = 1; i < updatePacks.length; i++){
			UpdatePack pack = updatePacks[i];
			gui.setProgress1(i);
			gui.setProgress1Max(updatePacks.length);
			try {
				SidedUpdatePack common = pack.getCommon();
				SidedUpdatePack client = pack.getClient();
				SidedUpdatePack server = pack.getServer();
				if(common != null){
					int _I = i;
					common.apply(minecraftDir, (cur, max) -> {
						gui.setPBar1DescText("正在应用更新包" + updatePath[_I].getVersionName() + "的公共部分");
						gui.setProgress2(cur);
						gui.setProgress2Max(max);
						gui.setPBar2ProgressText(String.format("%d/%d", cur, max));
					}, (status) -> {
						gui.setPBar2DescText(status);
					});
				}
				if(isServer && server != null){
					int _I = i;
					server.apply(minecraftDir, (cur, max) -> {
						gui.setPBar1DescText("正在应用更新包" + updatePath[_I].getVersionName() + "的服务端部分");
						gui.setProgress2(cur);
						gui.setProgress2Max(max);
						gui.setPBar2ProgressText(String.format("%d/%d", cur, max));
					}, (status) -> {
						gui.setPBar2DescText(status);
					});
				}else if(client != null){
					int _I = i;
					client.apply(minecraftDir, (cur, max) -> {
						gui.setPBar1DescText("正在应用更新包" + updatePath[_I].getVersionName() + "的客户端部分");
						gui.setProgress2(cur);
						gui.setProgress2Max(max);
						gui.setPBar2ProgressText(String.format("%d/%d", cur, max));
					}, (status) -> {
						gui.setPBar2DescText(status);
					});
				}
			}catch (Exception e){
				LoggingUtils.getLogger().log(Level.SEVERE, "应用更新包" + updatePath[i].getVersionName() + "失败", e);
				showErrorDialog("应用更新包" + updatePath[i].getVersionName() + "失败：" + e.getMessage() + "\n"
				 + "请尝试重新运行更新器，如果频繁失败可尝试重新下载完整包（并报告这个问题）");
				return;
			}
		}
		state = STATE_FINALIZE_UPDATE;
	}
	
	private static void finalizeUpdate(){
		SwingUtilities.invokeLater(() -> {
			SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
				@Override
				protected Void doInBackground() {
					gui.setInfoLabelText("正在完成更新...");
					resultingVersion = updatePath[updatePath.length - 1].getVersionName();
					UpdatePathSolver.writeCurrentVersionNumber(resultingVersion);
					gui.setInfoLabelText("正在清理工作目录");
					UpdaterFileUtils.clearWorkDir();
					
					state = STATE_SUCCESS_EXIT;
					return null;
				}
			};
			worker.execute();
		});
	}
	
	
	
	private static void checkLocalVersion(){
		SwingUtilities.invokeLater(() -> {
			localCurrentVersion = UpdatePathSolver.getCurrentVersionNumber(manifest);
			state = STATE_CALCULATE_DIFF;
		});
	}
	
	private static void checkVersionDiff(){
		SwingUtilities.invokeLater(() -> {
			SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
				@Override
				protected Void doInBackground() {
					gui.setPBar1DescText("正在计算更新路径");
					gui.setPBar1ProgressText("-");
					UpdatePathSolver.UpdatePath path = UpdatePathSolver.solveUpdatePath(manifest, localCurrentVersion);
					if(path.getPath().length > 0 && (!path.isNoUpdate() && !path.isEndOfLife())){
						if (path.getPath()[path.getPath().length - 1].getVersionName().equals(localCurrentVersion)) {
							path = new UpdatePathSolver.UpdatePath(true);
						}
					}
					gui.setPBar1DescText("已计算更新路径");
					gui.setPBar1ProgressText("-");
					if(path.isNoUpdate()){
						showInfoDialog("当前版本已是最新版本");
					}else if(path.isEndOfLife()){
						showErrorDialog("当前版本已停止支持，请重新下载完整包");
					}else if(path.isSuccess()){
						updatePath = path.getPath();
						state = STATE_ASK_UPDATE;
						gui.setInfoLabelText("发现新版本");
						gui.setInfoText(UpdatePathSolver.formatUpdatePath(path));
						gui.setButtonYesText("更新");
						gui.setButtonNoText("取消");
					}else{
						showErrorDialog("更新路径计算失败：" + path.getMessage() + "，请重试或报告这个问题");
					}
					return null;
				}
			};
			worker.execute();
		});
	}
	
	private static void failedExit(){
		beforeExit();
		SwingUtilities.invokeLater(() -> {
			JOptionPane.showMessageDialog(gui,
			"更新失败，请再次尝试，或重新下载完整包",
			"更新失败", JOptionPane.ERROR_MESSAGE);
			actuallyExit();
		});
	}
	
	private static void successExit(){
		beforeExit();
		SwingUtilities.invokeLater(() -> {
			JOptionPane.showMessageDialog(gui,
			"客户端已成功更新到 " + resultingVersion + "，可以启动游戏！",
			"更新成功", JOptionPane.INFORMATION_MESSAGE);
			actuallyExit();
		});
	}
	
	private static void beforeExit(){
		gui.dispose();
	}
	
	private static void afterExit(){
		System.exit(0);
	}
	
	
	
	public static void onExitRequest(){
		state = STATE_FAILED_EXIT;
	}
	
	public static void actuallyExit(){
		state = STATE_ACTUALLY_EXIT;
	}
}
