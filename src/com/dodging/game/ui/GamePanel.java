package com.dodging.game.ui;

import com.dodging.game.model.Ball;
import com.dodging.game.model.GameState;
import com.dodging.game.model.Particle;
import com.dodging.game.model.Player;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 게임 메인 렌더링 및 게임 루프를 처리하는 패널
 */
public class GamePanel extends JPanel implements ActionListener {
    public static final int PANEL_WIDTH = 800;
    public static final int PANEL_HEIGHT = 600;
    public static final int FPS = 60;
    public static final int DELAY = 1000 / FPS;

    private static final Random RANDOM = new Random();

    private final Timer gameTimer;
    private GameState gameState = GameState.READY;

    // 플레이어 객체
    private final Player player;

    // 공 및 파티클 리스트
    private final List<Ball> balls = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();

    // 공 스폰 주기 관리
    private int spawnCounter = 0;
    private int spawnInterval = 25; // 기본 25프레임 (시간 경과에 따라 단축)
    private double speedMultiplier = 1.0;

    // 점수 및 시간 관리
    private long gameStartTime = 0;
    private long totalElapsedTime = 0; // 누적 생존 시간 (ms)
    private long pauseStartTime = 0;
    private int score = 0;
    private int bestScore = 0;
    private double bestSurvivalSeconds = 0.0;
    private boolean isNewRecord = false;
    private int currentLevel = 1;

    // 키 입력 플래그
    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean leftPressed = false;
    private boolean rightPressed = false;

    public GamePanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(new Color(20, 22, 28));
        setFocusable(true);

        player = new Player(PANEL_WIDTH / 2.0, PANEL_HEIGHT / 2.0);

        gameTimer = new Timer(DELAY, this);
        gameTimer.start();

