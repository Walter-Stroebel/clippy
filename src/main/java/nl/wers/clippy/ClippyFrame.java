/*
 */
package nl.wers.clippy;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;

/**
 * ClippyFrame represents the main GUI for the Clippy application, providing an
 * interface for users to manage and access clipboard history.
 *
 * <p>
 * Design Considerations:</p>
 * <ul>
 * <li><strong>Interface</strong>: The application is designed to operate in
 * fullscreen mode, maximizing the use of screen real estate. This is especially
 * beneficial for users with multi-screen setups. The interface can be easily
 * hidden and restored using standard window controls.</li>
 * <li><strong>Toolbar</strong>: A JToolBar replaces the traditional menu bar,
 * providing direct access to primary actions. Each tab (representing a group)
 * also contains an inner JToolBar for actions specific to that group.</li>
 * <li><strong>Group Management</strong>: The JTabbedPane organizes groups,
 * allowing users to easily switch between different sets of clipboard history.
 * Each group is represented as a tab.</li>
 * <li><strong>Content Preview</strong>: Tabs are populated with previews of
 * clipboard items, including text chunks and images. Large text blocks are
 * truncated for readability, and images are displayed as thumbnails. The design
 * is responsive, adapting based on screen resolution to display more or larger
 * blocks.</li>
 * <li><strong>Interaction</strong>: Left-clicking a clipboard item selects it,
 * while right-clicking hides it. Hidden items are managed by prefixing the
 * underlying file with "hide_". An option in the toolbar allows users to toggle
 * the visibility of hidden items.</li>
 * <li><strong>Extensibility</strong>: The design philosophy is pragmatic,
 * catering primarily to the developer's workflow. However, it's built with
 * extensibility in mind, allowing other developers to adapt or expand upon it
 * based on their requirements.</li>
 * </ul>
 *
 * @author Walter Stroebel
 */
public class ClippyFrame extends JFrame {

    /**
     * Initialize the GUI.
     *
     * @param clippy application instance.
     */
    private final JTabbedPane tabbedPane;
    private Config config;

