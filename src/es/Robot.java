package es;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import net.sf.clipsrules.jni.CLIPSException;
import net.sf.clipsrules.jni.Environment;
import net.sf.clipsrules.jni.FactInstance;
import net.sf.clipsrules.jni.SlotValue;

public class Robot {
	private static final int FLOOR_COST = 1;
	private static final int CARPET_COST = 2;
	
	private int autoSpeed = 1;
	
	private FloorModel model;
	private int x, y;
	private int dirtyCount;
	private int rotation;
	
	private List<RobotMovementListener> movementListeners = new ArrayList<>();
	private List<RobotBatteryListener> batteryListeners = new ArrayList<>();

	private boolean isOn;
	private boolean isCleaning;
	private boolean isReturning;
	private boolean isCharging;

	private int battery;

	private Environment clips;

	private static class Node {
		private int x, y;
		private Node parent;

		public Node(int x, int y, Node parent) {
			this.x = x;
			this.y = y;
			this.parent = parent;
		}

		public Node(int x, int y) {
			this(x, y, null);
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + x;
			result = prime * result + y;
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (!(obj instanceof Node))
				return false;
			Node other = (Node) obj;
			if (x != other.x)
				return false;
			if (y != other.y)
				return false;
			return true;
		}

	}

	public Robot(FloorModel model) {
		this.model = model;

		reset();

		this.clips = CLIPSUtil.initCLIPS(this);
	}

