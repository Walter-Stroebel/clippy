/*
 */
package nl.wers.clippy;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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
 * <li><strong>Group Management</strong>: The main display is a JTabbedPane
 * which organizes different sets of clipboard history. Each set is represented
 * as a tab.</li>
 * <li><strong>Content Preview</strong>: Tabs are initially populated with a
 * list of selectable clipboard items, including text chunks (first few lines)
 * and images (thumbnail). The top bar is mostly context sensitive, containing
 * functions to work with (selected) items in the current tab.</li>
 * <li><strong>Item view</strong>: Any item can be expanded, using up to the
 * entire tab. For text this will be in the form of a simple editor (JTextArea).
 * Images will be a scalable image viewer.</li>
 * <li><strong>Extensibility</strong>: The design philosophy is pragmatic,
 * catering primarily to the developer's workflow. However, it's built with
 * extensibility in mind, allowing other developers to adapt or expand upon it
 * based on their requirements.</li>
 * <li><strong>Target Audience Alignment</strong>: Clippy is designed for
 * knowledgeable individuals working with or learning about LLMs in chat mode.
 * Users are assumed to have a development-like mindset or at least some
 * relevant training, as the tool's primary function is to provide a secure,
 * out-of-band mass data transfer option.</li>
 * <li><strong>Collaborative Development</strong>: In time we hope for
 * collaboration on GitHub and we encourage contributions from designers or
 * other developers.
 * </ul>
 *
 * @author Walter Stroebel
 */
public class ClippyFrame extends JFrame {

    private static final int TNAIL_SIZE = 200;
    private static final String VIEW = "View";
    public static final int TPREV_SIZE = 200;

    /**
     * Initialize the GUI.
     *
     * @param clippy application instance.
     */
    private final JTabbedPane tabbedPane;
    private final Config config;
    private File selectedFile = null;
    private final JButton itemToCB;

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
        final JCheckBox iAmSure = new JCheckBox("Sure?");
        Dimension fixedSize = new Dimension(200, 25); // Example dimensions
        groupNameField.setPreferredSize(fixedSize);
        groupNameField.setMaximumSize(fixedSize);
        toolBar.add(groupNameField);

