/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.Minecraft
 *  org.lwjgl.opengl.Display
 */
package noppes.npcs.client.gui.swing;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import net.minecraft.client.Minecraft;
import noppes.npcs.client.gui.util.IJTextAreaListener;
import org.lwjgl.opengl.Display;

public class GuiJTextArea
extends JDialog
implements WindowListener {
    public IJTextAreaListener listener;
    private JTextArea area;

    public GuiJTextArea(String text) {
        this.setDefaultCloseOperation(2);
        this.setSize(Display.getWidth() - 40, Display.getHeight() - 40);
        this.setLocation(Display.getX() + 20, Display.getY() + 20);
        this.area = new JTextArea(text);
        JScrollPane scroll = new JScrollPane(this.area);
        scroll.setVerticalScrollBarPolicy(22);
        this.add(scroll);
        this.addWindowListener(this);
        this.setVisible(true);
    }

    public GuiJTextArea setListener(IJTextAreaListener listener) {
        this.listener = listener;
        return this;
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
    }

    @Override
    public void windowClosed(WindowEvent e) {
        if (this.listener == null) {
            return;
        }
        Minecraft.func_71410_x().func_152344_a(() -> this.listener.saveText(this.area.getText()));
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }
}

