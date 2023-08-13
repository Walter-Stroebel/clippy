/*
 */
package nl.wers.clippy;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

/**
 *
 * @author walter
 */
public class ClippyFrame extends JFrame {

    public ClippyFrame(final Clippy clippy) {
        setTitle("Clippy");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        setLocationRelativeTo(null);
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Options");
        menuBar.add(menu);
        JMenuItem exitItem = new JMenuItem("Exit");
        exitItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.exit(0);
            }
        });
        menu.add(exitItem);
        JMenuItem newGroupItem = new JMenuItem("New Group");
        newGroupItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clippy.createNewGroup();
            }
        });
        menu.add(newGroupItem);
        JMenuItem selectGroupItem = new JMenuItem("Select Group");
        selectGroupItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clippy.selectExistingGroup();
            }
        });
        menu.add(selectGroupItem);
        setJMenuBar(menuBar);
    }

}
