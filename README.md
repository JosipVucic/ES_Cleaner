# ES_Cleaner

ES_Cleaner is an expert system designed for simulating the behavior of a floor cleaning robot. The simulation is presented through a Java Swing GUI, providing an interactive and visual representation of the robot's cleaning capabilities. The expert system, developed in CLIPS, governs the decision-making process of the virtual cleaning robot.

## Features:

- **Java Swing GUI:**
  - Utilizes a Java Swing-based graphical user interface to simulate the floor cleaning robot's actions.

- **CLIPS Expert System:**
  - The decision-making logic for the robot is implemented using CLIPS, an expert system development tool.

## Building the Project:

To build the JAR file for ES_Cleaner, follow these steps:

1. **Include lib Folder:**
   - Ensure the `lib` folder is included in the project.

2. **CLIPSJNI.dll:**
   - The `CLIPSJNI.dll` file is required for running executable JAR files. Place it in the same folder as the JAR files.
   - Note: `CLIPSJNI.dll` comes in 32-bit and 64-bit versions. Rename the appropriate version to `CLIPSJNI.dll` based on your system architecture.

## Running the Application:

- Use the provided `run.bat` script to execute the JAR file on Windows.

## Note:

- The `CLIPSJNI.dll` file is crucial for the proper functioning of the executable JAR files. Ensure it is placed in the same directory as the JAR files.

## License:

This project is licensed under the [MIT License](LICENSE), encouraging collaboration and sharing.
