package nl.wers.clippy;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/**
 * Clippy is a system tray application designed to monitor the clipboard and
 * provide server socket functionality. It is primarily meant as an aid when
 * working with LLM's in a chat interface, using the clipboard as a out-of-bound
 * communication channel.
 *
 * <p>
 * Key Features:
 * <ul>
 * <li>Initializes a custom tray icon and adds it to the system tray.</li>
 * <li>Displays the GUI when the tray icon is left-clicked.</li>
 * <li>Manages a working directory inside the user's home directory under
 * ".clippy".</li>
 * <li>Provides functionality to place text on the clipboard.</li>
 * <li>Initializes a server socket on port 25432 bound to localhost to listen
 * for incoming connections.</li>
 * <li>Monitors the clipboard contents periodically.</li>
 * <li>Manages directories (groups) under the ".clippy" directory.</li>
 * </ul>
 * </p><p>
 * Design notes.
 * <ul>
 * <li>The designer dislikes lambda's as they hide implementation details.
 * Likewise, implicit immutability of variables leads unnatural coding as in the
 * develops mind, the variable could be modified, only discovering later that it
 * can not. So, the code base has the Java level set to 7 but of course any JRE
 * can be used. Testing was done on JRE11.</li>
 * <li>All of the application was designed and written by one developer and one
 * LLM, some inconsistency exists, live with it or make a pull request and fix
 * it :)</li>
 * <li>WIP User interface.</li>
 * </ul>
 *
 * @author Walter Stroebel
 * @version 1.0
 */
public class Clippy {

    /**
     * The home directory constant, pointing to the user's home directory.
     */
    private static final File HOME_DIRECTORY = new File(System.getProperty("user.home"));
    /**
     * The port number used for the server socket functionality.
     */
    private static final int PORT = 25432;

