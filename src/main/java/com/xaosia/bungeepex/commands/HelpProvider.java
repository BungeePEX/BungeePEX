package com.xaosia.bungeepex.commands;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

import com.xaosia.bungeepex.BungeePEX;
import com.xaosia.bungeepex.utils.ChatColor;
import com.xaosia.bungeepex.utils.Lang;
import com.xaosia.bungeepex.utils.Lang.MessageType;
import com.xaosia.bungeepex.platform.MessageEncoder;
import com.xaosia.bungeepex.platform.MessageEncoder.ClickEvent;
import com.xaosia.bungeepex.platform.MessageEncoder.HoverEvent;
import com.xaosia.bungeepex.platform.PlatformPlugin;
import com.xaosia.bungeepex.platform.Sender;

public class HelpProvider
{

    private static final int PAGE_SIZE = 7;
    private static final List<HelpEntry> HELP_ENTRIES = new ArrayList<>();

    static
    {
        HELP_ENTRIES.add(new HelpEntry(null,/*                                   */ makeClickCommand("/bpex", Lang.translate(MessageType.HELP_WELCOME))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.help",/*                     */ makeSuggestCommand("/bpex help [page]", Lang.translate(MessageType.HELP_HELP))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.reload",/*                   */ makeClickCommand("/bpex reload", Lang.translate(MessageType.HELP_RELOAD))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.debug",/*                    */ makeSuggestCommand("/bpex debug <true|false>", Lang.translate(MessageType.HELP_DEBUG))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.users",/*                    */ makeSuggestCommand("/bpex users [-c]", Lang.translate(MessageType.HELP_USERS))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.user.info",/*                */ makeSuggestCommand("/bpex user <user> info [server [world]]", Lang.translate(MessageType.HELP_USER_INFO))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.user.delete",/*              */ makeSuggestCommand("/bpex user <user> delete", Lang.translate(MessageType.HELP_USER_DELETE))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.user.display",/*             */ makeSuggestCommand("/bpex user <user> display [displayname [server [world]]]", Lang.translate(MessageType.HELP_USER_DISPLAY))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.user.prefix",/*              */ makeSuggestCommand("/bpex user <user> prefix [prefix [server [world]]]", Lang.translate(MessageType.HELP_USER_PREFIX))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.user.suffix",/*              */ makeSuggestCommand("/bpex user <user> suffix [suffix [server [world]]]", Lang.translate(MessageType.HELP_USER_SUFFIX))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.user.perms.add",/*           */ makeSuggestCommand("/bpex user <user> addperm <perm> [server [world]]", Lang.translate(MessageType.HELP_USER_ADDPERM))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.user.perms.remove",/*        */ makeSuggestCommand("/bpex user <user> removeperm <perm> [server [world]]", Lang.translate(MessageType.HELP_USER_REMOVEPERM))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.user.perms.has",/*           */ makeSuggestCommand("/bpex user <user> has <perm> [server [world]]",Lang.translate(MessageType.HELP_USER_HAS))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.user.perms.list",/*          */ makeSuggestCommand("/bpex user <user> list [server [world]]", Lang.translate(MessageType.HELP_USER_LIST))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.user.group.add",/*           */ makeSuggestCommand("/bpex user <user> addgroup <group>",Lang.translate(MessageType.HELP_USER_ADDGROUP))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.user.group.remove",/*        */ makeSuggestCommand("/bpex user <user> removegroup <group>", Lang.translate(MessageType.HELP_USER_REMOVEGROUP))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.user.group.set",/*           */ makeSuggestCommand("/bpex user <user> setgroup <group>", Lang.translate(MessageType.HELP_USER_SETGROUP))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.user.groups",/*              */ makeSuggestCommand("/bpex user <user> groups", Lang.translate(MessageType.HELP_USER_GROUPS))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.groups",/*                   */ makeClickCommand("/bpex groups", Lang.translate(MessageType.HELP_GROUPS))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.group.info",/*               */ makeSuggestCommand("/bpex group <group> info [server [world]]", Lang.translate(MessageType.HELP_GROUP_INFO))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.group.users",/*              */ makeSuggestCommand("/bpex group <group> users [-c]", Lang.translate(MessageType.HELP_GROUP_USERS))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.group.create",/*             */ makeSuggestCommand("/bpex group <group> create", Lang.translate(MessageType.HELP_GROUP_CREATE))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.group.delete",/*             */ makeSuggestCommand("/bpex group <group> delete", Lang.translate(MessageType.HELP_GROUP_DELETE))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.group.inheritances.add",/*   */ makeSuggestCommand("/bpex group <group> addinherit <addgroup>", Lang.translate(MessageType.HELP_GROUP_ADDINHERIT))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.group.inheritances.remove",/**/ makeSuggestCommand("/bpex group <group> removeinherit <removegroup>", Lang.translate(MessageType.HELP_GROUP_REMOVEINHERIT))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.group.rank",/*               */ makeSuggestCommand("/bpex group <group> rank <new rank>", Lang.translate(MessageType.HELP_GROUP_RANK))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.group.weight",/*             */ makeSuggestCommand("/bpex group <group> weight <new weight>",Lang.translate(MessageType.HELP_GROUP_WEIGHT))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.group.ladder",/*             */ makeSuggestCommand("/bpex group <group> ladder <new ladder>", Lang.translate(MessageType.HELP_GROUP_LADDER))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.group.default",/*            */ makeSuggestCommand("/bpex group <group> default <true|false>", Lang.translate(MessageType.HELP_GROUP_DEFAULT))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.group.display",/*            */ makeSuggestCommand("/bpex group <group> display [displayname [server [world]]]",Lang.translate(MessageType.HELP_GROUP_DISPLAY))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.group.prefix",/*             */ makeSuggestCommand("/bpex group <group> prefix [prefix [server [world]]]", Lang.translate(MessageType.HELP_GROUP_PREFIX))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.group.suffix",/*             */ makeSuggestCommand("/bpex group <group> suffix [suffix [server [world]]]", Lang.translate(MessageType.HELP_GROUP_SUFFIX))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.group.perms.add",/*          */ makeSuggestCommand("/bpex group <group> addperm <perm> [server [world]]", Lang.translate(MessageType.HELP_GROUP_ADDPERM))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.group.perms.remove",/*       */ makeSuggestCommand("/bpex group <group> removeperm <perm> [server [world]]", Lang.translate(MessageType.HELP_GROUP_REMOVEPERM))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.group.perms.has",/*          */ makeSuggestCommand("/bpex group <group> has <perm> [server [world]]", Lang.translate(MessageType.HELP_GROUP_HAS))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.group.perms.list",/*         */ makeSuggestCommand("/bpex group <group> list",Lang.translate(MessageType.HELP_GROUP_LIST))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.promote",/*                  */ makeSuggestCommand("/bpex promote <user> [ladder]",Lang.translate(MessageType.HELP_PROMOTE))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.demote",/*                   */ makeSuggestCommand("/bpex demote <user> [ladder]", Lang.translate(MessageType.HELP_DEMOTE))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.format",/*                   */ makeClickCommand("/bpex format", Lang.translate(MessageType.HELP_FORMAT))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.cleanup",/*                  */ makeClickCommand("/bpex cleanup",Lang.translate(MessageType.HELP_CLEANUP))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.migrate",/*                  */ makeSuggestCommand("/bpex migrate <backend> [yaml|mysql|mysql2]", Lang.translate(MessageType.HELP_MIGRATE_BACKEND))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.migrate",/*                  */ makeSuggestCommand("/bpex migrate <useuuid> [true|false]", Lang.translate(MessageType.HELP_MIGRATE_USEUUID))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.migrate",/*                  */ makeSuggestCommand("/bpex migrate <uuidplayerdb> [None|YAML|MySQL]",Lang.translate(MessageType.HELP_MIGRATE_UUIDPLAYERDB))));
        HELP_ENTRIES.add(new HelpEntry("bungeepex.uuid",/*                     */ makeSuggestCommand("/bpex uuid <player|uuid> [-rm]", Lang.translate(MessageType.HELP_UUID))));
// template        helpentries.add(new HelpEntry(null, makeClickCommand("/bp help", "Shows").color(ChatColor.GRAY)));
    }

