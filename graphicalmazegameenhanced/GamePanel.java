package com.mycompany.graphicalmazegameenhanced;

import javax.swing.*;
import java.awt.*;
import java.util.Random;

public class GamePanel extends JPanel {
    private final GraphicalMazeGameEnhanced game;
    private final MonsterManager monsterManager;
    private final StoryManager storyManager;
    private final Random random = new Random();

    public GamePanel(GraphicalMazeGameEnhanced game, MonsterManager mm, StoryManager sm) {
        this.game = game;
        this.monsterManager = mm;
        this.storyManager = sm;
        setPreferredSize(new Dimension(GraphicalMazeGameEnhanced.COLS * GraphicalMazeGameEnhanced.CELL_SIZE,
                                       GraphicalMazeGameEnhanced.ROWS * GraphicalMazeGameEnhanced.CELL_SIZE));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw the game as normal
        char[][] maze = game.getMaze();
        if (maze == null) return;
        int ROWS = GraphicalMazeGameEnhanced.ROWS;
        int COLS = GraphicalMazeGameEnhanced.COLS;
        int CELL_SIZE = GraphicalMazeGameEnhanced.CELL_SIZE;
        float glowAlpha = game.getGlowAlpha();
        int playerX = game.getPlayerX();
        int playerY = game.getPlayerY();
        int playerFacing = game.getPlayerFacing();
        int currentLevel = game.getCurrentLevel();

        for (int i = 0; i < ROWS; i++) {
            for (int j = 0; j < COLS; j++) {
                int x = j * CELL_SIZE;
                int y = i * CELL_SIZE;

                // Ground with patterns (same colors as original)
                if (currentLevel == 1) {
                    g2d.setPaint(new GradientPaint(x, y, new Color(144, 238, 144), x + CELL_SIZE, y + CELL_SIZE, new Color(100, 200, 100)));
                    g2d.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                    g2d.setColor(new Color(80, 160, 80, 100));
                    g2d.fillOval(x + 10, y + 10, 5, 5);
                } else if (currentLevel == 2) {
                    g2d.setPaint(new GradientPaint(x, y, new Color(50, 150, 50), x + CELL_SIZE, y + CELL_SIZE, new Color(30, 100, 30)));
                    g2d.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                    g2d.setColor(new Color(100, 80, 60, 100));
                    g2d.fillRect(x + 15, y + 15, 5, 5);
                } else {
                    g2d.setPaint(new GradientPaint(x, y, new Color(0, 50, 100), x + CELL_SIZE, y + CELL_SIZE, new Color(0, 20, 50)));
                    g2d.fillRect(x, y, CELL_SIZE, CELL_SIZE);
                    g2d.setColor(new Color(255, 255, 255, 100));
                    g2d.fillOval(x + 20, y + 20, 3, 3);
                }

                char cell = maze[i][j];
                if (cell == '#' || cell == 'W') {
                    boolean isBuilding = currentLevel == 1 && (i + j) % 2 == 0;
                    drawWall(g2d, x, y, cell == 'W' ? false : isBuilding, currentLevel);
                } else if (cell == 'T') {
                    drawDecoration(g2d, x, y, currentLevel);
                } else if (cell == 'A' || cell == 'S' || cell == 'C') {
                    if (!game.hasObjectiveItem() && Math.abs(game.getPlayerX() - i) <= 2 && Math.abs(game.getPlayerY() - j) <= 2) {
                        g2d.setColor(new Color(1.0f, 1.0f, 0.0f, glowAlpha * 0.5f));
                        g2d.fillOval(x - 20, y - 20, CELL_SIZE + 40, CELL_SIZE + 40);
                        g2d.setColor(new Color(1.0f, 1.0f, 0.0f, glowAlpha));
                        g2d.fillOval(x - 15, y - 15, CELL_SIZE + 30, CELL_SIZE + 30);
                        drawParticles(g2d, x, y);
                    }
                    drawObjectiveItem(g2d, x, y, cell);
                } else if (cell == 'E') {
                    drawExit(g2d, x, y, glowAlpha);
                } else if (cell == 'G') {
                    drawPerson(g2d, x, y, Color.MAGENTA, 2, true, currentLevel);
                } else if (cell == 'P') {
                    drawPerson(g2d, x, y, Color.BLUE, playerFacing, false, currentLevel);
                } else if (cell == 'M' || cell == 'B') {
                    if (cell == 'M') {
                        drawPerson(g2d, x, y, currentLevel == 1 ? Color.RED : currentLevel == 2 ? new Color(0,100,0) : new Color(0,150,255), 2, false, currentLevel);
                    } else {
                        drawPerson(g2d, x, y, new Color(120, 0, 120), 2, false, currentLevel);
                        g2d.setColor(new Color(200, 0, 200, 50));
                        g2d.fillOval(x + 5, y - 5, CELL_SIZE - 10, CELL_SIZE + 10);
                    }
                }

                if (cell == '.' || cell == 'T' || cell == 'P' || cell == 'M' || cell == 'B') {
                    if ((i + j) % 3 == 0) {
                        g2d.setColor(currentLevel == 1 ? new Color(169, 169, 169) : new Color(139, 69, 19));
                        g2d.fillRect(x + 10, y + 20, CELL_SIZE - 20, 10);
                    }
                }
            }
        }

        // Draw speech bubble if exists
        if (storyManager.hasActiveSpeech()) {
            storyManager.drawSpeechBubble(g2d, game.getPlayerX(), game.getPlayerY(), game.getCellSize());
        }

        // NEW: Pause overlay (drawn on top)
        if (game.isPaused()) {
            // Dark semi-transparent overlay
            g2d.setColor(new Color(0, 0, 0, 150));
            g2d.fillRect(0, 0, getWidth(), getHeight());
            // "PAUSED" text (centered, large, white)
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 48));
            FontMetrics fm = g2d.getFontMetrics();
            String pausedText = "PAUSED";
            int textX = (getWidth() - fm.stringWidth(pausedText)) / 2;
            int textY = (getHeight() / 2) - (fm.getHeight() / 2) + fm.getAscent();
            g2d.drawString(pausedText, textX, textY);
            // Instructions
            g2d.setFont(new Font("Arial", Font.PLAIN, 18));
            String resumeText = "Press P to resume";
            textX = (getWidth() - fm.stringWidth(resumeText)) / 2;
            g2d.drawString(resumeText, textX, textY + 40);
        }
    }

    // All drawing helper methods (unchanged)
    private void drawWall(Graphics2D g, int x, int y, boolean isBuilding, int currentLevel) {
        int CELL_SIZE = GraphicalMazeGameEnhanced.CELL_SIZE;
        if (currentLevel == 2 || !isBuilding) {
            g.setPaint(new GradientPaint(x, y, new Color(60, 160, 60), x + CELL_SIZE, y + CELL_SIZE, new Color(30, 100, 30)));
            g.fillOval(x, y, CELL_SIZE, CELL_SIZE);
            g.setColor(new Color(0, 120, 0, 150));
            g.fillOval(x + 10, y + 10, CELL_SIZE - 20, CELL_SIZE - 20);
            g.setColor(new Color(0, 80, 0, 100));
            g.drawLine(x + 15, y + 15, x + 35, y + 35);
        } else if (currentLevel == 3) {
            g.setPaint(new GradientPaint(x, y, new Color(120, 120, 180), x + CELL_SIZE, y + CELL_SIZE, new Color(70, 70, 120)));
            g.fillRect(x, y, CELL_SIZE, CELL_SIZE);
            g.setColor(new Color(200, 200, 255, 150));
            g.fillOval(x + 5, y + 5, 10, 10);
            g.fillOval(x + 35, y + 35, 10, 10);
            g.setColor(new Color(255, 255, 255, 50));
            g.drawRect(x + 2, y + 2, CELL_SIZE - 4, CELL_SIZE - 4);
        } else {
            g.setPaint(new GradientPaint(x, y, new Color(139, 69, 19), x + CELL_SIZE, y + CELL_SIZE, new Color(100, 50, 10)));
            g.fillRect(x, y, CELL_SIZE, CELL_SIZE);
            g.setColor(new Color(80, 40, 0));
            g.drawLine(x + 2, y + 2, x + CELL_SIZE - 2, y + 2);
            g.drawLine(x + 2, y + 2, x + 2, y + CELL_SIZE - 2);
            g.setColor(Color.YELLOW);
            g.fillRect(x + 10, y + 10, 10, 10);
            g.fillRect(x + 30, y + 30, 10, 10);
        }
    }

    private void drawDecoration(Graphics2D g, int x, int y, int currentLevel) {
        if (currentLevel == 3) {
            g.setColor(new Color(0, 200, 255, 150));
            g.fillOval(x + 15, y + 15, 20, 20);
            g.setColor(new Color(255, 255, 255, 100));
            g.fillOval(x + 20, y + 20, 10, 10);
        } else {
            g.setColor(new Color(139, 69, 19));
            g.fillRect(x + 20, y + 30, 10, 20);
            g.setColor(Color.GREEN);
            g.fillOval(x + 5, y + 5, 40, 40);
        }
    }

    private void drawObjectiveItem(Graphics2D g, int x, int y, char type) {
        g.setColor(type == 'A' ? Color.YELLOW : type == 'S' ? Color.WHITE : new Color(255, 200, 0));
        int[] xp = {x + 25, x + 10, x + 40};
        int[] yp = {y + 10, y + 40, y + 40};
        g.fillPolygon(xp, yp, 3);
    }

    private void drawExit(Graphics2D g, int x, int y, float glowAlpha) {
        g.setPaint(new GradientPaint(x, y, new Color(0, 100, 0), x + 40, y + 40, new Color(0, 150, 0)));
        g.fillRect(x + 10, y + 10, 30, 40);
        g.setColor(new Color(255, 215, 0));
        g.drawRect(x + 8, y + 8, 34, 44);
        g.setColor(new Color(255, 255, 0, (int)(glowAlpha * 255)));
        g.fillOval(x + 20, y + 30, 5, 5);
    }

    private void drawPerson(Graphics2D g, int x, int y, Color color, int facing, boolean isSage, int currentLevel) {
        g.setColor(color);
        g.fillOval(x + 15, y + 5, 20, 20);
        g.setColor(new Color(255, 220, 200));
        g.fillOval(x + 18, y + 8, 14, 14);
        g.setColor(color);
        g.fillRect(x + 22, y + 25, 6, 15);
        g.drawLine(x + 25, y + 28, x + 15, y + 23);
        g.drawLine(x + 25, y + 28, x + 35, y + 23);
        g.drawLine(x + 24, y + 40, x + 20, y + 45);
        g.drawLine(x + 26, y + 40, x + 30, y + 45);
        if (isSage) {
            g.setColor(new Color(200, 0, 200, 150));
            g.fillPolygon(new int[]{x + 15, x + 25, x + 35}, new int[]{y + 25, y + 40, y + 25}, 3);
            g.setColor(Color.GRAY);
            g.fillRect(x + 23, y + 10, 4, 10);
        } else {
            g.setColor(new Color(150, 150, 150, 150));
            g.fillRect(x + 20, y + 25, 10, 10);
            g.setColor(Color.BLACK);
            g.fillRect(x + 22, y + 30, 6, 2);
        }
        if (currentLevel == 3 && !isSage && color != Color.BLUE) {
            g.setColor(new Color(0, 255, 255, 100));
            g.fillOval(x + 10, y, 30, 30);
            g.setColor(Color.WHITE);
            g.fillOval(x + 20, y + 10, 4, 4);
            g.fillOval(x + 26, y + 10, 4, 4);
        }
        g.setColor(Color.BLACK);
        switch (facing) {
            case 0: g.drawLine(x + 25, y + 15, x + 25, y + 5); break;
            case 1: g.drawLine(x + 25, y + 15, x + 35, y + 15); break;
            case 2: g.drawLine(x + 25, y + 15, x + 25, y + 25); break;
            case 3: g.drawLine(x + 25, y + 15, x + 15, y + 15); break;
        }
    }

    private void drawParticles(Graphics2D g, int x, int y) {
        g.setColor(new Color(1.0f, 1.0f, 0.0f, 0.5f));
        for (int i = 0; i < 8; i++) {
            int px = x + 25 + random.nextInt(20) - 10;
            int py = y + 25 + random.nextInt(20) - 10;
            g.fillOval(px, py, 5, 5);
        }
    }
}