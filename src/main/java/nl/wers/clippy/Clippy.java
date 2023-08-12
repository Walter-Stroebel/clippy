package nl.wers.clippy;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
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
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.Timer;

public class Clippy {

    // Setting up the home directory constant
    public static final File HOME_DIRECTORY = new File(System.getProperty("user.home"));
    private static final int PORT = 25432;

    public static void main(String[] args) {
        if (!SystemTray.isSupported()) {
            System.out.println("SystemTray is not supported");
            return;
        }
        final Clippy instance = new Clippy();
        instance.init();
    }

    private final AtomicReference<String> latestData = new AtomicReference<>();
    private final AtomicReference<String> lastClipboardText = new AtomicReference<>();

    private final AtomicReference<File> workDir = new AtomicReference<>(initializeWorkDir());

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
        // Add components to popup menu using ActionListener
        MenuItem newGroupItem = new MenuItem("New Group");
        newGroupItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                createNewGroup();
            }
        });
        popup.add(newGroupItem);

        MenuItem selectGroupItem = new MenuItem("Select Group");
        selectGroupItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectExistingGroup();
            }
        });
        popup.add(selectGroupItem);

        trayIcon.setPopupMenu(popup);

        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            System.out.println("TrayIcon could not be added.");
        }
        initializeClipboardMonitor();
    }

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

    private void placeOnClipboard(String... texts) {
        StringBuilder combinedText = new StringBuilder();
        for (String text : texts) {
            combinedText.append(text);
        }
        String finalText = combinedText.toString();
        lastClipboardText.set(finalText);  // Set the lastClipboardText to avoid re-processing

        // Now, place the finalText on the clipboard
        StringSelection selection = new StringSelection(finalText);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
    }

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

    private void initializeClipboardMonitor() {
        final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Timer timer = new Timer(1000, new ClipboardMonitor(clipboard));
        timer.start();
    }

    private void createNewGroup() {
        String groupName = JOptionPane.showInputDialog(null, "Enter the name for the new group:", "New Group", JOptionPane.PLAIN_MESSAGE);
        if (groupName != null && !groupName.trim().isEmpty()) {
            File newGroupDir = new File(workDir.get().getParentFile(), groupName);
            if (!newGroupDir.exists()) {
                newGroupDir.mkdir();
            }
            workDir.set(newGroupDir);
        }
    }

    private void selectExistingGroup() {
        JFileChooser chooser = new JFileChooser(workDir.get().getParentFile());
        chooser.setDialogTitle("Select Group");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
            workDir.set(chooser.getSelectedFile());
        }
    }

    class ClipboardMonitor implements ActionListener {

        private final Clipboard clipboard;

        public ClipboardMonitor(Clipboard clipboard) {
            this.clipboard = clipboard;
        }
        private int lastImageHash;

        @Override
        public void actionPerformed(ActionEvent e) {
            Transferable contents = clipboard.getContents(null);
            if (contents != null) {
                // Handle text data
                if (contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    try {
                        String currentText = (String) contents.getTransferData(DataFlavor.stringFlavor);
                        if (!currentText.equals(lastClipboardText.get())) {
                            lastClipboardText.set(currentText);

                            // Check for plantUML content
                            if (currentText.startsWith("@startuml")) {
                                handlePlantUML(lastClipboardText.get());
                            } else {
                                File outputFile = new File(workDir.get(), UUID.randomUUID().toString() + ".txt");
                                // Save the current text to the new file
                                try ( FileWriter writer = new FileWriter(outputFile)) {
                                    writer.write(currentText);
                                } catch (IOException ex) {
                                    Logger.getLogger(Clippy.class.getName()).log(Level.SEVERE, null, ex);
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
                            File imageFile = new File(workDir.get(), UUID.randomUUID().toString() + ".png");
                            ImageIO.write(currentImage, "png", imageFile);
                        }
                    } catch (Exception ex) {
                        Logger.getLogger(Clippy.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }

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
                }

            } catch (Exception e) {
                Logger.getLogger(Clippy.class.getName()).log(Level.SEVERE, null, e);
            }
        }

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

        private BufferedImage resizeImage(BufferedImage originalImage, int width, int height) {
            BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = resized.createGraphics();
            g2d.drawImage(originalImage.getScaledInstance(width, height, Image.SCALE_SMOOTH), 0, 0, null);
            g2d.dispose();
            return resized;
        }

        private int getImageHash(BufferedImage image) {
            BufferedImage smallImage = resizeImage(image, 64, 64); // Resize for faster hashing
            return Arrays.hashCode(smallImage.getRGB(0, 0, smallImage.getWidth(), smallImage.getHeight(), null, 0, smallImage.getWidth()));
        }
    }

}
