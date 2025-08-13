package org.a.playerTaskEss;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;

public class MenuListener implements Listener {
    private final PlayerTaskEss plugin;

    public MenuListener(PlayerTaskEss plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().equals(plugin.getDailyTaskMenu().getMenuTitle())) {
            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack clickedItem = event.getCurrentItem();

        if (clickedItem == null || clickedItem.getType() == Material.AIR) {
            return;
        }

        // 检查是否是刷新按钮
        if (clickedItem.getType() == Material.ARROW) {
            ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.getDisplayName().equals("§e刷新任务")) {
                handleRefreshButton(player);
                return;
            }
        }

        // 获取点击物品的元数据
        ItemMeta meta = clickedItem.getItemMeta();
        if (meta == null || !meta.hasLore()) {
            return;
        }

        // 从物品lore中获取任务ID
        String taskId = null;
        for (String loreLine : meta.getLore()) {
            if (loreLine.startsWith("ID:")) {
                taskId = loreLine.substring(3).trim();
                break;
            }
        }

        if (taskId == null) {
            return;
        }

        // 处理任务
        handleTask(player, taskId);
    }
    
    private void handleRefreshButton(Player player) {
        // 检查玩家权限
        if (!player.hasPermission("playertaskess.refresh")) {
            player.sendMessage("§c你没有权限刷新任务!");
            return;
        }
        
        // 刷新任务
        plugin.getDailyTaskMenu().refreshMenu(player);
        player.sendMessage("§a任务已刷新!");
    }

    private void handleTask(Player player, String taskId) {
        TaskManager taskManager = plugin.getTaskManager();
        Economy economy = plugin.getEconomy();
        TaskConfig taskConfig = plugin.getTaskConfig();

        // 获取任务配置
        TaskConfig.Task task = taskConfig.getAllTasks().stream()
            .filter(t -> t.getId().equals(taskId))
            .findFirst()
            .orElse(null);

        if (task == null) {
            plugin.getLogger().warning("找不到任务配置: " + taskId);
            player.sendMessage("§c任务配置错误: 找不到任务ID " + taskId);
            return;
        }

        // 检查是否可以完成任务
        if (taskManager.isTaskCompleted(player.getUniqueId(), taskId)) {
            player.sendMessage("§c你已经完成过这个任务了!");
            return;
        }

        // 根据任务类型处理
        boolean canComplete = false;
        
        switch (task.getType()) {
            case COLLECT:
                canComplete = handleCollectTask(player, task, taskManager);
                break;
            case FISH:
            case HARVEST:
            case TRADE:
            case KILL:
            case CRAFT:
            case MINE:
            case PLACE:
                canComplete = handleProgressTask(player, task, taskManager);
                break;
        }

        if (canComplete) {
            // 完成任务
            taskManager.markTaskCompleted(player.getUniqueId(), taskId);
            economy.depositPlayer(player, task.getPrice());
            player.sendMessage("§a成功完成任务! 获得" + task.getPrice() + "游戏币");
            
            // 重新打开菜单以更新显示
            plugin.getDailyTaskMenu().openMenu(player);
        }
    }
    
    private boolean handleCollectTask(Player player, TaskConfig.Task task, TaskManager taskManager) {
        // 解析所需物品
        Material material = null;
        int amount = 1;
        try {
            String[] requiredItemParts = task.getRequiredItem().split(":");
            if (requiredItemParts.length == 0) {
                throw new IllegalArgumentException("所需物品配置为空");
            }
            
            material = Material.matchMaterial(requiredItemParts[0]);
            if (material == null) {
                throw new IllegalArgumentException("无效的物品类型: " + requiredItemParts[0]);
            }
            
            amount = requiredItemParts.length > 1 ? Integer.parseInt(requiredItemParts[1]) : 1;
            if (amount <= 0) {
                throw new IllegalArgumentException("物品数量必须大于0");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("任务物品配置错误 - 任务ID: " + task.getId() + " 错误: " + e.getMessage());
            player.sendMessage("§c任务配置错误: " + e.getMessage());
            return false;
        }

        // 检查玩家是否有足够物品
        ItemStack requiredItem = new ItemStack(material, amount);
        if (!taskManager.hasRequiredItem(player, requiredItem)) {
            player.sendMessage("§c你的背包中没有足够的" + task.getName() + "!");
            return false;
        }

        // 移除物品
        taskManager.removeRequiredItem(player, requiredItem);
        return true;
    }
    
    private boolean handleProgressTask(Player player, TaskConfig.Task task, TaskManager taskManager) {
        int progress = taskManager.getTaskProgress(player.getUniqueId(), task.getId());
        int requiredAmount = task.getRequiredAmount();
        
        if (progress < requiredAmount) {
            player.sendMessage("§c任务进度不足! 当前进度: " + progress + "/" + requiredAmount);
            return false;
        }
        
        return true;
    }
}
