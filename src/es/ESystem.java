package es;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;

import net.sf.clipsrules.jni.CLIPSException;

@SuppressWarnings("serial")
public class ESystem extends JFrame {

	private FloorModel model;
	private Robot robot;
	private String paint = "UWall";

	JPanel gridPanel;
	JTable grid;
	JSlider slider;
	JProgressBar batteryBar;
	RobotBatteryListener rbl = new RobotBatteryListener() {
		
		@Override
		public void onBatteryChanged(int battery) {
			batteryBar.setValue(battery);
		}
	};

	private static final String[] TILES = { "UFloor", "UCarpet", "UWall", "UCStation", "DFloor", "DCarpet", "DCStation",
			"Wall", "CFloor", "CCarpet", "CCStation", "BCrumb" };
	private static final String[] TILE_NAMES = { "Undiscovered Floor", "Undiscovered Carpet", "Undiscovered Wall", "Undiscovered Charging Station", "Dirty Floor",
			"Dirty Carpet", "Dirty Charging Station", "Wall", "Clean Floor", "Clean Carpet", "Clean Charging Station",
			"Bread crumb" };

	public ESystem() {
		model = new FloorModel(15, 15);
		robot = new Robot(model);
		robot.addMovementListener(model);
		robot.fireMovementListeners();

		setTitle("ES Cleaner");
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setLocation(20, 20);
		initGUI();
		setSize(850, 400);
		setVisible(true);
		;
	}

