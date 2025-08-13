package org.a.playerTaskEss;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class TaskManager {
    private final Map<UUID, Set<String>> completedTasks = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastCompletionTimes = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Integer>> taskProgress = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerDailyTaskDate = new ConcurrentHashMap<>();
    
    private final JavaPlugin plugin;
    private final File dataFile;
    private final FileConfiguration dataConfig;
    
    public TaskManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "player_data.yml");
        this.dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadPlayerData();
    }
    
    public void savePlayerData() {
        try {
            // 保存完成的任务
            for (Map.Entry<UUID, Set<String>> entry : completedTasks.entrySet()) {
                String playerId = entry.getKey().toString();
                dataConfig.set("players." + playerId + ".completed_tasks", new ArrayList<>(entry.getValue()));
                dataConfig.set("players." + playerId + ".last_completion_time", lastCompletionTimes.get(entry.getKey()));
                dataConfig.set("players." + playerId + ".daily_task_date", playerDailyTaskDate.get(entry.getKey()));
                
                // 保存任务进度
                Map<String, Integer> progress = taskProgress.get(entry.getKey());
                if (progress != null) {
                    for (Map.Entry<String, Integer> progressEntry : progress.entrySet()) {
                        dataConfig.set("players." + playerId + ".progress." + progressEntry.getKey(), progressEntry.getValue());
                    }
                }
            }
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().severe("保存玩家数据失败: " + e.getMessage());
        }
    }
    
    private void loadPlayerData() {
        if (!dataFile.exists()) {
            return;
        }
        
        for (String playerIdStr : dataConfig.getConfigurationSection("players").getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(playerIdStr);
                Set<String> completed = new HashSet<>(dataConfig.getStringList("players." + playerIdStr + ".completed_tasks"));
                completedTasks.put(playerId, completed);
                
                long lastTime = dataConfig.getLong("players." + playerIdStr + ".last_completion_time", 0);
                if (lastTime > 0) {
                    lastCompletionTimes.put(playerId, lastTime);
                }
                
                String dailyDate = dataConfig.getString("players." + playerIdStr + ".daily_task_date");
                if (dailyDate != null) {
                    playerDailyTaskDate.put(playerId, dailyDate);
                }
                
                // 加载任务进度
                Map<String, Integer> progress = new HashMap<>();
                if (dataConfig.contains("players." + playerIdStr + ".progress")) {
                    for (String taskId : dataConfig.getConfigurationSection("players." + playerIdStr + ".progress").getKeys(false)) {
                        int value = dataConfig.getInt("players." + playerIdStr + ".progress." + taskId);
                        progress.put(taskId, value);
                    }
                }
                taskProgress.put(playerId, progress);
                
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("无效的玩家UUID: " + playerIdStr);
            }
        }
    }
    
    public boolean canCompleteTask(UUID playerId, String taskId) {
        // 检查每日任务是否重置
        if (!isSameDay(playerId)) {
            resetDailyTasks(playerId);
        }
        return !isTaskCompleted(playerId, taskId);
    }
    
    private boolean isSameDay(UUID playerId) {
        String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String lastDate = playerDailyTaskDate.get(playerId);
        return currentDate.equals(lastDate);
    }
    
    private void resetDailyTasks(UUID playerId) {
        completedTasks.remove(playerId);
        taskProgress.remove(playerId);
        String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        playerDailyTaskDate.put(playerId, currentDate);
    }
    
    public boolean isTaskCompleted(UUID playerId, String taskId) {
        Set<String> tasks = completedTasks.get(playerId);
        return tasks != null && tasks.contains(taskId);
    }
    
    public void markTaskCompleted(UUID playerId, String taskId) {
        completedTasks.computeIfAbsent(playerId, k -> ConcurrentHashMap.newKeySet()).add(taskId);
        lastCompletionTimes.put(playerId, System.currentTimeMillis());
        savePlayerData();
    }
    
    public boolean hasRequiredItem(Player player, ItemStack requiredItem) {
        return player.getInventory().containsAtLeast(requiredItem, requiredItem.getAmount());
    }
    
    public void removeRequiredItem(Player player, ItemStack requiredItem) {
        player.getInventory().removeItem(requiredItem);
    }
    
    public boolean tryCompleteTask(Player player, String taskId, ItemStack requiredItem) {
        if (canCompleteTask(player.getUniqueId(), taskId) && hasRequiredItem(player, requiredItem)) {
            removeRequiredItem(player, requiredItem);
            markTaskCompleted(player.getUniqueId(), taskId);
            return true;
        }
        return false;
    }
    
    // 任务进度相关方法
    public int getTaskProgress(UUID playerId, String taskId) {
        Map<String, Integer> progress = taskProgress.get(playerId);
        return progress != null ? progress.getOrDefault(taskId, 0) : 0;
    }
    
    public void incrementTaskProgress(UUID playerId, String taskId, int amount) {
        Map<String, Integer> progress = taskProgress.computeIfAbsent(playerId, k -> new HashMap<>());
        progress.put(taskId, progress.getOrDefault(taskId, 0) + amount);
        savePlayerData();
    }
    
    public void setTaskProgress(UUID playerId, String taskId, int value) {
        Map<String, Integer> progress = taskProgress.computeIfAbsent(playerId, k -> new HashMap<>());
        progress.put(taskId, value);
        savePlayerData();
    }
    
    public boolean isTaskTypeCompleted(UUID playerId, String taskId, int requiredAmount) {
        int progress = getTaskProgress(playerId, taskId);
        return progress >= requiredAmount;
    }
    
    public void onDisable() {
        savePlayerData();
    }
}
