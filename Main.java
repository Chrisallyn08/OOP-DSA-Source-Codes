package game;

import javax.swing.JFrame;
import java.awt.Color;

public class Main {
    public static void main(String[] args) {
        JFrame window = new JFrame("Stickman Brawl");
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);

        // Background stays black until repaint
        window.getContentPane().setBackground(Color.BLACK);

        // Start with MenuPanel instead of GamePanel
        MenuPanel menu = new MenuPanel(window);
        window.add(menu);

        window.pack();
        window.setLocationRelativeTo(null);
        window.setVisible(true);
    }
}
