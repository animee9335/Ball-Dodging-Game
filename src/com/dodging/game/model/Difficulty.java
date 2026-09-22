package com.dodging.game.model;

import java.awt.*;

/**
 * 난이도 설정 (초급 / 중급 / 고급)
 * 수치만 바꾸면 난이도 밸런스를 조정할 수 있도록 한 곳에 모아 두었습니다.
 */
public enum Difficulty {
    //        이름    영문      설명                         스폰시작 스폰최소 단축주기(초) 속도시작 속도증가(20초당) 속도최대 조준편차 2연발레벨 2연발확률 아이템주기(초) 점수배율  색상
    EASY("초급", "EASY", "느긋하게 즐기는 입문 난이도", 34, 14, 4.0, 0.80, 0.25, 2.0, 170, 6, 0.25, 3.5, 1.0, new Color(72, 214, 140)),
    NORMAL("중급", "NORMAL", "기존 게임과 같은 표준 난이도", 25, 9, 3.0, 1.00, 0.35, 2.6, 120, 4, 0.35, 4.0, 1.5, new Color(255, 190, 60)),
    HARD("고급", "HARD", "빠르고 촘촘한 탄막 도전", 18, 6, 2.2, 1.25, 0.45, 3.2, 80, 2, 0.50, 5.0, 2.0, new Color(255, 88, 104));

    private final String label;
    private final String englishLabel;
    private final String description;
    private final int spawnIntervalStart;   // 공 생성 간격 시작값 (프레임)
    private final int spawnIntervalMin;     // 공 생성 간격 최소값 (프레임)
    private final double spawnDecaySeconds; // 몇 초마다 생성 간격이 1프레임씩 줄어드는지
    private final double speedStart;        // 공 속도 배율 시작값
    private final double speedGrowthPer20s; // 20초마다 증가하는 속도 배율
    private final double speedMax;          // 공 속도 배율 상한
    private final double aimSpread;         // 공이 플레이어를 조준할 때의 편차(px) - 작을수록 정확
    private final int doubleSpawnLevel;     // 이 레벨부터 공이 2개씩 나올 수 있음
    private final double doubleSpawnChance; // 2개 동시 생성 확률
    private final double itemIntervalSeconds; // 아이템 생성 주기
    private final double scoreMultiplier;   // 점수 배율
    private final Color color;

    Difficulty(String label, String englishLabel, String description,
               int spawnIntervalStart, int spawnIntervalMin, double spawnDecaySeconds,
               double speedStart, double speedGrowthPer20s, double speedMax, double aimSpread,
               int doubleSpawnLevel, double doubleSpawnChance,
               double itemIntervalSeconds, double scoreMultiplier, Color color) {
        this.label = label;
        this.englishLabel = englishLabel;
        this.description = description;
        this.spawnIntervalStart = spawnIntervalStart;
        this.spawnIntervalMin = spawnIntervalMin;
        this.spawnDecaySeconds = spawnDecaySeconds;
        this.speedStart = speedStart;
        this.speedGrowthPer20s = speedGrowthPer20s;
        this.speedMax = speedMax;
        this.aimSpread = aimSpread;
        this.doubleSpawnLevel = doubleSpawnLevel;
        this.doubleSpawnChance = doubleSpawnChance;
        this.itemIntervalSeconds = itemIntervalSeconds;
        this.scoreMultiplier = scoreMultiplier;
        this.color = color;
    }

    /** 경과 시간에 따른 공 생성 간격(프레임) */
    public int spawnIntervalAt(double seconds) {
        return Math.max(spawnIntervalMin, spawnIntervalStart - (int) (seconds / spawnDecaySeconds));
    }

    /** 경과 시간에 따른 공 속도 배율 */
    public double speedMultiplierAt(double seconds) {
        return Math.min(speedMax, speedStart + (seconds / 20.0) * speedGrowthPer20s);
    }

    /** 메뉴 카드에 표시할 체감 수치 (1~3) */
    public int speedRating() {
        return ordinal() + 1;
    }

    public int densityRating() {
        return ordinal() + 1;
    }

    public String getLabel() {
        return label;
    }

    public String getEnglishLabel() {
        return englishLabel;
    }

    public String getDescription() {
        return description;
    }

    public double getAimSpread() {
        return aimSpread;
    }

    public int getDoubleSpawnLevel() {
        return doubleSpawnLevel;
    }

    public double getDoubleSpawnChance() {
        return doubleSpawnChance;
    }

    public double getItemIntervalSeconds() {
        return itemIntervalSeconds;
    }

    public double getScoreMultiplier() {
        return scoreMultiplier;
    }

    public Color getColor() {
        return color;
    }
}
