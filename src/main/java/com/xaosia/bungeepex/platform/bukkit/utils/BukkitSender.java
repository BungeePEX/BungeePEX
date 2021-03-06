package com.xaosia.bungeepex.platform.bukkit.utils;

import java.util.UUID;

import com.xaosia.bungeepex.platform.bukkit.BukkitConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;
import com.xaosia.bungeepex.BungeePEX;
import com.xaosia.bungeepex.platform.MessageEncoder;
import com.xaosia.bungeepex.platform.Sender;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

@Getter
@AllArgsConstructor
public class BukkitSender implements Sender
{

    private final CommandSender sender;

    @Override
    public void sendMessage(String message)
    {
        sender.sendMessage(message);
    }

    @Override
    public void sendMessage(MessageEncoder encoder)
    {
        BukkitMessageEncoder e = (BukkitMessageEncoder) encoder;
        if (isPlayer() && BungeePEX.getInstance().getPlugin().isChatApiPresent())
        {
            BaseComponent[] converted = BukkitMessageEncoder.convert(e.create());
            ((Player) sender).spigot().sendMessage(converted);
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
        if (!BungeePEX.getInstance().getConfig().isUseUUIDs())
        {
            throw new IllegalStateException("useuuid not enabled but uuid functionality called");
        }
        if (sender instanceof ConsoleCommandSender)
        {
            return UUID.fromString("00000000-0000-0000-0000-000000000000");
        }
        else if (sender instanceof Player)
        {
            return ((Player) sender).getUniqueId();
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
        if (sender instanceof Player)
        {
            return ((BukkitConfig) BungeePEX.getInstance().getConfig()).getServername();
        }
        else
        {
            return null;
        }
    }

    @Override
    public String getWorld()
    {
        if (sender instanceof Player)
        {
            World w = ((Player) sender).getWorld();
            return w != null ? w.getName() : null;
        }
        else
        {
            return null;
        }
    }

    @Override
    public boolean isConsole()
    {
        return sender instanceof ConsoleCommandSender;
    }

    @Override
    public boolean isPlayer()
    {
        return sender instanceof Player;
    }

    @Override
    public boolean isOperator()
    {
        return isPlayer() && ((Player) sender).isOp();
    }

}
