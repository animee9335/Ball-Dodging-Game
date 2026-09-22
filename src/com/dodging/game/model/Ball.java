package com.dodging.game.model;

import java.awt.*;
import java.util.Random;

/**
 * 날아오는 공(장애물) 클래스
 */
public class Ball {
    public enum BallType {
        NORMAL, // 일반 공
        FAST,   // 고속 공 (작고 빠름)
        BIG     // 대형 공 (크고 느림)
    }

    private static final Random RANDOM = new Random();

    private double x;
    private double y;
    private final double vx;
    private final double vy;
    private final double radius;
    private final Color color;
    private final BallType type;

    public Ball(double x, double y, double vx, double vy, double radius, Color color, BallType type) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.radius = radius;
        this.color = color;
        this.type = type;
    }

    /**
     * 화면 외곽에서 무작위로 공을 생성
     * @param screenWidth 화면 가로 너비
     * @param screenHeight 화면 세로 높이
     * @param speedMultiplier 난이도에 따른 속도 배율
     * @param targetX 유인 타겟 X 좌표 (플레이어 위치 등)
     * @param targetY 유인 타겟 Y 좌표 (플레이어 위치 등)
     */
    public static Ball createRandomBall(int screenWidth, int screenHeight, double speedMultiplier, double targetX, double targetY) {
        // 공 타입 무작위 결정 (70% 일반, 20% 고속, 10% 대형)
        int roll = RANDOM.nextInt(100);
        BallType type;
        double baseSpeed;
        double radius;
        Color color;

        if (roll < 70) {
            type = BallType.NORMAL;
            baseSpeed = 3.0 + RANDOM.nextDouble() * 1.8; // 3.0 ~ 4.8
            radius = 8.0;
            color = new Color(255, 87, 87); // 코랄 레드
        } else if (roll < 90) {
            type = BallType.FAST;
            baseSpeed = 5.5 + RANDOM.nextDouble() * 2.2; // 5.5 ~ 7.7
            radius = 5.5;
            color = new Color(255, 204, 0); // 황금 옐로우
        } else {
            type = BallType.BIG;
            baseSpeed = 1.8 + RANDOM.nextDouble() * 1.0; // 1.8 ~ 2.8
            radius = 16.0;
            color = new Color(187, 107, 255); // 퍼플
        }

        double finalSpeed = baseSpeed * speedMultiplier;

        // 4개 테두리 중 하나에서 스폰 (0: 상, 1: 하, 2: 좌, 3: 우)
        int side = RANDOM.nextInt(4);
        double spawnX = 0;
        double spawnY = 0;
        double margin = radius + 5;

        switch (side) {
            case 0: // 상단
                spawnX = RANDOM.nextDouble() * screenWidth;
                spawnY = -margin;
                break;
            case 1: // 하단
                spawnX = RANDOM.nextDouble() * screenWidth;
                spawnY = screenHeight + margin;
                break;
            case 2: // 좌측
                spawnX = -margin;
                spawnY = RANDOM.nextDouble() * screenHeight;
                break;
            case 3: // 우측
                spawnX = screenWidth + margin;
                spawnY = RANDOM.nextDouble() * screenHeight;
                break;
        }

        // 목표 위치: 플레이어 위치 근처 또는 화면 내부 무작위 지점
        // 플레이어를 완전히 정조준하면 피하기 어려우므로 오프셋(편차) 적용
        double spread = 120.0;
        double aimX = targetX + (RANDOM.nextDouble() * 2 - 1) * spread;
        double aimY = targetY + (RANDOM.nextDouble() * 2 - 1) * spread;

        // 이동 각도 계산
        double angle = Math.atan2(aimY - spawnY, aimX - spawnX);
        double vx = Math.cos(angle) * finalSpeed;
        double vy = Math.sin(angle) * finalSpeed;

        return new Ball(spawnX, spawnY, vx, vy, radius, color, type);
    }

    /**
     * 공 위치 업데이트
     */
    public void update() {
        x += vx;
        y += vy;
    }

    /**
     * 화면을 완전히 벗어났는지 확인 (여유 버퍼 60px)
     */
    public boolean isOutOfBounds(int screenWidth, int screenHeight) {
        double buffer = radius + 60;
        return x < -buffer || x > screenWidth + buffer || y < -buffer || y > screenHeight + buffer;
    }

    /**
     * 플레이어와의 원형 충돌 판정 (거리 제곱 비교)
     */
    public boolean collidesWith(Player player) {
        double dx = x - player.getX();
        double dy = y - player.getY();
        double distanceSq = dx * dx + dy * dy;
        double minDistance = radius + player.getRadius();
        return distanceSq <= (minDistance * minDistance);
    }

    /**
     * 공 렌더링
     */
    public void draw(Graphics2D g2d) {
        int drawX = (int) Math.round(x - radius);
        int drawY = (int) Math.round(y - radius);
        int diameter = (int) Math.round(radius * 2);

        // 글로우 효과
        g2d.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
        g2d.fillOval(drawX - 3, drawY - 3, diameter + 6, diameter + 6);

        // 본체
        g2d.setColor(color);
        g2d.fillOval(drawX, drawY, diameter, diameter);

        // 하이라이트 (입체감)
        int hlRadius = Math.max(2, (int) (radius * 0.4));
        int hlX = (int) Math.round(x - radius * 0.4);
        int hlY = (int) Math.round(y - radius * 0.4);
        g2d.setColor(new Color(255, 255, 255, 180));
        g2d.fillOval(hlX, hlY, hlRadius, hlRadius);
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

    public BallType getType() {
        return type;
    }
}
