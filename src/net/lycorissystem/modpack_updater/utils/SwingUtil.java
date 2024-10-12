package net.lycorissystem.modpack_updater.utils;


import java.awt.*;

public class SwingUtil {
	public static Point getCenteredPosition(int width, int height) {
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		int x = (screenSize.width - width) / 2;
		int y = (screenSize.height - height) / 2;
		return new Point(x, y);
	}
}
