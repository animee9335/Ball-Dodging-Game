package com.dodging.game.model;

import java.awt.*;
import java.awt.geom.Path2D;
import java.util.Random;

/**
 * 맵에 떨어지는 아이템
 * - STAR : 보너스 점수
 * - BOMB : 화면에 있는 공을 전부 파괴
 * 어떤 아이템이든 연속으로 먹으면 콤보가 쌓이고, 일정 콤보에 도달하면 FEVER 가 발동합니다.
 */
public class Item {
    public enum ItemType {
        STAR("별", "보너스 점수", new Color(255, 205, 64)),
        BOMB("폭탄", "화면의 공 전부 파괴", new Color(255, 96, 150));

        private final String label;
        private final String effect;
        private final Color color;

        ItemType(String label, String effect, Color color) {
            this.label = label;
            this.effect = effect;
            this.color = color;
        }

        public String getLabel() {
            return label;
        }

        public String getEffect() {
            return effect;
        }

        public Color getColor() {
            return color;
        }
    }

    private static final Random RANDOM = new Random();

    public static final double RADIUS = 11.0;
    public static final int LIFETIME_FRAMES = 7 * 60; // 7초 후 사라짐
    private static final int BLINK_FRAMES = 2 * 60;   // 사라지기 2초 전부터 깜빡임
    private static final int POP_IN_FRAMES = 14;      // 등장 애니메이션

    private final double x;
    private final double y;
    private final ItemType type;
    private int age = 0;

    public Item(double x, double y, ItemType type) {
        this.x = x;
        this.y = y;
        this.type = type;
    }

    /**
     * 플레이 영역 안에서, 플레이어와 너무 가깝지 않은 위치에 아이템 생성
     * @param bombChance 폭탄이 나올 확률 (0~1)
     */
    public static Item createRandom(int width, int height, int topMargin, double playerX, double playerY, double bombChance) {
        ItemType type = RANDOM.nextDouble() < bombChance ? ItemType.BOMB : ItemType.STAR;
        double margin = 48;
        double x = 0;
        double y = 0;
        for (int attempt = 0; attempt < 20; attempt++) {
            x = margin + RANDOM.nextDouble() * (width - margin * 2);
            y = topMargin + margin + RANDOM.nextDouble() * (height - topMargin - margin * 2);
            double dx = x - playerX;
            double dy = y - playerY;
            if (dx * dx + dy * dy > 140 * 140) {
                break; // 플레이어와 충분히 떨어진 위치
            }
        }
        return new Item(x, y, type);
    }

    public void update() {
        age++;
    }

    public boolean isExpired() {
        return age >= LIFETIME_FRAMES;
    }

    /** 획득 판정은 조금 넉넉하게 (+4px) */
    public boolean collidesWith(Player player) {
        double dx = x - player.getX();
        double dy = y - player.getY();
        double r = RADIUS + player.getRadius() + 4;
        return dx * dx + dy * dy <= r * r;
    }

    public void draw(Graphics2D g2d) {
        int remaining = LIFETIME_FRAMES - age;
        if (remaining < BLINK_FRAMES && (age / 6) % 2 == 0) {
            return; // 곧 사라짐을 알리는 깜빡임
        }
        double scale = Math.min(1.0, age / (double) POP_IN_FRAMES);
        // 살짝 튕기는 등장 효과
        scale = scale < 1.0 ? scale * (1.15 - 0.15 * scale) : 1.0;
        double bob = Math.sin(age * 0.09) * 3.0;
        drawIcon(g2d, type, x, y + bob, RADIUS * scale, age * 0.03, age);
    }

    /**
     * 아이템 아이콘 그리기 (게임 화면과 메뉴 범례에서 공용으로 사용)
     */
    public static void drawIcon(Graphics2D g2d, ItemType type, double cx, double cy, double r, double rotation, int tick) {
        if (r <= 0.5) return;
        Color c = type.getColor();

        // 은은한 발광
        double pulse = 1.0 + Math.sin(tick * 0.12) * 0.12;
        double glowR = r * 1.9 * pulse;
        g2d.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 45));
        g2d.fill(new java.awt.geom.Ellipse2D.Double(cx - glowR, cy - glowR, glowR * 2, glowR * 2));

        if (type == ItemType.STAR) {
            Path2D star = new Path2D.Double();
            for (int i = 0; i < 10; i++) {
                double ang = rotation - Math.PI / 2 + i * Math.PI / 5;
                double rr = (i % 2 == 0) ? r * 1.15 : r * 0.5;
                double px = cx + Math.cos(ang) * rr;
                double py = cy + Math.sin(ang) * rr;
                if (i == 0) star.moveTo(px, py);
                else star.lineTo(px, py);
            }
            star.closePath();
            g2d.setColor(c);
            g2d.fill(star);
            g2d.setColor(new Color(255, 245, 200));
            g2d.setStroke(new BasicStroke(1.2f));
            g2d.draw(star);
        } else {
            // 폭탄 본체
            double br = r * 0.95;
            g2d.setColor(new Color(44, 40, 58));
            g2d.fill(new java.awt.geom.Ellipse2D.Double(cx - br, cy - br + 1, br * 2, br * 2));
            g2d.setColor(c);
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.draw(new java.awt.geom.Ellipse2D.Double(cx - br, cy - br + 1, br * 2, br * 2));
            // 하이라이트
            g2d.setColor(new Color(255, 255, 255, 150));
            double hr = br * 0.32;
            g2d.fill(new java.awt.geom.Ellipse2D.Double(cx - br * 0.5, cy - br * 0.45, hr, hr));
            // 심지
            double fx = cx + br * 0.55;
            double fy = cy - br * 0.7;
            g2d.setColor(new Color(210, 200, 180));
            g2d.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2d.draw(new java.awt.geom.Line2D.Double(fx, fy, fx + r * 0.35, fy - r * 0.35));
            // 불꽃 (깜빡임)
            double sr = r * (0.28 + ((tick / 4) % 2) * 0.1);
            double sx = fx + r * 0.35;
            double sy = fy - r * 0.35;
            g2d.setColor(new Color(255, 170, 40));
            g2d.fill(new java.awt.geom.Ellipse2D.Double(sx - sr, sy - sr, sr * 2, sr * 2));
            g2d.setColor(new Color(255, 245, 190));
            g2d.fill(new java.awt.geom.Ellipse2D.Double(sx - sr * 0.45, sy - sr * 0.45, sr * 0.9, sr * 0.9));
        }
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public ItemType getType() {
        return type;
    }
}
