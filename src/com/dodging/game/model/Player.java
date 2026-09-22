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

    // 무적 상태 (게임 시작 시 1.5초간 안전) - 프레임 단위로 관리하여 일시정지 중에는 줄어들지 않음
    private int invincibleFrames = 0;
    private int animTick = 0;
    private boolean fever = false;

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
        animTick++;
        if (invincibleFrames > 0) {
            invincibleFrames--;
        }

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
                        fever ? new Color(255, 200, 60) : new Color(0, 229, 255)
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
        return invincibleFrames > 0;
    }

    public void setInvincibleFrames(int frames) {
        this.invincibleFrames = frames;
    }

    /** 상단 HUD 영역으로 들어가지 않도록 최소 Y 좌표 제한 */
    public void clampMinY(double minY) {
        if (y < minY) {
            y = minY;
        }
    }

    public void setFever(boolean fever) {
        this.fever = fever;
    }

    /**
     * 플레이어 그래픽 렌더링
     */
    public void draw(Graphics2D g2d) {
        // 무적 시간 동안은 깜빡임 연출
        if (isInvincible()) {
            if ((animTick / 6) % 2 == 0) {
                return;
            }
        }

        int drawX = (int) Math.round(x - radius);
        int drawY = (int) Math.round(y - radius);
        int diameter = (int) Math.round(radius * 2);

        // 피버 중에는 금색 오라가 회전
        if (fever) {
            double pulse = 6 + Math.sin(animTick * 0.2) * 3;
            int auraD = (int) Math.round(diameter + pulse * 2 + 6);
            g2d.setColor(new Color(255, 200, 60, 50));
            g2d.fillOval((int) Math.round(x - auraD / 2.0), (int) Math.round(y - auraD / 2.0), auraD, auraD);
            g2d.setColor(new Color(255, 215, 90, 200));
            g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            int ringD = diameter + 12;
            g2d.drawArc((int) Math.round(x - ringD / 2.0), (int) Math.round(y - ringD / 2.0), ringD, ringD, animTick * 6, 110);
            g2d.drawArc((int) Math.round(x - ringD / 2.0), (int) Math.round(y - ringD / 2.0), ringD, ringD, animTick * 6 + 180, 110);
        }

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
        this.fever = false;
        this.animTick = 0;
        setInvincibleFrames(90); // 리셋 시 1.5초(90프레임) 무적
    }
}