    private static MessageEncoder makeClickCommand(String cmd, String help)
    {
        return enc()
                //cmd
                .append(cmd)
                .color(ChatColor.GOLD)
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, enc().append("Click to execute")))
                //reset
                .append("")
                .reset()
                //seperator
                .append(" - ")
                .color(ChatColor.WHITE)
                //help
                .append(help)
                .color(ChatColor.GRAY);
    }

    private static MessageEncoder makeSuggestCommand(String cmd, String help)
    {
        return enc()
                //cmd
                .append(cmd)
                .color(ChatColor.GOLD)
                .event(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, cmd))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, enc().append("Click to suggest")))
                //reset
                .append("")
                .reset()
                //seperator
                .append(" - ")
                .color(ChatColor.WHITE)
                //help
                .append(help)
                .color(ChatColor.GRAY);
    }

    private static PlatformPlugin plugin()
    {
        return BungeePEX.getInstance().getPlugin();
    }

    private static MessageEncoder enc()
    {
        return plugin().newMessageEncoder();
    }

    public static void sendHelpHeader(Sender sender, int page)
    {
        sender.sendMessage(enc().append("                  ------ BungeePerms - Help - Page " + (page + 1) + " -----").color(ChatColor.GOLD));
        sender.sendMessage(enc().append("Aliases: ").color(ChatColor.GRAY).append("/bp").color(ChatColor.GOLD)
                .append("       ").color(ChatColor.GRAY).append("<required>").color(ChatColor.GOLD)
                .append("       ").color(ChatColor.GRAY).append("[optional]").color(ChatColor.GOLD));
    }

    public static void sendHelpPage(Sender sender, int page)
    {
        sendHelpHeader(sender, page);

        int index = -1;
        for (HelpEntry he : HELP_ENTRIES)
        {
            if (he.getPermission() != null && !BungeePEX.getInstance().getPermissionsChecker().hasPermOrConsole(sender, he.getPermission()))
            {
                continue;
            }

            index++;
            if (index < page * PAGE_SIZE)
            {
                continue;
            }
            else if (index < (page + 1) * PAGE_SIZE)
            {
                sender.sendMessage(he.getMessage());
            }
            else
            {
                break;
            }
        }
    }

    @Getter
    @AllArgsConstructor
    private static class HelpEntry
    {

        private final String permission;
        private final MessageEncoder message;
    }
}
