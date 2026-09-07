/*
 * 版权所有 重庆骄智科技有限公司 保留所有权利
 * Copyright © 2020-2025 All rights reserved. 
 * www.joyzl.com
 */
package com.joyzl.watcher;

import static com.joyzl.network.Utility.isEmpty;
import static com.joyzl.network.Utility.noEmpty;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.border.EmptyBorder;

import com.joyzl.logger.Logger;

/**
 * 主窗口
 * 
 * @author simon (ZhangXi TEL:13883833982)
 * @date 2025年10月24日
 */
final class Window {

	private static Window instance;

	/** 显示主窗口 */
	public static boolean show() {
		if (GraphicsEnvironment.isHeadless()) {
			return false;
		}
		if (instance == null) {
			try {
				instance = new Window("JOYZL Watcher");
			} catch (HeadlessException e) {
				// 没有桌面环境
				return false;
			}
		}
		if (instance != null) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					instance.showFrame();
					instance.refreshFrame();
				}
			});
			return true;
		}
		return false;
	}

	/** 刷新主窗口 */
	public static void refresh() {
		if (instance != null) {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					instance.refreshFrame();
				}
			});
		}
	}

	/** 关闭主窗口 */
	public static void close() {
		if (instance != null) {
			instance.frame.dispose();
			instance = null;
		}
	}

	private final JFrame frame;
	private final JLabel model, watch, server, detect, upload, number;

	/** 创建主窗口 */
	private Window(String title) {
		// 创建主窗口
		frame = new JFrame(title);
		frame.setMinimumSize(new Dimension(380, 180));
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

		// 加载窗口图标
		final List<Image> icons = new ArrayList<>();
		try {
			URL url = getClass().getResource(getClass().getSimpleName() + ".png");
			if (url != null) {
				icons.add(ImageIO.read(url));
			}
			url = getClass().getResource(getClass().getSimpleName() + "16.png");
			if (url != null) {
				icons.add(ImageIO.read(url));
			}
			url = getClass().getResource(getClass().getSimpleName() + "32.png");
			if (url != null) {
				icons.add(ImageIO.read(url));
			}
			url = getClass().getResource(getClass().getSimpleName() + "48.png");
			if (url != null) {
				icons.add(ImageIO.read(url));
			}
			url = getClass().getResource(getClass().getSimpleName() + "64.png");
			if (url != null) {
				icons.add(ImageIO.read(url));
			}
		} catch (IOException e) {
			Logger.error(e);
		}
		frame.setIconImages(icons);

		// 垂直布局面板
		final JPanel main = new JPanel();
		main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
		main.setBorder(new EmptyBorder(10, 10, 10, 10));

		// LABEL SERVER
		server = new JLabel();
		server.setAlignmentX(Component.LEFT_ALIGNMENT);
		main.add(server);

		number = new JLabel();
		number.setAlignmentX(Component.LEFT_ALIGNMENT);
		main.add(number);

		// 水平布局面板
		final JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		main.add(panel);

		detect = new JLabel();
		detect.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		detect.setFont(detect.getFont().deriveFont(detect.getFont().getSize2D() * 2));
		detect.setHorizontalAlignment(SwingConstants.RIGHT);
		panel.add(detect);

		// 间距
		panel.add(Box.createHorizontalStrut(20));

		upload = new JLabel();
		upload.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
		upload.setFont(upload.getFont().deriveFont(upload.getFont().getSize2D() * 2));
		upload.setHorizontalAlignment(SwingConstants.LEFT);
		panel.add(upload);

		// LABEL MODEL
		model = new JLabel();
		model.setAlignmentX(Component.LEFT_ALIGNMENT);
		main.add(model);

		// LABEL WATCH
		watch = new JLabel();
		watch.setAlignmentX(Component.LEFT_ALIGNMENT);
		main.add(watch);

		// 将面板添加到窗口
		frame.getContentPane().add(main);
		frame.setTransferHandler(new FileDropHandler("匹配上传"));

		// 双击选择目录
		watch.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					selectDirectory();
				}
			}
		});
		// 双击选择模型
		model.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					selectModel();
				}
			}
		});
		// 双击输入地址
		server.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					inputServer();
				}
			}
		});
		// 双击输入编号
		number.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2) {
					inputNumber();
				}
			}
		});
	}

	/** 显示主窗口 */
	private void showFrame() {
		// 将窗口居中显示
		frame.setLocationRelativeTo(null);
		// 显示窗口
		frame.setVisible(true);
	}

	/** 刷新主窗口显示文本 */
	private void refreshFrame() {
		if (Application.model() != null) {
			model.setText(Application.model().name());
		} else {
			if (isEmpty(Application.setting().getModel())) {
				model.setText("未指定模型");
			} else {
				model.setText("无效模型：" + Application.setting().getModel());
			}
		}

		if (isEmpty(Application.setting().getNumber())) {
			number.setText("未指定编号");
		} else {
			number.setText(Application.setting().getNumber());
		}

		if (Application.uploader() != null) {
			server.setText(Application.uploader().url());
			// 已上传文件数
			upload.setText(Integer.toString(Application.uploader().size()));
		} else {
			if (isEmpty(Application.setting().getHTTPServer())) {
				server.setText("未指定服务");
			} else {
				server.setText("无效服务：" + Application.setting().getHTTPServer());
			}
		}

		if (Application.watcher() != null) {
			watch.setText(Application.watcher().path().toString());
			// 已提交任务数
			detect.setText(Integer.toString(Application.watcher().size()));
		} else {
			if (isEmpty(Application.setting().getWatch())) {
				watch.setText("未指定目录");
			} else if (Application.model() == null || Application.uploader() == null) {
				// 有任何一个前置条件未满足时
				// 无法断定目录无效
				watch.setText(Application.setting().getWatch());
			} else {
				watch.setText("无效目录：" + Application.setting().getWatch());
			}
		}
	}

	/** 选择目录对话框 */
	private void selectDirectory() {
		final JFileChooser chooser = new JFileChooser();
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle("选择目录");
		if (noEmpty(Application.setting().getWatch())) {
			chooser.setCurrentDirectory(new File(Application.setting().getWatch()));
		}
		final int result = chooser.showOpenDialog(frame);
		if (result == JFileChooser.APPROVE_OPTION) {
			final File current = chooser.getSelectedFile();
			final Path previous;
			if (noEmpty(Application.setting().getWatch())) {
				previous = Path.of(Application.setting().getWatch());
			} else {
				previous = null;
			}
			try {
				if (Avoidance.change(previous, current.toPath())) {
					Application.setting().setWatch(current.getAbsolutePath());
					Application.setting().save();
					Application.reset();
				} else {
					error("目录已在监视中");
				}
			} catch (IOException e) {
				error(e.getMessage());
			} finally {
				refreshFrame();
			}
		}
	}

	/** 选择模型对话框 */
	private void selectModel() {
		final List<Model> models;
		try {
			models = Model.loads(Path.of("patterns"));
		} catch (IOException e) {
			error(e.getMessage());
			return;
		}

		final JLabel example = new JLabel();
		final JComboBox<Model> combo = new JComboBox<>();
		combo.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Model model = (Model) combo.getSelectedItem();
				if (model != null) {
					example.setText("");
					for (ModelFile f : model.files()) {
						example.setText(example.getText() + f.example());
					}
				} else {
					example.setText(null);
				}
			}
		});
		for (Model m : models) {
			combo.addItem(m);
		}
		if (Application.model() != null) {
			combo.setSelectedItem(Application.model());
		} else {
			combo.setSelectedIndex(0);
		}

		final JPanel panel = new JPanel(new BorderLayout(10, 10));
		panel.add(combo, BorderLayout.CENTER);
		panel.add(example, BorderLayout.SOUTH);

		final int result = JOptionPane.showConfirmDialog(frame, panel, //
			"选择模型", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (result == JOptionPane.OK_OPTION) {
			final Model model = (Model) combo.getSelectedItem();
			if (model != null) {
				// System.out.println(model.path());
				try {
					Application.setting().setModel(model.path().toString());
					Application.setting().save();
					Application.reset();
				} catch (IOException e) {
					error(e.getMessage());
				} finally {
					refreshFrame();
				}
			}
		}
	}

	private void inputServer() {
		final Object text = JOptionPane.showInputDialog(frame, //
			"接收文件的服务端接口(Archive)", "服务端接口", //
			JOptionPane.PLAIN_MESSAGE, null, //
			null, Application.setting().getHTTPServer());
		if (text != null && noEmpty(text.toString())) {
			try {
				final URL url = new URL(text.toString());
				Application.setting().setHTTPServer(url.toString());
				Application.setting().save();
				Application.reset();
			} catch (IOException e) {
				error(e.getMessage());
			} finally {
				refreshFrame();
			}
		}
	}

	private void inputNumber() {
		final Object text = JOptionPane.showInputDialog(frame, //
			"标识本机的编号", "本机编号", //
			JOptionPane.PLAIN_MESSAGE, null, //
			null, Application.setting().getNumber());
		if (text != null && noEmpty(text.toString())) {
			try {
				Application.setting().setNumber(text.toString());
				Application.setting().save();
				if (Application.uploader() != null) {
					Application.uploader().setNumber(Application.setting().getNumber());
				}
			} catch (IOException e) {
				error(e.getMessage());
			} finally {
				refreshFrame();
			}
		}
	}

	public static void error(String text) {
		if (GraphicsEnvironment.isHeadless()) {
			return;
		}
		try {
			JOptionPane.showMessageDialog(null, text, "错误", JOptionPane.INFORMATION_MESSAGE);
		} catch (HeadlessException e) {
			// 没有桌面环境
			return;
		}
	}

	/** 文件/目录拖放 */
	private class FileDropHandler extends TransferHandler {
		private static final long serialVersionUID = 1L;

		private FileDropHandler(String name) {
			super(name);
		}

		@Override
		public boolean canImport(TransferSupport support) {
			// 检查拖放的数据是否是文件列表
			return Application.watcher() != null && support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
		}

		@Override
		public boolean importData(TransferSupport support) {
			if (canImport(support)) {
				try {
					// 获取拖放的数据
					final Transferable transferable = support.getTransferable();
					final Object data = transferable.getTransferData(DataFlavor.javaFileListFlavor);
					if (data != null) {
						if (data instanceof List<?> items) {
							for (Object item : items) {
								if (item instanceof File file) {
									Application.watcher().match(file);
								}
							}
							return true;
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return false;
		}
	}
}