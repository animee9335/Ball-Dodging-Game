package com.dodging.game.model;

import java.awt.*;

/**
 * 획득 점수, 콤보 등을 잠깐 띄웠다가 위로 사라지게 하는 텍스트 효과
 */
public class FloatingText {
    private final String text;
    private final double x;
    private double y;
    private final Color color;
    private final Font font;
    private final int lifetime;
    private int age = 0;

    public FloatingText(String text, double x, double y, Color color, Font font, int lifetime) {
        this.text = text;
        this.x = x;
        this.y = y;
        this.color = color;
        this.font = font;
        this.lifetime = lifetime;
    }

    public void update() {
        age++;
        y -= 0.8;
    }

    public boolean isDead() {
        return age >= lifetime;
    }

    public void draw(Graphics2D g2d) {
        float alpha = 1.0f - Math.max(0f, (age - lifetime * 0.5f) / (lifetime * 0.5f));
        alpha = Math.max(0f, Math.min(1f, alpha));
        Composite original = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        int tx = (int) Math.round(x - fm.stringWidth(text) / 2.0);
        int ty = (int) Math.round(y);
        // 가독성을 위한 그림자
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.drawString(text, tx + 1, ty + 2);
        g2d.setColor(color);
        g2d.drawString(text, tx, ty);
        g2d.setComposite(original);
    }
}