	public void auto() throws CLIPSException {
		do {
			nextStep();
			try {
				TimeUnit.MILLISECONDS.sleep(1000/autoSpeed);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		} while (isOn);
	}

	public void nextStep() throws CLIPSException {
		clips.reset();

		isCleaning = true;
		readX(rotation + 0, "F");
		readX(rotation + 180, "B");
		readX(rotation + 90, "L");
		readX(rotation - 90, "R");
		applyReading("C", x, y);
		applyState();

		clips.run();
	}

	public void handleTrap() {
		if (dirtyCount > 0 && battery > 10) {
			findDirtyTile();
		} else {
			if (battery <= 10)
				System.out.println("Low battery!");
			if (dirtyCount == 0)
				System.out.println("Finished cleaning!");
			findChargingStation();
		}
	}

	public void findChargingStation() {
		System.out.println("Searching for charging station.");

		List<String> goal = new ArrayList<>();
		goal.add("CCStation");
		goal.add("DCStation");
		isReturning = true;

		depositBreadcrumbs(goal);
	}

	public void findDirtyTile() {
		System.out.println("Searching for dirty surface.");

		List<String> goal = new ArrayList<>();
		goal.add("DFloor");
		goal.add("DCarpet");
		goal.add("DCStation");

		depositBreadcrumbs(goal);
	}

	private void depositBreadcrumbs(List<String> goal) {
		Node n = bfs(goal);
		if (n == null) {
			System.out.println("Search failed, shutting down.");
			isOn = false;
		} else {
			while (n.parent != null) {
				n = n.parent;
				model.setOverlayValueAt("BCrumb", n.x, n.y);
			}
		}

		isCleaning = false;
	}

	private Node bfs(List<String> goal) {
		List<Node> open = new LinkedList<>();
		Set<Node> visited = new HashSet<>();

		open.add(new Node(x, y));

		while (open.size() != 0) {
			Node n = open.remove(0);
			visited.add(n);
			if (goal.contains(model.getValueAt(n.x, n.y))) {
				return n;
			}
			for (Node m : expand(n)) {
				if (!visited.contains(m))
					open.add(m);
			}
		}
		return null;
	}

	private List<Node> expand(Node n) {
		List<Node> list = new ArrayList<>();

		list.add(tileToNode(n.x - 1, n.y, n));
		list.add(tileToNode(n.x + 1, n.y, n));
		list.add(tileToNode(n.x, n.y - 1, n));
		list.add(tileToNode(n.x, n.y + 1, n));

		list.removeAll(Collections.singleton(null));

		return list;
	}

	private Node tileToNode(int x, int y, Node n) {
		if (x < 0 || y < 0 || x >= model.getRowCount() || y >= model.getColumnCount())
			return null;

		String v = (String) model.getValueAt(x, y);

		if (v.contains("Wall") || v.startsWith("U"))
			return null;

		return new Node(x, y, n);
	}
	
	public void cleanFloor() {
		clean("Cleaning floor.", FLOOR_COST);
	}
	
	public void cleanCarpet() {
		clean("Cleaning carpet.", CARPET_COST);
	}

	private void clean(String msg, int cost) {
		System.out.println(msg);
		battery-=cost;
		
		String old = (String) model.getValueAt(x, y);
		model.setValueAt("C" + old.substring(1), x, y);
		dirtyCount--;
		
		fireBatteryListeners();
	}
	
	public void charge() {
		isReturning = false;

		if (battery < 100) {
			isCharging = true;
			System.out.println("Battery level: " + battery);
			System.out.println("Charging.");
			battery += 10;
			battery = Math.min(battery, 100);
			fireBatteryListeners();
		} else {
			isCharging = false;
			System.out.println("Battery full.");
			if (dirtyCount > 0) {
				findDirtyTile();
			} else {
				System.out.println("Task completed! Shutting down.");
				isOn = false;
			}
		}

	}

	public void fwd() {
		// fireListeners();

		switch (rotation) {
		case 90 -> x -= 1;
		case 180 -> y -= 1;
		case 270 -> x += 1;
		case 0 -> y += 1;
		default -> throw new IllegalArgumentException("Unexpected value: " + rotation);
		}
		;

		fireMovementListeners();

		System.out.println("Moving forward.");
	}

	public void rt90() {
		rotation -= 90;
		if (rotation == -90)
			rotation = 270;
		
		fireMovementListeners();

		System.out.println("Turning right.");
	}

	public void lt90() {
		rotation += 90;
		if (rotation == 360)
			rotation = 0;
		
		fireMovementListeners();

		System.out.println("Turning left.");
	}

	private void applyReading(String direction, int x, int y) throws CLIPSException {
		if (x < 0 || y < 0 || x >= model.getRowCount() || y >= model.getColumnCount()) {
			clips.assertString("(wall " + direction + ")");
			return;
		}
		String reading = (String) model.getValueAt(x, y);

		switch (reading) {
		case "UFloor", "UCarpet", "UCStation" -> {
			clips.assertString("(" + reading.substring(1) + " " + direction + ")");
			model.setValueAt("D" + reading.substring(1), x, y);
			dirtyCount++;
		}
		case "DFloor", "DCarpet", "DCStation" -> {
			clips.assertString("(" + reading.substring(1) + " " + direction + ")");
		}
		case "Wall" -> {
			clips.assertString("(wall " + direction + ")");
		}
		case "UWall" -> {
			clips.assertString("(wall " + direction + ")");
			model.setValueAt(reading.substring(1), x, y);
		}
		case "CFloor", "CCarpet", "CCStation" -> {
			clips.assertString("(" + reading.substring(1) + " " + direction + ")");
			clips.assertString("(clean " + direction + ")");
		}
		case "BCrumb" -> {
			clips.assertString("(BCrumb " + direction + ")");
			isCleaning = false;
		}
		}

	}

	private void applyState() throws CLIPSException {
		if (!isCharging) {
			if (isCleaning)
				clips.assertString("(isCleaning)");
			else
				model.setOverlayValueAt(null, x, y);
		} else {
			clips.assertString("(isCharging)");
		}
		if (isOn)
			clips.assertString("(isOn)");
		if (isReturning)
			clips.assertString("(isReturning)");
		
		if(battery <= 10)
			clips.assertString("(lowBattery)");
	}

	private void readX(int relrot, String direction) throws CLIPSException {
		switch (relrot) {
		case 90, 450 -> applyReading(direction, x - 1, y);
		case 180 -> applyReading(direction, x, y - 1);
		case 270, -90 -> applyReading(direction, x + 1, y);
		case 0, 360 -> applyReading(direction, x, y + 1);
		default -> throw new IllegalArgumentException("Unexpected value: " + rotation);
		}
		;
	}

	public void printFacts() {
		for (FactInstance f : clips.getFactList()) {
			System.out.print(f.getRelationName() + " ");
			for (SlotValue s : f.getSlotValues()) {
				System.out.print(s.getSlotValue() + " ");
			}
			System.out.println();
		}
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public boolean isOn() {
		return isOn;
	}

	public void setOn(boolean isOn) {
		this.isOn = isOn;
	}

	public void addMovementListener(RobotMovementListener listener) {
		if (listener != null)
			movementListeners.add(listener);
	}

	public void removeMovementListener(RobotMovementListener listener) {
		movementListeners.remove(listener);
	}

	public void fireMovementListeners() {
		for (RobotMovementListener l : movementListeners)
			l.onMove(x, y, rotation);
	}
	
	public void addBatteryListener(RobotBatteryListener listener) {
		if (listener != null)
			batteryListeners.add(listener);
	}

	public void removeBatteryListener(RobotBatteryListener listener) {
		batteryListeners.remove(listener);
	}

	public void fireBatteryListeners() {
		for (RobotBatteryListener l : batteryListeners)
			l.onBatteryChanged(battery);
	}

	public void reset() {
		dirtyCount = 0;
		rotation = 90;
		battery = 100;
		isOn = true;
		isCleaning = true;
		isReturning = false;
		isCharging = false;

		if (!((String) model.getValueAt(x, y)).contains("CStation"))
			full: for (int i = 0; i < model.getRowCount(); i++) {
				for (int j = 0; j < model.getColumnCount(); j++) {
					if (((String) model.getValueAt(x, y)).contains("Wall")
							&& (((String) model.getValueAt(i, j)).contains("Floor")
									|| ((String) model.getValueAt(i, j)).contains("Carpet"))) {
						this.x = i;
						this.y = j;
					}
					if (((String) model.getValueAt(i, j)).contains("CStation")) {
						this.x = i;
						this.y = j;
						break full;
					}
				}
			}
		fireMovementListeners();
		fireBatteryListeners();

		System.out.println("Robot at (" + x + ", " + y + ")");
		System.out.println("Battery level: " + battery);

	}

	public void set(int x, int y) {
		this.x = x;
		this.y = y;
		fireMovementListeners();

	}
	
	public int getAutoSpeed() {
		return autoSpeed;
	}

	public void setAutoSpeed(int autoSpeed) {
		this.autoSpeed = autoSpeed;
	}
}
