package com.dodging.game.model;

import java.awt.*;
import java.util.List;
import java.util.Random;

/**
 * 플레이어 캐릭터 클래스
 */
public class Player {
    private static final Random RANDOM = new Random();

    private double x;
    private double y;
    private final double radius;
    private final double speed;

    // 무적 상태 (게임 시작 시 1.5초간 안전)
    private long invincibleEndTime = 0;

    public Player(double startX, double startY) {
        this.x = startX;
        this.y = startY;
        this.radius = 12.0; // 반지름 (지름 24픽셀)
        this.speed = 5.2;   // 이동 속도
    }

    /**
     * 키 입력에 따른 플레이어 위치 업데이트 및 이동 잔상 파티클 생성
     */
    public void update(boolean up, boolean down, boolean left, boolean right, int screenWidth, int screenHeight, List<Particle> particles) {
        double dx = 0;
        double dy = 0;

        if (up) dy -= 1;
        if (down) dy += 1;
        if (left) dx -= 1;
        if (right) dx += 1;

        boolean isMoving = (dx != 0 || dy != 0);

        if (isMoving) {
            // 대각선 이동 시 속도 정규화 (1 / sqrt(2) ≈ 0.7071)
            double factor = 1.0 / Math.sqrt(2);
            if (dx != 0 && dy != 0) {
                dx *= factor;
                dy *= factor;
            }

            // 이동 시 후방 부스터 파티클 생성
            if (RANDOM.nextDouble() < 0.6) {
                double pvx = -dx * 1.5 + (RANDOM.nextDouble() - 0.5);
                double pvy = -dy * 1.5 + (RANDOM.nextDouble() - 0.5);
                particles.add(new Particle(
                        x + (RANDOM.nextDouble() - 0.5) * 6,
                        y + (RANDOM.nextDouble() - 0.5) * 6,
                        pvx, pvy, 4.0, 0.7f, 0.05f,
                        new Color(0, 229, 255)
                ));
            }
        }

        x += dx * speed;
        y += dy * speed;

        // 화면 경계 제한
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

    public boolean isInvincible() {
        return System.currentTimeMillis() < invincibleEndTime;
    }

    public void setInvincibleDuration(long durationMs) {
        this.invincibleEndTime = System.currentTimeMillis() + durationMs;
    }

    /**
     * 플레이어 그래픽 렌더링
     */
    public void draw(Graphics2D g2d) {
        // 무적 시간 동안은 깜빡임 연출
        if (isInvincible()) {
            if ((System.currentTimeMillis() / 100) % 2 == 0) {
                return;
            }
        }

        int drawX = (int) Math.round(x - radius);
        int drawY = (int) Math.round(y - radius);
        int diameter = (int) Math.round(radius * 2);

        // 외부 네온 글로우 효과
        g2d.setColor(new Color(0, 229, 255, 60));
        g2d.fillOval(drawX - 4, drawY - 4, diameter + 8, diameter + 8);

        // 플레이어 본체 (시안 블루)
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
        setInvincibleDuration(1500); // 리셋 시 1.5초 무적
    }
}