    /**
     * Initializes the Clippy system tray application.Checks for system tray
     * support and sets up the tray icon and its functionalities.
     *
     * @param args not used.
     */
    public static void main(String[] args) {
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray is not supported");
            return;
        }
        final Clippy instance = new Clippy();
        instance.init();
    }
    /**
     * Holds the latest data received from the server socket.
     */

    final AtomicReference<String> latestData = new AtomicReference<>();
    /**
     * Holds the last text content detected on the clipboard.
     */
    final AtomicReference<String> lastClipboardText = new AtomicReference<>(null);

    /**
     * Represents the current working directory for the application.
     */
    final AtomicReference<File> workDir = new AtomicReference<>(initializeWorkDir());
    private final ClippyFrame gui;
    /**
     * The system clipboard instance.
     */
    private final Clipboard clipboard;
    private int lastImageHash;
    private final String OUTPUT_SEPARATOR = "\n---CMD_OUTPUT_SEPARATOR---\n";

    /**
     * Constructor for the Clippy class. Initializes the GUI frame for the
     * application.
     */
    public Clippy() {
        this.gui = new ClippyFrame(this);
        clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Timer timer = new Timer(1000, new ActionListener() {

            /**
             * Checks the clipboard for changes in its content and handles them.
             *
             * @param e The action event triggering this method.
             */
            @Override
            public void actionPerformed(ActionEvent e) {
                handleClipboard();
            }
        });
        timer.start();
    }

    /**
     * Initializes the system tray icon, server socket, and clipboard monitor.
     *
     * @throws HeadlessException if GraphicsEnvironment.isHeadless() returns
     * true
     */
    private void init() throws HeadlessException {
        initializeServerSocket();

        final PopupMenu popup = new PopupMenu();

        // Creating a custom icon using BufferedImage and Graphics2D
        BufferedImage iconImage = new BufferedImage(24, 24, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = iconImage.createGraphics();

        // Drawing a dark gray filled rectangle
        g2d.setColor(Color.DARK_GRAY);
        g2d.fillRect(6, 6, 12, 12);

        // Drawing a lighter gray outlined rectangle on top
        g2d.setColor(Color.LIGHT_GRAY);
        g2d.drawRect(8, 8, 8, 8);

        g2d.dispose();
        final TrayIcon trayIcon = new TrayIcon(iconImage, "Clippy");

        final SystemTray tray = SystemTray.getSystemTray();

        // Add components to popup menu using ActionListener
        MenuItem exitItem = new MenuItem("Exit");
        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        popup.add(exitItem);

        trayIcon.setPopupMenu(popup);
        trayIcon.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    gui.setVisible(true);
                }
            }
        });

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.out.println("TrayIcon could not be added.");
        }
        initializeClipboardMonitor();
    }

    /**
     * Crude unique name generation.
     * <p>
     * Note: There's a slight risk of a race condition since this method isn't
     * thread-safe.
     *
     * @param ext The file extension to use. While ".txt" or ".png" are
     * expected, other extensions are also acceptable. A leading dot and
     * lowercase are enforced.
     * @return A sufficiently unique filename in the current working directory,
     * also known as the clipping group.
     */
    private String generateUniqueFilename(String ext) {
        long timestamp = System.currentTimeMillis();
        String saveExt = ext.startsWith(".") ? ext.toLowerCase() : "." + ext.toLowerCase();
        File file = new File(workDir.get(), timestamp + saveExt);

        while (file.exists()) {
            timestamp++;
            file = new File(workDir.get(), timestamp + saveExt);
        }

        return file.getAbsolutePath();
    }

    /**
     * Initializes the working directory for the application under the user's
     * home directory. If the directory doesn't exist, it creates one.
     *
     * @return The default working directory.
     */
    private File initializeWorkDir() {
        File homeDir = new File(HOME_DIRECTORY, ".clippy");
        if (!homeDir.exists()) {
            homeDir.mkdir();
        }

        File defaultGroup = new File(homeDir, "default");
        if (!defaultGroup.exists()) {
            defaultGroup.mkdir();
        }

        return defaultGroup;
    }

    /**
     * Places the provided texts onto the system clipboard.
     *
     * @param texts The array of texts to be placed on the clipboard.
     */
    void placeOnClipboard(String... texts) {
        StringBuilder combinedText = new StringBuilder();
        for (String text : texts) {
            combinedText.append(null == text ? "(null)" : text);
        }
        String finalText = combinedText.toString();
        System.out.println("Sending [[" + finalText + "]] to clipboard");
        lastClipboardText.set(finalText);  // Set the lastClipboardText to avoid re-processing

        // Now, place the finalText on the clipboard
        StringSelection selection = new StringSelection(finalText);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }

    /**
     * Initializes a server socket on port 25432 bound to localhost. Listens for
     * incoming connections and reads data from them.
     */
    private void initializeServerSocket() {
        try {
            final ServerSocket serverSocket = new ServerSocket(PORT, 0, InetAddress.getByName("localhost"));

            // Start the handler in a new thread
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try ( Socket clientSocket = serverSocket.accept();  BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
                            String inputLine;
                            StringBuilder receivedData = new StringBuilder();
                            while ((inputLine = in.readLine()) != null) {
                                receivedData.append(inputLine).append("\n");
                            }
                            latestData.set(receivedData.toString());

                            // Save the received data to a file
                            String filename = generateUniqueFilename(".txt");
                            File outputFile = new File(workDir.get(), filename);
                            try ( BufferedWriter out = new BufferedWriter(new FileWriter(outputFile))) {
                                out.write(receivedData.toString());
                            }
                        } catch (IOException ex) {
                            Logger.getLogger(Clippy.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }).start();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Another instance of Clippy is already running.", "Error", JOptionPane.ERROR_MESSAGE);
            System.exit(0);
        }
    }

    /**
     * Initializes a clipboard monitor that checks the clipboard contents
     * periodically.
     */
    private void initializeClipboardMonitor() {
    }

    void toClipboardItem(String name) {
        File fileToRead = new File(workDir.get(), name);
        if (fileToRead.exists() && fileToRead.isFile()) {
            try {
                String content = new String(Files.readAllBytes(fileToRead.toPath()), StandardCharsets.UTF_8);
                placeOnClipboard(content);
            } catch (IOException ex) {
                Logger.getLogger(Clippy.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            Logger.getLogger(Clippy.class.getName()).log(Level.SEVERE, "File not found: {0}", name);
        }
    }

    void hideItem(String name) {
        File originalFile = new File(workDir.get(), name);
        if (originalFile.exists() && originalFile.isFile()) {
            File hiddenFile = new File(workDir.get(), "hide_" + name);
            if (!hiddenFile.exists()) {
                boolean renamed = originalFile.renameTo(hiddenFile);
                if (!renamed) {
                    Logger.getLogger(Clippy.class.getName()).log(Level.SEVERE, "Failed to rename: {0}", name);
                }
            } else {
                System.out.println("A hidden file with the same name already exists.");
                Logger.getLogger(Clippy.class.getName()).log(Level.SEVERE, "A hidden file with the name {0} already exists.", name);
            }
        } else {
            Logger.getLogger(Clippy.class.getName()).log(Level.SEVERE, "File not found: {0}", name);
        }
    }

    public void handleClipboard() {
        Transferable contents = clipboard.getContents(null);
        if (contents != null) {
            // Handle text data
            if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                try {
                    String currentText = (String) contents.getTransferData(DataFlavor.stringFlavor);
                    // ignore context on clipboard on start-up as that can be really weird and unexpected
                    if (null == lastClipboardText.get()) {
                        lastClipboardText.set(currentText);
                    } else if (!currentText.equals(lastClipboardText.get())) {
                        lastClipboardText.set(currentText);

                        // Check for plantUML content
                        if (currentText.startsWith("@startuml")) {
                            handlePlantUML(lastClipboardText.get());
                        } else {
                            File outputFile = new File(generateUniqueFilename(".txt"));
                            // Save the current text to the new file
                            try ( FileWriter writer = new FileWriter(outputFile)) {
                                writer.write(currentText);
                            } catch (IOException ex) {
                                Logger.getLogger(Clippy.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            StringBuilder cat = null;
                            while (currentText.contains("$@")) {
                                int cmd = currentText.indexOf("$@");
                                if (cmd >= 0) {
                                    int eoc = currentText.indexOf("@$", cmd);
                                    if (eoc > cmd + 2) {
                                        if (null == cat) {
                                            cat = new StringBuilder();
                                        } else {
                                            cat.append(OUTPUT_SEPARATOR);
                                        }
                                        cat.append(handleCommand(currentText.substring(cmd + 2, eoc)));
                                        currentText = new StringBuilder(currentText).delete(cmd, eoc + 2).toString();
                                    }
                                }
                            }
                            if (null!=cat){
                                placeOnClipboard(cat.toString());
                            }
                        }
                    }
                } catch (Exception ex) {
                    Logger.getLogger(Clippy.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            // Handle image data
            if (contents.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                try {
                    BufferedImage currentImage = (BufferedImage) contents.getTransferData(DataFlavor.imageFlavor);
                    int currentHash = getImageHash(currentImage);
                    // Assuming a variable lastImageHash to store the last detected image hash
                    if (currentHash != lastImageHash) {
                        lastImageHash = currentHash;
                        File imageFile = new File(generateUniqueFilename(".png"));
                        ImageIO.write(currentImage, "png", imageFile);
                    }
                } catch (Exception ex) {
                    Logger.getLogger(Clippy.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    /**
     * Handles PlantUML content detected on the clipboard.
     *
     * @param currentText The detected PlantUML content.
     */
    private void handlePlantUML(String currentText) {
        JTextField filenameField = new JTextField(15);
        JPanel panel = new JPanel();
        panel.add(new JLabel("Filename (without extension):"));
        panel.add(filenameField);

        Object[] options = {"PNG", "ASCII", "Cancel"};
        int choice = JOptionPane.showOptionDialog(null, panel, "PlantUML", JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

        // If "Cancel" is pressed or no filename is provided
        if (choice == 2 || filenameField.getText().trim().isEmpty()) {
            return;
        }

        String filename = filenameField.getText().trim();
        String fullFilename = filename + ".txt";
        File outputFile = new File(workDir.get(), fullFilename);

        // Handle backups
        if (outputFile.exists()) {
            File backupFile = new File(workDir.get(), filename + ".bak");
            backupFile.delete(); // Delete existing backup
            outputFile.renameTo(backupFile); // Rename current file to backup
        }

        // Save the current text to the new file
        try ( FileWriter writer = new FileWriter(outputFile)) {
            writer.write(currentText);
        } catch (IOException ex) {
            Logger.getLogger(Clippy.class.getName()).log(Level.SEVERE, null, ex);
        }

        String formatFlag = (choice == 1) ? "-ttxt" : "-tpng"; // Use -ttxt for ASCII, default to -tpng

        // Run PlantUML
        try {
            ProcessBuilder pb = new ProcessBuilder("plantuml", formatFlag, outputFile.getAbsolutePath());
            pb.directory(workDir.get());
            Process process = pb.start();
            process.waitFor();

            if (choice == 1) {
                File asciiOutputFile = new File(workDir.get(), filename + ".atxt");
                String asciiContent = new String(Files.readAllBytes(asciiOutputFile.toPath()), StandardCharsets.UTF_8);
                placeOnClipboard(asciiContent);
            } else {
                File pngOutputFile = new File(workDir.get(), filename + ".png");
                displayImage(pngOutputFile);
            }

        } catch (Exception e) {
            Logger.getLogger(Clippy.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    /**
     * Displays an image in a JFrame.
     *
     * @param imageFile The file containing the image to display.
     */
    private void displayImage(File imageFile) {
        JFrame frame = new JFrame("Generated UML Diagram");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(800, 600);  // Adjust size as needed
        frame.setLocationRelativeTo(null);  // Center the frame

        ImageIcon imageIcon = new ImageIcon(imageFile.getAbsolutePath());
        JLabel label = new JLabel(imageIcon);
        JScrollPane scrollPane = new JScrollPane(label);
        frame.add(scrollPane);

        frame.setVisible(true);
    }

    /**
     * Resizes a given image to the specified width and height.
     *
     * @param originalImage The original image to resize.
     * @param width The desired width.
     * @param height The desired height.
     * @return The resized image.
     */
    private BufferedImage resizeImage(BufferedImage originalImage, int width, int height) {
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.drawImage(originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);
        g2d.dispose();
        return resized;
    }

    /**
     * Computes a hash for the given image. This is used to detect changes in
     * the image content.
     *
     * @param image The image to compute the hash for.
     * @return The computed hash value.
     */
    private int getImageHash(BufferedImage image) {
        BufferedImage smallImage = resizeImage(image, 64, 64); // Resize for faster hashing
        return Arrays.hashCode(smallImage.getRGB(0, 0, smallImage.getWidth(), smallImage.getHeight(), null, 0, smallImage.getWidth()));
    }

    private String handleCommand(final String cmdString) {
        System.out.println("Executing " + cmdString + " in " + Config.getInstance(this).getCodeBase());
        // To capture output
        final StringBuilder output = new StringBuilder();
        try {
            ProcessBuilder pb = new ProcessBuilder(Arrays.asList(cmdString.trim().split(" ")));
            pb.directory(Config.getInstance(this).getCodeBase());
            pb.redirectErrorStream(true);
            final Process process = pb.start();

            Thread outputThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try ( BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            output.append(line).append("\n");
                        }
                    } catch (IOException e) {
                        Logger.getLogger(Clippy.class.getName()).log(Level.SEVERE, null, e);
                        output.append("\nYour command ").append(cmdString).append(" caused exception ").append(e.getMessage());
                    }
                }
            });
            outputThread.start();

            if (process.waitFor(10, TimeUnit.SECONDS)) {
                outputThread.join(); // Wait for the output reading thread to finish
            } else {
                process.destroy();
                output.append("\nYour command ").append(cmdString).append(" timed out");
            }
        } catch (Exception ex) {
            Logger.getLogger(Clippy.class.getName()).log(Level.SEVERE, null, ex);
            output.append("\nYour command ").append(cmdString).append(" caused exception ").append(ex.getMessage());
        }
        return output.toString();
    }

}
