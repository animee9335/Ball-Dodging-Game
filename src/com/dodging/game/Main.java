package com.dodging.game;

import com.dodging.game.ui.GameWindow;

import javax.swing.*;

/**
 * 애플리케이션 시작 지점
 */
public class Main {
    public static void main(String[] args) {
        // Swing GUI는 Event Dispatch Thread에서 안전하게 실행
        SwingUtilities.invokeLater(() -> {
            GameWindow window = new GameWindow();
            window.setVisible(true);
        });
    }
}
