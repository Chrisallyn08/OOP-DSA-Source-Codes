package game;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import javax.swing.*;

import game.entities.Player;

/**
 * GamePanel:
 * - loads character stats HashMap (Katana, Axe, Scythe)
 * - handles combat + hit detection
 * - applies knockback + hitflash by calling Player.takeDamage(amount, dir, force)
 * - manages DamageText queue (LinkedList used as Queue)
 *
 * Added: AIController inner class (queue-based) that drives player2 by toggling keyHandler booleans.
 */
public class GamePanel extends JPanel implements Runnable, MouseListener, MouseMotionListener {
    //background
    private Image backgroundImg;

    // =========================
    //       WIN SYSTEM
    // =========================
    private boolean gameOver = false;
    private String winnerText = "";


    // =========================
    //     COUNTDOWN SYSTEM
    // =========================
    private long countdownStart = System.currentTimeMillis();
    private int countdown = 3;
    private boolean countdownFinished = false;
    private boolean playedFightSound = false;
    private boolean playedNumberSound = false;

    // for player vs bot
    private AIController aiController;
    private boolean vsAI = true;  // enable AI mode

    private String p1Weapon;
    private String p2Weapon;

    private Thread gameThread;
    private final int FPS = 60;
    private final KeyHandler keyHandler;

    private Player player1;
    private Player player2;

    private final int groundOffset = 100;

    // Damage text queue
    private Queue<DamageText> damageTexts = new LinkedList<>();

    // Top Buttons (center of screen)
    private java.awt.Rectangle replayBtn = new java.awt.Rectangle(350, 10, 80, 30);
    private java.awt.Rectangle exitBtn   = new java.awt.Rectangle(450, 10, 80, 30);

    private boolean replayHover = false;
    private boolean exitHover = false;

    public GamePanel(String p1Weapon, String p2Weapon, boolean vsAI) {
        this.p1Weapon = p1Weapon;
        this.p2Weapon = p2Weapon;
        this.vsAI = vsAI;

        setPreferredSize(new Dimension(800, 600));
        setBackground(Color.BLACK);
        setFocusable(true);
        setDoubleBuffered(true);

        addMouseListener(this);
        addMouseMotionListener(this);

        keyHandler = new KeyHandler();
        addKeyListener(keyHandler);

        requestFocusInWindow();
        startGameThread();

        // Load background
        backgroundImg = new ImageIcon(getClass().getResource("/game/assets/backgrounds/battle_bg.png")).getImage();

    }

    public void startGameThread() {
        gameThread = new Thread(this);
        gameThread.start();
    }

