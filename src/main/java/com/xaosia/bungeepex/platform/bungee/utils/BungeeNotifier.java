package com.xaosia.bungeepex.platform.bungee.utils;

import java.util.UUID;

import lombok.RequiredArgsConstructor;
import com.xaosia.bungeepex.BungeePEX;
import com.xaosia.bungeepex.platform.bungee.BungeeConfig;
import com.xaosia.bungeepex.PermissionGroup;
import com.xaosia.bungeepex.Statics;
import com.xaosia.bungeepex.PermissionUser;
import com.xaosia.bungeepex.platform.NetworkNotifier;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;

@RequiredArgsConstructor
public class BungeeNotifier implements NetworkNotifier
{

    private final BungeeConfig config;

    @Override
    public void deleteUser(PermissionUser u, String origin)
    {
        if (config.isUseUUIDs())
        {
            sendPM(u.getUUID(), "deleteUser;" + u.getUUID(), origin);
        }
        else
        {
            sendPM(u.getName(), "deleteUser;" + u.getName(), origin);
        }
    }

    @Override
    public void deleteGroup(PermissionGroup g, String origin)
    {
        sendPMAll("deleteGroup;" + g.getName(), origin);
    }

    @Override
    public void reloadUser(PermissionUser u, String origin)
    {
        if (config.isUseUUIDs())
        {
            sendPM(u.getUUID(), "reloadUser;" + u.getUUID(), origin);
        }
        else
        {
            sendPM(u.getName(), "reloadUser;" + u.getName(), origin);
        }
    }

    @Override
    public void reloadGroup(PermissionGroup g, String origin)
    {
        sendPMAll("reloadGroup;" + g.getName(), origin);
    }

    @Override
    public void reloadUsers(String origin)
    {
        sendPMAll("reloadUsers", origin);
    }

    @Override
    public void reloadGroups(String origin)
    {
        sendPMAll("reloadGroups", origin);
    }

    @Override
    public void reloadAll(String origin)
    {
        sendPMAll("reloadall", origin);
    }

    public void sendUUIDAndPlayer(String name, UUID uuid)
    {
        if (config.isUseUUIDs())
        {
            sendPM(uuid, "uuidcheck;" + name + ";" + uuid, null);
        }
    }

    //bukkit-bungeeperms reload information functions
    private void sendPM(String player, String msg, String origin)
    {
        //if standalone no network messages
        if (config.getNetworkType() == NetworkType.Standalone)
        {
            return;
        }

        ProxiedPlayer pp = ProxyServer.getInstance().getPlayer(player);
        if (pp != null && pp.getServer() != null)
        {
            //ignore servers not in config and netork type is server dependend
            if (config.getNetworkType() == NetworkType.ServerDependend
                    && !Statics.listContains(config.getNetworkServers(), pp.getServer().getInfo().getName()))
            {
                return;
            }
            if (config.getNetworkType() == NetworkType.ServerDependendBlacklist
                    && Statics.listContains(config.getNetworkServers(), pp.getServer().getInfo().getName()))
            {
                return;
            }

            //no feedback loop
            if (origin != null && pp.getServer().getInfo().getName().equalsIgnoreCase(origin))
            {
                return;
            }

            //send message
            pp.getServer().getInfo().sendData(BungeePEX.CHANNEL, msg.getBytes());
            sendConfig(pp.getServer().getInfo());
        }
    }

    private void sendPM(UUID player, String msg, String origin)
    {
        //if standalone no network messages
        if (config.getNetworkType() == NetworkType.Standalone)
        {
            return;
        }

        ProxiedPlayer pp = ProxyServer.getInstance().getPlayer(player);
        if (pp != null && pp.getServer() != null)
        {
            //ignore servers not in config and netork type is server dependend
            if (config.getNetworkType() == NetworkType.ServerDependend
                    && !Statics.listContains(config.getNetworkServers(), pp.getServer().getInfo().getName()))
            {
                return;
            }
            if (config.getNetworkType() == NetworkType.ServerDependendBlacklist
                    && Statics.listContains(config.getNetworkServers(), pp.getServer().getInfo().getName()))
            {
                return;
            }

            //no feedback loop
            if (origin != null && pp.getServer().getInfo().getName().equalsIgnoreCase(origin))
            {
                return;
            }

            //send message
            pp.getServer().getInfo().sendData(BungeePEX.CHANNEL, msg.getBytes());
            sendConfig(pp.getServer().getInfo());
        }
    }

    private void sendPMAll(String msg, String origin)
    {
        //if standalone no network messages
        if (config.getNetworkType() == NetworkType.Standalone)
        {
            return;
        }

        for (ServerInfo si : ProxyServer.getInstance().getConfig().getServers().values())
        {
            //ignore servers not in config and netork type is server dependend
            if (config.getNetworkType() == NetworkType.ServerDependend
                    && !Statics.listContains(config.getNetworkServers(), si.getName()))
            {
                return;
            }
            if (config.getNetworkType() == NetworkType.ServerDependendBlacklist
                    && Statics.listContains(config.getNetworkServers(), si.getName()))
            {
                return;
            }

            //no feedback loop
            if (origin != null && si.getName().equalsIgnoreCase(origin))
            {
                continue;
            }

            //send message
            si.sendData(BungeePEX.CHANNEL, msg.getBytes());
            sendConfig(si);
        }
    }

    private long lastConfigUpdate = 0;

    private void sendConfig(ServerInfo info)
    {
        synchronized (this)
        {
            long now = System.currentTimeMillis();
            if (lastConfigUpdate + 5 * 60 * 1000 < now)
            {
                lastConfigUpdate = now;
                info.sendData(BungeePEX.CHANNEL, ("configcheck;" + info.getName() + ";" + config.getBackEndType() + ";" + config.getBackEndType() + ";" + config.isUseUUIDs()).getBytes());
            }
        }
    }
}
