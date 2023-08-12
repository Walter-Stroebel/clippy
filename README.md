# Project: Clippy
A utility to assist with clipboard management and other functionalities.

## Language
Java 7 (with JRE 11)

## Key Features and Progress

### System Tray Integration
- Clippy resides in the system tray with a custom icon.
- Basic menu with an "Exit" option.

### Server Socket
- A server socket is initialized on a specific port (25432) to detect if another instance of Clippy is running.
- If another instance is detected, a JOptionPane message is displayed, and the program exits.

### Clipboard Monitoring
- Clippy monitors the system clipboard for changes.
- Detects and stores new text and image data from the clipboard.
- Uses an AtomicReference for storing the latest clipboard text and another for the latest data received from the server socket.

### Data Storage
- New text data from the clipboard is written to a file with a random UUID filename.
- New image data from the clipboard is saved as a PNG using ImageIO.

### Grouping Mechanism
- Data is stored in a directory structure based on "groups".
- Users can create a new group or select an existing group.
- The working directory (workDir) is an AtomicReference pointing to the current group directory.

### UML Diagramming
- Discussed the potential of integrating Clippy with PlantUML.
- Outlined a process where UML text can be copied to the clipboard, and Clippy automatically generates and displays the corresponding UML diagram.

