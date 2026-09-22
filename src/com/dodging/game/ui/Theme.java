package com.dodging.game.ui;

import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * UI 색상, 폰트, 공통 그리기 도구 모음
 * 폰트는 OS에 설치된 한글 폰트 중 첫 번째로 찾은 것을 사용합니다 (Windows: 맑은 고딕).
 */
final class Theme {
    private Theme() {
    }

    // ---- 색상 팔레트 ----
    static final Color BG_TOP = new Color(18, 20, 32);
    static final Color BG_BOTTOM = new Color(9, 10, 17);
    static final Color GRID = new Color(255, 255, 255, 9);
    static final Color SURFACE = new Color(26, 29, 44, 238);
    static final Color SURFACE_RAISED = new Color(36, 40, 58, 245);
    static final Color BORDER = new Color(255, 255, 255, 26);
    static final Color TEXT = new Color(238, 241, 250);
    static final Color TEXT_DIM = new Color(148, 156, 178);
    static final Color TEXT_FAINT = new Color(100, 108, 130);
    static final Color ACCENT = new Color(0, 212, 255);
    static final Color GOLD = new Color(255, 200, 60);
    static final Color DANGER = new Color(255, 88, 104);
    static final Color SCRIM = new Color(7, 8, 14, 185);

    private static final String[] PREFERRED_FAMILIES = {
            "Malgun Gothic", "맑은 고딕", "Apple SD Gothic Neo", "NanumGothic", "나눔고딕",
            "Noto Sans CJK KR", "Noto Sans KR", "Dialog"
    };
    private static final String FAMILY = detectFamily();
    private static final Map<String, Font> FONT_CACHE = new HashMap<>();

    private static String detectFamily() {
        try {
            Set<String> installed = new HashSet<>(Arrays.asList(
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
            for (String name : PREFERRED_FAMILIES) {
                if (installed.contains(name)) {
                    return name;
                }
            }
        } catch (Exception ignored) {
            // 폰트 목록을 못 읽으면 기본 논리 폰트 사용
        }
        return Font.DIALOG;
    }

    static Font font(int style, float size) {
        String key = style + ":" + size;
        return FONT_CACHE.computeIfAbsent(key, k -> new Font(FAMILY, style, 1).deriveFont(style, size));
    }

    static Color alpha(Color c, int a) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, a)));
    }

    static Color mix(Color a, Color b, double t) {
        t = Math.max(0, Math.min(1, t));
        return new Color(
                (int) (a.getRed() + (b.getRed() - a.getRed()) * t),
                (int) (a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                (int) (a.getBlue() + (b.getBlue() - a.getBlue()) * t),
                (int) (a.getAlpha() + (b.getAlpha() - a.getAlpha()) * t));
    }

    /** 둥근 모서리 카드 (배경 + 1px 테두리) */
    static void card(Graphics2D g, double x, double y, double w, double h, double arc, Color fill, Color border) {
        RoundRectangle2D shape = new RoundRectangle2D.Double(x, y, w, h, arc, arc);
        if (fill != null) {
            g.setColor(fill);
            g.fill(shape);
        }
        if (border != null) {
            g.setColor(border);
            g.setStroke(new BasicStroke(1.2f));
            g.draw(shape);
        }
    }

    /** 가운데 정렬 텍스트 */
    static void centered(Graphics2D g, String text, double cx, double baselineY) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (float) (cx - fm.stringWidth(text) / 2.0), (float) baselineY);
    }

    /** 오른쪽 정렬 텍스트 */
    static void rightAligned(Graphics2D g, String text, double rightX, double baselineY) {
        FontMetrics fm = g.getFontMetrics();
        g.drawString(text, (float) (rightX - fm.stringWidth(text)), (float) baselineY);
    }

    /** 가로 진행 바 */
    static void progressBar(Graphics2D g, double x, double y, double w, double h, double ratio, Color track, Color fill) {
        ratio = Math.max(0, Math.min(1, ratio));
        card(g, x, y, w, h, h, track, null);
        if (ratio > 0) {
            card(g, x, y, Math.max(h, w * ratio), h, h, fill, null);
        }
    }

    /** 키 안내용 작은 키캡 박스, 그린 폭을 반환 */
    static int keycap(Graphics2D g, String key, int x, int baselineY) {
        g.setFont(font(Font.BOLD, 12f));
        FontMetrics fm = g.getFontMetrics();
        int w = Math.max(22, fm.stringWidth(key) + 12);
        card(g, x, baselineY - 15, w, 21, 7, new Color(255, 255, 255, 18), new Color(255, 255, 255, 45));
        g.setColor(TEXT);
        centered(g, key, x + w / 2.0, baselineY);
        return w;
    }
}
