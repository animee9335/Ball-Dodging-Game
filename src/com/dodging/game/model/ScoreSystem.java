package com.dodging.game.model;

/**
 * 점수 · 콤보 · 피버 규칙을 관리하는 클래스
 *
 * <ul>
 *   <li>생존 점수: 1초당 100점 × 난이도 배율 × 피버 배율</li>
 *   <li>콤보: 아이템을 먹은 뒤 {@link #COMBO_WINDOW_FRAMES} 안에 다음 아이템을 먹으면 콤보 +1
 *       (시간이 지나면 콤보 초기화). 콤보가 높을수록 아이템 점수가 커짐</li>
 *   <li>피버: 콤보가 {@link #FEVER_COMBO}에 도달하면 {@link #FEVER_FRAMES} 동안 모든 점수 ×2.
 *       피버 중 아이템을 먹으면 피버 시간이 늘어남</li>
 * </ul>
 */
public class ScoreSystem {
    public static final int FPS = 60;
    public static final int COMBO_WINDOW_FRAMES = 7 * FPS;  // 콤보 유지 시간 7초
    public static final int FEVER_COMBO = 3;                // 3연속 획득 시 피버
    public static final int FEVER_FRAMES = 8 * FPS;         // 피버 지속 8초
    public static final int FEVER_EXTEND_FRAMES = 2 * FPS;  // 피버 중 획득 시 +2초 (최대 8초)
    public static final double FEVER_MULTIPLIER = 2.0;

    public static final int STAR_POINTS = 300;
    public static final int BOMB_BASE_POINTS = 100;
    public static final int BOMB_POINTS_PER_BALL = 30;

    private final Difficulty difficulty;
    private final double tickSeconds;

    private double score = 0;
    private int combo = 0;
    private int maxCombo = 0;
    private int comboTimer = 0;
    private int feverTimer = 0;
    private int feverCount = 0;
    private int itemsCollected = 0;
    private int ballsDestroyed = 0;

    /** 아이템 획득 결과 (화면 연출용) */
    public static class CollectResult {
        public final int points;
        public final int combo;
        public final boolean feverStarted;

        CollectResult(int points, int combo, boolean feverStarted) {
            this.points = points;
            this.combo = combo;
            this.feverStarted = feverStarted;
        }
    }

    public ScoreSystem(Difficulty difficulty, double tickSeconds) {
        this.difficulty = difficulty;
        this.tickSeconds = tickSeconds;
    }

    /** 매 프레임 호출: 생존 점수 적립 및 콤보/피버 타이머 감소 */
    public void tick() {
        score += 100.0 * tickSeconds * difficulty.getScoreMultiplier() * currentFeverMultiplier();

        if (comboTimer > 0) {
            comboTimer--;
            if (comboTimer == 0) {
                combo = 0; // 제한 시간 안에 다음 아이템을 못 먹으면 콤보 끊김
            }
        }
        if (feverTimer > 0) {
            feverTimer--;
        }
    }

    /**
     * 아이템 획득 처리
     * @param type 아이템 종류
     * @param destroyedBalls 폭탄으로 파괴한 공 개수 (별이면 0)
     */
    public CollectResult collect(Item.ItemType type, int destroyedBalls) {
        itemsCollected++;
        ballsDestroyed += destroyedBalls;

        combo++;
        maxCombo = Math.max(maxCombo, combo);
        comboTimer = COMBO_WINDOW_FRAMES;

        boolean feverStarted = false;
        if (isFever()) {
            feverTimer = Math.min(FEVER_FRAMES, feverTimer + FEVER_EXTEND_FRAMES);
        } else if (combo >= FEVER_COMBO) {
            feverTimer = FEVER_FRAMES;
            feverCount++;
            feverStarted = true;
        }

        int base = (type == Item.ItemType.STAR)
                ? STAR_POINTS
                : BOMB_BASE_POINTS + BOMB_POINTS_PER_BALL * destroyedBalls;

        double points = base * comboMultiplier() * difficulty.getScoreMultiplier() * currentFeverMultiplier();
        int gained = (int) Math.round(points);
        score += gained;
        return new CollectResult(gained, combo, feverStarted);
    }

    /** 콤보 배율: 1콤보 ×1.0, 2콤보 ×1.5, 3콤보 ×2.0 ... 최대 ×4.0 */
    public double comboMultiplier() {
        return Math.min(4.0, 1.0 + 0.5 * Math.max(0, combo - 1));
    }

    public double currentFeverMultiplier() {
        return isFever() ? FEVER_MULTIPLIER : 1.0;
    }

    public boolean isFever() {
        return feverTimer > 0;
    }

    public int getScore() {
        return (int) score;
    }

    public int getCombo() {
        return combo;
    }

    public int getMaxCombo() {
        return maxCombo;
    }

    /** 콤보 유지 남은 비율 (0~1) */
    public double comboTimeRatio() {
        return comboTimer / (double) COMBO_WINDOW_FRAMES;
    }

    /** 피버 남은 비율 (0~1) */
    public double feverTimeRatio() {
        return feverTimer / (double) FEVER_FRAMES;
    }

    public int getFeverCount() {
        return feverCount;
    }

    public int getItemsCollected() {
        return itemsCollected;
    }

    public int getBallsDestroyed() {
        return ballsDestroyed;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }
}
