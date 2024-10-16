package com.whiteiverson.minecraft.playtime_plugin;

/**
 * TickTask is a Runnable that processes player data at regular intervals.
 */
public class TickTask implements Runnable {
	
	private final Main main;
	
	public TickTask() {
		this.main = Main.getInstance();
	}
    
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
        	if (main.getConfig().getBoolean("logging.debug", false)) {
	            Main.getInstance().getLogger().severe("An error occurred while processing players: " + e.getMessage());
	            e.printStackTrace();
        	}
        }
    }
}