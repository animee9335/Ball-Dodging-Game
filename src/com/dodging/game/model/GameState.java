package com.dodging.game.model;

/**
 * 게임의 진행 상태를 나타내는 열거형
 */
public enum GameState {
    READY,      // 게임 시작 전 대기 상태
    PLAYING,    // 게임 진행 중
    PAUSED,     // 일시 정지
    GAME_OVER   // 충돌 후 게임 종료
}
