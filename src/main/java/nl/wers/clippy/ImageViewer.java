/*
 */
package nl.wers.clippy;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Simple image viewer with drag and scale.
 *
 * @author walter
 */
public class ImageViewer {

    private static final int MESSAGE_HEIGHT = 200;
    private static final int MESSAGE_WIDTH = 800;

    private ImageObject imgObj;

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

    public JPanel getScalePanPanel() {
        final JPanel ret = new JPanelImpl();
        return ret;
    }

    public JFrame getScalePanFrame() {
        final JFrame ret = new JFrame();
        ret.setAlwaysOnTop(true);
        ret.getContentPane().setLayout(new BorderLayout());
        ret.getContentPane().add(new JPanelImpl(), BorderLayout.CENTER);
        ret.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        ret.setSize(Math.min(800, imgObj.getWidth()), Math.min(600, imgObj.getHeight()));
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                ret.setVisible(true);
            }
        });
        return ret;
    }

    private class JPanelImpl extends JPanel {

        int ofsX = 0;
        int ofsY = 0;
        double scale = 1;

        public JPanelImpl() {
            MouseAdapter ma = new MouseAdapter() {
                private int lastX, lastY;

                @Override
                public void mousePressed(MouseEvent e) {
                    lastX = e.getX();
                    lastY = e.getY();
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
                    repaint(); // Repaint the panel to reflect the new scale
                }
            };
            addMouseListener(ma);
            addMouseMotionListener(ma);
            addMouseWheelListener(ma);
            imgObj.addListener(new ImageObject.ImageObjectListener() {
                @Override
                public void imageChanged(ImageObject imgObj) {
                    repaint(); // Repaint the panel to reflect any changes
                }
            });
        }

        @Override
        public void paint(Graphics g) {
            g.setColor(Color.DARK_GRAY);
            g.fillRect(0, 0, getWidth(), getHeight());
            int scaledWidth = (int) (imgObj.getWidth() * scale);
            int scaledHeight = (int) (imgObj.getHeight() * scale);
            g.drawImage(imgObj.getScaledInstance(scaledWidth, scaledHeight, BufferedImage.SCALE_FAST), ofsX, ofsY, null);
        }
    }
}
