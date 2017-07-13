package com.xaosia.bungeepex.platform.bungee.utils;

import com.xaosia.bungeepex.BungeePEX;
import com.xaosia.bungeepex.platform.PluginMessageSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;

public class BungeePluginMessageSender implements PluginMessageSender
{
    
    @Override
    public void sendPluginMessage(String target, String channel, String msg)
    {
        ServerInfo si = ProxyServer.getInstance().getServerInfo(target);
        if (si == null)
        {
            BungeePEX.getLogger().info("No server found for " + target);
            return;
        }
        
        si.sendData(channel, msg.getBytes());
    }
}
