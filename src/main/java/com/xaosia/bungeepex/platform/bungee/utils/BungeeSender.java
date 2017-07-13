package com.xaosia.bungeepex.platform.bungee.utils;

import java.util.UUID;

import com.xaosia.bungeepex.platform.bungee.listeners.BungeeEventListener;
import lombok.AllArgsConstructor;
import lombok.Getter;
import com.xaosia.bungeepex.BungeePEX;
import com.xaosia.bungeepex.platform.MessageEncoder;
import com.xaosia.bungeepex.platform.Sender;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

@Getter
@AllArgsConstructor
public class BungeeSender implements Sender
{

    private CommandSender sender;

    @Override
    public void sendMessage(String message)
    {
        sender.sendMessage(message);
    }

    @Override
    public void sendMessage(MessageEncoder encoder)
    {
        BungeeMessageEncoder e = (BungeeMessageEncoder) encoder;
        if (BungeePEX.getInstance().getPlugin().isChatApiPresent())
        {
            BaseComponent[] converted = BungeeMessageEncoder.convert(e.create());
            sender.sendMessage(converted);
        }
        else
        {
            sender.sendMessage(e.toString());
        }
    }

    @Override
    public String getName()
    {
        return sender.getName();
    }

    @Override
    public UUID getUUID()
    {
        if (isConsole())
        {
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
        else if (sender instanceof ProxiedPlayer)
        {
            return ((ProxiedPlayer) sender).getUniqueId();
        }
        else
        {
            return null;
//            throw new UnsupportedOperationException("CommandSender derivative " + sender.getClass().getName() + " is unknown!");
        }
    }

    @Override
    public String getServer()
    {
        if (sender instanceof ProxiedPlayer)
        {
            ProxiedPlayer pp = (ProxiedPlayer) sender;
            return pp.getServer() != null ? pp.getServer().getInfo().getName() : null;
        }
        else
        {
            return null;
        }
    }

    @Override
    public String getWorld()
    {
        BungeeEventListener l = (BungeeEventListener) BungeePEX.getInstance().getEventListener();
        return l.getPlayerWorlds().get(sender.getName());
    }

    @Override
    public boolean isConsole()
    {
        return sender.getClass().getName().equals("net.md_5.bungee.command.ConsoleCommandSender");
    }

    @Override
    public boolean isPlayer()
    {
        return sender instanceof ProxiedPlayer;
    }

    @Override
    public boolean isOperator()
    {
        return false;
    }

}
