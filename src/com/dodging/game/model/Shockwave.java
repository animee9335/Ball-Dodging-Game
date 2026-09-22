package com.dodging.game.model;

import java.awt.*;
import java.awt.geom.Ellipse2D;

/**
 * 폭탄 아이템 사용 시 퍼져나가는 충격파 링
 */
public class Shockwave {
    private final double x;
    private final double y;
    private final double maxRadius;
    private final Color color;
    private final int lifetime;
    private int age = 0;

    public Shockwave(double x, double y, double maxRadius, Color color, int lifetime) {
        this.x = x;
        this.y = y;
        this.maxRadius = maxRadius;
        this.color = color;
        this.lifetime = lifetime;
    }

    public void update() {
        age++;
    }

    public boolean isDead() {
        return age >= lifetime;
    }

    public void draw(Graphics2D g2d) {
        double t = age / (double) lifetime;
        double eased = 1 - Math.pow(1 - t, 3); // ease-out
        double r = maxRadius * eased;
        int alpha = (int) (200 * (1 - t));
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, alpha)));
        g2d.setStroke(new BasicStroke((float) (10 * (1 - t) + 1.5)));
        g2d.draw(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
    }
}
