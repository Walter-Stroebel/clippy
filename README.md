# Project: Clippy
A utility to assist with clipboard management and other functionalities. Developed collaboratively in an interactive session with GPT-4, Clippy evolved from a simple clipboard monitor to a versatile tool with multiple features.

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

### Development Journey
Our development process was iterative and collaborative. Here's a brief overview:

1. **Initial Setup**: We started with a basic setup where Clippy resided in the system tray and monitored the clipboard for changes.
2. **Server Socket Integration**: To ensure only one instance of Clippy runs, we integrated a server socket mechanism.
3. **Data Storage**: We introduced a mechanism to store clipboard data into files, organized into groups.
4. **UML Integration**: Recognizing the potential of UML in development, we integrated PlantUML. This allowed Clippy to generate UML diagrams from text copied to the clipboard.
5. **User Interaction**: To enhance user experience, we used JOptionPane for interactions, allowing users to specify filenames and choose between PNG and ASCII outputs for UML.
6. **Refactoring**: Throughout the process, we continuously refactored the code to improve its structure and maintainability.

This project is a testament to the power of collaboration and iterative development. It evolved from a simple idea into a tool that can aid developers in their daily tasks.
