package com.dodging.game.ui;

import com.dodging.game.model.Ball;
import com.dodging.game.model.GameState;
import com.dodging.game.model.Player;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 게임 메인 렌더링 및 게임 루프를 처리하는 패널
 */
public class GamePanel extends JPanel implements ActionListener {
    public static final int PANEL_WIDTH = 800;
    public static final int PANEL_HEIGHT = 600;
    public static final int FPS = 60;
    public static final int DELAY = 1000 / FPS;

    private final Timer gameTimer;
    private GameState gameState = GameState.READY;

    // 플레이어 객체
    private final Player player;

    // 공 리스트
    private final List<Ball> balls = new ArrayList<>();

    // 공 스폰 주기 관리
    private int spawnCounter = 0;
    private int spawnInterval = 25; // 25프레임마다 1개 스폰 (약 0.4초)

    // 키 입력 플래그
    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean leftPressed = false;
    private boolean rightPressed = false;

    public GamePanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(new Color(24, 26, 32));
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
                resetKeyStates();
                repaint();
            }
        } else if (gameState == GameState.PAUSED) {
            if (keyCode == KeyEvent.VK_P) {
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
        spawnCounter = 0;
        resetKeyStates();
        gameState = GameState.PLAYING;
    }

    private void triggerGameOver() {
        gameState = GameState.GAME_OVER;
        resetKeyStates();
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

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameState == GameState.PLAYING) {
            updateGame();
        }
        repaint();
    }

    private void updateGame() {
        // 1. 플레이어 위치 업데이트 (방향키 조작)
        player.update(upPressed, downPressed, leftPressed, rightPressed, PANEL_WIDTH, PANEL_HEIGHT);

        // 2. 공 스폰 관리
        spawnCounter++;
        if (spawnCounter >= spawnInterval) {
            spawnCounter = 0;
            balls.add(Ball.createRandomBall(PANEL_WIDTH, PANEL_HEIGHT, 1.0, player.getX(), player.getY()));
        }

        // 3. 공 이동 및 충돌 판정
        for (int i = 0; i < balls.size(); i++) {
            Ball ball = balls.get(i);
            ball.update();

            // 충돌 감지
            if (ball.collidesWith(player)) {
                triggerGameOver();
                return;
            }
        }

        // 4. 화면 밖으로 벗어난 공 제거
        balls.removeIf(b -> b.isOutOfBounds(PANEL_WIDTH, PANEL_HEIGHT));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // 안티앨리어싱 설정
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // 공 렌더링
        for (Ball ball : balls) {
            ball.draw(g2d);
        }

        // 플레이어 렌더링
        player.draw(g2d);

        // 상태별 오버레이 화면 렌더링
        if (gameState == GameState.READY) {
            drawReadyScreen(g2d);
        } else if (gameState == GameState.PAUSED) {
            drawPausedScreen(g2d);
        } else if (gameState == GameState.GAME_OVER) {
            drawGameOverScreen(g2d);
        }
    }

    private void drawReadyScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 160));
        g2d.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 40));
        drawCenteredString(g2d, "공피하기 게임 (Ball Dodging)", PANEL_HEIGHT / 2 - 60);

        g2d.setFont(new Font("Malgun Gothic", Font.PLAIN, 20));
        g2d.setColor(new Color(200, 200, 200));
        drawCenteredString(g2d, "키보드 방향키(↑, ↓, ←, →)로 공을 피하세요!", PANEL_HEIGHT / 2);
        
        g2d.setColor(new Color(255, 215, 0));
        g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 22));
        drawCenteredString(g2d, "시작하려면 SPACE 또는 ENTER 키를 누르세요", PANEL_HEIGHT / 2 + 60);
    }

    private void drawPausedScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 160));
        g2d.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 36));
        drawCenteredString(g2d, "일시 정지 (PAUSED)", PANEL_HEIGHT / 2 - 20);

        g2d.setFont(new Font("Malgun Gothic", Font.PLAIN, 18));
        g2d.setColor(new Color(220, 220, 220));
        drawCenteredString(g2d, "계속하려면 P 키를 누르세요", PANEL_HEIGHT / 2 + 30);
    }

    private void drawGameOverScreen(Graphics2D g2d) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        g2d.setColor(new Color(255, 75, 75));
        g2d.setFont(new Font("Malgun Gothic", Font.BOLD, 44));
        drawCenteredString(g2d, "GAME OVER", PANEL_HEIGHT / 2 - 40);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Malgun Gothic", Font.PLAIN, 20));
        drawCenteredString(g2d, "다시 시작하려면 R 또는 SPACE 키를 누르세요", PANEL_HEIGHT / 2 + 30);
    }

    private void drawCenteredString(Graphics2D g2d, String text, int y) {
        FontMetrics fm = g2d.getFontMetrics();
        int x = (PANEL_WIDTH - fm.stringWidth(text)) / 2;
        g2d.drawString(text, x, y);
    }
}
