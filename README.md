# Project: Clippy
A robust utility crafted to facilitate clipboard management, providing a blend of standard and innovative features. 
The project sprouted from an interactive session with GPT-4 and matured into a versatile toolkit.

## Language
Java 7 (with JRE 11)

## Key Features and Progress

### System Tray Integration
- Resides in the system tray with a distinct icon.
- Features a minimalistic menu with an "Exit" functionality.

### Server Socket
- Initializes a server socket on port 25432 to ascertain if another Clippy instance is active.
- On detecting a duplicate instance, Clippy alerts the user with a JOptionPane message and gracefully terminates.
- The main purpose of the server socket is to allow the user to push extra content, for instance "df|nc 0 25432". 

### Clipboard Monitoring
- Vigilantly observes the system clipboard for alterations.
- Efficiently captures and archives new text and image content from the clipboard.
- Utilizes two separate AtomicReferences: one for the latest clipboard text and the other for the freshest data received from the server socket.

### Data Storage
- Clipboard text is diligently archived into a file with a unique UUID as its name.
- Clipboard images are seamlessly saved in PNG format using the proficient ImageIO.

### Grouping Mechanism
- Organizes data meticulously within a directory structure based on distinct "groups".
- Empowers users to either sculpt a new group or navigate through existing ones.
- The 'workDir' (working directory) is an AtomicReference pointing towards the currently active group directory.

### GUI Interaction
- Clippy's main GUI is primarily maximized for a holistic view, but users can customize its placement.
- The GUI state (size, location, and maximized status) is preserved across sessions.

### UML Diagramming
- Pondered the integration of Clippy with the dynamic PlantUML.
- Devised a streamlined process where UML diagrams are auto-generated by Clippy upon detecting relevant text in the clipboard.

### Development Odyssey
Our developmental trajectory was an amalgamation of iterative decisions and consistent collaboration. A snapshot of our journey:

1. **Genesis**: Initiated with Clippy anchored in the system tray, keenly monitoring clipboard alterations.
2. **Server Socket Implementation**: Infused a server socket mechanism to ensure the singularity of Clippy instances.
3. **Data Archival**: Conceived a mechanism to seamlessly store clipboard data into structured files.
4. **UML Fusion**: Envisaged the potential of UML in the developmental phase, leading to an integrated PlantUML.
5. **User Interactions Augmented**: Leveraged JOptionPane for interactive sessions, enabling users to define filenames and select between PNG and ASCII for UML visualizations.
6. **Refinement**: Incessantly refined the codebase to elevate its architecture and maintainability quotient.

Clippy stands as a paradigm of the prowess of collective efforts and evolutionary development, metamorphosing a nascent idea into an indispensable developer's companion.
