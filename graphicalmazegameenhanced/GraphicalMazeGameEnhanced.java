package com.mycompany.graphicalmazegameenhanced;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;

public class GraphicalMazeGameEnhanced extends JFrame implements ActionListener {

    public static final int CELL_SIZE = 50;
    public static final int ROWS = 10;
    public static final int COLS = 10;
    private static final int MONSTER_MOVE_DELAY = 300;
    private static final int GLOW_ANIMATION_SPEED = 80;
    private static final int CHECKPOINT_INTERVAL = 10;

    // state
    private int currentLevel = 1;
    private final int MAX_LEVEL = 4;
    private char[][] maze;
    private int playerX = 1;
    private int playerY = 1;
    private int playerFacing = 2;

    // Pause and checkpoint state
    private boolean isPaused = false;
    private final List<Checkpoint> checkpoints = new ArrayList<>();
    private int moveCount = 0;

    // managers
    private MonsterManager monsterManager;
    private StoryManager storyManager;
    private SaveLoadManager saveLoadManager;
    private SoundManager soundManager;

    // other
    private boolean hasObjectiveItem = false;
    private int sageInteractionStage = 0;
    private String currentObjective = "Find the Sage for guidance on the curse.";
    private Timer monsterTimer;
    private Timer glowTimer;
    private float glowAlpha = 0.5f;
    private boolean glowIncreasing = true;

    // UI
    private GamePanel gamePanel;
    private JScrollPane logScrollPane;

