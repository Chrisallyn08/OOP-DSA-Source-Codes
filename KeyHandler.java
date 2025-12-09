package game;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * KeyHandler - maps keyboard input to boolean flags used by Player.
 *
 * Player 1:
 *   Movement: A D W S
 *   Dodge: Q
 *   Basic attack: E
 *   Skill1: R
 *   Skill2: T
 *
 * Player 2:
 *   Movement: LEFT RIGHT UP DOWN
 *   Dodge: SLASH '/'
 *   Basic attack: PERIOD '.'
 *   Skill1: COMMA ','
 *   Skill2: M
 */
public class KeyHandler implements KeyListener {

    public boolean enterPressed = false;

    // Player 1 movement
    public boolean aPressed, dPressed, wPressed, sPressed;
    // Player 1 dodge
    public boolean qPressed;
    // Player 1 attacks/skills
    public boolean ePressed; // basic
    public boolean rPressed; // skill 1
    public boolean tPressed; // skill 2

    // Player 2 movement
    public boolean leftPressed, rightPressed, upPressed, downPressed;
    // Player 2 dodge
    public boolean slashPressed;
    // Player 2 attacks/skills
    public boolean periodPressed; // basic (.)
    public boolean commaPressed;  // skill 1 (,)
    public boolean mPressed;      // skill 2 (M)

    @Override
    public void keyTyped(KeyEvent e) { /* not used */ }

    @Override
    public void keyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        if (code == KeyEvent.VK_ENTER) {
            enterPressed = true;
        }

        // --- Player 1 movement ---
        if (code == KeyEvent.VK_A) aPressed = true;
        if (code == KeyEvent.VK_D) dPressed = true;
        if (code == KeyEvent.VK_W) wPressed = true;
        if (code == KeyEvent.VK_S) sPressed = true;

        // --- Player 1 dodge & attacks ---
        if (code == KeyEvent.VK_Q) qPressed = true;    // dodge
        if (code == KeyEvent.VK_E) ePressed = true;    // basic attack
        if (code == KeyEvent.VK_R) rPressed = true;    // skill 1
        if (code == KeyEvent.VK_T) tPressed = true;    // skill 2

        // --- Player 2 movement ---
        if (code == KeyEvent.VK_LEFT) leftPressed = true;
        if (code == KeyEvent.VK_RIGHT) rightPressed = true;
        if (code == KeyEvent.VK_UP) upPressed = true;
        if (code == KeyEvent.VK_DOWN) downPressed = true;

        // --- Player 2 dodge & attacks ---
        if (code == KeyEvent.VK_SLASH) slashPressed = true;        // dodge ('/')
        if (code == KeyEvent.VK_PERIOD) periodPressed = true;      // basic attack (.)
        if (code == KeyEvent.VK_COMMA) commaPressed = true;        // skill 1 (,)
        if (code == KeyEvent.VK_M) mPressed = true;                // skill 2 (M)
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int code = e.getKeyCode();

        if (code == KeyEvent.VK_ENTER) {
            enterPressed = false;
        }
        // --- Player 1 movement ---
        if (code == KeyEvent.VK_A) aPressed = false;
        if (code == KeyEvent.VK_D) dPressed = false;
        if (code == KeyEvent.VK_W) wPressed = false;
        if (code == KeyEvent.VK_S) sPressed = false;

        // --- Player 1 dodge & attacks ---
        if (code == KeyEvent.VK_Q) qPressed = false;
        if (code == KeyEvent.VK_E) ePressed = false;
        if (code == KeyEvent.VK_R) rPressed = false;
        if (code == KeyEvent.VK_T) tPressed = false;

        // --- Player 2 movement ---
        if (code == KeyEvent.VK_LEFT) leftPressed = false;
        if (code == KeyEvent.VK_RIGHT) rightPressed = false;
        if (code == KeyEvent.VK_UP) upPressed = false;
        if (code == KeyEvent.VK_DOWN) downPressed = false;

        // --- Player 2 dodge & attacks ---
        if (code == KeyEvent.VK_SLASH) slashPressed = false;
        if (code == KeyEvent.VK_PERIOD) periodPressed = false;
        if (code == KeyEvent.VK_COMMA) commaPressed = false;
        if (code == KeyEvent.VK_M) mPressed = false;
    }
}
