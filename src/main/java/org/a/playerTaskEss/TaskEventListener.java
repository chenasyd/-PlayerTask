package org.a.playerTaskEss;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;

import java.util.List;

public class TaskEventListener implements Listener {
    private final PlayerTaskEss plugin;

    public TaskEventListener(PlayerTaskEss plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            Player player = event.getPlayer();
            TaskManager taskManager = plugin.getTaskManager();
            TaskConfig taskConfig = plugin.getTaskConfig();
            
            // 检查玩家是否有钓鱼任务
            String currentDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
            List<TaskConfig.Task> dailyTasks = taskConfig.getDailyTasks(currentDate);
            
            for (TaskConfig.Task task : dailyTasks) {
                if (task.getType() == TaskConfig.TaskType.FISH && 
                    !taskManager.isTaskCompleted(player.getUniqueId(), task.getId())) {
                    taskManager.incrementTaskProgress(player.getUniqueId(), task.getId(), 1);
                    player.sendMessage("§a钓鱼任务进度更新: " + task.getName());
                }
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();
        TaskManager taskManager = plugin.getTaskManager();
        TaskConfig taskConfig = plugin.getTaskConfig();
        
        String currentDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
        List<TaskConfig.Task> dailyTasks = taskConfig.getDailyTasks(currentDate);
        
        for (TaskConfig.Task task : dailyTasks) {
            if (taskManager.isTaskCompleted(player.getUniqueId(), task.getId())) {
                continue;
            }
            
            switch (task.getType()) {
                case MINE:
                    // 检查是否是目标方块
                    if (isTargetMaterial(blockType, task.getTargetMaterials())) {
                        taskManager.incrementTaskProgress(player.getUniqueId(), task.getId(), 1);
                        player.sendMessage("§a挖矿任务进度更新: " + task.getName());
                    }
                    break;
                case HARVEST:
                    // 检查是否是农作物
                    if (isCropMaterial(blockType)) {
                        taskManager.incrementTaskProgress(player.getUniqueId(), task.getId(), 1);
                        player.sendMessage("§a收割任务进度更新: " + task.getName());
                    }
                    break;
            }
        }
    }

    @EventHandler
    public void onCraftItem(CraftItemEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        
        Player player = (Player) event.getWhoClicked();
        ItemStack craftedItem = event.getCurrentItem();
        TaskManager taskManager = plugin.getTaskManager();
        TaskConfig taskConfig = plugin.getTaskConfig();
        
        String currentDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
        List<TaskConfig.Task> dailyTasks = taskConfig.getDailyTasks(currentDate);
        
        for (TaskConfig.Task task : dailyTasks) {
            if (task.getType() == TaskConfig.TaskType.CRAFT && 
                !taskManager.isTaskCompleted(player.getUniqueId(), task.getId())) {
                if (isTargetMaterial(craftedItem.getType(), task.getTargetMaterials())) {
                    taskManager.incrementTaskProgress(player.getUniqueId(), task.getId(), craftedItem.getAmount());
                    player.sendMessage("§a制作任务进度更新: " + task.getName());
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Merchant)) {
            return;
        }
        
        Player player = event.getPlayer();
        Merchant merchant = (Merchant) event.getRightClicked();
        TaskManager taskManager = plugin.getTaskManager();
        TaskConfig taskConfig = plugin.getTaskConfig();
        
        String currentDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
        List<TaskConfig.Task> dailyTasks = taskConfig.getDailyTasks(currentDate);
        
        for (TaskConfig.Task task : dailyTasks) {
            if (task.getType() == TaskConfig.TaskType.TRADE && 
                !taskManager.isTaskCompleted(player.getUniqueId(), task.getId())) {
                // 检查是否有交易任务
                taskManager.incrementTaskProgress(player.getUniqueId(), task.getId(), 1);
                player.sendMessage("§a交易任务进度更新: " + task.getName());
            }
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) {
            return;
        }
        
        Player player = event.getEntity().getKiller();
        TaskManager taskManager = plugin.getTaskManager();
        TaskConfig taskConfig = plugin.getTaskConfig();
        
        String currentDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
        List<TaskConfig.Task> dailyTasks = taskConfig.getDailyTasks(currentDate);
        
        for (TaskConfig.Task task : dailyTasks) {
            if (task.getType() == TaskConfig.TaskType.KILL && 
                !taskManager.isTaskCompleted(player.getUniqueId(), task.getId())) {
                // 检查是否是目标实体
                if (isTargetEntity(event.getEntity().getType().name(), task.getTargetMaterials())) {
                    taskManager.incrementTaskProgress(player.getUniqueId(), task.getId(), 1);
                    player.sendMessage("§a击杀任务进度更新: " + task.getName());
                }
            }
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Material blockType = event.getBlock().getType();
        TaskManager taskManager = plugin.getTaskManager();
        TaskConfig taskConfig = plugin.getTaskConfig();
        
        String currentDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE);
        List<TaskConfig.Task> dailyTasks = taskConfig.getDailyTasks(currentDate);
        
        for (TaskConfig.Task task : dailyTasks) {
            if (task.getType() == TaskConfig.TaskType.PLACE && 
                !taskManager.isTaskCompleted(player.getUniqueId(), task.getId())) {
                if (isTargetMaterial(blockType, task.getTargetMaterials())) {
                    taskManager.incrementTaskProgress(player.getUniqueId(), task.getId(), 1);
                    player.sendMessage("§a放置任务进度更新: " + task.getName());
                }
            }
        }
    }

    private boolean isTargetMaterial(Material material, List<String> targetMaterials) {
        if (targetMaterials == null || targetMaterials.isEmpty()) {
            return true; // 如果没有指定目标材料，则所有材料都算
        }
        
        for (String target : targetMaterials) {
            if (material.name().equals(target.toUpperCase())) {
                return true;
            }
        }
        return false;
    }

    private boolean isCropMaterial(Material material) {
        return material == Material.WHEAT ||
               material == Material.POTATOES ||
               material == Material.CARROTS ||
               material == Material.BEETROOTS ||
               material == Material.MELON ||
               material == Material.PUMPKIN ||
               material == Material.SUGAR_CANE ||
               material == Material.CACTUS ||
               material == Material.COCOA ||
               material == Material.NETHER_WART;
    }

    private boolean isTargetEntity(String entityType, List<String> targetEntities) {
        if (targetEntities == null || targetEntities.isEmpty()) {
            return true; // 如果没有指定目标实体，则所有实体都算
        }
        
        for (String target : targetEntities) {
            if (entityType.equals(target.toUpperCase())) {
                return true;
            }
        }
        return false;
    }
}
