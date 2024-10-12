package net.lycorissystem.modpack_updater.gui;

import javax.swing.*;
import java.awt.*;

public class UpdaterGUI extends JFrame {
	
	private JProgressBar progressBar1;
	private JProgressBar progressBar2;
	private JButton ButtonYes;
	private JButton ButtonNo;
	private JButton ButtonExit;
	private JLabel LabelTextDisplay;
	private JLabel LabelPBar1;
	private JLabel LabelPBar2;
	private JLabel LabelPBar2Progress;
	private JLabel LabelPBar1Progress;
	private JTextPane TextInfoDisplay;
	private JLabel AuthorInfoLabel;
	private JPanel PanelContent;
	
	public UpdaterGUI() throws HeadlessException {
		this.setTitle("SnowFantasy.net 整合包更新器");
		this.setContentPane(PanelContent);
	}
	
	public void setProgress1(int progress) {
		progressBar1.setValue(progress);
	}
	
	public void setProgress2(int progress) {
		progressBar2.setValue(progress);
		progressBar2.repaint();
	}
	
	public void setProgress1Max(int max) {
		progressBar1.setMaximum(max);
	}
	
	public void setProgress2Max(int max) {
		progressBar2.setMaximum(max);
	}
	
	public void setInfoLabelText(String text) {
		LabelTextDisplay.setText(text);
	}
	
	public void setInfoText(String text) {
		TextInfoDisplay.setText(text);
	}
	
	public void setPBar1DescText(String text) {
		LabelPBar1.setText(text);
	}
	
	public void setPBar2DescText(String text) {
		LabelPBar2.setText(text);
	}
	
	public void setPBar1ProgressText(String text) {
		LabelPBar1Progress.setText(text);
	}
	
	public void setPBar2ProgressText(String text) {
		LabelPBar2Progress.setText(text);
	}
	
	public void setButtonYesText(String text) {
		ButtonYes.setText(text);
	}
	
	public void setButtonNoText(String text) {
		ButtonNo.setText(text);
	}
	
	public void setButtonYesEnabled(boolean enabled) {
		ButtonYes.setEnabled(enabled);
	}
	
	public void setButtonNoEnabled(boolean enabled) {
		ButtonNo.setEnabled(enabled);
	}
	
	public void setButtonExitEnabled(boolean enabled) {
		ButtonExit.setEnabled(enabled);
	}
	
	public void setButtonYesCallback(Runnable callback) {
		ButtonYes.addActionListener(e -> callback.run());
	}
	
	public void setButtonNoCallback(Runnable callback) {
		ButtonNo.addActionListener(e -> callback.run());
	}
	
	public void setButtonExitCallback(Runnable callback) {
		ButtonExit.addActionListener(e -> callback.run());
	}
}