    public GraphicalMazeGameEnhanced() {
        setTitle("The Cursed Labyrinth - Enhanced");
        setLayout(new BorderLayout());

        // Managers
        monsterManager = new MonsterManager(this);
        storyManager = new StoryManager();
        saveLoadManager = new SaveLoadManager();
        soundManager = new SoundManager();

        // Game panel
        gamePanel = new GamePanel(this, monsterManager, storyManager);
        add(gamePanel, BorderLayout.CENTER);

        // Quest log
        logScrollPane = storyManager.createLogScrollPane();
        logScrollPane.setPreferredSize(new Dimension(COLS * CELL_SIZE, 150));
        add(logScrollPane, BorderLayout.SOUTH);

        setSize(COLS * CELL_SIZE + 16, (ROWS * CELL_SIZE + 150) + 39);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyPress(e);
            }
        });
        setFocusable(true);

        loadLevel(1);

        monsterTimer = new Timer(MONSTER_MOVE_DELAY, this);
        monsterTimer.start();
        glowTimer = new Timer(GLOW_ANIMATION_SPEED, e -> {
            if (!isPaused) {
                if (glowIncreasing) {
                    glowAlpha += 0.07f;
                    if (glowAlpha >= 0.9f) glowIncreasing = false;
                } else {
                    glowAlpha -= 0.07f;
                    if (glowAlpha <= 0.3f) glowIncreasing = true;
                }
            }
            gamePanel.repaint();
        });
        glowTimer.start();

        setVisible(true);

        // NEW: Start background music
        soundManager.startBackgroundMusic();
    }

    // Expose pause state
    public boolean isPaused() { return isPaused; }

    // Expose state
    public int getCurrentLevel() { return currentLevel; }
    public int getRows() { return ROWS; }
    public int getCols() { return COLS; }
    public int getCellSize() { return CELL_SIZE; }
    public float getGlowAlpha() { return glowAlpha; }
    public boolean hasObjectiveItem() { return hasObjectiveItem; }
    public void setHasObjectiveItem(boolean v) { hasObjectiveItem = v; }
    public int getPlayerX() { return playerX; }
    public int getPlayerY() { return playerY; }
    public int getPlayerFacing() { return playerFacing; }
    public char[][] getMaze() { return maze; }
    public MonsterManager getMonsterManager() { return monsterManager; }
    public StoryManager getStoryManager() { return storyManager; }
    public SoundManager getSoundManager() { return soundManager; }

    // Checkpoint class (inner)
    public static class Checkpoint {
        int level;
        int playerX, playerY, playerFacing;
        boolean hasObjectiveItem;
        char[][] maze;
        List<int[]> monsterPositions; // {x, y, facing}

        public Checkpoint(int level, int px, int py, int pf, boolean hasItem, char[][] m, List<int[]> mons) {
            this.level = level;
            this.playerX = px; this.playerY = py; this.playerFacing = pf;
            this.hasObjectiveItem = hasItem;
            this.maze = cloneMaze(m);
            this.monsterPositions = new ArrayList<>(mons);
        }

        private static char[][] cloneMaze(char[][] src) {
            char[][] dest = new char[src.length][src[0].length];
            for (int i = 0; i < src.length; i++) dest[i] = src[i].clone();
            return dest;
        }
    }

    // Save checkpoint
    private void saveCheckpoint() {
        List<int[]> mons = monsterManager.getAllMonsterPositions();
        checkpoints.add(new Checkpoint(currentLevel, playerX, playerY, playerFacing, hasObjectiveItem, maze, mons));
        if (checkpoints.size() > 5) checkpoints.remove(0); // Keep last 5
        storyManager.appendToLog("Checkpoint saved.\n");
    }

    // Load last checkpoint
    private void loadLastCheckpoint() {
        if (checkpoints.isEmpty()) {
            storyManager.appendToLog("No checkpoints available.\n");
            soundManager.playEvent("locked");
            return;
        }
        Checkpoint cp = checkpoints.remove(checkpoints.size() - 1);
        currentLevel = cp.level;
        playerX = cp.playerX; playerY = cp.playerY; playerFacing = cp.playerFacing;
        hasObjectiveItem = cp.hasObjectiveItem;
        maze = cp.maze;
        monsterManager.setAllMonsterPositions(cp.monsterPositions);
        // Replace P in maze
        for (int i = 0; i < maze.length; i++) {
            for (int j = 0; j < maze[i].length; j++) {
                if (maze[i][j] == 'P') maze[i][j] = '.';
            }
        }
        maze[playerX][playerY] = 'P';
        storyManager.appendToLog("Loaded from last checkpoint.\n");
        soundManager.playEvent("pickup");
        gamePanel.repaint();
    }

    // Key press handling
    private void handleKeyPress(KeyEvent e) {
        if (isPaused) {
            if (e.getKeyCode() == KeyEvent.VK_P) {
                resumeGame();
                return;
            }
            return; // Block other inputs when paused
        }

        int key = e.getKeyCode();
        int newX = playerX;
        int newY = playerY;
        int newFacing = playerFacing;

        switch (key) {
            case KeyEvent.VK_W, KeyEvent.VK_UP -> { newX--; newFacing = 0; }
            case KeyEvent.VK_A, KeyEvent.VK_LEFT -> { newY--; newFacing = 3; }
            case KeyEvent.VK_S, KeyEvent.VK_DOWN -> { newX++; newFacing = 2; }
            case KeyEvent.VK_D, KeyEvent.VK_RIGHT -> { newY++; newFacing = 1; }
            case KeyEvent.VK_P -> { pauseGame(); return; }
            case KeyEvent.VK_C -> { loadLastCheckpoint(); return; }
            case KeyEvent.VK_SPACE -> { interactWithSageOrBoss(); return; }
            case KeyEvent.VK_V -> { saveLoadManager.saveGame(this); return; }
            case KeyEvent.VK_L -> { saveLoadManager.loadGame(this); return; }
            case KeyEvent.VK_H -> { showHelp(); return; }
            default -> { return; }
        }

        if (isValidMove(newX, newY)) {
            char targetCell = maze[newX][newY];
            if ((currentLevel == 1 && targetCell == 'A') || (currentLevel == 2 && targetCell == 'S') || (currentLevel == 3 && targetCell == 'C')) {
                hasObjectiveItem = true;
                maze[newX][newY] = '.';
                String itemName = currentLevel == 1 ? "Crystal of Eternity" : currentLevel == 2 ? "Ancient Altar Seal" : "Celestial Spire Placement";
                storyManager.appendToLog("You acquired the " + itemName + "!\nNew Objective: Find the exit door.\n");
                currentObjective = "Find the exit door.";
                soundManager.playEvent("pickup");
            }

            if (targetCell == 'E') {
                if (hasObjectiveItem) {
                    if (currentLevel < MAX_LEVEL) loadLevel(currentLevel + 1);
                    else winGame();
                    return;
                } else {
                    storyManager.appendToLog("The exit door is sealed without the required item.\n");
                    soundManager.playEvent("locked");
                    return;
                }
            }

            if (monsterManager.isMonsterAt(newX, newY) || MonsterManager.isTrapAt(maze, newX, newY)) {
                loseGame();
                return;
            }

            char underlying = maze[playerX][playerY];
            maze[playerX][playerY] = (underlying == 'P') ? '.' : underlying;
            playerX = newX; playerY = newY; playerFacing = newFacing;
            maze[playerX][playerY] = 'P';

            if (isPlayerOnMonster()) loseGame();

            // Checkpoint every 10 moves
            moveCount++;
            if (moveCount % CHECKPOINT_INTERVAL == 0) {
                saveCheckpoint();
            }

            gamePanel.repaint();
        }
    }

    // Pause/Resume methods
    private void pauseGame() {
        isPaused = true;
        monsterTimer.stop();
        glowTimer.stop();
        soundManager.pauseBackgroundMusic(); // NEW: Pause background music
        storyManager.appendToLog("Game paused. Press P to resume.\n");
        soundManager.playEvent("locked");
        gamePanel.repaint();
    }

    public void resumeGame() {
        isPaused = false;
        monsterTimer.start();
        glowTimer.start();
        soundManager.resumeBackgroundMusic(); // NEW: Resume background music
        storyManager.appendToLog("Game resumed.\n");
        soundManager.playEvent("pickup");
        gamePanel.repaint();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!isPaused) {
            monsterManager.moveMonsters();
            checkStoryTriggers();
            if (isPlayerOnMonster()) loseGame();
        }
        gamePanel.repaint();
    }

    private void interactWithSageOrBoss() {
        int[] sagePos = MazeData.getSagePositionForLevel(currentLevel);
        int sx = sagePos[0], sy = sagePos[1];
        int dx = Math.abs(playerX - sx);
        int dy = Math.abs(playerY - sy);
        if (dx <= 1 && dy <= 1 && (dx + dy > 0)) {
            interactWithSage();
            return;
        }

        if (currentLevel == 4) {
            int[] bossPos = monsterManager.getBossPosition();
            if (bossPos != null) {
                int bx = bossPos[0], by = bossPos[1];
                dx = Math.abs(playerX - bx); dy = Math.abs(playerY - by);
                if (dx <= 1 && dy <= 1 && (dx + dy > 0)) {
                    if (hasObjectiveItem) {
                        monsterManager.killBoss();
                        storyManager.appendToLog("You used the Crystal's power to shatter the Corrupted Warden.\n");
                        soundManager.playEvent("boss_defeat");
                        maze[bx][by] = '.';
                        currentObjective = "Place the Crystal at the Spire to finish.";
                        storyManager.appendToLog("New Objective: " + currentObjective + "\n");
                        gamePanel.repaint();
                    } else {
                        loseGame();
                    }
                    return;
                }
            }
        }

        storyManager.appendToLog("There's nothing to interact with here.\n");
    }

    public void loadLevel(int level) {
        try {
            currentLevel = level;
            hasObjectiveItem = false;
            sageInteractionStage = 0;
            monsterManager.resetMonstersForLevel(level);

            maze = MazeData.getMazeClone(level);

            if (level == 1) {
                currentObjective = "Find the Sage for guidance on the curse.";
                storyManager.appendToLog("Level 1: The Cursed Labyrinth\nJournal Entry: I am Elara, seeking the Crystal of Eternity.\n");
                storyManager.appendToLog("Controls: WASD/Arrows to move, SPACE to interact, P to pause/resume, C to load checkpoint, H for help, V to save, L to load.\n");
                storyManager.appendToLog("Current Objective: " + currentObjective + "\n");
                MazeData.addRandomDecorations(maze, 5);
            } else if (level == 2) {
                currentObjective = "Find the Ancient Altar ('S') to seal the curse.";
                storyManager.appendToLog("Level 2: The Enchanted Forest\nSeal the Altar and find the exit.\n");
                storyManager.appendToLog("Current Objective: " + currentObjective + "\n");
                MazeData.addRandomDecorations(maze, 10);
            } else if (level == 3) {
                currentObjective = "Place the Crystal at the Celestial Spire ('C').";
                storyManager.appendToLog("Level 3: The Celestial Ruins\nPlace the Crystal to end the curse.\n");
                storyManager.appendToLog("Current Objective: " + currentObjective + "\n");
                MazeData.addRandomDecorations(maze, 8);
            } else if (level == 4) {
                currentObjective = "Confront the Corrupted Warden ('B') and restore the Spire.";
                storyManager.appendToLog("Level 4: The Warden's Vault\nDefeat the Warden and place the Crystal.\n");
                storyManager.appendToLog("Current Objective: " + currentObjective + "\n");
                MazeData.addRandomDecorations(maze, 6);
            }

            playerX = 1; playerY = 1; playerFacing = 2;
            storyManager.appendToLog("Entered Level " + level + ".\n");

            // Clear checkpoints when loading new level
            checkpoints.clear();
            moveCount = 0;

            gamePanel.repaint();
        } catch (Exception e) {
            storyManager.appendToLog("Error loading level: " + e.getMessage() + "\n");
        }
    }

    private void interactWithSage() {
        storyManager.appendToLog("You speak with the Sage.\n");
        storyManager.showSpeechBubble("Seek the Crystal to break the curse!");
        soundManager.playEvent("sage");
        sageInteractionStage++;
    }

    private boolean isValidMove(int x, int y) {
        return x >= 0 && x < ROWS && y >= 0 && y < COLS && maze[x][y] != '#' && maze[x][y] != 'W' && maze[x][y] != 'G';
    }

    private void checkStoryTriggers() {
        // Placeholder
    }

    private boolean isPlayerOnMonster() {
        return monsterManager.isMonsterAt(playerX, playerY);
    }

    private void showHelp() {
        storyManager.appendToLog("Controls: WASD/Arrows to move, SPACE to interact, P to pause/resume, C to load checkpoint, H for help, V to save, L to load.\n");
    }

    private void winGame() {
        storyManager.appendToLog("Congratulations! You have restored the Crystal and broken the curse!\n");
        soundManager.playEvent("win");
        soundManager.stopBackgroundMusic(); // NEW: Stop background music
        JOptionPane.showMessageDialog(this, "You Win! The Crystal of Eternity shines brightly.");
        System.exit(0);
    }

    private void loseGame() {
        storyManager.appendToLog("You have been defeated by a monster or trap!\n");
        soundManager.playEvent("lose");
        soundManager.stopBackgroundMusic(); // NEW: Stop background music
        JOptionPane.showMessageDialog(this, "Game Over! You were defeated.");
        System.exit(0);
    }
}