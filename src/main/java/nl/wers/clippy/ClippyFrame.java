/*
 */
package nl.wers.clippy;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JToolBar;

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

    public ClippyFrame(final Clippy clippy) {
        setTitle("Clippy");

        // Set JFrame to fullscreen
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        // Initialize JTabbedPane for groups
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
        addGroupTab("Sample Group", clippy);
        // In your GUI initialization method, add the mouse listener to the parent container
        tabbedPane.addMouseListener(new ItemMouseListener(tabbedPane));
    }

    private void handleSelection(Component comp) {
        // TODO Your selection logic here
        // For instance, if 'comp' is a JTextArea, you can get its text:
        // String selectedText = ((JTextArea) comp).getText();
    }

    private void handleHide(Component comp) {
        // TODO Your hide logic here
        // For instance, if each component has a name corresponding to its underlying file:
        // String filename = comp.getName();
        // Rename the file with the 'hide_' prefix
    }

    private void addGroupTab(String groupName, Clippy clippy) {
        JPanel groupPanel = new JPanel(new BorderLayout());

        // Inner JToolBar for the group
        JToolBar groupToolBar = new JToolBar();
        JButton copyButton = new JButton(new AbstractAction("Copy") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                // Placeholder: Copy action for this group
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
                // Check if the file is a text or image
                if (file.getName().endsWith(".PNG")) {
                    // TODO: Create an image thumbnail and add it to the panel
                } else {
                    // TODO: Read the text file, truncate if necessary, and add a JTextArea to the panel
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

        public ItemMouseListener(Container cont) {
            this.cont = cont;
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            // Iterate over all components in the parent container
            for (Component comp : cont.getComponents()) { // Assuming 'centerPanel' is the parent container
                if (comp.getBounds().contains(e.getPoint())) {
                    // Handle left-click
                    if (e.getButton() == MouseEvent.BUTTON1) {
                        // Selection logic here
                        // For instance, if you have a method to handle selection:
                        // handleSelection(comp);
                    } // Handle right-click
                    else if (e.getButton() == MouseEvent.BUTTON3) {
                        // Hide logic here
                        // For instance, if you have a method to handle hiding:
                        // handleHide(comp);
                    }
                    break; // Exit loop once the clicked component is found
                }
            }
        }
    }
}
