package org.spectrum.gametimeleaderboard;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;

import java.util.Comparator;
import java.util.List;
import java.util.Arrays;
import java.util.stream.Collectors;

public class GameTimeLeaderboard extends JavaPlugin {

    private Scoreboard scoreboard;

    @Override
    public void onEnable() {

        // Get the scoreboard manager and create a new scoreboard
        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        scoreboard = scoreboardManager.getNewScoreboard();

        // Register a new scoreboard objective
        Objective objective = scoreboard.registerNewObjective("GameTimeLeaderboard", "dummy", "Game Time Leaderboard");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        // Register the command handler
        getCommand("playtime").setExecutor(this);

        // Create a scheduled task to update the leaderboard every minute
        new BukkitRunnable() {
            @Override
            public void run() {
                updateLeaderboard();
            }
        }.runTaskTimer(this, 0, 20 * 3); // Executes every 60 seconds (1 minute)
        updateLeaderboard();

        getLogger().info("GameTimeLeaderboard plugin has been loaded. Author: Spectrum_Tim");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("playtime")) {
            if (args.length != 1) {
                sender.sendMessage("Usage: /playtime <player>");
                return true;
            }

            String playerName = args[0];
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
            if (player == null || !player.hasPlayedBefore()) {
                sender.sendMessage("Player does not exist or has never joined the server: " + playerName);
                return true;
            }

            long playTime = getPlayerPlayTime(player);
            double playTimeInHours = playTime / 72000.0; // Convert to hours

            sender.sendMessage(player.getName() + "'s playtime is: " + String.format("%.2f", playTimeInHours) + " hours");
            return true;
        }
        return false;
    }

    private void updateLeaderboard() {
        // Use streams to convert array to list, processing all offline players
        List<OfflinePlayer> allPlayers = Arrays.stream(Bukkit.getOfflinePlayers())
                .filter(player -> player.hasPlayedBefore()) // Filter out players who have never joined
                .sorted(Comparator.comparingLong(this::getPlayerPlayTime).reversed()) // Sort by playtime in descending order
                .limit(5) // Limit to top 5 players
                .collect(Collectors.toList());

        // Update the scoreboard display
        Objective objective = scoreboard.getObjective("GameTimeLeaderboard");
        if (objective != null) {
            objective.unregister(); // Unregister the old objective

            objective = scoreboard.registerNewObjective("GameTimeLeaderboard", "dummy", "Game Time Leaderboard");
            objective.setDisplaySlot(DisplaySlot.SIDEBAR);

            int rank = 1;
            for (OfflinePlayer player : allPlayers) {
                double playTimeInHours = getPlayerPlayTime(player) / 72000.0; // Convert to hours

                // Display player name and playtime (in hours) on the scoreboard, sorted from high to low
                Score score = objective.getScore(player.getName() + " - " + String.format("%.2f", playTimeInHours) + "h");
                score.setScore(rank); // Set score to rank players, without adjusting it (no 6 - rank)

                rank++;
            }
        }

        // Apply the scoreboard to all online players
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.setScoreboard(scoreboard);
        }
    }

    private long getPlayerPlayTime(OfflinePlayer player) {
        return player.getStatistic(Statistic.PLAY_ONE_MINUTE);
    }
}
