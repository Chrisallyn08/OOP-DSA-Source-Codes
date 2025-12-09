package game;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Handles weapon attack animations for Katana, Axe, Scythe.
 * Player only calls setAttack(type) and draw().
 */

public class AnimationController {

    private String weaponType;
    private int attackType = 0;

    private BufferedImage[] frames;
    private int frameIndex = 0;
    private long lastFrameTime = 0;

    private final int frameSpeed = 60; // ms per frame

    public AnimationController(String weaponType) {
        this.weaponType = weaponType;
    }

    public void setWeapon(String weapon) {
        this.weaponType = weapon;
    }

    /** Load frames for a specific attack **/
    private void loadFrames(int attackType) {
        try {
            String base = "/game/sprites/weapons/" + weaponType + "/attack" + attackType;

            File folder = new File(getClass().getResource(base).toURI());
            File[] files = folder.listFiles((d, name) -> name.endsWith(".png"));

            if (files == null || files.length == 0) {
                System.out.println("No frames found: " + base);
                frames = null;
                return;
            }

            frames = new BufferedImage[files.length];

            for (int i = 0; i < files.length; i++) {
                frames[i] = ImageIO.read(files[i]);
            }

        } catch (Exception e) {
            System.out.println("Error loading weapon animation: " + e.getMessage());
            frames = null;
        }
    }

    /** Called by Player when an attack begins **/
    public void startAttack(int type) {
        this.attackType = type;
        loadFrames(type);
        this.frameIndex = 0;
        this.lastFrameTime = System.currentTimeMillis();
    }

    /** Update animation frame **/
    public void update() {
        if (frames == null) return;

        long now = System.currentTimeMillis();
        if (now - lastFrameTime >= frameSpeed) {
            frameIndex++;
            if (frameIndex >= frames.length) frameIndex = frames.length - 1; // hold last frame
            lastFrameTime = now;
        }
    }

    /** Draw weapon animation **/
    public void draw(Graphics g, int x, int y, int facingDir) {
        if (frames == null) return;

        BufferedImage f = frames[frameIndex];

        if (facingDir == -1) {
            g.drawImage(f, x + f.getWidth(), y, -f.getWidth(), f.getHeight(), null);
        } else {
            g.drawImage(f, x, y, null);
        }
    }
}