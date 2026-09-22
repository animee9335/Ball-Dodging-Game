package com.dodging.game.ui;

import com.dodging.game.model.Ball;
import com.dodging.game.model.Difficulty;
import com.dodging.game.model.FloatingText;
import com.dodging.game.model.GameState;
import com.dodging.game.model.Item;
import com.dodging.game.model.Particle;
import com.dodging.game.model.Player;
import com.dodging.game.model.ScoreSystem;
import com.dodging.game.model.Shockwave;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 게임 루프, 입력 처리, 렌더링을 담당하는 메인 패널
 *
 * 화면 구성
 *  - READY     : 메인 메뉴 (난이도 선택 카드 + 아이템 설명)
 *  - PLAYING   : 게임 화면 + 상단 HUD + 콤보/피버 게이지
 *  - PAUSED    : 일시정지 카드
 *  - GAME_OVER : 결과 카드 (점수, 생존 시간, 최대 콤보 등)
 */
public class GamePanel extends JPanel implements ActionListener {
    public static final int PANEL_WIDTH = 800;
    public static final int PANEL_HEIGHT = 600;
    public static final int FPS = 60;
    public static final int DELAY = 1000 / FPS;
    /** 한 프레임이 나타내는 게임 시간(초). 시간은 프레임 누적으로 계산하므로 일시정지가 자동 반영됨 */
    private static final double TICK_SECONDS = DELAY / 1000.0;

    private static final int HUD_HEIGHT = 56;
    private static final int MAX_ITEMS_ON_FIELD = 2;
    private static final int GAME_OVER_INPUT_DELAY = 30; // 게임오버 직후 0.5초간 재시작 입력 무시

    private static final Random RANDOM = new Random();

    // ---- 레이아웃 (마우스 클릭 판정에도 사용) ----
    private static final int CARD_W = 204;
    private static final int CARD_H = 184;
    private static final int CARD_GAP = 20;
    private static final int CARDS_X = (PANEL_WIDTH - (CARD_W * 3 + CARD_GAP * 2)) / 2;
    private static final int CARDS_Y = 184;
    private static final Rectangle START_BUTTON = new Rectangle(PANEL_WIDTH / 2 - 130, 394, 260, 50);

    private static final Rectangle OVER_CARD = new Rectangle(PANEL_WIDTH / 2 - 240, 110, 480, 380);
    private static final Rectangle OVER_RETRY = new Rectangle(OVER_CARD.x + 28, OVER_CARD.y + 300, 204, 50);
    private static final Rectangle OVER_MENU = new Rectangle(OVER_CARD.x + OVER_CARD.width - 28 - 204, OVER_CARD.y + 300, 204, 50);

    private static final Rectangle PAUSE_CARD = new Rectangle(PANEL_WIDTH / 2 - 200, PANEL_HEIGHT / 2 - 110, 400, 220);
    private static final Rectangle PAUSE_RESUME = new Rectangle(PAUSE_CARD.x + 28, PAUSE_CARD.y + 136, 164, 48);
    private static final Rectangle PAUSE_MENU = new Rectangle(PAUSE_CARD.x + PAUSE_CARD.width - 28 - 164, PAUSE_CARD.y + 136, 164, 48);

    private final Timer gameTimer;
    private GameState gameState = GameState.READY;

    // 게임 오브젝트
    private final Player player;
    private final List<Ball> balls = new ArrayList<>();
    private final List<Item> items = new ArrayList<>();
    private final List<Particle> particles = new ArrayList<>();
    private final List<FloatingText> floatingTexts = new ArrayList<>();
    private final List<Shockwave> shockwaves = new ArrayList<>();

    // 난이도 / 진행 상태
    private Difficulty selectedDifficulty = Difficulty.NORMAL;
    private Difficulty difficulty = Difficulty.NORMAL;
    private ScoreSystem scoreSystem = new ScoreSystem(Difficulty.NORMAL, TICK_SECONDS);
    private int playFrames = 0;       // PLAYING 상태에서만 증가
    private int spawnCounter = 0;
    private int itemSpawnCounter = 0;
    private int currentLevel = 1;

    // 기록 (난이도별 최고 점수)
    private final Map<Difficulty, Integer> bestScores = new EnumMap<>(Difficulty.class);
    private boolean isNewRecord = false;

    // 연출
    private int globalTick = 0;       // 애니메이션용 (항상 증가)
    private int gameOverTick = 0;
    private int shakeFrames = 0;
    private double shakeStrength = 0;
    private float flashAlpha = 0f;
    private Color flashColor = Color.WHITE;
    private BufferedImage backgroundCache;

    // 입력
    private boolean upPressed = false;
    private boolean downPressed = false;
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private int hoverCard = -1;
    private Rectangle hoverButton = null;

