package com.mycompany.graphicalmazegameenhanced;

import javax.swing.*;
import java.awt.*;
import java.util.Timer;
import java.util.TimerTask;

public class StoryManager {
    private final JTextArea storyLog = new JTextArea();
    private String activeSpeech = "";
    private boolean showingSpeech = false;

    public StoryManager() {
        storyLog.setEditable(false);
        storyLog.setWrapStyleWord(true);
        storyLog.setLineWrap(true);
        storyLog.setFont(new Font("Serif", Font.PLAIN, 14));
    }

    public JScrollPane createLogScrollPane() {
        JScrollPane sp = new JScrollPane(storyLog);
        return sp;
    }

    public void appendToLog(String text) {
        SwingUtilities.invokeLater(() -> {
            storyLog.append(text);
            storyLog.setCaretPosition(storyLog.getDocument().getLength());
        });
    }

    public void showSpeechBubble(String text) {
        activeSpeech = text;
        showingSpeech = true;
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                showingSpeech = false;
                activeSpeech = "";
            }
        }, 3500);
    }

    public boolean hasActiveSpeech() {
        return showingSpeech && activeSpeech != null && !activeSpeech.isEmpty();
    }

    public void drawSpeechBubble(Graphics2D g, int playerRow, int playerCol, int cellSize) {
        if (!hasActiveSpeech()) return;

        int px = playerCol * cellSize;
        int py = playerRow * cellSize;
        int bx = px - 10;
        int by = py - 40;
        int bw = 360;
        int bh = 60;

        g.setColor(new Color(255, 255, 255, 230));
        g.fillRoundRect(bx, by, bw, bh, 20, 20);
        g.setColor(Color.BLACK);
        g.drawRoundRect(bx, by, bw, bh, 20, 20);

        g.setFont(new Font("Serif", Font.PLAIN, 12));
        drawStringWrapped(g, activeSpeech.replaceAll("\n", " "), bx + 10, by + 20, bw - 20);
    }

    private void drawStringWrapped(Graphics2D g, String text, int x, int y, int maxWidth) {
        FontMetrics fm = g.getFontMetrics();
        String[] words = text.split(" ");
        String line = "";
        int lineHeight = fm.getHeight();
        int curY = y;
        for (String w : words) {
            String test = line.isEmpty() ? w : line + " " + w;
            if (fm.stringWidth(test) > maxWidth) {
                g.drawString(line, x, curY);
                line = w;
                curY += lineHeight;
            } else {
                line = test;
            }
        }
        if (!line.isEmpty()) g.drawString(line, x, curY);
    }
}