/*
 * 版权所有 重庆骄智科技有限公司 保留所有权利
 * Copyright © 2020-2025 All rights reserved. 
 * www.joyzl.com
 */
package com.joyzl.watcher;

import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.imageio.ImageIO;

import com.joyzl.logger.Logger;

/**
 * 系统托盘图标
 * 
 * @author simon (ZhangXi TEL:13883833982)
 * @date 2025年10月21日
 */
public class Tray implements ActionListener, MouseListener {

	private static Tray instance;

	public static void show() throws Exception {
		if (instance == null) {
			if (SystemTray.isSupported()) {
				final Image image;
				final File imageFile = new File("Tray.png");
				if (imageFile.exists()) {
					image = ImageIO.read(imageFile);
				} else {
					image = ImageIO.read(Tray.class.getResource("Tray.png"));
				}

				final TrayIcon icon = new TrayIcon(image, "JOYZL Watcher");
				icon.setImageAutoSize(true);

				final Tray tray = new Tray(icon);
				icon.addActionListener(tray);
				icon.addMouseListener(tray);
				instance = tray;

				final PopupMenu menu = new PopupMenu();
				final MenuItem item1 = new MenuItem("Open");
				item1.addActionListener(tray);
				item1.setActionCommand("open");
				menu.add(item1);
				final MenuItem item2 = new MenuItem("Exit");
				item2.addActionListener(tray);
				item2.setActionCommand("exit");
				menu.add(item2);

				icon.setPopupMenu(menu);

				SystemTray.getSystemTray().add(icon);
			}
		}
	}

	public static void close() {
		if (instance != null) {
			SystemTray.getSystemTray().remove(instance.icon);
			instance = null;
		}
	}

	final TrayIcon icon;

	Tray(TrayIcon icon) {
		this.icon = icon;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		Logger.debug(e.getActionCommand());
		if ("exit".equals(e.getActionCommand())) {
			Application.close();
		} else {
			Window.show();
		}
	}

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
	}

	@Override
	public void mouseReleased(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}
}