    public ClippyFrame(final Clippy clippy) {
        setTitle("Clippy");

        // Initialize Config
        config = Config.getInstance(clippy);

        // Set JFrame based on saved properties or defaults
        int x = config.getGuiX();
        int y = config.getGuiY();
        int width = config.getGuiWidth();
        int height = config.getGuiHeight();
        setBounds(x, y, width, height);
        setAlwaysOnTop(true);
        // Check if the window was maximized last session and set accordingly
        boolean wasMaximized = config.getMaximized();
        System.out.format("Showing GUI at %d,%d as %dx%d; max=%s\n", x, y, width, height, wasMaximized);
        if (wasMaximized) {
            setExtendedState(JFrame.MAXIMIZED_BOTH);
        }

        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

        // Initialize JTabbedPane for groups
        // Add listeners to update Config when the window is resized, moved or state changed
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                updateConfig();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
                updateConfig();
            }
        });

        addWindowStateListener(new WindowStateListener() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                updateConfig();
            }
        });
        tabbedPane = new JTabbedPane();
        getContentPane().add(tabbedPane, BorderLayout.CENTER);

        // Initialize JToolBar for primary actions
        JToolBar toolBar = new JToolBar();
        // Input field for new group name
        final JTextField groupNameField = new JTextField(15); // 15 columns wide
        Dimension fixedSize = new Dimension(200, 25); // Example dimensions
        groupNameField.setPreferredSize(fixedSize);
        groupNameField.setMaximumSize(fixedSize);
        toolBar.add(groupNameField);

        JButton newGroupButton = new JButton(new AbstractAction("New Group") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                String groupName = groupNameField.getText().trim();
                if (!groupName.isEmpty()) {
                    createNewGroup(clippy, groupName);
                    groupNameField.setText(""); // Clear the input field
                }
            }
        });
        toolBar.add(newGroupButton);

        getContentPane().add(toolBar, BorderLayout.NORTH);
        File[] groups = clippy.workDir.get().getParentFile().listFiles();
        if (null == groups) {
            // at the very least an array with "default" should be returned.
            Logger.getLogger(ClippyFrame.class.getName()).log(Level.SEVERE, "Logic error!");
            System.exit(0);
        }
        for (File g : groups) {
            if (g.isDirectory()) {
                addGroupTab(g.getName(), clippy);
            }
        }
        // In your GUI initialization method, add the mouse listener to the parent container
        tabbedPane.addMouseListener(new ItemMouseListener(clippy, tabbedPane));
    }

    private void updateConfig() {
        // Save the size and position to Config
        config.setGuiX(getX());
        config.setGuiY(getY());
        config.setGuiWidth(getWidth());
        config.setGuiHeight(getHeight());

        // Save the maximized state to Config
        boolean isMaximized = (getExtendedState() & JFrame.MAXIMIZED_BOTH) != 0;
        config.setMaximized(isMaximized);
    }

    private void addGroupTab(String groupName, Clippy clippy) {
        JPanel groupPanel = new JPanel(new BorderLayout());

        // Inner JToolBar for the group
        JToolBar groupToolBar = new JToolBar();
        JButton copyButton = new JButton(new AbstractAction("Copy") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                // TODO copy the selected item, if any, to the clipboard.
            }
        });
        groupToolBar.add(copyButton);
        groupPanel.add(groupToolBar, BorderLayout.NORTH);

        // Initialize the content preview panel for the group
        JPanel contentPreviewPanel = new JPanel();
        populateContentPreviewPanel(contentPreviewPanel, new File(clippy.workDir.get(), groupName)); // Populate the panel with previews
        groupPanel.add(contentPreviewPanel, BorderLayout.CENTER);

        tabbedPane.addTab(groupName, groupPanel);
    }

    /**
     * Populates the given panel with previews of clipboard items from the
     * specified directory.
     *
     * @param panel The panel to populate.
     * @param groupDir The directory containing the clipboard items.
     */
    private void populateContentPreviewPanel(JPanel panel, File groupDir) {
        File[] files = groupDir.listFiles();
        if (files != null) {
            for (File file : files) {
                // Skip hidden files
                if (file.getName().startsWith("hide_")) {
                    continue;
                }

                // Handling PNG files
                if (file.getName().toLowerCase().endsWith(".png")) {
                    ImageIcon icon = new ImageIcon(file.getAbsolutePath()); // Load the image
                    Image img = icon.getImage().getScaledInstance(100, 100, Image.SCALE_FAST); // Create a thumbnail of fixed size, say 100x100
                    JLabel imgLabel = new JLabel(new ImageIcon(img));
                    imgLabel.setName(file.getName()); // Set file name as the component name for later retrieval
                    panel.add(imgLabel);
                } else { // Handling text files
                    try {
                        String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                        if (content.length() > 500) { // Truncate after 500 characters for display purposes
                            content = content.substring(0, 500) + "...";
                        }
                        JTextArea textArea = new JTextArea(content);
                        textArea.setEditable(false);
                        textArea.setName(file.getName()); // Set file name as the component name for later retrieval
                        panel.add(textArea);
                    } catch (IOException ex) {
                        Logger.getLogger(ClippyFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        }
    }

    /**
     * Creates a new group (directory) under the ".clippy" directory.
     */
    void createNewGroup(final Clippy clippy, String groupName) {
        File newGroupDir = new File(clippy.workDir.get().getParentFile(), groupName);
        if (!newGroupDir.exists()) {
            newGroupDir.mkdir();
        }
        clippy.workDir.set(newGroupDir);
    }

    private class ItemMouseListener extends MouseAdapter {

        private final Container cont;
        private final Clippy clippy;

        public ItemMouseListener(Clippy clippy, Container cont) {
            this.clippy = clippy;
            this.cont = cont;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            // Iterate over all components in the parent container
            for (Component comp : cont.getComponents()) { // Assuming 'centerPanel' is the parent container
                if (comp.getBounds().contains(e.getPoint())) {
                    // Handle left-click
                    if (SwingUtilities.isLeftMouseButton(e)) {
                        clippy.toClipboardItem(comp.getName());
                    } else if (SwingUtilities.isRightMouseButton(e)) {
                        clippy.hideItem(comp.getName());
                    }
                    break; // Exit loop once the clicked component is found
                }
            }
        }
    }
}
