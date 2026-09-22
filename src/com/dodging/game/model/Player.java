package com.dodging.game.model;

import java.awt.*;

/**
 * 플레이어 캐릭터 클래스
 */
public class Player {
    private double x;
    private double y;
    private final double radius;
    private final double speed;

    public Player(double startX, double startY) {
        this.x = startX;
        this.y = startY;
        this.radius = 12.0; // 반지름 (지름 24픽셀)
        this.speed = 5.0;   // 이동 속도
    }

    /**
     * 키 입력에 따른 플레이어 위치 업데이트
     * 대각선 이동 시 속도가 빨라지지 않도록 정규화 처리
     */
    public void update(boolean up, boolean down, boolean left, boolean right, int screenWidth, int screenHeight) {
        double dx = 0;
        double dy = 0;

        if (up) dy -= 1;
        if (down) dy += 1;
        if (left) dx -= 1;
        if (right) dx += 1;

        if (dx != 0 && dy != 0) {
            // 대각선 이동 시 속도 정규화 (1 / sqrt(2) ≈ 0.7071)
            double factor = 1.0 / Math.sqrt(2);
            dx *= factor;
            dy *= factor;
        }

        x += dx * speed;
        y += dy * speed;

        // 화면 경계 제한 (반지름만큼 안쪽에 위치하도록 클램핑)
        if (x - radius < 0) {
            x = radius;
        } else if (x + radius > screenWidth) {
            x = screenWidth - radius;
        }

        if (y - radius < 0) {
            y = radius;
        } else if (y + radius > screenHeight) {
            y = screenHeight - radius;
        }
    }

    /**
     * 플레이어 그래픽 렌더링
     */
    public void draw(Graphics2D g2d) {
        int drawX = (int) Math.round(x - radius);
        int drawY = (int) Math.round(y - radius);
        int diameter = (int) Math.round(radius * 2);

        // 외부 네온 글로우 효과
        g2d.setColor(new Color(0, 229, 255, 60));
        g2d.fillOval(drawX - 4, drawY - 4, diameter + 8, diameter + 8);

        // 플레이어 본체 (시안 블루 그라데이션)
        g2d.setColor(new Color(0, 200, 255));
        g2d.fillOval(drawX, drawY, diameter, diameter);

        // 플레이어 중심 코어 (밝은 화이트)
        int coreRadius = (int) (radius * 0.5);
        int coreX = (int) Math.round(x - coreRadius);
        int coreY = (int) Math.round(y - coreRadius);
        g2d.setColor(Color.WHITE);
        g2d.fillOval(coreX, coreY, coreRadius * 2, coreRadius * 2);

        // 외곽선
        g2d.setColor(new Color(150, 240, 255));
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawOval(drawX, drawY, diameter, diameter);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getRadius() {
        return radius;
    }

    public void reset(double startX, double startY) {
        this.x = startX;
        this.y = startY;
    }
}
