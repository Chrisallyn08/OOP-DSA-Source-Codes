package game;

import java.awt.*;

public class MenuButton {

    private String text;
    private Rectangle bounds;
    private boolean hovered = false;

    public MenuButton(String text, int x, int y, int width, int height) {
        this.text = text;
        this.bounds = new Rectangle(x, y, width, height);
    }

    public void draw(Graphics g) {

        Graphics2D g2 = (Graphics2D) g;
        // Disable antialiasing for pixel look
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        // Retro colors
        Color bg = hovered ? new Color(95, 70, 50) : new Color(60, 40, 30);
        g.setColor(bg);
        g.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);

        // Pixel-style border (double border)
        g.setColor(new Color(20, 10, 5));
        g.drawRect(bounds.x, bounds.y, bounds.width, bounds.height);
        g.drawRect(bounds.x + 1, bounds.y + 1, bounds.width - 2, bounds.height - 2);

        // Pixel-ish text
        g.setColor(Color.WHITE);
        g.setFont(new Font("Monospaced", Font.BOLD, 22));

        FontMetrics fm = g.getFontMetrics();
        int tx = bounds.x + (bounds.width - fm.stringWidth(text)) / 2;
        int ty = bounds.y + (bounds.height + fm.getAscent()) / 2 - 4;

        g.drawString(text, tx, ty);
    }

    public void setHovered(boolean h) {
        this.hovered = h;
    }

    public boolean contains(int mx, int my) {
        return bounds.contains(mx, my);
    }


    public boolean isHovered() {
        return hovered;
    }

    public String getText() {
        return text;
    }
}