        toolBar.add(new JButton(new AbstractAction("New Group") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                String groupName = groupNameField.getText().trim();
                if (!groupName.isEmpty()) {
                    createNewGroup(clippy, groupName);
                    groupNameField.setText(""); // Clear the input field
                }
            }
        }));
        toolBar.addSeparator();
        itemToCB = new JButton(new AbstractAction("Item -> CB") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (null != selectedFile && selectedFile.exists()) {
                    clippy.toClipboardItem(selectedFile);
                }
            }
        });
        itemToCB.setEnabled(false);
        toolBar.add(itemToCB);
        toolBar.addSeparator();
        toolBar.add(new JButton(new AbstractAction("Delete group") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                if (!iAmSure.isSelected()) {
                    JOptionPane.showMessageDialog(ClippyFrame.this, "You are not sure.");
                } else {
                    iAmSure.setSelected(false);
                    File[] all = clippy.workDir.get().listFiles();
                    if (null != all) {
                        for (File f : all) {
                            f.delete();
                        }
                    }
                    if (!clippy.workDir.get().getName().equals(Clippy.DEFAULT_GROUP)) {
                        clippy.workDir.get().delete();
                        tabbedPane.remove(tabbedPane.getSelectedIndex());
                        refreshGroupTab(new File(clippy.workDir.get().getParentFile(), Clippy.DEFAULT_GROUP));
                    }
                }
            }
        }));
        toolBar.add(iAmSure);
        getContentPane().add(toolBar, BorderLayout.NORTH);
        File[] groups = clippy.workDir.get().getParentFile().listFiles();
        if (null == groups) {
            // at the very least an array with "default" should be returned.
            Logger.getLogger(ClippyFrame.class.getName()).log(Level.SEVERE, "Logic error!");
            System.exit(0);
        }
        for (File g : groups) {
            if (g.isDirectory()) {
                addGroupTab(g);
            }
        }
        refreshGroupTab(clippy.workDir.get());
        tabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent ce) {
                JTabbedPane sourceTabbedPane = (JTabbedPane) ce.getSource();
                int selectedIndex = sourceTabbedPane.getSelectedIndex();
                String title = sourceTabbedPane.getTitleAt(selectedIndex);
                if (!title.equals(VIEW)) {
                    clippy.workDir.set(new File(clippy.workDir.get().getParentFile(), title));
                }
            }
        });
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

    private long fileToTimestamp(File t) {
        long ft1;
        try {
            String s = t.getName();
            s = s.substring(0, s.indexOf('.'));
            ft1 = Long.parseLong(s);
        } catch (Exception any) {
            // it is not a timestamp
            ft1 = t.lastModified();
        }
        return ft1;
    }

    private String bestFileToTimeLabel(File t) {
        long ft = fileToTimestamp(t);
        return String.format("%1$tF %1$tT", ft);
    }

    public final void refreshGroupTab(File group) {
        int index = tabbedPane.indexOfTab(group.getName());
        if (index != -1) {
            tabbedPane.remove(index);
        }
        addGroupTab(group);
        tabbedPane.setSelectedIndex(tabbedPane.getComponentCount() - 1);
    }

    private void addGroupTab(File g) {
        Box groupPanel = Box.createVerticalBox();
        JScrollPane sPane = new JScrollPane(groupPanel);
        tabbedPane.addTab(g.getName(), sPane);
        File[] files = g.listFiles();
        if (files != null) {
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File t, File t1) {
                    long ft1 = fileToTimestamp(t);
                    long ft2 = fileToTimestamp(t1);
                    // reverse sort by age
                    return -Long.compare(ft1, ft2);
                }
            });
            ButtonGroup bGrp = new ButtonGroup();
            for (File file : files) {
                if (groupPanel.getComponentCount() > 0) {
                    groupPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
                }
                Box line = Box.createHorizontalBox();
                JToggleButton bt = new JToggleButton(new AbstractAction(bestFileToTimeLabel(file)) {
                    @Override
                    public void actionPerformed(ActionEvent ae) {
                        Object source = ae.getSource();
                        if (source instanceof JToggleButton) {
                            JToggleButton bt = (JToggleButton) source;
                            if (bt.isSelected()) {
                                for (int i = 0; i < tabbedPane.getComponentCount(); i++) {
                                    if (tabbedPane.getTitleAt(i).equals(VIEW)) {
                                        tabbedPane.remove(i);
                                        break;
                                    }
                                }
                                selectedFile = new File(bt.getName());
                                if (!selectedFile.exists()) {
                                    selectedFile = null;
                                    itemToCB.setEnabled(false);
                                } else {
                                    if (selectedFile.getName().endsWith(".png")) {
                                        itemToCB.setEnabled(false);
                                        tabbedPane.add(VIEW, new ImageViewer(selectedFile).getViewPanel());
                                        tabbedPane.setSelectedIndex(tabbedPane.getComponentCount() - 1);
                                    } else {
                                        itemToCB.setEnabled(true);
                                        try {
                                            String content = new String(Files.readAllBytes(selectedFile.toPath()), StandardCharsets.UTF_8);
                                            JTextArea ta = new JTextArea();
                                            ta.setText(content);
                                            tabbedPane.add(VIEW, new JScrollPane(ta));
                                            tabbedPane.setSelectedIndex(tabbedPane.getComponentCount() - 1);
                                        } catch (IOException ex) {
                                            Logger.getLogger(Clippy.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    }
                                }
                            }
                        }
                    }
                });
                bt.setName(file.getAbsolutePath());
                bGrp.add(bt);
                line.add(bt);
                // Handling PNG files
                if (file.getName().toLowerCase().endsWith(".png")) {
                    ImageIcon icon = new ImageIcon(file.getAbsolutePath()); // Load the image
                    JLabel imgLabel = new JLabel(bestFileToTimeLabel(file),
                            new ImageIcon(scaleMax(icon.getImage(), TNAIL_SIZE, TNAIL_SIZE, Color.darkGray)),
                            SwingConstants.HORIZONTAL);
                    imgLabel.setName(file.getName()); // Set file name as the component name for later retrieval
                    line.add(imgLabel);
                } else { // Handling text files
                    try {
                        String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                        if (content.length() > 500) { // Truncate after X characters for display purposes
                            content = content.substring(0, TPREV_SIZE) + "...";
                        }
                        JTextArea textArea = new JTextArea(content);
                        textArea.setEditable(false);
                        textArea.setName(file.getName()); // Set file name as the component name for later retrieval
                        line.add(textArea);
                    } catch (IOException ex) {
                        Logger.getLogger(ClippyFrame.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                line.add(Box.createHorizontalGlue());
                groupPanel.add(line);
            }
            sPane.getViewport().setViewPosition(new Point());
        }
    }

    /**
     * Scale this image keeping ratio to maximum width or height.This assumes
     * the output should fit in a rectangle and returns the "biggest" image
     * possible on a canvas filled with the background color.
     *
     * @param image Image to scale.
     * @param nw The maximum width or height.
     * @param nh The maximum height.
     * @param backGround If the resulting image is smaller then the requested
     * size, center it on a canvas filled with this color.
     * @return Scaled image.
     */
    public BufferedImage scaleMax(Image image, int nw, int nh, Color backGround) {
        int rw = nw;
        int rh = nh;
        double wr = (double) getWidth() / (double) rw;
        double hr = (double) getHeight() / (double) rh;
        if (wr > hr) {
            rh = (int) Math.round(getHeight() / wr);
        } else {
            rw = (int) Math.round(getWidth() / hr);
        }
        ImageIcon ii = new ImageIcon(image.getScaledInstance(rw, rh, Image.SCALE_FAST));

        BufferedImage ret = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D gr = ret.createGraphics();
        gr.setColor(backGround);
        gr.fillRect(0, 0, nw, nh);
        int x = (nw - rw) / 2;
        int y = (nh - rh) / 2;
        gr.drawImage(ii.getImage(), x, y, null);
        gr.dispose();
        return ret;
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
        refreshGroupTab(clippy.workDir.get());
    }
}