    public GamePanel() {
        setPreferredSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));
        setBackground(Theme.BG_BOTTOM);
        setFocusable(true);
        setDoubleBuffered(true);

        player = new Player(PANEL_WIDTH / 2.0, PANEL_HEIGHT / 2.0);

        gameTimer = new Timer(DELAY, this);
        gameTimer.start();

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

        addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                resetKeyStates();
                if (gameState == GameState.PLAYING) {
                    pauseGame(); // 창 포커스를 잃으면 자동 일시정지
                }
            }
        });

        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                updateHover(e.getPoint());
            }

            @Override
            public void mousePressed(MouseEvent e) {
                requestFocusInWindow();
                handleClick(e.getPoint());
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);
    }

    // =====================================================================
    // 입력 처리
    // =====================================================================

    private void handleMovementKey(int keyCode, boolean isPressed) {
        switch (keyCode) {
            case KeyEvent.VK_UP, KeyEvent.VK_W -> upPressed = isPressed;
            case KeyEvent.VK_DOWN, KeyEvent.VK_S -> downPressed = isPressed;
            case KeyEvent.VK_LEFT, KeyEvent.VK_A -> leftPressed = isPressed;
            case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> rightPressed = isPressed;
            default -> {
            }
        }
    }

    private void resetKeyStates() {
        upPressed = false;
        downPressed = false;
        leftPressed = false;
        rightPressed = false;
    }

    private void handleGlobalKey(int keyCode) {
        switch (gameState) {
            case READY -> {
                switch (keyCode) {
                    case KeyEvent.VK_LEFT, KeyEvent.VK_A -> selectDifficulty(selectedDifficulty.ordinal() - 1);
                    case KeyEvent.VK_RIGHT, KeyEvent.VK_D -> selectDifficulty(selectedDifficulty.ordinal() + 1);
                    case KeyEvent.VK_1, KeyEvent.VK_NUMPAD1 -> selectDifficulty(0);
                    case KeyEvent.VK_2, KeyEvent.VK_NUMPAD2 -> selectDifficulty(1);
                    case KeyEvent.VK_3, KeyEvent.VK_NUMPAD3 -> selectDifficulty(2);
                    case KeyEvent.VK_SPACE, KeyEvent.VK_ENTER -> startGame(selectedDifficulty);
                    default -> {
                    }
                }
            }
            case PLAYING -> {
                if (keyCode == KeyEvent.VK_P || keyCode == KeyEvent.VK_ESCAPE) {
                    pauseGame();
                }
            }
            case PAUSED -> {
                if (keyCode == KeyEvent.VK_P || keyCode == KeyEvent.VK_ESCAPE || keyCode == KeyEvent.VK_SPACE) {
                    gameState = GameState.PLAYING;
                } else if (keyCode == KeyEvent.VK_M) {
                    goToMenu();
                }
            }
            case GAME_OVER -> {
                if (gameOverTick < GAME_OVER_INPUT_DELAY) {
                    return;
                }
                if (keyCode == KeyEvent.VK_R || keyCode == KeyEvent.VK_SPACE || keyCode == KeyEvent.VK_ENTER) {
                    startGame(difficulty);
                } else if (keyCode == KeyEvent.VK_M || keyCode == KeyEvent.VK_ESCAPE) {
                    goToMenu();
                }
            }
        }
    }

    private void selectDifficulty(int index) {
        Difficulty[] values = Difficulty.values();
        int clamped = Math.max(0, Math.min(values.length - 1, index));
        selectedDifficulty = values[clamped];
    }

    private Rectangle cardBounds(int index) {
        return new Rectangle(CARDS_X + index * (CARD_W + CARD_GAP), CARDS_Y, CARD_W, CARD_H);
    }

    private void updateHover(Point p) {
        hoverCard = -1;
        hoverButton = null;
        if (gameState == GameState.READY) {
            for (int i = 0; i < Difficulty.values().length; i++) {
                if (cardBounds(i).contains(p)) {
                    hoverCard = i;
                }
            }
            if (START_BUTTON.contains(p)) hoverButton = START_BUTTON;
        } else if (gameState == GameState.GAME_OVER) {
            if (OVER_RETRY.contains(p)) hoverButton = OVER_RETRY;
            if (OVER_MENU.contains(p)) hoverButton = OVER_MENU;
        } else if (gameState == GameState.PAUSED) {
            if (PAUSE_RESUME.contains(p)) hoverButton = PAUSE_RESUME;
            if (PAUSE_MENU.contains(p)) hoverButton = PAUSE_MENU;
        }
        boolean clickable = hoverCard >= 0 || hoverButton != null;
        setCursor(Cursor.getPredefinedCursor(clickable ? Cursor.HAND_CURSOR : Cursor.DEFAULT_CURSOR));
    }

    private void handleClick(Point p) {
        switch (gameState) {
            case READY -> {
                for (int i = 0; i < Difficulty.values().length; i++) {
                    if (cardBounds(i).contains(p)) {
                        // 이미 선택된 카드를 한 번 더 누르면 바로 시작
                        if (selectedDifficulty.ordinal() == i) {
                            startGame(selectedDifficulty);
                        } else {
                            selectDifficulty(i);
                        }
                        return;
                    }
                }
                if (START_BUTTON.contains(p)) startGame(selectedDifficulty);
            }
            case GAME_OVER -> {
                if (gameOverTick < GAME_OVER_INPUT_DELAY) return;
                if (OVER_RETRY.contains(p)) startGame(difficulty);
                else if (OVER_MENU.contains(p)) goToMenu();
            }
            case PAUSED -> {
                if (PAUSE_RESUME.contains(p)) gameState = GameState.PLAYING;
                else if (PAUSE_MENU.contains(p)) goToMenu();
            }
            default -> {
            }
        }
        updateHover(p);
    }

    // =====================================================================
    // 상태 전환
    // =====================================================================

    public void startGame(Difficulty chosen) {
        difficulty = chosen;
        selectedDifficulty = chosen;
        scoreSystem = new ScoreSystem(chosen, TICK_SECONDS);
        player.reset(PANEL_WIDTH / 2.0, (PANEL_HEIGHT + HUD_HEIGHT) / 2.0);
        balls.clear();
        items.clear();
        particles.clear();
        floatingTexts.clear();
        shockwaves.clear();
        playFrames = 0;
        spawnCounter = 0;
        // 첫 아이템은 시작 2초 후 등장
        itemSpawnCounter = Math.max(0, itemIntervalFrames() - 2 * FPS);
        currentLevel = 1;
        isNewRecord = false;
        flashAlpha = 0f;
        shakeFrames = 0;
        resetKeyStates();
        hoverButton = null;
        hoverCard = -1;
        setCursor(Cursor.getDefaultCursor());
        gameState = GameState.PLAYING;
    }

    private void pauseGame() {
        gameState = GameState.PAUSED;
        resetKeyStates();
        repaint();
    }

    private void goToMenu() {
        balls.clear();
        items.clear();
        particles.clear();
        floatingTexts.clear();
        shockwaves.clear();
        flashAlpha = 0f;
        resetKeyStates();
        gameState = GameState.READY;
    }

    private void triggerGameOver() {
        gameState = GameState.GAME_OVER;
        gameOverTick = 0;
        resetKeyStates();
        player.setFever(false);
        createExplosion(player.getX(), player.getY(), 44, 1.0,
                new Color(255, 75, 75), new Color(255, 165, 0), new Color(255, 220, 0), Theme.ACCENT, Color.WHITE);
        startShake(18, 7);
        flash(Theme.DANGER, 0.35f);

        int score = scoreSystem.getScore();
        if (score > bestScores.getOrDefault(difficulty, 0)) {
            bestScores.put(difficulty, score);
            isNewRecord = true;
        }
    }

    // =====================================================================
    // 게임 루프
    // =====================================================================

    @Override
    public void actionPerformed(ActionEvent e) {
        globalTick++;
        switch (gameState) {
            case READY -> updateMenuAmbient();
            case PLAYING -> updateGame();
            case GAME_OVER -> {
                gameOverTick++;
                updateEffects();
            }
            case PAUSED -> {
            }
        }
        repaint();
    }

    private double elapsedSeconds() {
        return playFrames * TICK_SECONDS;
    }

    private int itemIntervalFrames() {
        int base = (int) Math.round(difficulty.getItemIntervalSeconds() * FPS);
        // 피버 중에는 아이템이 2배 빨리 나와 콤보를 이어가기 쉬움
        return scoreSystem.isFever() ? base / 2 : base;
    }

    private void updateGame() {
        // 1. 시간 및 난이도
        playFrames++;
        double seconds = elapsedSeconds();
        int newLevel = 1 + (int) (seconds / 8.0);
        if (newLevel > currentLevel) {
            currentLevel = newLevel;
            floatingTexts.add(new FloatingText("LEVEL " + currentLevel, PANEL_WIDTH / 2.0, PANEL_HEIGHT / 2.0 - 40,
                    Theme.alpha(Theme.TEXT, 220), Theme.font(Font.BOLD, 26f), 70));
        }
        int spawnInterval = difficulty.spawnIntervalAt(seconds);
        double speedMultiplier = difficulty.speedMultiplierAt(seconds);

        // 2. 점수 / 콤보 / 피버 타이머
        boolean wasFever = scoreSystem.isFever();
        scoreSystem.tick();
        if (wasFever && !scoreSystem.isFever()) {
            floatingTexts.add(new FloatingText("FEVER 종료", player.getX(), player.getY() - 30,
                    Theme.TEXT_DIM, Theme.font(Font.BOLD, 14f), 50));
        }
        player.setFever(scoreSystem.isFever());

        // 3. 플레이어 이동
        player.update(upPressed, downPressed, leftPressed, rightPressed, PANEL_WIDTH, PANEL_HEIGHT, particles);
        clampPlayerBelowHud();

        // 4. 이펙트
        updateEffects();

        // 5. 공 생성
        spawnCounter++;
        if (spawnCounter >= spawnInterval) {
            spawnCounter = 0;
            spawnBall(speedMultiplier);
            if (currentLevel >= difficulty.getDoubleSpawnLevel() && RANDOM.nextDouble() < difficulty.getDoubleSpawnChance()) {
                spawnBall(speedMultiplier);
            }
        }

        // 6. 아이템 생성 / 획득 / 소멸
        itemSpawnCounter++;
        if (itemSpawnCounter >= itemIntervalFrames()) {
            itemSpawnCounter = 0;
            if (items.size() < MAX_ITEMS_ON_FIELD) {
                double bombChance = balls.size() >= 6 ? 0.28 : 0.12; // 공이 많을수록 폭탄 확률 증가
                items.add(Item.createRandom(PANEL_WIDTH, PANEL_HEIGHT, HUD_HEIGHT, player.getX(), player.getY(), bombChance));
            }
        }
        for (int i = items.size() - 1; i >= 0; i--) {
            Item item = items.get(i);
            item.update();
            if (item.collidesWith(player)) {
                items.remove(i);
                collectItem(item);
            } else if (item.isExpired()) {
                items.remove(i);
                createExplosion(item.getX(), item.getY(), 8, 0.4, Theme.alpha(item.getType().getColor(), 200));
            }
        }

        // 7. 공 이동 및 충돌
        boolean playerInvincible = player.isInvincible();
        for (Ball ball : balls) {
            ball.update();
            if (!playerInvincible && ball.collidesWith(player)) {
                triggerGameOver();
                return;
            }
        }
        balls.removeIf(b -> b.isOutOfBounds(PANEL_WIDTH, PANEL_HEIGHT));
    }

    private void spawnBall(double speedMultiplier) {
        balls.add(Ball.createRandomBall(PANEL_WIDTH, PANEL_HEIGHT, speedMultiplier,
                player.getX(), player.getY(), difficulty.getAimSpread()));
    }

    /** 플레이어가 HUD 뒤로 숨지 않도록 상단 이동 제한 */
    private void clampPlayerBelowHud() {
        player.clampMinY(HUD_HEIGHT + player.getRadius());
    }

    private void collectItem(Item item) {
        ScoreSystem.CollectResult result;
        if (item.getType() == Item.ItemType.BOMB) {
            int destroyed = balls.size();
            for (Ball ball : balls) {
                createExplosion(ball.getX(), ball.getY(), 7, 0.8, ball.getColor(), Color.WHITE);
            }
            balls.clear();
            shockwaves.add(new Shockwave(item.getX(), item.getY(), 900, item.getType().getColor(), 40));
            shockwaves.add(new Shockwave(item.getX(), item.getY(), 600, Color.WHITE, 30));
            flash(item.getType().getColor(), 0.28f);
            startShake(12, 5);
            result = scoreSystem.collect(Item.ItemType.BOMB, destroyed);
            floatingTexts.add(new FloatingText(destroyed > 0 ? "공 " + destroyed + "개 파괴!" : "BOMB!",
                    item.getX(), item.getY() - 34, item.getType().getColor(), Theme.font(Font.BOLD, 17f), 60));
        } else {
            createExplosion(item.getX(), item.getY(), 16, 0.7, item.getType().getColor(), new Color(255, 245, 200));
            result = scoreSystem.collect(Item.ItemType.STAR, 0);
        }

        Color pointColor = scoreSystem.isFever() ? Theme.GOLD : Theme.TEXT;
        floatingTexts.add(new FloatingText("+" + String.format("%,d", result.points),
                item.getX(), item.getY() - 12, pointColor, Theme.font(Font.BOLD, 18f), 55));
        if (result.combo >= 2) {
            floatingTexts.add(new FloatingText("COMBO ×" + result.combo,
                    item.getX(), item.getY() + 22, Theme.ACCENT, Theme.font(Font.BOLD, 14f), 55));
        }
        if (result.feverStarted) {
            floatingTexts.add(new FloatingText("FEVER!  점수 ×2", PANEL_WIDTH / 2.0, PANEL_HEIGHT / 2.0,
                    Theme.GOLD, Theme.font(Font.BOLD, 40f), 90));
            flash(Theme.GOLD, 0.3f);
            shockwaves.add(new Shockwave(player.getX(), player.getY(), 260, Theme.GOLD, 36));
        }
    }

    private void updateEffects() {
        for (Particle p : particles) p.update();
        particles.removeIf(Particle::isDead);
        for (FloatingText t : floatingTexts) t.update();
        floatingTexts.removeIf(FloatingText::isDead);
        for (Shockwave w : shockwaves) w.update();
        shockwaves.removeIf(Shockwave::isDead);
        if (flashAlpha > 0) flashAlpha = Math.max(0f, flashAlpha - 0.02f);
        if (shakeFrames > 0) shakeFrames--;
    }

    /** 메뉴 배경에서 공이 천천히 흘러가는 연출 (충돌 없음) */
    private void updateMenuAmbient() {
        if (globalTick % 22 == 0 && balls.size() < 18) {
            balls.add(Ball.createRandomBall(PANEL_WIDTH, PANEL_HEIGHT, 0.55,
                    PANEL_WIDTH / 2.0, PANEL_HEIGHT / 2.0, 360));
        }
        for (Ball b : balls) b.update();
        balls.removeIf(b -> b.isOutOfBounds(PANEL_WIDTH, PANEL_HEIGHT));
        updateEffects();
    }

    private void createExplosion(double x, double y, int count, double power, Color... colors) {
        for (int i = 0; i < count; i++) {
            double angle = RANDOM.nextDouble() * 2 * Math.PI;
            double speed = (1.5 + RANDOM.nextDouble() * 6.0) * power;
            double size = 3.0 + RANDOM.nextDouble() * 5.0 * power;
            float decay = 0.02f + (float) (RANDOM.nextDouble() * 0.03);
            Color color = colors[RANDOM.nextInt(colors.length)];
            particles.add(new Particle(x, y, Math.cos(angle) * speed, Math.sin(angle) * speed, size, 1.0f, decay, color));
        }
    }

    private void startShake(int frames, double strength) {
        shakeFrames = frames;
        shakeStrength = strength;
    }

    private void flash(Color color, float alpha) {
        flashColor = color;
        flashAlpha = alpha;
    }

    // =====================================================================
    // 렌더링
    // =====================================================================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        drawBackground(g2d);

        // ---- 월드 (화면 흔들림 적용) ----
        Graphics2D world = (Graphics2D) g2d.create();
        if (shakeFrames > 0) {
            double s = shakeStrength * (shakeFrames / 18.0);
            world.translate((RANDOM.nextDouble() * 2 - 1) * s, (RANDOM.nextDouble() * 2 - 1) * s);
        }
        for (Shockwave w : shockwaves) w.draw(world);
        for (Item item : items) item.draw(world);
        for (Particle p : particles) p.draw(world);
        for (Ball ball : balls) ball.draw(world);
        if (gameState == GameState.PLAYING || gameState == GameState.PAUSED) {
            player.draw(world);
        }
        for (FloatingText t : floatingTexts) t.draw(world);
        world.dispose();

        if (flashAlpha > 0) {
            g2d.setColor(Theme.alpha(flashColor, (int) (flashAlpha * 255)));
            g2d.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        }

        // ---- UI ----
        switch (gameState) {
            case READY -> drawMenu(g2d);
            case PLAYING -> drawHud(g2d);
            case PAUSED -> {
                drawHud(g2d);
                drawPaused(g2d);
            }
            case GAME_OVER -> drawGameOver(g2d);
        }
        g2d.dispose();
    }

    private void drawBackground(Graphics2D g2d) {
        if (backgroundCache == null) {
            backgroundCache = new BufferedImage(PANEL_WIDTH, PANEL_HEIGHT, BufferedImage.TYPE_INT_RGB);
            Graphics2D bg = backgroundCache.createGraphics();
            bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            bg.setPaint(new GradientPaint(0, 0, Theme.BG_TOP, 0, PANEL_HEIGHT, Theme.BG_BOTTOM));
            bg.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
            // 은은한 도트 그리드
            bg.setColor(new Color(255, 255, 255, 16));
            for (int x = 20; x < PANEL_WIDTH; x += 40) {
                for (int y = 20; y < PANEL_HEIGHT; y += 40) {
                    bg.fillOval(x - 1, y - 1, 2, 2);
                }
            }
            // 비네트
            bg.setPaint(new RadialGradientPaint(new Point2D.Double(PANEL_WIDTH / 2.0, PANEL_HEIGHT / 2.0),
                    PANEL_WIDTH * 0.7f, new float[]{0.55f, 1f},
                    new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 120)}));
            bg.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
            bg.dispose();
        }
        g2d.drawImage(backgroundCache, 0, 0, null);

        // 피버 중에는 가장자리가 금빛으로 맥동
        if ((gameState == GameState.PLAYING || gameState == GameState.PAUSED) && scoreSystem.isFever()) {
            int a = (int) (70 + Math.sin(globalTick * 0.15) * 30);
            g2d.setPaint(new RadialGradientPaint(new Point2D.Double(PANEL_WIDTH / 2.0, PANEL_HEIGHT / 2.0),
                    PANEL_WIDTH * 0.65f, new float[]{0.5f, 1f},
                    new Color[]{new Color(255, 190, 60, 0), new Color(255, 170, 40, a)}));
            g2d.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);
        }
    }

    // ---------------------------------------------------------------- HUD

    private void drawHud(Graphics2D g2d) {
        boolean fever = scoreSystem.isFever();

        g2d.setColor(new Color(14, 16, 26, 215));
        g2d.fillRect(0, 0, PANEL_WIDTH, HUD_HEIGHT);
        g2d.setColor(fever ? Theme.alpha(Theme.GOLD, 160) : Theme.BORDER);
        g2d.fillRect(0, HUD_HEIGHT - 1, PANEL_WIDTH, fever ? 2 : 1);

        // 난이도 배지
        drawPill(g2d, difficulty.getLabel(), 18, 16, difficulty.getColor());

        // 스탯
        drawStat(g2d, "SCORE", String.format("%,d", scoreSystem.getScore()), 96, fever ? Theme.GOLD : Theme.TEXT, 22f);
        drawStat(g2d, "TIME", String.format("%.1f초", elapsedSeconds()), 236, Theme.TEXT, 18f);
        drawStat(g2d, "LEVEL", String.valueOf(currentLevel), 340, Theme.TEXT, 18f);
        drawStat(g2d, "MULTI", String.format("×%.1f", difficulty.getScoreMultiplier() * scoreSystem.currentFeverMultiplier()),
                420, fever ? Theme.GOLD : Theme.TEXT_DIM, 18f);

        int best = bestScores.getOrDefault(difficulty, 0);
        g2d.setFont(Theme.font(Font.BOLD, 10.5f));
        g2d.setColor(Theme.TEXT_FAINT);
        Theme.rightAligned(g2d, "BEST", PANEL_WIDTH - 20, 22);
        g2d.setFont(Theme.font(Font.BOLD, 18f));
        g2d.setColor(best > 0 ? Theme.alpha(Theme.GOLD, 230) : Theme.TEXT_FAINT);
        Theme.rightAligned(g2d, best > 0 ? String.format("%,d", best) : "-", PANEL_WIDTH - 20, 44);

        // 콤보 위젯 (좌측 상단)
        int combo = scoreSystem.getCombo();
        if (combo > 0) {
            int x = 16;
            int y = HUD_HEIGHT + 10;
            Theme.card(g2d, x, y, 176, 44, 12, new Color(20, 23, 36, 220), Theme.BORDER);
            g2d.setFont(Theme.font(Font.BOLD, 11f));
            g2d.setColor(Theme.TEXT_DIM);
            g2d.drawString("COMBO", x + 12, y + 18);
            g2d.setFont(Theme.font(Font.BOLD, 16f));
            g2d.setColor(Theme.ACCENT);
            g2d.drawString("×" + combo, x + 58, y + 19);
            // 피버까지 남은 단계 (피버 중이면 금색으로 모두 채움)
            for (int i = 0; i < ScoreSystem.FEVER_COMBO; i++) {
                boolean filled = fever || i < combo;
                int dx = x + 176 - 16 - (ScoreSystem.FEVER_COMBO - 1 - i) * 14;
                g2d.setColor(filled ? (fever ? Theme.GOLD : Theme.ACCENT) : new Color(255, 255, 255, 30));
                g2d.fillOval(dx - 4, y + 10, 8, 8);
            }
            Theme.progressBar(g2d, x + 12, y + 29, 176 - 24, 5, scoreSystem.comboTimeRatio(),
                    new Color(255, 255, 255, 22), Theme.ACCENT);
        }

        // 피버 배너 (상단 중앙)
        if (fever) {
            int w = 250;
            int x = PANEL_WIDTH / 2 - w / 2;
            int y = HUD_HEIGHT + 10;
            double pulse = 0.5 + 0.5 * Math.sin(globalTick * 0.2);
            Theme.card(g2d, x, y, w, 44, 14, new Color(48, 36, 10, 225), Theme.alpha(Theme.GOLD, (int) (120 + 100 * pulse)));
            g2d.setFont(Theme.font(Font.BOLD, 16f));
            g2d.setColor(Theme.GOLD);
            g2d.drawString("FEVER", x + 16, y + 20);
            g2d.setFont(Theme.font(Font.BOLD, 13f));
            g2d.setColor(new Color(255, 236, 190));
            Theme.rightAligned(g2d, "점수 ×2", x + w - 16, y + 20);
            Theme.progressBar(g2d, x + 16, y + 29, w - 32, 6, scoreSystem.feverTimeRatio(),
                    new Color(255, 255, 255, 25), Theme.GOLD);
        }
    }

    private void drawStat(Graphics2D g2d, String label, String value, int x, Color valueColor, float size) {
        g2d.setFont(Theme.font(Font.BOLD, 10.5f));
        g2d.setColor(Theme.TEXT_FAINT);
        g2d.drawString(label, x, 22);
        g2d.setFont(Theme.font(Font.BOLD, size));
        g2d.setColor(valueColor);
        g2d.drawString(value, x, 44);
    }

    private void drawPill(Graphics2D g2d, String text, int x, int y, Color color) {
        g2d.setFont(Theme.font(Font.BOLD, 13f));
        FontMetrics fm = g2d.getFontMetrics();
        int w = fm.stringWidth(text) + 22;
        Theme.card(g2d, x, y, w, 26, 26, Theme.alpha(color, 40), Theme.alpha(color, 150));
        g2d.setColor(color);
        Theme.centered(g2d, text, x + w / 2.0, y + 18);
    }

    // --------------------------------------------------------------- MENU

    private void drawMenu(Graphics2D g2d) {
        g2d.setColor(new Color(8, 9, 15, 150));
        g2d.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        // 타이틀
        g2d.setFont(Theme.font(Font.BOLD, 44f));
        g2d.setColor(Theme.TEXT);
        Theme.centered(g2d, "BALL DODGE", PANEL_WIDTH / 2.0, 100);
        g2d.setFont(Theme.font(Font.PLAIN, 15f));
        g2d.setColor(Theme.TEXT_DIM);
        Theme.centered(g2d, "날아오는 공을 피하고, 아이템을 모아 FEVER를 노리세요", PANEL_WIDTH / 2.0, 130);

        g2d.setFont(Theme.font(Font.BOLD, 11.5f));
        g2d.setColor(Theme.TEXT_FAINT);
        Theme.centered(g2d, "난이도 선택", PANEL_WIDTH / 2.0, CARDS_Y - 14);

        Difficulty[] values = Difficulty.values();
        for (int i = 0; i < values.length; i++) {
            drawDifficultyCard(g2d, values[i], cardBounds(i), values[i] == selectedDifficulty, hoverCard == i);
        }

        // 시작 버튼
        drawButton(g2d, START_BUTTON, selectedDifficulty.getLabel() + " 시작하기", selectedDifficulty.getColor(),
                true, hoverButton == START_BUTTON);

        drawItemLegend(g2d, CARDS_X, 468, PANEL_WIDTH - CARDS_X * 2, 72);

        drawKeyHints(g2d, 570, new String[][]{
                {"← →", "난이도"}, {"Enter", "시작"}, {"WASD", "이동"}, {"P", "일시정지"}
        });
    }

    private void drawDifficultyCard(Graphics2D g2d, Difficulty d, Rectangle r, boolean selected, boolean hover) {
        int lift = selected ? 4 : (hover ? 2 : 0);
        int x = r.x;
        int y = r.y - lift;
        Color c = d.getColor();

        if (selected) {
            // 선택 카드 발광
            Theme.card(g2d, x - 4, y - 4, r.width + 8, r.height + 8, 22, Theme.alpha(c, 28), null);
        }
        Theme.card(g2d, x, y, r.width, r.height, 18,
                selected ? Theme.SURFACE_RAISED : Theme.SURFACE,
                selected ? Theme.alpha(c, 220) : (hover ? new Color(255, 255, 255, 60) : Theme.BORDER));

        // 상단 액센트 라인
        Theme.card(g2d, x + 20, y + 14, 28, 4, 4, c, null);

        g2d.setFont(Theme.font(Font.BOLD, 26f));
        g2d.setColor(selected ? Theme.TEXT : Theme.alpha(Theme.TEXT, 210));
        g2d.drawString(d.getLabel(), x + 20, y + 52);
        g2d.setFont(Theme.font(Font.BOLD, 11f));
        g2d.setColor(Theme.alpha(c, selected ? 255 : 170));
        Theme.rightAligned(g2d, d.getEnglishLabel(), x + r.width - 18, y + 50);

        g2d.setFont(Theme.font(Font.PLAIN, 12.5f));
        g2d.setColor(Theme.TEXT_DIM);
        g2d.drawString(d.getDescription(), x + 20, y + 76);

        g2d.setColor(Theme.BORDER);
        g2d.fillRect(x + 20, y + 90, r.width - 40, 1);

        drawMeterRow(g2d, "공 속도", d.speedRating(), x + 20, y + 114, r.width - 40, c);
        drawMeterRow(g2d, "공 밀도", d.densityRating(), x + 20, y + 136, r.width - 40, c);

        g2d.setFont(Theme.font(Font.PLAIN, 12.5f));
        g2d.setColor(Theme.TEXT_DIM);
        g2d.drawString("점수 배율", x + 20, y + 158);
        g2d.setFont(Theme.font(Font.BOLD, 12.5f));
        g2d.setColor(Theme.TEXT);
        Theme.rightAligned(g2d, String.format("×%.1f", d.getScoreMultiplier()), x + r.width - 20, y + 158);

        int best = bestScores.getOrDefault(d, 0);
        g2d.setFont(Theme.font(Font.PLAIN, 11.5f));
        g2d.setColor(best > 0 ? Theme.alpha(Theme.GOLD, 220) : Theme.TEXT_FAINT);
        g2d.drawString(best > 0 ? String.format("최고 %,d점", best) : "기록 없음", x + 20, y + 176);
    }

    private void drawMeterRow(Graphics2D g2d, String label, int level, int x, int y, int w, Color c) {
        g2d.setFont(Theme.font(Font.PLAIN, 12.5f));
        g2d.setColor(Theme.TEXT_DIM);
        g2d.drawString(label, x, y);
        for (int i = 0; i < 3; i++) {
            int bx = x + w - (3 - i) * 22 + 4;
            Theme.card(g2d, bx, y - 8, 18, 6, 6, i < level ? c : new Color(255, 255, 255, 28), null);
        }
    }

    private void drawItemLegend(Graphics2D g2d, int x, int y, int w, int h) {
        Theme.card(g2d, x, y, w, h, 16, new Color(20, 23, 36, 200), Theme.BORDER);
        int colW = w / 3;
        String[][] text = {
                {"별", "보너스 +" + ScoreSystem.STAR_POINTS + "점"},
                {"폭탄", "화면의 공 전부 파괴"},
                {"FEVER", ScoreSystem.FEVER_COMBO + "연속 획득 시 점수 ×2"}
        };
        for (int i = 0; i < 3; i++) {
            int cx = x + colW * i;
            double iconX = cx + 34;
            double iconY = y + h / 2.0;
            if (i == 0) {
                Item.drawIcon(g2d, Item.ItemType.STAR, iconX, iconY, 11, globalTick * 0.03, globalTick);
            } else if (i == 1) {
                Item.drawIcon(g2d, Item.ItemType.BOMB, iconX, iconY, 11, 0, globalTick);
            } else {
                // 피버 아이콘: 금색 배지
                Theme.card(g2d, iconX - 17, iconY - 12, 34, 24, 10, Theme.alpha(Theme.GOLD, 45), Theme.alpha(Theme.GOLD, 180));
                g2d.setFont(Theme.font(Font.BOLD, 12f));
                g2d.setColor(Theme.GOLD);
                Theme.centered(g2d, "×2", iconX, iconY + 5);
            }
            g2d.setFont(Theme.font(Font.BOLD, 13.5f));
            g2d.setColor(Theme.TEXT);
            g2d.drawString(text[i][0], cx + 62, y + h / 2 - 4);
            g2d.setFont(Theme.font(Font.PLAIN, 12f));
            g2d.setColor(Theme.TEXT_DIM);
            g2d.drawString(text[i][1], cx + 62, y + h / 2 + 14);
            if (i > 0) {
                g2d.setColor(Theme.BORDER);
                g2d.fillRect(cx, y + 16, 1, h - 32);
            }
        }
    }

    /** 하단 키 안내 (키캡 + 설명) 가운데 정렬 */
    private void drawKeyHints(Graphics2D g2d, int baselineY, String[][] hints) {
        g2d.setFont(Theme.font(Font.BOLD, 12f));
        FontMetrics keyFm = g2d.getFontMetrics();
        g2d.setFont(Theme.font(Font.PLAIN, 12.5f));
        FontMetrics labelFm = g2d.getFontMetrics();
        int gap = 22;
        int total = 0;
        for (String[] h : hints) {
            total += Math.max(22, keyFm.stringWidth(h[0]) + 12) + 7 + labelFm.stringWidth(h[1]) + gap;
        }
        total -= gap;
        int x = (PANEL_WIDTH - total) / 2;
        for (String[] h : hints) {
            x += Theme.keycap(g2d, h[0], x, baselineY) + 7;
            g2d.setFont(Theme.font(Font.PLAIN, 12.5f));
            g2d.setColor(Theme.TEXT_DIM);
            g2d.drawString(h[1], x, baselineY);
            x += labelFm.stringWidth(h[1]) + gap;
        }
    }

    private void drawButton(Graphics2D g2d, Rectangle r, String text, Color color, boolean primary, boolean hover) {
        if (primary) {
            Color fill = hover ? Theme.mix(color, Color.WHITE, 0.15) : color;
            Theme.card(g2d, r.x, r.y + 3, r.width, r.height, 14, Theme.alpha(Color.BLACK, 70), null); // 그림자
            Theme.card(g2d, r.x, r.y, r.width, r.height, 14, fill, null);
            g2d.setColor(new Color(16, 18, 26));
        } else {
            Theme.card(g2d, r.x, r.y, r.width, r.height, 14,
                    hover ? new Color(255, 255, 255, 30) : new Color(255, 255, 255, 14), new Color(255, 255, 255, 50));
            g2d.setColor(Theme.TEXT);
        }
        g2d.setFont(Theme.font(Font.BOLD, 16f));
        FontMetrics fm = g2d.getFontMetrics();
        int ty = r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2;
        Theme.centered(g2d, text, r.getCenterX(), ty);
    }

    // ------------------------------------------------------------- PAUSED

    private void drawPaused(Graphics2D g2d) {
        g2d.setColor(Theme.SCRIM);
        g2d.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        Rectangle c = PAUSE_CARD;
        Theme.card(g2d, c.x, c.y, c.width, c.height, 22, Theme.SURFACE, Theme.BORDER);

        // 일시정지 아이콘
        int iconCx = c.x + c.width / 2;
        Theme.card(g2d, iconCx - 11, c.y + 30, 7, 24, 4, Theme.TEXT, null);
        Theme.card(g2d, iconCx + 4, c.y + 30, 7, 24, 4, Theme.TEXT, null);

        g2d.setFont(Theme.font(Font.BOLD, 24f));
        g2d.setColor(Theme.TEXT);
        Theme.centered(g2d, "일시 정지", iconCx, c.y + 90);
        g2d.setFont(Theme.font(Font.PLAIN, 13f));
        g2d.setColor(Theme.TEXT_DIM);
        Theme.centered(g2d, "P / ESC 계속   ·   M 메뉴", iconCx, c.y + 116);

        drawButton(g2d, PAUSE_RESUME, "계속하기", difficulty.getColor(), true, hoverButton == PAUSE_RESUME);
        drawButton(g2d, PAUSE_MENU, "메뉴로", Theme.TEXT, false, hoverButton == PAUSE_MENU);
    }

    // ---------------------------------------------------------- GAME OVER

    private void drawGameOver(Graphics2D g2d) {
        // 결과 카드가 부드럽게 나타나도록 페이드 인
        float t = Math.min(1f, gameOverTick / 20f);
        Composite original = g2d.getComposite();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, t));

        g2d.setColor(Theme.SCRIM);
        g2d.fillRect(0, 0, PANEL_WIDTH, PANEL_HEIGHT);

        Rectangle c = OVER_CARD;
        int slide = (int) ((1 - t) * 24);
        int cy = c.y + slide;
        int cx = c.x + c.width / 2;
        Theme.card(g2d, c.x, cy, c.width, c.height, 24, Theme.SURFACE, Theme.BORDER);

        g2d.setFont(Theme.font(Font.BOLD, 32f));
        g2d.setColor(Theme.DANGER);
        Theme.centered(g2d, "GAME OVER", cx, cy + 56);

        // 난이도 + 기록 배지
        String sub;
        Color subColor;
        if (isNewRecord) {
            sub = "★ 새 최고 기록!";
            subColor = Theme.GOLD;
        } else {
            sub = String.format("최고 기록 %,d점", bestScores.getOrDefault(difficulty, 0));
            subColor = Theme.TEXT_DIM;
        }
        g2d.setFont(Theme.font(Font.BOLD, 13f));
        FontMetrics fm = g2d.getFontMetrics();
        int pillW = fm.stringWidth(difficulty.getLabel()) + 22;
        int subW = fm.stringWidth(sub);
        int rowX = cx - (pillW + 10 + subW) / 2;
        drawPill(g2d, difficulty.getLabel(), rowX, cy + 70, difficulty.getColor());
        g2d.setFont(Theme.font(Font.BOLD, 13f));
        g2d.setColor(subColor);
        g2d.drawString(sub, rowX + pillW + 10, cy + 88);

        // 최종 점수
        g2d.setFont(Theme.font(Font.BOLD, 11.5f));
        g2d.setColor(Theme.TEXT_FAINT);
        Theme.centered(g2d, "최종 점수", cx, cy + 128);
        g2d.setFont(Theme.font(Font.BOLD, 46f));
        g2d.setColor(Theme.TEXT);
        Theme.centered(g2d, String.format("%,d", scoreSystem.getScore()), cx, cy + 176);

        // 통계 4칸
        String[][] stats = {
                {String.format("%.1f초", elapsedSeconds()), "생존 시간"},
                {"×" + scoreSystem.getMaxCombo(), "최대 콤보"},
                {scoreSystem.getItemsCollected() + "개", "획득 아이템"},
                {scoreSystem.getFeverCount() + "회", "FEVER"}
        };
        int boxW = 98;
        int gap = 10;
        int sx = cx - (boxW * 4 + gap * 3) / 2;
        for (int i = 0; i < 4; i++) {
            int bx = sx + i * (boxW + gap);
            Theme.card(g2d, bx, cy + 200, boxW, 70, 14, new Color(255, 255, 255, 10), Theme.BORDER);
            g2d.setFont(Theme.font(Font.BOLD, 18f));
            g2d.setColor(i == 3 && scoreSystem.getFeverCount() > 0 ? Theme.GOLD : Theme.TEXT);
            Theme.centered(g2d, stats[i][0], bx + boxW / 2.0, cy + 232);
            g2d.setFont(Theme.font(Font.PLAIN, 11.5f));
            g2d.setColor(Theme.TEXT_DIM);
            Theme.centered(g2d, stats[i][1], bx + boxW / 2.0, cy + 254);
        }

        Rectangle retry = new Rectangle(OVER_RETRY.x, OVER_RETRY.y + slide, OVER_RETRY.width, OVER_RETRY.height);
        Rectangle menu = new Rectangle(OVER_MENU.x, OVER_MENU.y + slide, OVER_MENU.width, OVER_MENU.height);
        drawButton(g2d, retry, "다시 하기  (R)", difficulty.getColor(), true, hoverButton == OVER_RETRY);
        drawButton(g2d, menu, "메뉴  (M)", Theme.TEXT, false, hoverButton == OVER_MENU);

        g2d.setComposite(original);
    }

    // =====================================================================
    // 외부 접근용
    // =====================================================================

    public GameState getGameState() {
        return gameState;
    }

    public Player getPlayer() {
        return player;
    }

    public List<Ball> getBalls() {
        return balls;
    }

    public List<Item> getItems() {
        return items;
    }

    public ScoreSystem getScoreSystem() {
        return scoreSystem;
    }

    public int getScore() {
        return scoreSystem.getScore();
    }

    public int getBestScore(Difficulty d) {
        return bestScores.getOrDefault(d, 0);
    }

    public Difficulty getSelectedDifficulty() {
        return selectedDifficulty;
    }
}
