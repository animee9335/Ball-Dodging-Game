package com.dodging.game.ui;

import com.dodging.game.model.GameState;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

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

    public GamePanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(new Color(24, 26, 32));
        setFocusable(true);

        gameTimer = new Timer(DELAY, this);
        gameTimer.start();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleGlobalKey(e.getKeyCode());
            }
        });
    }

    private void handleGlobalKey(int keyCode) {
        if (gameState == GameState.READY) {
            if (keyCode == KeyEvent.VK_SPACE || keyCode == KeyEvent.VK_ENTER) {
                startGame();
            }
        } else if (gameState == GameState.PLAYING) {
            if (keyCode == KeyEvent.VK_P) {
                gameState = GameState.PAUSED;
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
        gameState = GameState.PLAYING;
    }

    public GameState getGameState() {
        return gameState;
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameState == GameState.PLAYING) {
            updateGame();
        }
        repaint();
    }

    private void updateGame() {
        // 추후 플레이어 및 공 업데이트 로직 연결
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // 안티앨리어싱 설정
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        if (gameState == GameState.READY) {
            drawReadyScreen(g2d);
        } else if (gameState == GameState.PAUSED) {
            drawPausedScreen(g2d);
        } else if (gameState == GameState.GAME_OVER) {
            drawGameOverScreen(g2d);
        }
    }

    private void drawReadyScreen(Graphics2D g2d) {
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
