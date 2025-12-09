package game;

import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.ArrayList;

public class MenuPanel extends JPanel implements MouseListener, MouseMotionListener {

    private JFrame window;
    private ArrayList<MenuButton> buttons = new ArrayList<>();
    private BufferedImage background;

    public MenuPanel(JFrame window) {
        this.window = window;
        setPreferredSize(new Dimension(800, 600));

        loadBackground();

        // Buttons
        buttons.add(new MenuButton("PLAY (P1 vs P2)", 280, 200, 240, 60));
        buttons.add(new MenuButton("PLAY (P1 vs BOT)", 280, 280, 240, 60));
        buttons.add(new MenuButton("QUIT", 280, 360, 240, 60));

        addMouseListener(this);
        addMouseMotionListener(this);
    }

    private void playClickSound() {
        try {
            Clip clip = AudioSystem.getClip();
            AudioInputStream audioInputStream =
                    AudioSystem.getAudioInputStream(
                            getClass().getResource("/game/assets/sfx/click.wav")
                    );

            clip.open(audioInputStream);
            clip.start();
        } catch (Exception e) {
            System.out.println("Error playing click sound");
            e.printStackTrace();
        }
    }

    private void loadBackground() {
        try {
            background = ImageIO.read(
                    getClass().getResource("/game/assets/backgrounds/menu_bg.png")
            );
        } catch (Exception e) {
            System.out.println("Could not load menu background.");
            e.printStackTrace();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw background
        if (background != null) {
            g2.drawImage(background, 0, 0, getWidth(), getHeight(), null);
        }

        // Dark overlay
        g2.setColor(new Color(0,0,0,120));
        g2.fillRect(0,0,getWidth(), getHeight());

        // Title
        String title = "STICK BRAWL";
        g.setFont(new Font("Monospaced", Font.BOLD, 40));

        int titleX = (getWidth() - g.getFontMetrics().stringWidth(title)) / 2;
        int titleY = 140;

        // Glow
        g2.setColor(new Color(255,255,255,70));
        g2.drawString(title, titleX+3, titleY+3);

        // Main title
        g2.setColor(Color.WHITE);
        g2.drawString(title, titleX, titleY);

        // Draw buttons
        for (MenuButton b : buttons) b.draw(g2);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int mx = e.getX();
        int my = e.getY();

        for (MenuButton b : buttons) {
            if (b.contains(mx,my)) {
                playClickSound();

                switch (b.getText()) {
                    case "PLAY (P1 vs P2)" -> {
                        CharacterSelectPanel cs = new CharacterSelectPanel(window, false);
                        window.setContentPane(cs);
                        window.revalidate();
                        cs.requestFocusInWindow();
                    }
                    case "PLAY (P1 vs BOT)" -> {
                        CharacterSelectPanel cs = new CharacterSelectPanel(window, true);
                        window.setContentPane(cs);
                        window.revalidate();
                        cs.requestFocusInWindow();
                    }
                    case "QUIT" -> System.exit(0);
                }
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int mx = e.getX();
        int my = e.getY();
        for (MenuButton b : buttons) b.setHovered(b.contains(mx,my));
        repaint();
    }

    // Unused
    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseDragged(MouseEvent e) {}
}
