package nl.infcomtec.simpleimage;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;

/**
 * Simple image viewer with drag and scale.
 *
 * @author Walter Stroebel
 */
public class ImageViewer {

    private static final int MESSAGE_HEIGHT = 200;
    private static final int MESSAGE_WIDTH = 800;
    public List<Component> tools;

    protected ImageObject imgObj;
    private LUT lut;

    public ImageViewer(ImageObject imgObj) {
        this.imgObj = imgObj;
    }

    public ImageViewer(Image image) {
        imgObj = new ImageObject(image);
    }

    public ImageViewer(File f) {
        try {
            imgObj = new ImageObject(ImageIO.read(f));
        } catch (Exception ex) {
            showError(f);
        }
    }

    /**
     * Although there are many reasons an image may not load, all we can do
     * about it is tell the user by creating an image with the failure.
     *
     * @param f File we tried to load.
     */
    private void showError(File f) {
        BufferedImage img = new BufferedImage(MESSAGE_WIDTH, MESSAGE_HEIGHT, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D gr = img.createGraphics();
        String msg1 = "Cannot open image:";
        String msg2 = f.getAbsolutePath();
        Font font = gr.getFont();
        float points = 24;
        int w, h;
        do {
            font = font.deriveFont(points);
            gr.setFont(font);
            FontMetrics metrics = gr.getFontMetrics(font);
            w = Math.max(metrics.stringWidth(msg1), metrics.stringWidth(msg2));
            h = metrics.getHeight() * 2; // For two lines of text
            if (w > MESSAGE_WIDTH - 50 || h > MESSAGE_HEIGHT / 3) {
                points--;
            }
        } while (w > MESSAGE_WIDTH || h > MESSAGE_HEIGHT / 3);
        gr.drawString(msg1, 50, MESSAGE_HEIGHT / 3);
        gr.drawString(msg2, 50, MESSAGE_HEIGHT / 3 * 2);
        gr.dispose();
        imgObj = new ImageObject(img);
    }

    public synchronized ImageViewer addShadowView() {
        ButtonGroup bg = new ButtonGroup();
        addChoice(bg, new AbstractAction("Dark") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                lut = LUT.darker();
                imgObj.putImage(null);
            }
        });
        addChoice(bg, new AbstractAction("Normal") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                lut = LUT.unity();
                imgObj.putImage(null);
            }
        });
        addChoice(bg, new AbstractAction("Bright") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                lut = LUT.brighter();
                imgObj.putImage(null);
            }
        });
        addChoice(bg, new AbstractAction("Brighter") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                lut = LUT.sqrt(0);
                imgObj.putImage(null);
            }
        });
        addChoice(bg, new AbstractAction("Extreme") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                lut = LUT.sqrt2();
                imgObj.putImage(null);
            }
        });
        return this;
    }

    /**
     * Add a choice,
     *
     * @param group Only one can be active.
     * @param action Choice.
     */
    public synchronized ImageViewer addChoice(ButtonGroup group, Action action) {
        if (null == tools) {
            tools = new LinkedList<>();
        }
        JCheckBox button = new JCheckBox(action);
        group.add(button);
        tools.add(button);
        return this;
    }

    public synchronized ImageViewer addButton(Action action) {
        if (null == tools) {
            tools = new LinkedList<>();
        }
        tools.add(new JButton(action));
        return this;
    }

    /**
     * Show the image in a JPanel component with simple pan(drag) and zoom(mouse
     * wheel).
     *
     * @return the JPanel.
     */
    public JPanel getScalePanPanel() {
        final JPanel ret = new JPanelImpl();
        return ret;
    }

    /**
     * Show the image in a JPanel component with simple pan(drag) and zoom(mouse
     * wheel). Includes a JToolbar if tools are provided, see add functions.
     *
     * @return the JPanel.
     */
    public JPanel getScalePanPanelTools() {
        final JPanel outer = new JPanel();
        outer.setLayout(new BorderLayout());
        outer.add(new JPanelImpl(), BorderLayout.CENTER);
        if (null != tools) {
            JToolBar tb = new JToolBar();
            for (Component c : tools) {
                tb.add(c);
            }
            outer.add(tb, BorderLayout.NORTH);
        }
        return outer;
    }

    /**
     * Show the image in a JFrame component with simple pan(drag) and zoom(mouse
     * wheel). Includes a JToolbar if tools are provided, see add functions.
     *
     * @return the JFrame.
     */
    public JFrame getScalePanFrame() {
        final JFrame ret = new JFrame();
        ret.setAlwaysOnTop(true);
        ret.getContentPane().setLayout(new BorderLayout());
        ret.getContentPane().add(new JPanelImpl(), BorderLayout.CENTER);
        if (null != tools) {
            JToolBar tb = new JToolBar();
            for (Component c : tools) {
                tb.add(c);
            }
            ret.getContentPane().add(tb, BorderLayout.NORTH);
        }
        ret.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        ret.pack();
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                ret.setVisible(true);
            }
        });
        return ret;
    }

    public ImageObject getImageObject() {
        return imgObj;
    }

    private class JPanelImpl extends JPanel {

        int ofsX = 0;
        int ofsY = 0;
        double scale = 1;
        private BufferedImage dispImage = null;

        public JPanelImpl() {
            MouseAdapter ma = new MouseAdapter() {
                private int lastX, lastY;

                @Override
                public void mousePressed(MouseEvent e) {
                    lastX = e.getX();
                    lastY = e.getY();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    imgObj.forwardMouse(ImageObject.MouseEvents.clicked, e);
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    ofsX += e.getX() - lastX;
                    ofsY += e.getY() - lastY;
                    lastX = e.getX();
                    lastY = e.getY();
                    repaint(); // Repaint the panel to reflect the new position
                }

                @Override
                public void mouseWheelMoved(MouseWheelEvent e) {
                    int notches = e.getWheelRotation();
                    scale += notches * 0.1; // Adjust the scaling factor
                    if (scale < 0.1) {
                        scale = 0.1; // Prevent the scale from becoming too small
                    }
                    dispImage = null;
                    repaint(); // Repaint the panel to reflect the new scale
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
            addMouseWheelListener(ma);
            imgObj.addListener(new ImageObject.ImageObjectListener("Repaint") {
                @Override
                public void imageChanged(ImageObject imgObj) {
                    dispImage = null;
                    repaint(); // Repaint the panel to reflect any changes
                }
            });
            Dimension dim = new Dimension(imgObj.getWidth(), imgObj.getHeight());
            setPreferredSize(dim);
        }

        @Override
        public void paint(Graphics g) {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(0, 0, getWidth(), getHeight());
            if (null == dispImage) {
                int scaledWidth = (int) (imgObj.getWidth() * scale);
                int scaledHeight = (int) (imgObj.getHeight() * scale);
                dispImage = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = dispImage.createGraphics();
                g2.drawImage(imgObj.getScaledInstance(scaledWidth, scaledHeight, BufferedImage.SCALE_FAST), 0, 0, null);
                g2.dispose();
                if (null != lut) {
                    dispImage = lut.apply(dispImage);
                }
            }
            g.drawImage(dispImage, ofsX, ofsY, null);
        }
    }
}
