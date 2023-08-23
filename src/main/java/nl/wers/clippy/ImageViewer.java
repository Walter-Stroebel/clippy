/*
 */
package nl.wers.clippy;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.swing.JPanel;

/**
 * Simple image viewer with drag and scale.
 * @author walter
 */
public class ImageViewer {
    private static final int FAIL_IMAGE_HEIGHT = 200;
    private static final int FAIL_IMG_WIDTH = 800;

    private BufferedImage image;

    private int ofsX = 0;
    private int ofsY = 0;
    private double scale = 1;
    public ImageViewer(File f) {
        try {
            image = ImageIO.read(f);
        } catch (IOException ex) {
            image = new BufferedImage(FAIL_IMG_WIDTH, FAIL_IMAGE_HEIGHT, BufferedImage.TYPE_BYTE_BINARY);
            Graphics2D gr = image.createGraphics();
            String msg1 = "Cannot open image:";
            String msg2 = f.getAbsolutePath();
            Font font = gr.getFont();
            boolean msgTooBig = false;
            float points = 24;
            int w, h;
            do {
                font = font.deriveFont(points);
                gr.setFont(font);
                FontMetrics metrics = gr.getFontMetrics(font);
                w = Math.max(metrics.stringWidth(msg1), metrics.stringWidth(msg2));
                h = metrics.getHeight() * 2; // For two lines of text
                if (w > FAIL_IMG_WIDTH - 50 || h > FAIL_IMAGE_HEIGHT / 3) {
                    points--;
                }
            } while (w > FAIL_IMG_WIDTH || h > FAIL_IMAGE_HEIGHT / 3);

            gr.drawString(msg1, 50, FAIL_IMAGE_HEIGHT / 3);
            gr.drawString(msg2, 50, FAIL_IMAGE_HEIGHT / 3 * 2);
            gr.dispose();
        }
    }

    public JPanel getViewPanel() {
        final JPanel ret = new JPanel() {
            @Override
            public void paint(Graphics g) {
                g.setColor(Color.DARK_GRAY);
                g.fillRect(0, 0, getWidth(), getHeight());
                int scaledWidth = (int) (image.getWidth() * scale);
                int scaledHeight = (int) (image.getHeight() * scale);
                g.drawImage(image.getScaledInstance(scaledWidth, scaledHeight, BufferedImage.SCALE_FAST), ofsX, ofsY, null);
            }
        };
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
                ret.repaint(); // Repaint the panel to reflect the new position
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                int notches = e.getWheelRotation();
                scale += notches * 0.1; // Adjust the scaling factor
                if (scale < 0.1) {
                    scale = 0.1; // Prevent the scale from becoming too small
                }
                ret.repaint(); // Repaint the panel to reflect the new scale
            }
        };
        ret.addMouseListener(ma);
        ret.addMouseMotionListener(ma);
        ret.addMouseWheelListener(ma);
        return ret;
    }
}
