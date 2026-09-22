package com.dodging.game.model;

import java.awt.*;

/**
 * 게임 시각 효과를 위한 파티클 클래스 (추진체 잔상, 폭발 효과 등)
 */
public class Particle {
    private double x;
    private double y;
    private final double vx;
    private final double vy;
    private final double size;
    private float alpha;
    private final float decay;
    private final Color color;

    public Particle(double x, double y, double vx, double vy, double size, float alpha, float decay, Color color) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.size = size;
        this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
        this.decay = decay;
        this.color = color;
    }

    public void update() {
        x += vx;
        y += vy;
        alpha -= decay;
        if (alpha < 0.0f) {
            alpha = 0.0f;
        }
    }

    public boolean isDead() {
        return alpha <= 0.0f;
    }

    public void draw(Graphics2D g2d) {
        if (isDead()) return;

        Composite originalComposite = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        g2d.setColor(color);

        int drawX = (int) Math.round(x - size / 2.0);
        int drawY = (int) Math.round(y - size / 2.0);
        int s = (int) Math.round(size);

        g2d.fillOval(drawX, drawY, s, s);
        g2d.setComposite(originalComposite);
    }
}
