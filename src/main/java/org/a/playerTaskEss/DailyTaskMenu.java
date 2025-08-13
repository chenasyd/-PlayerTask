package org.a.playerTaskEss;

import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class DailyTaskMenu {
    private final String menuTitle;
    private final Material borderMaterial;
    private final Material completedMaterial;
    private final int menuSize;
    
    private final TaskManager taskManager;
    private final TaskConfig taskConfig;

    public DailyTaskMenu(TaskManager taskManager, TaskConfig taskConfig) {
        this.taskManager = taskManager;
        this.taskConfig = taskConfig;
        
        FileConfiguration config = taskConfig.getPlugin().getConfig();
        this.menuTitle = config.getString("menu.title", "每日任务");
        this.menuSize = config.getInt("menu.size", 54);
        this.borderMaterial = Material.matchMaterial(
            config.getString("menu.border-material", "BLACK_STAINED_GLASS_PANE")
        );
        this.completedMaterial = Material.matchMaterial(
            config.getString("menu.completed-material", "GRAY_DYE")
        );
    }

    public void openMenu(Player player) {
        Inventory menu = Bukkit.createInventory(null, menuSize, menuTitle);
        
        // 添加边框和装饰
        addBorder(menu);
        addDecorations(menu);
        
        // 获取当前日期的任务
        String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        List<TaskConfig.Task> tasks = taskConfig.getDailyTasks(currentDate);
        addTasks(menu, player, tasks);

        player.openInventory(menu);
    }
    
    public void refreshMenu(Player player) {
        String currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        taskConfig.refreshDailyTasks(currentDate);
        openMenu(player);
    }

    private void addBorder(Inventory menu) {
        ItemStack border = new ItemStack(borderMaterial);
        ItemMeta meta = border.getItemMeta();
        meta.setDisplayName(" ");
        border.setItemMeta(meta);

        // 顶部和底部边框
        for (int i = 0; i < 9; i++) {
            menu.setItem(i, border);
            menu.setItem(i + (5 * 9), border);
        }

        // 侧边边框
        for (int i = 1; i < 5; i++) {
            menu.setItem(i * 9, border);
            menu.setItem(i * 9 + 8, border);
        }
    }

    private void addDecorations(Inventory menu) {
        FileConfiguration config = taskConfig.getPlugin().getConfig();
        
        // 顶部中间装饰
        ItemStack titleDecor = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = titleDecor.getItemMeta();
        meta.setDisplayName(config.getString("menu.decorations.title.name", "§6每日任务奖励"));
        meta.setLore(config.getStringList("menu.decorations.title.lore"));
        titleDecor.setItemMeta(meta);
        menu.setItem(4, titleDecor);
        
        // 底部中间装饰
        ItemStack bottomDecor = new ItemStack(Material.EMERALD);
        meta = bottomDecor.getItemMeta();
        meta.setDisplayName(config.getString("menu.decorations.bottom.name", "§a任务中心"));
        meta.setLore(config.getStringList("menu.decorations.bottom.lore"));
        bottomDecor.setItemMeta(meta);
        menu.setItem(49, bottomDecor);
        
        // 刷新按钮
        ItemStack refreshButton = new ItemStack(Material.ARROW);
        meta = refreshButton.getItemMeta();
        meta.setDisplayName("§e刷新任务");
        meta.setLore(Arrays.asList("§7点击刷新今日任务", "§c注意：刷新后任务将重新随机"));
        refreshButton.setItemMeta(meta);
        menu.setItem(45, refreshButton);
    }

    private void addTasks(Inventory menu, Player player, List<TaskConfig.Task> tasks) {
        int[] taskSlots = {19, 21, 23, 25, 28, 30, 32, 34};
        UUID playerId = player.getUniqueId();
        
        for (int i = 0; i < tasks.size() && i < taskSlots.length; i++) {
            TaskConfig.Task task = tasks.get(i);
            boolean isCompleted = taskManager.isTaskCompleted(playerId, task.getId());
            
            ItemStack taskItem = createTaskItem(
                isCompleted ? completedMaterial : task.getMaterial(),
                "§e" + task.getName(),
                "§7" + task.getDescription(),
                "§a奖励: " + task.getPrice() + "游戏币",
                player,
                task
            );
            
            menu.setItem(taskSlots[i], taskItem);
        }
    }

    private ItemStack createTaskItem(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        meta.setLore(Arrays.asList(lore));
        item.setItemMeta(meta);
        return item;
    }

    public String getMenuTitle() {
        return menuTitle;
    }

    private ItemStack createTaskItem(Material material, String name, String description, String reward, Player player, TaskConfig.Task task) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);

        boolean isCompleted = taskManager.isTaskCompleted(player.getUniqueId(), task.getId());
        String status;
        
        List<String> lore = new ArrayList<>();
        lore.add(description);
        
        // 根据任务类型显示不同的进度信息
        if (task.getType() == TaskConfig.TaskType.COLLECT) {
            // 收集任务显示背包中的物品数量
            int currentAmount = getPlayerItemCount(player, task.getRequiredItem());
            int requiredAmount = task.getRequiredAmount();
            lore.add("§7进度: " + currentAmount + "/" + requiredAmount);
            status = isCompleted ? "§c已完成" : (currentAmount >= requiredAmount ? "§a可完成" : "§e进行中");
        } else {
            // 其他任务类型显示进度
            int progress = taskManager.getTaskProgress(player.getUniqueId(), task.getId());
            int requiredAmount = task.getRequiredAmount();
            lore.add("§7进度: " + progress + "/" + requiredAmount);
            status = isCompleted ? "§c已完成" : (progress >= requiredAmount ? "§a可完成" : "§e进行中");
        }
        
        // 添加详细描述
        List<String> detailedDesc = task.getDetailedDescription();
        if (detailedDesc != null && !detailedDesc.isEmpty()) {
            lore.addAll(detailedDesc);
        }
        
        lore.add(reward);
        lore.add("");
        lore.add(status);
        lore.add("§7类型: " + task.getType().getDisplayName());
        lore.add("ID: " + task.getId());

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
    
    private int getPlayerItemCount(Player player, String requiredItem) {
        try {
            String[] parts = requiredItem.split(":");
            Material material = Material.matchMaterial(parts[0]);
            if (material == null) return 0;
            
            int count = 0;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.getType() == material) {
                    count += item.getAmount();
                }
            }
            return count;
        } catch (Exception e) {
            return 0;
        }
    }
}