    @Override
    public void run() {
        while (getWidth() == 0 || getHeight() == 0) {
            try { Thread.sleep(10); } catch (InterruptedException ignored) {}
        }

        int groundY = getHeight() - groundOffset;

        // default weapons: Katana and Scythe (change strings to pick other characters)
        player1 = new Player(150, groundY, Color.CYAN, keyHandler, true, p1Weapon);
        player2 = new Player(600, groundY, Color.ORANGE, keyHandler, false, p2Weapon);

        int actualGroundY = getHeight() - 100;
        player1.setGroundY(actualGroundY);
        player2.setGroundY(actualGroundY);

        player1.setPanelWidth(getWidth());
        player2.setPanelWidth(getWidth());

        // initialize AI after players exist
        if (vsAI) {
            aiController = new AIController(player2, player1, keyHandler);
        }

        double drawInterval = 1000000000.0 / FPS;
        double nextDrawTime = System.nanoTime() + drawInterval;

        while (gameThread != null) {
            update();
            repaint();

            try {
                double remaining = nextDrawTime - System.nanoTime();
                remaining /= 1000000;
                if (remaining < 0) remaining = 0;
                Thread.sleep((long) remaining);
                nextDrawTime += drawInterval;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void update() {

        // If game is over, allow restart
        if (gameOver) {
            if (keyHandler.enterPressed) {
                restartGame();
            }
            return; // stop updating players while game is over
        }

        // =========================================
        //           COUNTDOWN UPDATE
        // =========================================
        if (!countdownFinished) {
            long elapsed = (System.currentTimeMillis() - countdownStart) / 1000;

            int newCount = 3 - (int)elapsed;

            // Play number sound ONCE per number
            if (newCount != countdown && newCount > 0) {
                countdown = newCount;
                SoundPlayer.play("/game/assets/sfx/beep.wav"); // use your sound
            }

            // When reaching 0 → FIGHT
            if (newCount <= 0) {
                countdown = 0;

                if (!playedFightSound) {
                    SoundPlayer.play("/game/assets/sfx/fahh.wav");
                    playedFightSound = true;
                }

                // finish countdown after 1 more second
                if (elapsed >= 4) {
                    countdownFinished = true;
                }
            }

            // freeze game during countdown
            return;
        }

        if (player1 != null && player2 != null) {

            // PLAYER 1 uses real keyboard (KeyHandler booleans)
            player1.update();

            // PLAYER 2: controlled either by AI (vsAI) or by keyboard
            if (vsAI && aiController != null) {
                // AI decides occasionally and enqueues commands
                aiController.maybeThink();

                // apply a single command (FIFO) into keyHandler booleans for player2
                aiController.applyNextCommand();
                // then update player2 (Player reads keyHandler flags inside its update())
                player2.update();

                // clear player2 key booleans that AI set (so keys are frame-pressed only)
                aiController.clearPlayer2Keys();
            } else {
                // human 2-player
                player2.update();
            }

            handleCombat();
            checkPlayerCollision(player1, player2);

            // ===============================
            //         WIN CONDITION
            // ===============================
            if (!gameOver) {
                if (player1.getHealth() <= 0) {
                    gameOver = true;
                    winnerText = "PLAYER 2 WINS!";
                }
                else if (player2.getHealth() <= 0) {
                    gameOver = true;
                    winnerText = "PLAYER 1 WINS!";
                }
            }

            // update damage texts (move & expire)
            if (!damageTexts.isEmpty()) {
                ArrayList<DamageText> toRemove = new ArrayList<>();
                for (DamageText dt : damageTexts) {
                    dt.update();
                    if (dt.isExpired()) toRemove.add(dt);
                }
                damageTexts.removeAll(toRemove);
            }
        }
    }



    // HIT DETECTION: uses each player's attackHitbox and damage values
    private void handleCombat() {
        // player1 attacking player2
        if (player1.isAttacking()
                && player1.getAttackHitbox() != null
                && !player1.hasHitThisAttack()) {
            // Jump dodge
            if (player2.isJumping() && player1.getAttackHeight() == Player.AttackHeight.LOW)
                return;

            // Crouch dodge
            if (player2.isCrouching() && player1.getAttackHeight() == Player.AttackHeight.HIGH)
                return;
            // NEW: complete dodge invulnerability

            if (player2.isDodging()) return;

            if (player1.getAttackHitbox().intersects(player2.getHurtbox())) {
                int dmg = player1.getCurrentAttackDamage();

                int force = getKnockbackForce(player1.getWeaponType(), player1.getAttackType());
                if (force < 0) force = Math.abs(force);

                int dir = (player1.getX() < player2.getX()) ? 1 : -1;

                player2.takeDamage(dmg, dir, force, player1.getAttackType(), player1.getWeaponType());

                nudgeApartAfterHit(player1, player2, dir);

                damageTexts.add(new DamageText(player2.getX() + player2.getWidth()/2, player2.getY() - 8, dmg));

                player1.registerAttackHit();
            }
        }

        // player2 attacking player1
        if (player2.isAttacking()
                && player2.getAttackHitbox() != null
                && !player2.hasHitThisAttack()) {
            // Jump dodge
            if (player1.isJumping() && player2.getAttackHeight() == Player.AttackHeight.LOW)
                return;

            // Crouch dodge
            if (player1.isCrouching() && player2.getAttackHeight() == Player.AttackHeight.HIGH)
                return;
            // NEW
            if (player1.isDodging()) return;

            if (player2.getAttackHitbox().intersects(player1.getHurtbox())) {
                int dmg = player2.getCurrentAttackDamage();
                int force = getKnockbackForce(player2.getWeaponType(), player2.getAttackType());
                if (force < 0) force = Math.abs(force);

                int dir = (player2.getX() < player1.getX()) ? 1 : -1;

                player1.takeDamage(dmg, dir, force, player2.getAttackType(), player2.getWeaponType());

                nudgeApartAfterHit(player2, player1, dir);

                damageTexts.add(new DamageText(player1.getX() + player1.getWidth()/2, player1.getY() - 8, dmg));

                player2.registerAttackHit();
            }
        }
    }

    // Small helper to nudge target away from attacker after hit to prevent overlap-pulling glitches.
    private void nudgeApartAfterHit(Player attacker, Player target, int dir) {
        int minGap = 2; // pixels gap to ensure separation
        if (attacker.getX() < target.getX()) {
            int desiredX = attacker.getX() + attacker.getWidth() + minGap;
            if (target.getX() < desiredX) {
                target.setX(desiredX);
            } else {
                target.setX(target.getX() + dir * Math.max(1, (int)Math.round((double)dir * 1)));
            }
        } else {
            int desiredX = attacker.getX() - target.getWidth() - minGap;
            if (target.getX() > desiredX) {
                target.setX(desiredX);
            } else {
                target.setX(target.getX() + dir * Math.max(1, (int)Math.round((double)dir * 1)));
            }
        }
    }

    private int getKnockbackForce(String weapon, int attackType) {
        switch (weapon) {
            case "Katana":
                if (attackType == 1) return 8;
                if (attackType == 2) return 12;
                if (attackType == 3) return 15;
                break;
            case "Axe":
                if (attackType == 1) return 10;
                if (attackType == 2) return 16;
                if (attackType == 3) return 20;
                break;
            case "Scythe":
                if (attackType == 1) return 6;
                if (attackType == 2) return 18;
                if (attackType == 3) return 12;
                break;
        }
        return 8; // fallback
    }

    private void checkPlayerCollision(Player p1, Player p2) {
        if (p1.getX() < p2.getX() + p2.getWidth() &&
                p1.getX() + p1.getWidth() > p2.getX() &&
                p1.getY() < p2.getY() + p2.getHeight() &&
                p1.getY() + p1.getHeight() > p2.getY()) {

            int overlapX = Math.min(
                    p1.getX() + p1.getWidth() - p2.getX(),
                    p2.getX() + p2.getWidth() - p1.getX()
            );

            if (p1.getX() < p2.getX()) {
                p1.setX(p1.getX() - overlapX / 2);
                p2.setX(p2.getX() + overlapX / 2);
            } else {
                p1.setX(p1.getX() + overlapX / 2);
                p2.setX(p2.getX() - overlapX / 2);
            }
        }
    }

    //for the menu ingame
    private void restartGame() {

        gameOver = false;       // <-- YOU MUST ADD THIS
        winnerText = "";        // <-- Reset winner text


        int groundY = getHeight() - groundOffset;

        player1 = new Player(150, groundY, Color.CYAN, keyHandler, true, p1Weapon);
        player2 = new Player(600, groundY, Color.ORANGE, keyHandler, false, p2Weapon);

        int actualGroundY = getHeight() - 100;
        player1.setGroundY(actualGroundY);
        player2.setGroundY(actualGroundY);

        player1.setPanelWidth(getWidth());
        player2.setPanelWidth(getWidth());

        damageTexts.clear();

        // reinit AI after recreated players
        if (vsAI) {
            aiController = new AIController(player2, player1, keyHandler);
        }

        countdownStart = System.currentTimeMillis();
        countdown = 3;
        countdownFinished = false;
        playedFightSound = false;
        playedNumberSound = false;
    }

    private void goToMenu() {
        JFrame frame = (JFrame) getTopLevelAncestor();
        if (frame != null) {
            frame.getContentPane().removeAll();
            frame.getContentPane().add(new MenuPanel(frame));
            frame.revalidate();
            frame.repaint();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        int btnY = getHeight() - 50; // very bottom

        replayBtn.setBounds(getWidth()/2 - 140, btnY, 120, 35);
        exitBtn.setBounds(getWidth()/2 + 20,  btnY, 120, 35);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // ==================================================
        //                  BACKGROUND IMAGE
        // ==================================================
        if (backgroundImg != null) {
            g2.drawImage(backgroundImg, 0, 0, getWidth(), getHeight(), null);
        }

        // ====== Cinematic bars (top & bottom) ======
        g2.setColor(new Color(0, 0, 0, 75));
        g2.fillRect(0, 0, getWidth(), 45);
        g2.fillRect(0, getHeight() - 45, getWidth(), 45);

        // ====== Gradient overlay (makes colors pop) ======
        GradientPaint grad = new GradientPaint(
                0, 0, new Color(0, 0, 0, 40),
                0, getHeight(), new Color(0, 0, 0, 120)
        );
        g2.setPaint(grad);
        g2.fillRect(0, 0, getWidth(), getHeight());

        // ==================================================
        //                  PLAYER SHADOWS
        // ==================================================
        if (player1 != null) drawShadow(g2, player1);
        if (player2 != null) drawShadow(g2, player2);

        // ==================================================
        //                   DRAW PLAYERS
        // ==================================================
        if (player1 != null) player1.draw(g);
        if (player2 != null) player2.draw(g);

        // ==================================================
        //                   HEALTH BARS
        // ==================================================
        drawHPBar(g2, 50, 40, player1.getHealth(), player1.getMaxHealth(), "P1");
        drawHPBar(g2, getWidth() - 400, 40, player2.getHealth(), player2.getMaxHealth(), "P2");

        // ==================================================
        //                 COOLDOWN TEXT
        // ==================================================
        g2.setFont(new Font("Consolas", Font.BOLD, 14));
        g2.setColor(Color.WHITE);

        g2.drawString("BASIC " + player1.getCooldownRemainingSeconds(1), 50, 85);
        g2.drawString("SKILL1 " + player1.getCooldownRemainingSeconds(2), 50, 102);
        g2.drawString("SKILL2 " + player1.getCooldownRemainingSeconds(3), 50, 119);
        g2.drawString("DODGE " + player1.getCooldownRemainingSeconds(4), 50, 136);

        g2.drawString("BASIC " + player2.getCooldownRemainingSeconds(1), getWidth() - 200, 85);
        g2.drawString("SKILL1 " + player2.getCooldownRemainingSeconds(2), getWidth() - 200, 102);
        g2.drawString("SKILL2 " + player2.getCooldownRemainingSeconds(3), getWidth() - 200, 119);
        g2.drawString("DODGE " + player2.getCooldownRemainingSeconds(4), getWidth() - 200, 136);

        // ==================================================
        //                DAMAGE POPUP TEXTS
        // ==================================================
        for (DamageText dt : damageTexts) dt.draw(g);

        // ==================================================
        //                TOP MENU BUTTONS
        // ==================================================
        drawButtons(g2);

        // ==================================================
        //                COUNTDOWN TEXT
        // ==================================================
        if (!countdownFinished) drawCountdown(g2);

        // ==================================================
        //                 WIN SCREEN
        // ==================================================
        if (gameOver) drawWinScreen(g2);
    }

    private void drawShadow(Graphics2D g2, Player p) {
        int w = 60;
        int h = 15;
        int x = (int) p.getX() + p.getWidth()/2 - w/2;
        int y = (int) p.getY() + p.getHeight() - 5;

        g2.setColor(new Color(0, 0, 0, 120));
        g2.fillOval(x, y, w, h);
    }

    private void drawHPBar(Graphics2D g2, int x, int y, int hp, int maxHp, String label) {
        int w = 350;
        int h = 25;

        g2.setColor(Color.BLACK);
        g2.fillRect(x - 4, y - 4, w + 8, h + 8);

        g2.setColor(new Color(40, 40, 40));
        g2.fillRect(x, y, w, h);

        int filled = (int) ((hp / (float) maxHp) * w);
        g2.setColor(hp < maxHp * 0.3 ? Color.ORANGE : new Color(50, 200, 50));
        g2.fillRect(x, y, filled, h);

        g2.setColor(Color.WHITE);
        g2.drawRect(x, y, w, h);

        g2.setFont(new Font("Consolas", Font.BOLD, 16));
        g2.drawString(label + " HP: " + hp + "/" + maxHp, x, y - 6);
    }

    private void drawButtons(Graphics2D g2) {
        // Replay Button
        g2.setColor(replayHover ? new Color(100, 170, 100) : new Color(60, 60, 60));
        g2.fillRoundRect(replayBtn.x, replayBtn.y, replayBtn.width, replayBtn.height, 10, 10);

        g2.setColor(Color.WHITE);
        g2.setFont(new Font("Monospaced", Font.BOLD, 20));
        g2.drawString("REMATCH", replayBtn.x + 18, replayBtn.y + 26);

        // Exit Button
        g2.setColor(exitHover ? new Color(170, 80, 80) : new Color(60, 60, 60));
        g2.fillRoundRect(exitBtn.x, exitBtn.y, exitBtn.width, exitBtn.height, 10, 10);

        g2.setColor(Color.WHITE);
        g2.drawString("EXIT", exitBtn.x + 38, exitBtn.y + 26);
    }

    private void drawCountdown(Graphics2D g2) {
        g2.setFont(new Font("Monospaced", Font.BOLD, 90));

        String text = (countdown > 0) ? String.valueOf(countdown) : "FIGHT!";
        int tw = g2.getFontMetrics().stringWidth(text);

        int x = (getWidth() - tw) / 2;
        int y = getHeight() / 2;

        g2.setColor(Color.BLACK);
        g2.drawString(text, x+4, y+4);

        g2.setColor(Color.WHITE);
        g2.drawString(text, x, y);
    }

    private void drawWinScreen(Graphics2D g2) {
        g2.setColor(new Color(0,0,0,180));
        g2.fillRect(0,0,getWidth(),getHeight());

        g2.setFont(new Font("Monospaced", Font.BOLD, 70));
        int tw = g2.getFontMetrics().stringWidth(winnerText);

        g2.setColor(Color.BLACK);
        g2.drawString(winnerText, (getWidth()-tw)/2 + 4, getHeight()/2 + 4);

        g2.setColor(Color.WHITE);
        g2.drawString(winnerText, (getWidth()-tw)/2, getHeight()/2);

        g2.setFont(new Font("Monospaced", Font.BOLD, 30));
        String msg = "Press ENTER to restart";
        int sw = g2.getFontMetrics().stringWidth(msg);
        g2.drawString(msg, (getWidth()-sw)/2, getHeight()/2 + 60);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int mx = e.getX();
        int my = e.getY();

        if (replayBtn.contains(mx, my)) {
            restartGame();
        }

        if (exitBtn.contains(mx, my)) {
            goToMenu();
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int mx = e.getX();
        int my = e.getY();

        replayHover = replayBtn.contains(mx, my);
        exitHover = exitBtn.contains(mx, my);

        repaint();
    }

    @Override public void mouseDragged(MouseEvent e) {}
    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}

    // -------------------------
    // Inner AIController Class
    // -------------------------
    // Queue-based AI: generates a FIFO list of string commands and applies one per frame
    private class AIController {
        private final Queue<String> commands = new LinkedList<>();
        private final Player ai;        // controlled player (player2)
        private final Player target;    // target player (player1)
        private final KeyHandler keys;
        private final Random rng = new Random();

        // decision timing
        private int framesUntilNextThink = 0;
        private final int baseThinkIntervalFrames = 8; // AI reacts every ~8 frames by default

        public AIController(Player ai, Player target, KeyHandler keys) {
            this.ai = ai;
            this.target = target;
            this.keys = keys;
        }

        // maybe create new high-level commands (not every frame)
        public void maybeThink() {
            if (framesUntilNextThink > 0) {
                framesUntilNextThink--;
                return;
            }

            think();
            // add small random jitter to avoid robotic rhythm
            framesUntilNextThink = baseThinkIntervalFrames + rng.nextInt(6);
        }

        // Decide a small plan and enqueue commands (FIFO)
        private void think() {
            commands.clear(); // we produce fresh short plans each think

            int distance = Math.abs(ai.getX() - target.getX());

            // If low HP, occasionally back off
            if (ai.getHealth() < ai.getMaxHealth() * 0.25 && rng.nextDouble() < 0.6) {
                if (ai.getX() < target.getX()) commands.add("back_away");
                else commands.add("back_away");
                // sometimes jump/back + wait
                if (rng.nextDouble() < 0.35) commands.add("jump");
                return;
            }

            // Movement decisions
            if (distance > 170) {
                // far: move closer
                commands.add("move_closer");
                // occasionally do a short run then attack
                if (rng.nextDouble() < 0.5 && distance < 260) commands.add("attack");
            } else if (distance < 70) {
                // too close: back away + maybe dodge
                commands.add("back_away");
                if (rng.nextDouble() < 0.35) commands.add("jump");
            } else {
                // in mid-range: choose aggression or bait
                if (rng.nextDouble() < 0.65) {
                    commands.add("attack");
                } else {
                    // fake: step forward then step back
                    commands.add("move_closer");
                    commands.add("back_away");
                }
            }

            // Defensive: if target is attacking right now, try to jump or dodge sometimes
            if (target.isAttacking() && rng.nextDouble() < 0.6) {
                // choose jump vs back_away
                if (rng.nextDouble() < 0.6) commands.add("jump");
                else commands.add("back_away");
            }
        }

        // Apply a single next command by toggling the KeyHandler booleans for player2 keys
        // Player2 key mapping observed in Player.java:
        // leftPressed, rightPressed, upPressed, downPressed,
        // periodPressed (basic), commaPressed (skill1), mPressed (skill2), slashPressed (dodge)
        public void applyNextCommand() {
            // first, clear all player2 key flags to avoid carry-over (we want single-frame presses)
            clearPlayer2Keys();

            if (commands.isEmpty()) return;

            String cmd = commands.poll();
            if (cmd == null) return;

            switch (cmd) {
                case "move_closer":
                    if (ai.getX() < target.getX()) keys.rightPressed = true;
                    else keys.leftPressed = true;
                    break;
                case "back_away":
                    if (ai.getX() < target.getX()) keys.leftPressed = true;
                    else keys.rightPressed = true;
                    break;
                case "attack":
                    // preferentially use basic attack; if basic on cooldown, try skill1
                    int remainingBasic = ai.getCooldownRemainingSeconds(1);
                    int remainingSkill1 = ai.getCooldownRemainingSeconds(2);
                    if (remainingBasic == 0) {
                        keys.periodPressed = true; // basic
                    } else if (remainingSkill1 == 0 && rng.nextDouble() < 0.5) {
                        keys.commaPressed = true; // skill1
                    } else {
                        // fallback: still press basic (will be ignored if cooldown)
                        keys.periodPressed = true;
                    }
                    break;
                case "jump":
                    keys.upPressed = true;
                    break;
                default:
                    // nothing
            }
        }

        // Clear player2's key booleans so they are only "pressed" for a single frame
        public void clearPlayer2Keys() {
            keys.leftPressed = false;
            keys.rightPressed = false;
            keys.upPressed = false;
            keys.downPressed = false;

            keys.periodPressed = false;
            keys.commaPressed = false;
            keys.mPressed = false;
            keys.slashPressed = false;
            // Note: dodge uses slashPressed per Player.handleDodge()
            // If your KeyHandler uses different names, update these fields accordingly.
        }
    }

}