        // 키 이벤트 리스너 등록
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleGlobalKey(e.getKeyCode());
                handleMovementKey(e.getKeyCode(), true);
            }

            @Override
            public void keyReleased(KeyEvent e) {
                handleMovementKey(e.getKeyCode(), false);
            }
        });

        // 포커스를 잃었을 때 키 상태 초기화
        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                resetKeyStates();
            }
        });
    }

    private void handleMovementKey(int keyCode, boolean isPressed) {
        switch (keyCode) {
            case KeyEvent.VK_UP:
            case KeyEvent.VK_W:
                upPressed = isPressed;
                break;
            case KeyEvent.VK_DOWN:
            case KeyEvent.VK_S:
                downPressed = isPressed;
                break;
            case KeyEvent.VK_LEFT:
            case KeyEvent.VK_A:
                leftPressed = isPressed;
                break;
            case KeyEvent.VK_RIGHT:
            case KeyEvent.VK_D:
                rightPressed = isPressed;
                break;
            default:
                break;
        }
    }

    private void resetKeyStates() {
        upPressed = false;
        downPressed = false;
        leftPressed = false;
        rightPressed = false;
    }

    private void handleGlobalKey(int keyCode) {
        if (gameState == GameState.READY) {
            if (keyCode == KeyEvent.VK_SPACE || keyCode == KeyEvent.VK_ENTER) {
                startGame();
            }
        } else if (gameState == GameState.PLAYING) {
            if (keyCode == KeyEvent.VK_P) {
                gameState = GameState.PAUSED;
                pauseStartTime = System.currentTimeMillis();
                resetKeyStates();
                repaint();
            }
        } else if (gameState == GameState.PAUSED) {
            if (keyCode == KeyEvent.VK_P) {
                // 일시 정지 해제: 정지되었던 시간만큼 시작 시간 보정
                long pausedDuration = System.currentTimeMillis() - pauseStartTime;
                gameStartTime += pausedDuration;
                gameState = GameState.PLAYING;
            }
        } else if (gameState == GameState.GAME_OVER) {
            if (keyCode == KeyEvent.VK_R || keyCode == KeyEvent.VK_SPACE || keyCode == KeyEvent.VK_ENTER) {
                startGame();
            }
        }
    }

    public void startGame() {
        player.reset(PANEL_WIDTH / 2.0, PANEL_HEIGHT / 2.0);
        balls.clear();
        particles.clear();
        spawnCounter = 0;
        spawnInterval = 25;
        speedMultiplier = 1.0;
        totalElapsedTime = 0;
        gameStartTime = System.currentTimeMillis();
        score = 0;
        isNewRecord = false;
        currentLevel = 1;
        resetKeyStates();
        gameState = GameState.PLAYING;
    }

    private void triggerGameOver() {
        gameState = GameState.GAME_OVER;
        resetKeyStates();

        // 충돌 지점에 파티클 폭발 효과 생성
        createExplosion(player.getX(), player.getY());

        double survivalSeconds = totalElapsedTime / 1000.0;
        if (score > bestScore) {
            bestScore = score;
            bestSurvivalSeconds = survivalSeconds;
            isNewRecord = true;
        }
    }

    private void createExplosion(double x, double y) {
        Color[] colors = {
                new Color(255, 75, 75),
                new Color(255, 165, 0),
                new Color(255, 220, 0),
                new Color(0, 229, 255),
                Color.WHITE
        };

        for (int i = 0; i < 40; i++) {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double speed = 1.5 + RANDOM.nextDouble() * 6.0;
            double vx = Math.cos(angle) * speed;
            double vy = Math.sin(angle) * speed;
            double size = 3.0 + RANDOM.nextDouble() * 5.0;
            float decay = 0.02f + (float) (RANDOM.nextDouble() * 0.03);
            Color color = colors[RANDOM.nextInt(colors.length)];

            particles.add(new Particle(x, y, vx, vy, size, 1.0f, decay, color));
        }
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public Player getPlayer() {
        return player;
    }

    public List<Ball> getBalls() {
        return balls;
    }

    public int getScore() {
        return score;
    }

    public int getBestScore() {
        return bestScore;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameState == GameState.PLAYING) {
            updateGame();
        } else if (gameState == GameState.GAME_OVER) {
            // 게임오버 시에도 잔여 파티클은 자연스럽게 소멸
            updateParticles();
        }
        repaint();
    }

    private void updateParticles() {
        for (int i = 0; i < particles.size(); i++) {
            particles.get(i).update();
        }
        particles.removeIf(Particle::isDead);
    }

    private void updateGame() {
        // 1. 시간 및 난이도 업데이트
        totalElapsedTime = System.currentTimeMillis() - gameStartTime;
        double survivalSeconds = totalElapsedTime / 1000.0;

        score = (int) (survivalSeconds * 100);
        currentLevel = 1 + (int) (survivalSeconds / 8.0);
        spawnInterval = Math.max(8, 25 - (int) (survivalSeconds / 3.0));
        speedMultiplier = 1.0 + (survivalSeconds / 20.0) * 0.35;

        // 2. 플레이어 위치 및 잔상 업데이트
        player.update(upPressed, downPressed, leftPressed, rightPressed, PANEL_WIDTH, PANEL_HEIGHT, particles);

        // 3. 파티클 업데이트
        updateParticles();

        // 4. 공 스폰 관리
        spawnCounter++;
        if (spawnCounter >= spawnInterval) {
            spawnCounter = 0;
            balls.add(Ball.createRandomBall(PANEL_WIDTH, PANEL_HEIGHT, speedMultiplier, player.getX(), player.getY()));

            if (currentLevel >= 4 && Math.random() < 0.35) {
                balls.add(Ball.createRandomBall(PANEL_WIDTH, PANEL_HEIGHT, speedMultiplier, player.getX(), player.getY()));
            }
        }

        // 5. 공 이동 및 충돌 판정
        boolean playerInvincible = player.isInvincible();
        for (int i = 0; i < balls.size(); i++) {
            Ball ball = balls.get(i);
            ball.update();

            // 충돌 감지 (무적 상태가 아닐 때만 유효)
            if (!playerInvincible && ball.collidesWith(player)) {
                triggerGameOver();
                return;
            }
        }

        // 6. 화면 밖으로 벗어난 공 제거
        balls.removeIf(b -> b.isOutOfBounds(PANEL_WIDTH, PANEL_HEIGHT));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // 안티앨리어싱 설정
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 배경 그리드 렌더링
        drawBackgroundGrid(g2d);

        // 파티클 렌더링
        for (Particle particle : particles) {
            particle.draw(g2d);
        }

        // 공 렌더링
        for (Ball ball : balls) {
            ball.draw(g2d);
        }

        // 플레이어 렌더링 (게임 오버 시 숨김)
        if (gameState != GameState.GAME_OVER) {
            player.draw(g2d);
        }

        // 상단 HUD 렌더링 (게임 진행 중 또는 정지 시)
        if (gameState == GameState.PLAYING || gameState == GameState.PAUSED) {
            drawHUD(g2d);
        }

        // 상태별 오버레이 화면 렌더링
        if (gameState == GameState.READY) {
            drawReadyScreen(g2d);
        } else if (gameState == GameState.PAUSED) {
            drawPausedScreen(g2d);
        } else if (gameState == GameState.GAME_OVER) {
            drawGameOverScreen(g2d);
        }
    }

    private void drawBackgroundGrid(Graphics2D g2d) {
        g2d.setColor(new Color(30, 34, 44));
        g2d.setStroke(new BasicStroke(1.0f));
        int gridSize = 50;

        for (int x = 0; x < PANEL_WIDTH; x += gridSize) {
            g2d.drawLine(x, 0, x, PANEL_HEIGHT);
        }
        for (int y = 0; y < PANEL_HEIGHT; y += gridSize) {
            g2d.drawLine(0, y, PANEL_WIDTH, y);
        }
    }

    private void drawHUD(Graphics2D g2d) {
        g2d.setColor(new Color(15, 17, 24, 210));
        g2d.fillRect(0, 0, PANEL_WIDTH, 48);

        g2d.setColor(new Color(55, 62, 78));
        g2d.setStroke(new BasicStroke(1.2f));
        g2d.drawLine(0, 48, PANEL_WIDTH, 48);

        g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 15));

        // 생존 시간
        double seconds = totalElapsedTime / 1000.0;
        g2d.setColor(new Color(0, 229, 255));
        g2d.drawString(String.format("TIME: %.1fs", seconds), 24, 30);

        // 점수
        g2d.setColor(Color.WHITE);
        g2d.drawString(String.format("SCORE: %,d", score), 200, 30);

        // 레벨
        g2d.setColor(new Color(255, 190, 0));
        g2d.drawString(String.format("LEVEL: %d", currentLevel), 390, 30);

        // 공 개수
        g2d.setColor(new Color(190, 200, 220));
        g2d.drawString(String.format("BALLS: %d", balls.size()), 530, 30);

        // 최고 점수
        if (bestScore > 0) {
            g2d.setColor(new Color(255, 215, 0));
            g2d.drawString(String.format("BEST: %,d", bestScore), 670, 30);
        }
    }

    private void drawReadyScreen(Graphics2D g2d) {
        g2d.setColor(new Color(12, 14, 20, 200));
        g2d.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        // 타이틀 테두리 박스
        g2d.setColor(new Color(0, 229, 255, 40));
        g2d.fillRoundRect(PANEL_WIDTH / 2 - 280, PANEL_HEIGHT / 2 - 160, 560, 320, 24, 24);
        g2d.setColor(new Color(0, 229, 255, 120));
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawRoundRect(PANEL_WIDTH / 2 - 280, PANEL_HEIGHT / 2 - 160, 560, 320, 24, 24);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 38));
        drawCenteredString(g2d, "공피하기 게임 (Ball Dodging)", PANEL_HEIGHT / 2 - 80);

        g2d.setFont(new Font("Malgun Gothic", Font.PLAIN, 17));
        g2d.setColor(new Color(210, 220, 235));
        drawCenteredString(g2d, "조작: 키보드 방향키(↑, ↓, ←, →) 또는 WASD", PANEL_HEIGHT / 2 - 25);
        drawCenteredString(g2d, "시간이 지날수록 공의 속도와 개수가 증가합니다!", PANEL_HEIGHT / 2 + 10);
        drawCenteredString(g2d, "일시정지: P 키", PANEL_HEIGHT / 2 + 40);

        g2d.setColor(new Color(255, 215, 0));
        g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 21));
        drawCenteredString(g2d, "시작하려면 SPACE 또는 ENTER 키를 누르세요", PANEL_HEIGHT / 2 + 100);

        if (bestScore > 0) {
            g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 16));
            g2d.setColor(new Color(0, 229, 255));
            drawCenteredString(g2d, String.format("최고 기록: %,d점 (%.1f초)", bestScore, bestSurvivalSeconds), PANEL_HEIGHT / 2 + 135);
        }
    }

    private void drawPausedScreen(Graphics2D g2d) {
        g2d.setColor(new Color(10, 12, 18, 180));
        g2d.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 36));
        drawCenteredString(g2d, "일시 정지 (PAUSED)", PANEL_HEIGHT / 2 - 20);

        g2d.setFont(new Font("Malgun Gothic", Font.PLAIN, 18));
        g2d.setColor(new Color(220, 225, 240));
        drawCenteredString(g2d, "계속하려면 P 키를 누르세요", PANEL_HEIGHT / 2 + 30);
    }

    private void drawGameOverScreen(Graphics2D g2d) {
        g2d.setColor(new Color(10, 12, 18, 210));
        g2d.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        // 결과 박스
        g2d.setColor(new Color(255, 75, 75, 30));
        g2d.fillRoundRect(PANEL_WIDTH / 2 - 280, PANEL_HEIGHT / 2 - 160, 560, 320, 24, 24);
        g2d.setColor(new Color(255, 75, 75, 100));
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawRoundRect(PANEL_WIDTH / 2 - 280, PANEL_HEIGHT / 2 - 160, 560, 320, 24, 24);

        g2d.setColor(new Color(255, 75, 75));
        g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 46));
        drawCenteredString(g2d, "GAME OVER", PANEL_HEIGHT / 2 - 85);

        if (isNewRecord) {
            g2d.setColor(new Color(255, 215, 0));
            g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 19));
            drawCenteredString(g2d, "★ NEW RECORD! 최고 기록 달성! ★", PANEL_HEIGHT / 2 - 35);
        }

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 22));
        double seconds = totalElapsedTime / 1000.0;
        drawCenteredString(g2d, String.format("생존 시간: %.1f 초   |   최종 점수: %,d 점", seconds, score), PANEL_HEIGHT / 2 + 5);

        g2d.setFont(new Font("Malgun Gothic", Font.PLAIN, 17));
        g2d.setColor(new Color(180, 215, 255));
        drawCenteredString(g2d, String.format("최고 기록: %,d 점 (%.1f 초)", bestScore, bestSurvivalSeconds), PANEL_HEIGHT / 2 + 45);

        g2d.setColor(new Color(255, 215, 0));
        g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 21));
        drawCenteredString(g2d, "다시 시작하려면 R 또는 SPACE 키를 누르세요", PANEL_HEIGHT / 2 + 105);
    }

    private void drawCenteredString(Graphics2D g2d, String text, int y) {
        FontMetrics fm = g2d.getFontMetrics();
        int x = (PANEL_WIDTH - fm.stringWidth(text)) / 2;
        g2d.drawString(text, x, y);
    }
}
