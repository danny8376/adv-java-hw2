/**
 * Advance JAVA Assignment
 * 
 * Student Name : 蔡崴丞
 * Student No.  : 101403022
 * Class : Information Management - 2A
 * 
 * Filename : MainFrame.java
 * 
 * The main frame
 * 
 * Changelog:
 * Ver.1.2
 *   Improved: Dotted Lines
 * Ver.1.1
 *   Fixed: Center the brush drawing point with mouse
 *          Can't draw Oval & Rect from upper right to lower left
 *   Improved: Little performance improvement?
 *             Don't cut drawn image when resize (smaller)
 */
package advjava.hw.s101403022;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

import javax.swing.*;

public class MainFrame extends JFrame {
	static String MODE_STRING[] = {"筆刷", "直線", "橢圓", "矩形"};
	JPanel toolbox, paint, status;
	JLabel mousePos, tool, action;
	JComboBox<String> mode;
	JRadioButton sizeSmall, sizeMedium, sizeLarge;
	ButtonGroup sizeSelect;
	JCheckBox fill;
	JButton clearAll;
	BufferedImage canvas;
	Graphics2D drawing;
	Point lastPos, nowPos;
	boolean circleSquare = false, solidRecord = true, fillRecord = false;
	int sizePixel = 3;
	
	public MainFrame() {
		super("小畫家");
		
		// Use BorderLayout
		setLayout(new BorderLayout());
		
		// key listener for controlling
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher(){
			@Override
			public boolean dispatchKeyEvent(KeyEvent event) {
				switch(event.getID()) {
				case KeyEvent.KEY_PRESSED:
					if (event.getKeyCode() == KeyEvent.VK_SHIFT) MainFrame.this.circleSquare = true; 
					break;
				case KeyEvent.KEY_RELEASED:
					if (event.getKeyCode() == KeyEvent.VK_SHIFT) MainFrame.this.circleSquare = false; 
					break;
				}
				return false;
			}
			
		});
		
