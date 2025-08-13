package org.a.playerTaskEss;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerTaskEss extends JavaPlugin {
    private TaskManager taskManager;
    private TaskConfig taskConfig;
    private DailyTaskMenu dailyTaskMenu;
    private Economy economy;
    
    @Override
    public void onEnable() {
        // 保存默认配置
        saveDefaultConfig();
        
        // 初始化任务系统
        taskManager = new TaskManager(this);
        taskConfig = new TaskConfig(this);
        
        // 初始化经济系统
        if (!setupEconomy()) {
            getLogger().severe("未找到Vault经济系统，插件将禁用!");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        
        // 初始化每日任务菜单
        dailyTaskMenu = new DailyTaskMenu(taskManager, taskConfig);
        
        // 注册命令
        getCommand("dailytask").setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof Player)) {
                sender.sendMessage("只有玩家可以使用此命令!");
                return true;
            }
            dailyTaskMenu.openMenu((Player) sender);
            return true;
        });
        
        // 注册事件监听器
        getServer().getPluginManager().registerEvents(new MenuListener(this), this);
        getServer().getPluginManager().registerEvents(new TaskEventListener(this), this);
    }

    @Override
    public void onDisable() {
        if (taskManager != null) {
            taskManager.onDisable();
        }
        getLogger().info("PlayerTaskEss 插件已禁用!");
    }
    
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }
    
    public TaskManager getTaskManager() {
        return taskManager;
    }
    
    public Economy getEconomy() {
        return economy;
    }
    
    public DailyTaskMenu getDailyTaskMenu() {
        return dailyTaskMenu;
    }
    
    public TaskConfig getTaskConfig() {
        return taskConfig;
    }
}
