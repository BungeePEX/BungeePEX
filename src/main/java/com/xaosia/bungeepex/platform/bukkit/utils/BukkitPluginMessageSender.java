package com.xaosia.bungeepex.platform.bukkit.utils;

import com.xaosia.bungeepex.BungeePEX;
import com.xaosia.bungeepex.platform.PluginMessageSender;
import com.xaosia.bungeepex.platform.bukkit.BukkitPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BukkitPluginMessageSender implements PluginMessageSender
{

    @Override
    public void sendPluginMessage(String target, String channel, String msg)
    {
        if (!target.equalsIgnoreCase("bungee"))
        {
            return;
        }

        Player p = Bukkit.getOnlinePlayers().iterator().hasNext() ? Bukkit.getOnlinePlayers().iterator().next() : null;
        if (p == null)
        {
            BungeePEX.getLogger().info("No server found for " + target);
            return;
        }

        p.sendPluginMessage(BukkitPlugin.getInstance(), BungeePEX.CHANNEL, msg.getBytes());
    }
}
