package es;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.table.DefaultTableModel;

@SuppressWarnings("serial")
public class FloorModel extends DefaultTableModel implements RobotMovementListener {

	private String[][] overlay;
	private int robot_x, robot_y;
	private int robot_rotation;

	public FloorModel(int rowCount, int colCount) {
		super(rowCount, colCount);
		for (int i = 0; i < rowCount; i++) {
			for (int j = 0; j < colCount; j++) {
				if (i == 0 || j == 0 || i == rowCount - 1 || j == rowCount - 1)
					super.setValueAt("UWall", i, j);
				else
					super.setValueAt("UFloor", i, j);
			}
		}
		super.setValueAt("UCStation", 1, 1);
		overlay = new String[rowCount][colCount];
	}

	public FloorModel(String[][] data) {
		super(data.length, data[0].length);
		for (int i = 0; i < this.getRowCount(); i++) {
			for (int j = 0; j < this.getColumnCount(); j++) {
				super.setValueAt(data[i][j], i, j);
			}
		}
		overlay = new String[data.length][data[0].length];
	}

	@Override
	public Object getValueAt(int row, int column) {
		if (overlay[row][column] != null)
			return overlay[row][column];
		return super.getValueAt(row, column);
	}

	@Override
	public void setValueAt(Object aValue, int row, int column) {
		super.setValueAt(aValue, row, column);
		fireTableDataChanged();
	}

	public void setOverlayValueAt(Object aValue, int row, int column) {
		overlay[row][column] = (String) aValue;
		fireTableDataChanged();
	}

	@Override
	public void onMove(int x, int y, int rotation) {
		this.robot_x = x;
		this.robot_y = y;
		this.robot_rotation = rotation;

		fireTableDataChanged();
	}

	public void reset() {
		for (int i = 0; i < this.getRowCount(); i++) {
			for (int j = 0; j < this.getColumnCount(); j++) {
				setOverlayValueAt(null, i, j);
				switch ((String) getValueAt(i, j)) {
				case "UFloor", "DFloor", "CFloor" -> {
					setValueAt("UFloor", i, j);
				}
				case "UCarpet", "DCarpet", "CCarpet" -> {
					setValueAt("UCarpet", i, j);
				}
				case "Wall", "UWall" -> {
					setValueAt("UWall", i, j);
				}
				case "UCStation", "DCStation", "CCStation" -> {
					setValueAt("UCStation", i, j);
				}
				case "BCrumb" -> {

				}
				}
			}
		}
	}

	public static void toFile(Path outputFile, FloorModel model) throws IOException {
		List<String> output = new ArrayList<>();

		int rows = model.getRowCount();
		int cols = model.getColumnCount();

		String[][] data = new String[rows][cols];

		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < cols; j++) {
				data[i][j] = (String) model.getValueAt(i, j);
			}
			output.add(Arrays.stream(data[i]).collect(Collectors.joining("\t\t\t")));
		}

		Files.write(outputFile, output);

	}

	public static FloorModel fromFile(Path inputFile) throws IOException {
		List<String> input = Files.readAllLines(inputFile);

		input.removeIf(s -> s.startsWith("#"));

		int rows = input.size();
		int cols = input.get(0).split("\\s+").length;

		String[][] data = new String[rows][cols];
		String line;

		for (int i = 0; i < rows; i++) {
			line = input.get(i);
			String[] parts = line.split("\\s+");
			for (int j = 0; j < cols; j++) {
				data[i][j] = parts[j];
			}
		}

		return new FloorModel(data);
	}

	public int getRobot_x() {
		return robot_x;
	}

	public int getRobot_y() {
		return robot_y;
	}

	public int getRobot_rotation() {
		return robot_rotation;
	}
	
	

}
