package com.dodging.game.ui;

import javax.swing.*;

/**
 * 게임 메인 윈도우 프레임
 */
public class GameWindow extends JFrame {
    public GameWindow() {
        setTitle("공피하기 게임 (Ball Dodging Game)");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        GamePanel gamePanel = new GamePanel();
        add(gamePanel);

        pack();
        setLocationRelativeTo(null); // 화면 중앙 배치
    }
}