	private Action nextStepAction = new AbstractAction("Next step") {

		@Override
		public void actionPerformed(ActionEvent e) {
			toggleActions(false);

			robot.setOn(true);
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						robot.nextStep();
					} catch (CLIPSException e1) {
						e1.printStackTrace();
					}
					toggleActions(true);
				}
			}).start();

		}
	};

	private Action autoAction = new AbstractAction("Auto") {

		@Override
		public void actionPerformed(ActionEvent e) {
			toggleActions(false);

			robot.setOn(true);
			new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						robot.auto();
					} catch (CLIPSException e1) {
						e1.printStackTrace();
					}
					toggleActions(true);
				}
			}).start();

		}
	};

	private Action resetAction = new AbstractAction("Reset") {

		@Override
		public void actionPerformed(ActionEvent e) {
			model.reset();
			robot.reset();

		}
	};

	private Action stopAction = new AbstractAction("Stop") {

		@Override
		public void actionPerformed(ActionEvent e) {
			robot.setOn(false);
		}
	};

	private Action setStartAction = new AbstractAction("Place Robot") {

		@Override
		public void actionPerformed(ActionEvent e) {
			paint = "Robot";
		}
	};

	private Action saveAction = new AbstractAction("Save") {

		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser fc = new JFileChooser();
			fc.setDialogTitle("Open file");
			if (fc.showOpenDialog(ESystem.this) != JFileChooser.APPROVE_OPTION) {
				return;
			}
			File fileName = fc.getSelectedFile();
			Path filePath = fileName.toPath();

			try {
				FloorModel.toFile(filePath, model);
			} catch (IOException e1) {
				showError("Unable to write the selected file.", "Error saving file");
				return;
			}

		}
	};

	private Action loadAction = new AbstractAction("Load") {

		@Override
		public void actionPerformed(ActionEvent e) {
			JFileChooser fc = new JFileChooser();
			fc.setDialogTitle("Open file");
			if (fc.showOpenDialog(ESystem.this) != JFileChooser.APPROVE_OPTION) {
				return;
			}
			File fileName = fc.getSelectedFile();
			Path filePath = fileName.toPath();

			if (!Files.isReadable(filePath)) {
				showError("Unable to read the selected file.", "Error reading file");
				return;
			}
			var tmp = model;
			try {
				model = FloorModel.fromFile(filePath);
			} catch (IOException e1) {
				showError("Unable to read the selected file.", "Error reading file");
				return;
			}
			robot.removeMovementListener(tmp);
			grid.setModel(model);
			robot = new Robot(model);
			robot.addMovementListener(model);
			robot.addBatteryListener(rbl);
			robot.reset();
			robot.setAutoSpeed(slider.getValue());

			gridPanel.setSize(grid.getPreferredSize());

		}
	};

	private void showError(String message, String title) {
		String[] options = new String[] { "Ok" };
		JOptionPane.showOptionDialog(ESystem.this, message, title, JOptionPane.DEFAULT_OPTION,
				JOptionPane.ERROR_MESSAGE, null, options, options[0]);
	}

	private List<Action> tileActions = new ArrayList<>();

	private void toggleActions(boolean enabled) {
		nextStepAction.setEnabled(enabled);
		autoAction.setEnabled(enabled);
		for (var action : tileActions)
			action.setEnabled(enabled);
		stopAction.setEnabled(!enabled);
		resetAction.setEnabled(enabled);
		setStartAction.setEnabled(enabled);
		saveAction.setEnabled(enabled);
		loadAction.setEnabled(enabled);
	}

	private void initGUI() {

		setLayout(new BorderLayout());

		JPanel gridPanel = initGridPanel();
		JPanel controls = new JPanel(new GridLayout(9, 1));
		JPanel tiles = new JPanel(new GridLayout(12, 1));
		JPanel fileTools = new JPanel(new GridLayout(2, 1));

		controls.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(20, 5, 5, 5), "Controls",
				TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));
		tiles.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(20, 5, 5, 5), "Tiles",
				TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));
		fileTools.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEmptyBorder(20, 5, 5, 5),
				"Input/Output", TitledBorder.CENTER, TitledBorder.DEFAULT_POSITION));

		for (int i = 0; i < TILES.length; i++) {

			final int index = i;
			Action tileAction = new AbstractAction(TILE_NAMES[index]) {

				@Override
				public void actionPerformed(ActionEvent e) {
					paint = TILES[index];
				}
			};
			tiles.add(new JButton(tileAction));
			tileActions.add(tileAction);
		}
		JLabel sliderLabel = new JLabel("Speed");
		sliderLabel.setHorizontalAlignment(SwingConstants.CENTER);
		slider = new JSlider(1, 100, 10);
		slider.addChangeListener(new ChangeListener() {

			@Override
			public void stateChanged(ChangeEvent e) {
				robot.setAutoSpeed(slider.getValue());
			}
		});
		robot.setAutoSpeed(slider.getValue());
		
		JLabel batteryLabel = new JLabel("Battery level");
		batteryLabel.setHorizontalAlignment(SwingConstants.CENTER);
		batteryBar = new JProgressBar(10, 100);
		batteryBar.setValue(100);
		robot.addBatteryListener(rbl);
		

		fileTools.add(new JButton(saveAction));
		fileTools.add(new JButton(loadAction));

		controls.add(new JButton(nextStepAction));
		controls.add(new JButton(autoAction));
		controls.add(sliderLabel);
		controls.add(slider);
		controls.add(new JButton(stopAction));
		stopAction.setEnabled(false);
		controls.add(new JButton(resetAction));
		controls.add(new JButton(setStartAction));
		controls.add(batteryLabel);
		controls.add(batteryBar);

		JPanel tilePanel = new JPanel(new FlowLayout(FlowLayout.LEADING));
		tilePanel.add(tiles);

		JPanel controlPanel = new JPanel(new BorderLayout());
		controlPanel.add(controls, BorderLayout.NORTH);
		controlPanel.add(fileTools, BorderLayout.SOUTH);

		add(tilePanel, BorderLayout.WEST);
		add(gridPanel, BorderLayout.CENTER);
		add(controlPanel, BorderLayout.EAST);

	}

	private JPanel initGridPanel() {
		gridPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

		int rowCount = model.getRowCount();
		int colCount = model.getColumnCount();

		DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
			public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
					boolean hasFocus, int row, int column) {
				Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
				if (comp instanceof JLabel) {
					JLabel cell = (JLabel) comp;
					cell.setHorizontalAlignment(JLabel.CENTER);
					cell.setText("");
					if (value instanceof String) {
						String s = (String) value;
						switch (s) {
						case "UFloor" -> {
							cell.setBackground(Color.LIGHT_GRAY);
						}
						case "UCarpet" -> {
							cell.setBackground(Color.GRAY);
						}
						case "UWall" -> {
							cell.setBackground(Color.DARK_GRAY);
						}
						case "UCStation" -> {
							cell.setBackground(Color.decode("#262511"));
						}
						case "DFloor" -> {
							cell.setBackground(Color.decode("#589A8D"));
						}
						case "DCarpet" -> {
							cell.setBackground(Color.decode("#A6874E"));
						}
						case "DCStation" -> {
							cell.setBackground(Color.decode("#4F7302"));
						}
						case "Wall" -> {
							cell.setBackground(Color.decode("#0B2B40"));
						}
						case "CFloor" -> {
							cell.setBackground(Color.decode("#C7FFED"));
						}
						case "CCarpet" -> {
							cell.setBackground(Color.decode("#FFCB9A"));
						}
						case "CCStation" -> {
							cell.setBackground(Color.decode("#04D939"));
						}
						case "BCrumb" -> {
							cell.setBackground(Color.decode("#BF1304"));
						}
						}
					}
					if (row == model.getRobot_x() && column == model.getRobot_y()) {
						cell.setBackground(Color.BLACK);
						cell.setForeground(Color.WHITE);
						
						switch(model.getRobot_rotation()) {
						case 90 -> {cell.setText("▲");}
						case 180 -> {cell.setText("◄");}
						case 270 -> {cell.setText("▼");}
						case 0 -> {cell.setText("►");}
						}
					}
				}
				return comp;
			}
		};

		grid = new JTable(model);

		MouseAdapter adapter = new MouseAdapter() {
			
			private void mousePaint(MouseEvent e) {
				int row = grid.rowAtPoint(e.getPoint());
				int col = grid.columnAtPoint(e.getPoint());
				if (row >= 0 && col >= 0) {
					if (paint.equals("Robot")) {
						if (!((String) model.getValueAt(row, col)).contains("Wall")) {
							robot.set(row, col);
						}
					} else {
						model.setValueAt(paint, row, col);
					}
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				mousePaint(e);
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				mousePaint(e);
			}

		};
		
		grid.addMouseListener(adapter);
		grid.addMouseMotionListener(adapter);

		gridPanel.addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent e) {
				// Get new JTable component size
				Dimension size = gridPanel.getSize();

				int cellSize;

				if (size.height / rowCount > size.width / colCount) {
					cellSize = size.width / colCount - 1;
				} else {
					cellSize = size.height / rowCount - 1;
				}

				grid.setRowHeight(cellSize);

				for (int i = 0; i < grid.getColumnCount(); i++) {
					grid.getColumnModel().getColumn(i).setPreferredWidth(cellSize);
				}
			}
		});

		grid.setRowSelectionAllowed(false);
		grid.setShowGrid(true);
		grid.setGridColor(Color.black);
		grid.setBorder(BorderFactory.createLineBorder(Color.black, 2));
		grid.setDefaultRenderer(Object.class, renderer);

		gridPanel.add(grid);

		return gridPanel;
	}

	public static void main(String[] args) throws CLIPSException, FileNotFoundException {
		SwingUtilities.invokeLater(() -> {
			new ESystem().setVisible(true);
		});

	}

}
