package com.mycompany.graphicalmazegameenhanced;

import javax.sound.sampled.*;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.awt.Toolkit;

public class SoundManager {
    private final Map<String, String> eventToFile = new HashMap<>();
    private Clip backgroundClip; // NEW: For looping background music
    private long backgroundClipPosition; // NEW: To store pause position

    public SoundManager() {
        // Existing event mappings
        eventToFile.put("pickup", "/sounds/pickup.wav");
        eventToFile.put("locked", "/sounds/locked.wav");
        eventToFile.put("sage", "/sounds/sage.wav");
        eventToFile.put("glow", "/sounds/glow.wav");
        eventToFile.put("boss_defeat", "/sounds/boss_defeat.wav");
        eventToFile.put("win", "/sounds/win.wav");
        eventToFile.put("lose", "/sounds/lose.wav");
        // NEW: Sci-fi background track
        eventToFile.put("background", "/sounds/scifi_maze_track.wav");
    }

    // NEW: Start background music (looping)
    public void startBackgroundMusic() {
        stopBackgroundMusic(); // Ensure no existing clip is playing
        String resource = eventToFile.get("background");
        if (resource == null) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        try (InputStream is = getClass().getResourceAsStream(resource)) {
            if (is == null) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(is);
            backgroundClip = AudioSystem.getClip();
            backgroundClip.open(audioIn);
            backgroundClip.loop(Clip.LOOP_CONTINUOUSLY); // Loop indefinitely
            backgroundClip.start();
        } catch (Exception e) {
            Toolkit.getDefaultToolkit().beep();
            System.err.println("Error starting background music: " + e.getMessage());
        }
    }

    // NEW: Stop background music
    public void stopBackgroundMusic() {
        if (backgroundClip != null && backgroundClip.isRunning()) {
            backgroundClip.stop();
            backgroundClip.close();
            backgroundClip = null;
        }
        backgroundClipPosition = 0;
    }

    // NEW: Pause background music
    public void pauseBackgroundMusic() {
        if (backgroundClip != null && backgroundClip.isRunning()) {
            backgroundClipPosition = backgroundClip.getMicrosecondPosition();
            backgroundClip.stop();
        }
    }

    // NEW: Resume background music
    public void resumeBackgroundMusic() {
        if (backgroundClip != null && !backgroundClip.isRunning()) {
            backgroundClip.setMicrosecondPosition(backgroundClipPosition);
            backgroundClip.loop(Clip.LOOP_CONTINUOUSLY);
            backgroundClip.start();
        }
    }

    // Existing: Play one-shot event sound
    public void playEvent(String event) {
        String resource = eventToFile.get(event);
        if (resource == null) {
            Toolkit.getDefaultToolkit().beep();
            return;
        }
        try (InputStream is = getClass().getResourceAsStream(resource)) {
            if (is == null) {
                Toolkit.getDefaultToolkit().beep();
                return;
            }
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(is);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.start();
        } catch (Exception e) {
            Toolkit.getDefaultToolkit().beep();
        }
    }
}