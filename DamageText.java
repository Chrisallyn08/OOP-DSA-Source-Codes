package game;

import java.awt.Color;
import java.awt.Graphics;

public class DamageText {
    private float x, y;
    private final int damage;
    private final long startTime;
    private final long duration = 700; // ms

    public DamageText(float x, float y, int damage) {
        this.x = x;
        this.y = y;
        this.damage = damage;
        this.startTime = System.currentTimeMillis();
    }

    public void update() {
        y -= 0.8f; // float upward
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - startTime > duration;
    }

    public void draw(Graphics g) {
        // alpha not implemented simply — just draw yellow text
        g.setColor(Color.YELLOW);
        g.drawString(String.valueOf(damage), Math.round(x), Math.round(y));
    }
}
