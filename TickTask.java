package com.whiteiverson.minecraft.playtime_plugin;

/**
 * TickTask is a Runnable that processes player data at regular intervals.
 */
public class TickTask implements Runnable {
    
    /**
     * Executes the task to process players.
     * This method is called by the Bukkit scheduler.
     */
    @Override
    public void run() {
        try {
            Main.getInstance().getPlayTimeHandler().processPlayers();
        } catch (Exception e) {
            // Log any exceptions that occur during player processing
            Main.getInstance().getLogger().severe("An error occurred while processing players: " + e.getMessage());
            e.printStackTrace();
        }
    }
}