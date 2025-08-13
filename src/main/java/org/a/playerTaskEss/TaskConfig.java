package org.a.playerTaskEss;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public class TaskConfig {
    private static JavaPlugin plugin;
    private final Random random = new Random();
    private List<Task> allTasks = new ArrayList<>();
    private final Map<String, List<Task>> dailyTasks = new HashMap<>();

    public static JavaPlugin getPlugin() {
        return plugin;
    }

    public TaskConfig(JavaPlugin plugin) {
        TaskConfig.plugin = plugin;
        loadTasks();
    }

    private void loadTasks() {
        FileConfiguration config = this.plugin.getConfig();
        ConfigurationSection tasksSection = config.getConfigurationSection("tasks");

        if (tasksSection != null) {
            for (String key : tasksSection.getKeys(false)) {
                ConfigurationSection taskSection = tasksSection.getConfigurationSection(key);
                if (taskSection != null) {
                    Task task = new Task(
                        key,
                        taskSection.getString("name", "未知任务"),
                        taskSection.getString("description", ""),
                        Material.matchMaterial(taskSection.getString("material", "STONE")),
                        taskSection.getInt("price", 10),
                        taskSection.getString("required-item", "STONE"),
                        taskSection.getStringList("detailed-description"),
                        taskSection.getString("click-command", ""),
                        TaskType.valueOf(taskSection.getString("type", "COLLECT").toUpperCase()),
                        taskSection.getInt("required-amount", 1),
                        taskSection.getStringList("target-materials")
                    );
                    allTasks.add(task);
                }
            }
        }
    }

    public List<Task> getAllTasks() {
        return new ArrayList<>(allTasks);
    }

    public List<Task> getRandomTasks(int count) {
        if (allTasks.size() <= count) {
            return new ArrayList<>(allTasks);
        }

        return random.ints(0, allTasks.size())
            .distinct()
            .limit(count)
            .mapToObj(allTasks::get)
            .collect(Collectors.toList());
    }
    
    public List<Task> getDailyTasks(String date) {
        return dailyTasks.computeIfAbsent(date, k -> getRandomTasks(8));
    }
    
    public void refreshDailyTasks(String date) {
        dailyTasks.put(date, getRandomTasks(8));
    }

    public static class Task {
        private final String id;
        private final String name;
        private final String description;
        private final Material material;
        private final int price;
        private final String requiredItem;
        private final List<String> detailedDescription;
        private final String clickCommand;
        private final TaskType type;
        private final int requiredAmount;
        private final List<String> targetMaterials;

        public Task(String id, String name, String description, Material material, int price, String requiredItem) {
            this(id, name, description, material, price, requiredItem, null, "", TaskType.COLLECT, 1, null);
        }

        public Task(String id, String name, String description, Material material, int price, String requiredItem, 
                   List<String> detailedDescription, String clickCommand, TaskType type, int requiredAmount, 
                   List<String> targetMaterials) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.material = material;
            this.price = price;
            this.requiredItem = requiredItem;
            this.detailedDescription = detailedDescription;
            this.clickCommand = clickCommand;
            this.type = type;
            this.requiredAmount = requiredAmount;
            this.targetMaterials = targetMaterials;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getDescription() {
            return description;
        }

        public Material getMaterial() {
            return material;
        }

        public int getPrice() {
            return price;
        }

        public String getRequiredItem() {
            return requiredItem;
        }

        public List<String> getDetailedDescription() {
            return detailedDescription;
        }

        public String getClickCommand() {
            return clickCommand;
        }
        
        public TaskType getType() {
            return type;
        }
        
        public int getRequiredAmount() {
            return requiredAmount;
        }
        
        public List<String> getTargetMaterials() {
            return targetMaterials;
        }
    }
    
    public enum TaskType {
        COLLECT("收集任务"),
        FISH("钓鱼任务"),
        HARVEST("收割任务"),
        TRADE("交易任务"),
        KILL("击杀任务"),
        CRAFT("制作任务"),
        MINE("挖矿任务"),
        PLACE("放置任务");
        
        private final String displayName;
        
        TaskType(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
}
