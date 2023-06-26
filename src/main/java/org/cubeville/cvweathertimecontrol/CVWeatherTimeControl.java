package org.cubeville.cvweathertimecontrol;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.cubeville.cvipc.CVIPC;

public final class CVWeatherTimeControl extends JavaPlugin implements Listener {

    private Economy economy = null;

    private CVIPC cvipc;

    int weatherChangeTimer = 0;

    int timeChangeTimer = 0;

    boolean paidNight = false;
    World paidNightWorld;

    public void onEnable() {
        RegisteredServiceProvider<Economy> serviceProvider = getServer().getServicesManager().getRegistration(Economy.class);
        if (serviceProvider == null)
            return;
        this.economy = serviceProvider.getProvider();
        PluginManager pm = getServer().getPluginManager();
        this.cvipc = (CVIPC)pm.getPlugin("CVIPC");
        getServer().getScheduler().runTaskTimer(this, () -> {
            if (CVWeatherTimeControl.this.weatherChangeTimer > 0)
                CVWeatherTimeControl.this.weatherChangeTimer--;
            if (CVWeatherTimeControl.this.timeChangeTimer > 0)
                CVWeatherTimeControl.this.timeChangeTimer--;
            if(paidNight && paidNightWorld != null && (paidNightWorld.getTime() > 0L && paidNightWorld.getTime() < 13000L)) {
                paidNight = false;
            }
        },  1000L, 1000L);
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player))
            return true;
        Player player = (Player)sender;
        int cost = 0;
        if (command.getName().equals("instaday") || command.getName().equals("instanight")) {
            if (this.timeChangeTimer > 0) {
                player.sendMessage("§The time can be changed again in " + this.timeChangeTimer + " hours.");
                return true;
            }
            cost = 25;
        } else {
            if (this.weatherChangeTimer > 0) {
                player.sendMessage("§cThe weather can be changed again in " + this.weatherChangeTimer + " hours.");
                return true;
            }
            cost = 50;
        }
        if (!this.economy.hasAccount(player)) {
            player.sendMessage("§You don't have an account yet.");
            return true;
        }
        if (!this.economy.has(player, cost)) {
            player.sendMessage("§cYou don't have enough cubes, " + command.getName() + " costs " + cost + " cubes!");
            return true;
        }
        EconomyResponse re = this.economy.withdrawPlayer(player, cost);
        if (re.transactionSuccess()) {
            String type = null;
            if (command.getName().equals("instaday")) {
                type = "day";
                player.getLocation().getWorld().setTime(24000L);
                this.timeChangeTimer = 24;
            } else if (command.getName().equals("instanight")) {
                type = "night";
                player.getLocation().getWorld().setTime(38000L);
                this.timeChangeTimer = 24;
                this.paidNight = true;
                this.paidNightWorld = player.getLocation().getWorld();
            } else if (command.getName().equals("instasun")) {
                type = "sun";
                player.getLocation().getWorld().setStorm(false);
                this.weatherChangeTimer = 48;
            } else if (command.getName().equals("instastorm")) {
                type = "storm";
                player.getLocation().getWorld().setStorm(true);
                player.getLocation().getWorld().setThundering(true);
                this.weatherChangeTimer = 48;
            }
            if (type != null)
                this.cvipc.sendMessage("cmd|console|tr §d" + player.getDisplayName() + " paid " + cost + " cubes for " + type + " at " + player.getWorld().getName() +".");
        } else {
            player.sendMessage("§cTransaction failed.");
        }
        return true;
    }
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onBedEnter(PlayerBedEnterEvent e) {
        if(e.isCancelled()) return;
        if(!e.getBedEnterResult().equals(PlayerBedEnterEvent.BedEnterResult.OK)) return;
        if(paidNight) {
            e.setCancelled(true);
            e.getPlayer().sendMessage(ChatColor.RED + "You cannot sleep through nights that someone paid for!");
        }
    }
}
