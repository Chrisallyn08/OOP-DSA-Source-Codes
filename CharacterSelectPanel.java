package game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.LinkedList;

public class CharacterSelectPanel extends JPanel implements MouseListener, MouseMotionListener {

    private JFrame window;
    private Image backgroundImage;
    p

    private boolean selectingP2 = false;
    private boolean vsAI = false;

    rivate LinkedList<CharacterOption> options = new LinkedList<>();   private String p1Choice = null;
    private String p2Choice = null;

    private Font pixelFont = new Font("Monospaced", Font.BOLD, 28);

    public CharacterSelectPanel(JFrame window, boolean vsAI) {
        this.window = window;
        this.vsAI = vsAI;

        setPreferredSize(new Dimension(800,600));

        backgroundImage = new ImageIcon(getClass().getResource("/game/assets/backgrounds/character_bg.png")).getImage();

        options.add(new CharacterOption("Katana", 120, 260, 150, 150));
        options.add(new CharacterOption("Axe", 330, 260, 150, 150));
        options.add(new CharacterOption("Scythe", 540, 260, 150, 150));

        addMouseListener(this);
        addMouseMotionListener(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(backgroundImage, 0,0,getWidth(),getHeight(),this);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g2.setFont(pixelFont);

        String title = !selectingP2 ? "SELECT CHARACTER — PLAYER 1" : "SELECT CHARACTER — PLAYER 2";

        g2.setColor(new Color(0,0,0,180));
        g2.drawString(title, 128,118);
        g2.drawString(title, 132,118);
        g2.drawString(title, 130,116);
        g2.drawString(title, 130,120);

        g2.setColor(Color.WHITE);
        g2.drawString(title,130,118);

        for (CharacterOption opt : options) opt.draw(g2);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        int mx = e.getX();
        int my = e.getY();

        for (CharacterOption opt : options) {
            if (opt.contains(mx,my)) {
                SoundPlayer.play("/game/assets/sfx/click.wav");

                if (!selectingP2) {
                    p1Choice = opt.name;

                    if (vsAI) {
                        p2Choice = getRandomAIWeapon(p1Choice);
                        GamePanel gp = new GamePanel(p1Choice, p2Choice, true);
                        window.setContentPane(gp);
                        window.revalidate();
                        gp.requestFocusInWindow();
                    } else {
                        selectingP2 = true;
                        repaint();
                    }

                } else {
                    p2Choice = opt.name;
                    GamePanel gp = new GamePanel(p1Choice, p2Choice, false);
                    window.setContentPane(gp);
                    window.revalidate();
                    gp.requestFocusInWindow();
                }
            }
        }
    }

    private String getRandomAIWeapon(String p1Weapon) {
        String[] weapons = {"Katana","Axe","Scythe"};
        ArrayList<String> choices = new ArrayList<>();
        for (String w : weapons) if (!w.equals(p1Weapon)) choices.add(w);
        return choices.get((int)(Math.random()*choices.size()));
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        int mx = e.getX();
        int my = e.getY();
        for (CharacterOption opt : options) opt.setHover(opt.contains(mx,my));
        repaint();
    }

    // Unused
    @Override public void mousePressed(MouseEvent e) {}
    @Override public void mouseReleased(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    @Override public void mouseDragged(MouseEvent e) {}

    class CharacterOption {
        String name;
        Rectangle bounds;
        boolean hovered = false;

        public CharacterOption(String name,int x,int y,int w,int h){
            this.name = name;
            this.bounds = new Rectangle(x,y,w,h);
        }

        public void draw(Graphics2D g) {
            g.setFont(new Font("Monospaced", Font.BOLD, 20));
            Color boxColor = new Color(60,50,40,200);
            Color hoverColor = new Color(90,70,50,220);
            g.setColor(hovered ? hoverColor : boxColor);
            g.fillRect(bounds.x,bounds.y,bounds.width,bounds.height);

            g.setColor(new Color(20,15,10));
            g.drawRect(bounds.x,bounds.y,bounds.width,bounds.height);

            if ((name.equals(p1Choice) && selectingP2) || (name.equals(p2Choice) && !selectingP2)){
                g.setColor(new Color(255,220,80));
                g.setStroke(new BasicStroke(3));
                g.drawRect(bounds.x-2,bounds.y-2,bounds.width+4,bounds.height+4);
            }

            g.setColor(Color.WHITE);
            int textWidth = g.getFontMetrics().stringWidth(name);
            int tx = bounds.x + (bounds.width - textWidth)/2;
            int ty = bounds.y + bounds.height/2 + 8;
            g.drawString(name,tx,ty);
        }

        public boolean contains(int mx,int my){return bounds.contains(mx,my);}
        public void setHover(boolean h){hovered = h;}
    }
}