		// ============  Toolbox   ============
		this.toolbox = new JPanel();
		this.toolbox.setLayout(new GridLayout(0, 1));
		this.toolbox.add(new Label("[繪圖工具]"));
		this.mode = new JComboBox<String>(MODE_STRING);
		this.mode.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent event) {
				int selected = MainFrame.this.mode.getSelectedIndex();
				// change fill label text
				if (selected == 1) {
					//MainFrame.this.fill.setSelected(false);
					MainFrame.this.fill.setText("實/虛線(F)");
					MainFrame.this.drawing.setStroke(MainFrame.this.getStroke());
				}
				else MainFrame.this.fill.setText("填滿(F)");
				// it's unable to fill in brush & line mode so we disable it
				if (selected == 0) {
					MainFrame.this.fill.setSelected(false);
					MainFrame.this.fill.setEnabled(false);
				} else {
					MainFrame.this.fill.setSelected(selected == 1 ? MainFrame.this.solidRecord : MainFrame.this.fillRecord);
					MainFrame.this.fill.setEnabled(true);
					if (selected == 2) MainFrame.this.action.setText("按著SHIFT可限制為正圓");
					else if (selected == 3) MainFrame.this.action.setText("按著SHIFT可限制為正方形");
				}
				updateToolLabel();
			}
		});
		this.toolbox.add(this.mode);
		this.toolbox.add(new Label("[筆刷大小]"));
		// action listener - change stroke width
		ActionListener sizeChanged = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				MainFrame.this.sizePixel = getSizePixel();
				MainFrame.this.drawing.setStroke(MainFrame.this.getStroke());
				updateToolLabel();
			}
		};
		this.sizeSmall = new JRadioButton("小(A)", true);
		this.sizeSmall.addActionListener(sizeChanged);
		this.sizeSmall.setMnemonic(KeyEvent.VK_A);
		this.sizeMedium = new JRadioButton("中(S)");
		this.sizeMedium.addActionListener(sizeChanged);
		this.sizeMedium.setMnemonic(KeyEvent.VK_S);
		this.sizeLarge = new JRadioButton("大(D)");
		this.sizeLarge.addActionListener(sizeChanged);
		this.sizeLarge.setMnemonic(KeyEvent.VK_D);
		this.sizeSelect = new ButtonGroup();
		this.sizeSelect.add(this.sizeSmall);
		this.sizeSelect.add(this.sizeMedium);
		this.sizeSelect.add(this.sizeLarge);
		this.toolbox.add(this.sizeSmall);
		this.toolbox.add(this.sizeMedium);
		this.toolbox.add(this.sizeLarge);
		this.fill = new JCheckBox("填滿(F)");
		this.fill.setMnemonic(KeyEvent.VK_F);
		this.fill.setEnabled(false);
		// there won't be stroke in fill mode so we disable the size selections
		// use ItemListener here because this status can be changed by mode selection
		this.fill.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent event) {
				int selected = MainFrame.this.mode.getSelectedIndex();
				boolean checked = ((JCheckBox)event.getSource()).isSelected();
				if (selected == 1) MainFrame.this.solidRecord = checked;
				else if (selected != 0) MainFrame.this.fillRecord = checked;
				if (checked && selected != 1) {
					MainFrame.this.sizeSmall.setEnabled(false);
					MainFrame.this.sizeMedium.setEnabled(false);
					MainFrame.this.sizeLarge.setEnabled(false);
				} else {
					MainFrame.this.sizeSmall.setEnabled(true);
					MainFrame.this.sizeMedium.setEnabled(true);
					MainFrame.this.sizeLarge.setEnabled(true);
				}
				MainFrame.this.drawing.setStroke(MainFrame.this.getStroke());
				updateToolLabel();
			}
		});
		this.toolbox.add(this.fill);
		this.clearAll = new JButton("清除畫面");
		// clear - renew the canvas XP
		this.clearAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				MainFrame.this.action.setText("清除畫面");
				newCanvas(false);
				MainFrame.this.paint.repaint();
			}
		});
		this.toolbox.add(this.clearAll);
		
		// ============ Paint area ============
		this.paint = new JPanel() {
			@Override
			public void paintComponent(Graphics g) {
				// new one if there is no canvas
				if (MainFrame.this.canvas == null) newCanvas(false);
				// paint canvas to paint panel
				super.paintComponent(g);
				Graphics2D g2 = (Graphics2D)g;
				g2.drawImage(MainFrame.this.canvas, null, 0, 0);
				// this graphics needs to set every time
				g2.setStroke(MainFrame.this.getStroke());
				g2.setColor(Color.BLACK);
				// draw the preview
				drawNowDrawing(g2);
			}
		};
		// make border
		this.paint.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		// for resize
		this.paint.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent event) {
				newCanvas(true); // renew canvas & keep(copy) the content
			}
		});
		this.paint.addMouseMotionListener(new MouseMotionListener() {
			@Override
			public void mouseDragged(MouseEvent event) {
				Point pos = event.getPoint();
				MainFrame.this.mousePos.setText(String.format("游標位置 : (%d, %d)", pos.x, pos.y));
				MainFrame.this.action.setText("滑鼠拖曳");
				updatePaint(pos); // update preview info when dragging
			}
			
			@Override
			public void mouseMoved(MouseEvent event) {
				Point pos = event.getPoint();
				MainFrame.this.mousePos.setText(String.format("游標位置 : (%d, %d)", pos.x, pos.y));
			}
		});
		this.paint.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseEntered(MouseEvent event) {
				MainFrame.this.action.setText("滑鼠進入畫布");
			}
			
			@Override
			public void mouseExited(MouseEvent event) {
				MainFrame.this.action.setText("滑鼠移出畫布");
				MainFrame.this.mousePos.setText("游標位置 : 畫布外");
			}

			@Override
			public void mousePressed(MouseEvent event) {
				MainFrame.this.action.setText("滑鼠按下");
				startPaint(event.getPoint());
			}

			@Override
			public void mouseReleased(MouseEvent event) {
				MainFrame.this.action.setText("滑鼠放開");
				endPaint(event.getPoint());
			}
		});
		
		// ============ StatusBar  ============
		this.status = new JPanel();
		this.status.setLayout(new GridLayout(1, 3));
		this.mousePos = new JLabel("游標位置 : 畫布外", JLabel.LEFT);
		this.tool = new JLabel("Loading...", JLabel.CENTER);
		this.action = new JLabel("Alt-按鍵 為調整大小&填滿之快捷鍵", JLabel.RIGHT);
		this.status.add(this.mousePos);
		this.status.add(this.tool);
		this.status.add(this.action);
		
		
		// add main components to main frame
		this.add(this.toolbox, BorderLayout.WEST);
		this.add(this.paint, BorderLayout.CENTER);
		this.add(this.status, BorderLayout.PAGE_END);
		
		
		// init
		updateToolLabel();
	}
	
	private void newCanvas(boolean keepCont) {
		int oriWidth = this.canvas == null ? 0 : this.canvas.getWidth(),
				oriHeight = this.canvas == null ? 0 : this.canvas.getHeight(),
				width = Math.max(this.paint.getWidth(), oriWidth),
				height = Math.max(this.paint.getHeight(), oriHeight);
		BufferedImage newCanvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics2D newG = newCanvas.createGraphics();
		// fill background with white
		newG.setColor(Color.WHITE);
		newG.fillRect(0, 0, width, height);
		// copy content if specified to keep content
		if (keepCont) newG.drawImage(MainFrame.this.canvas, null, 0, 0);
		// set color to black for paint & set stroke width according to selection
		newG.setColor(Color.BLACK);
		newG.setStroke(this.getStroke());
		// replace the canvas & drawing graphics
		this.canvas = newCanvas;
		this.drawing = newG;
	}
	
	private String getSizeText() {
		if (this.sizeSmall.isSelected()) return "小";
		if (this.sizeMedium.isSelected()) return "中";
		if (this.sizeLarge.isSelected()) return "大";
		return "!";
	}
	
	private int getSizePixel() {
		if (this.sizeSmall.isSelected()) return 3;
		if (this.sizeMedium.isSelected()) return 7;
		if (this.sizeLarge.isSelected()) return 13;
		return 0;
	}
	
	private Stroke getStroke() {
		if (this.mode.getSelectedIndex() == 1 && !this.fill.isSelected()) {
			float dash[] = {1.7f * this.sizePixel, 2.3f * this.sizePixel};
			return new BasicStroke(this.sizePixel, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER, 50, dash, 0);
		} else {
			return new BasicStroke(this.sizePixel, BasicStroke.CAP_ROUND, BasicStroke.JOIN_MITER);
		}
	}
	
	private void updateToolLabel() {
		this.tool.setText("工具 : " + this.mode.getSelectedItem() +
				(this.fill.isSelected() && this.mode.getSelectedIndex() != 1 ? " , 填滿" :" , 筆刷大小 : " + getSizeText()) +
				(this.mode.getSelectedIndex() == 1 ? " , " + (this.fill.isSelected() ? "實線" : "虛線") : ""));
	}
	
	private void startPaint(Point pos) {
		// save current point for later usage
		lastPos = pos;
		// paint the first point when it's in brush mode
		if (MainFrame.this.mode.getSelectedIndex() == 0) {
			int offset = this.sizePixel / 2;
			this.drawing.fillOval(pos.x - offset, pos.y - offset, this.sizePixel, this.sizePixel);
			repaint();
		}
	}
	
	private void updatePaint(Point pos) {
		if (MainFrame.this.mode.getSelectedIndex() == 0) {
			// draw line between updates to avoid jumping points
			this.drawing.drawLine(lastPos.x, lastPos.y, pos.x, pos.y);
			lastPos = pos; // update the last point info
			repaint();
		} else {
			nowPos = pos; // save current point for drawing preview
			repaint();
		}
	}
	
	private void endPaint(Point pos) {
		if (MainFrame.this.mode.getSelectedIndex() == 0) {
			// nothing to do here
		} else {
			drawNowDrawing(this.drawing); // really draw the shape on the canvas
			repaint();
			nowPos = null; // clear the saved point for drawing preview
		}
	}
	
	private void drawNowDrawing(Graphics2D g) {
		if (nowPos == null) return;
		int width, height;
		switch (MainFrame.this.mode.getSelectedIndex()) {
		case 0: // brush
			// nothing to do here
			break;
		case 1: // line
			repaint();
			g.drawLine(lastPos.x, lastPos.y, nowPos.x, nowPos.y);
			break;
		case 2: // oval
			width = Math.abs(nowPos.x - lastPos.x);
			height = Math.abs(nowPos.y - lastPos.y);
			if (circleSquare) width = height = Math.min(width, height);
			if (this.fill.isSelected()) g.fillOval(Math.min(lastPos.x, nowPos.x), Math.min(lastPos.y, nowPos.y), width, height);
			else g.drawOval(Math.min(lastPos.x, nowPos.x), Math.min(lastPos.y, nowPos.y), width, height);
			break;
		case 3: // rect
			width = Math.abs(nowPos.x - lastPos.x);
			height = Math.abs(nowPos.y - lastPos.y);
			if (circleSquare) width = height = Math.min(width, height);
			if (this.fill.isSelected()) g.fillRect(Math.min(lastPos.x, nowPos.x), Math.min(lastPos.y, nowPos.y), width, height);
			else g.drawRect(Math.min(lastPos.x, nowPos.x), Math.min(lastPos.y, nowPos.y), width, height);
			break;
		}
	}
}
