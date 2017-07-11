package com.xaosia.bungeepex.backends;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.xaosia.bungeepex.PEXConfig;
import com.xaosia.bungeepex.BungeePEX;
import com.xaosia.bungeepex.utils.Config;
import com.xaosia.bungeepex.PermissionGroup;
import com.xaosia.bungeepex.utils.Lang;
import com.xaosia.bungeepex.Server;
import com.xaosia.bungeepex.Statics;
import com.xaosia.bungeepex.PermissionUser;
import com.xaosia.bungeepex.World;
import com.xaosia.bungeepex.platform.PlatformPlugin;

public class YAMLBackEnd implements BackEnd {

    private final String permspath;
    private Config permsconf;
    private final String uuidpath;
    private Config uuidconf;

    private final PlatformPlugin plugin;
    private final PEXConfig config;

    public YAMLBackEnd()
    {
        plugin = BungeePEX.getInstance().getPlugin();
        config = BungeePEX.getInstance().getConfig();

        checkPermFile();

        permspath = "/permissions.yml";
        uuidpath = "/uuidplayerdb.yml";

        permsconf = new Config(plugin, permspath);
        uuidconf = new Config(plugin, uuidpath);
    }

    @Override
    public BackEndType getType()
    {
        return BackEndType.YAML;
    }

    @Override
    public void load()
    {
        //load from table
        permsconf.load();

        //load uuid db
        uuidconf.load();
    }

    @Override
    public List<PermissionGroup> loadGroups()
    {
        List<PermissionGroup> ret = new ArrayList<>();

        List<String> groups = permsconf.getSubNodes("groups");
        for (String g : groups)
        {
            ret.add(loadGroup(g));
        }
        Collections.sort(ret);

        return ret;
    }

    @Override
    public List<PermissionUser> loadUsers()
    {
        List<PermissionUser> ret = new ArrayList<>();

        List<String> users = permsconf.getSubNodes("users");
        for (String u : users)
        {
            PermissionUser user = BungeePEX.getInstance().getConfig().isUseUUIDs() ? loadUser(UUID.fromString(u)) : loadUser(u);
            ret.add(user);
        }

        return ret;
    }

    @Override
    public PermissionGroup loadGroup(String group)
    {
        List<String> inheritances = permsconf.getListString("groups." + group + ".inheritances", new ArrayList<String>());
        List<String> permissions = permsconf.getListString("groups." + group + ".permissions", new ArrayList<String>());
        boolean isdefault = permsconf.getBoolean("groups." + group + ".default", false);
        int rank = permsconf.getInt("groups." + group + ".rank", 1000);
        int weight = permsconf.getInt("groups." + group + ".weight", 1000);
        String ladder = permsconf.getString("groups." + group + ".ladder", "default");
        String display = permsconf.getString("groups." + group + ".display", null);
        String prefix = permsconf.getString("groups." + group + ".prefix", null);
        String suffix = permsconf.getString("groups." + group + ".suffix", null);

        //per server perms
        Map<String, Server> servers = new HashMap<>();
        for (String server : permsconf.getSubNodes("groups." + group + ".servers"))
        {
            List<String> serverperms = permsconf.getListString("groups." + group + ".servers." + server + ".permissions", new ArrayList<String>());
            String sdisplay = permsconf.getString("groups." + group + ".servers." + server + ".display", null);
            String sprefix = permsconf.getString("groups." + group + ".servers." + server + ".prefix", null);
            String ssuffix = permsconf.getString("groups." + group + ".servers." + server + ".suffix", null);

            //per server world perms
            Map<String, World> worlds = new HashMap<>();
            for (String world : permsconf.getSubNodes("groups." + group + ".servers." + server + ".worlds"))
            {
                List<String> worldperms = permsconf.getListString("groups." + group + ".servers." + server + ".worlds." + world + ".permissions", new ArrayList<String>());
                String wdisplay = permsconf.getString("groups." + group + ".servers." + server + ".worlds." + world + ".display", null);
                String wprefix = permsconf.getString("groups." + group + ".servers." + server + ".worlds." + world + ".prefix", null);
                String wsuffix = permsconf.getString("groups." + group + ".servers." + server + ".worlds." + world + ".suffix", null);

                World w = new World(Statics.toLower(world), worldperms, wdisplay, wprefix, wsuffix);
                worlds.put(Statics.toLower(world), w);
            }

            servers.put(Statics.toLower(server), new Server(Statics.toLower(server), serverperms, worlds, sdisplay, sprefix, ssuffix));
        }

        PermissionGroup g = new PermissionGroup(group, inheritances, permissions, servers, rank, weight, ladder, isdefault, display, prefix, suffix);
        return g;
    }

    @Override
    public PermissionUser loadUser(String user)
    {
        if (!permsconf.keyExists("users." + user))
        {
            return null;
        }

        //load user from database
        List<String> sgroups = permsconf.getListString("users." + user + ".groups", new ArrayList<String>());
        List<String> perms = permsconf.getListString("users." + user + ".permissions", new ArrayList<String>());
        String display = permsconf.getString("users." + user + ".display", null);
        String prefix = permsconf.getString("users." + user + ".prefix", null);
        String suffix = permsconf.getString("users." + user + ".suffix", null);

        List<PermissionGroup> lgroups = new ArrayList<>();
        for (String s : sgroups)
        {
            PermissionGroup g = BungeePEX.getInstance().getPermissionsManager().getGroup(s);
            if (g != null)
            {
                lgroups.add(g);
            }
        }

        //per server perms
        Map<String, Server> servers = new HashMap<>();
        for (String server : permsconf.getSubNodes("users." + user + ".servers"))
        {
            List<String> serverperms = permsconf.getListString("users." + user + ".servers." + server + ".permissions", new ArrayList<String>());
            String sdisplay = permsconf.getString("users." + user + ".servers." + server + ".display", null);
            String sprefix = permsconf.getString("users." + user + ".servers." + server + ".prefix", null);
            String ssuffix = permsconf.getString("users." + user + ".servers." + server + ".suffix", null);

            //per server world perms
            Map<String, World> worlds = new HashMap<>();
            for (String world : permsconf.getSubNodes("users." + user + ".servers." + server + ".worlds"))
            {
                List<String> worldperms = permsconf.getListString("users." + user + ".servers." + server + ".worlds." + world + ".permissions", new ArrayList<String>());
                String wdisplay = permsconf.getString("users." + user + ".servers." + server + ".worlds." + world + ".display", null);
                String wprefix = permsconf.getString("users." + user + ".servers." + server + ".worlds." + world + ".prefix", null);
                String wsuffix = permsconf.getString("users." + user + ".servers." + server + ".worlds." + world + ".suffix", null);

                World w = new World(Statics.toLower(world), worldperms, wdisplay, wprefix, wsuffix);
                worlds.put(Statics.toLower(world), w);
            }

            servers.put(Statics.toLower(server), new Server(Statics.toLower(server), serverperms, worlds, sdisplay, sprefix, ssuffix));
        }

        UUID uuid = this.getUUID(user);
        PermissionUser u = new PermissionUser(user, uuid, lgroups, perms, servers, display, prefix, suffix);
        return u;
    }

    @Override
    public PermissionUser loadUser(UUID user)
    {
        if (!permsconf.keyExists("users." + user))
        {
            return null;
        }

        //load user from database
        List<String> sgroups = permsconf.getListString("users." + user + ".groups", new ArrayList<String>());
        List<String> perms = permsconf.getListString("users." + user + ".permissions", new ArrayList<String>());
        String display = permsconf.getString("users." + user + ".display", null);
        String prefix = permsconf.getString("users." + user + ".prefix", null);
        String suffix = permsconf.getString("users." + user + ".suffix", null);

        List<PermissionGroup> lgroups = new ArrayList<>();
        for (String s : sgroups)
        {
            PermissionGroup g = BungeePEX.getInstance().getPermissionsManager().getGroup(s);
            if (g != null)
            {
                lgroups.add(g);
            }
        }

        //per server perms
        Map<String, Server> servers = new HashMap<>();
        for (String server : permsconf.getSubNodes("users." + user + ".servers"))
        {
            List<String> serverperms = permsconf.getListString("users." + user + ".servers." + server + ".permissions", new ArrayList<String>());
            String sdisplay = permsconf.getString("users." + user + ".servers." + server + ".display", null);
            String sprefix = permsconf.getString("users." + user + ".servers." + server + ".prefix", null);
            String ssuffix = permsconf.getString("users." + user + ".servers." + server + ".suffix", null);

            //per server world perms
            Map<String, World> worlds = new HashMap<>();
            for (String world : permsconf.getSubNodes("users." + user + ".servers." + server + ".worlds"))
            {
                List<String> worldperms = permsconf.getListString("users." + user + ".servers." + server + ".worlds." + world + ".permissions", new ArrayList<String>());
                String wdisplay = permsconf.getString("users." + user + ".servers." + server + ".worlds." + world + ".display", null);
                String wprefix = permsconf.getString("users." + user + ".servers." + server + ".worlds." + world + ".prefix", null);
                String wsuffix = permsconf.getString("users." + user + ".servers." + server + ".worlds." + world + ".suffix", null);

                World w = new World(Statics.toLower(world), worldperms, wdisplay, wprefix, wsuffix);
                worlds.put(Statics.toLower(world), w);
            }

            servers.put(Statics.toLower(server), new Server(Statics.toLower(server), serverperms, worlds, sdisplay, sprefix, ssuffix));
        }

        String username = this.getPlayerName(user);
        PermissionUser u = new PermissionUser(username, user, lgroups, perms, servers, display, prefix, suffix);
        return u;
    }

    @Override
    public int loadVersion()
    {
        return permsconf.getInt("version", 1);
    }

    @Override
    public void saveVersion(int version, boolean savetodisk)
    {
        permsconf.setInt("version", version);

        if (savetodisk)
        {
            permsconf.save();
        }
    }

    @Override
    public boolean isUserInDatabase(PermissionUser user)
    {
        return permsconf.keyExists("users." + (BungeePEX.getInstance().getConfig().isUseUUIDs() ? user.getUUID().toString() : user.getName()));
    }

    private void checkPermFile()
    {
        File f = new File(plugin.getPluginFolder(), permspath);
        if (!f.isFile())
        {
            BungeePEX.getLogger().info(Lang.translate(Lang.MessageType.NO_PERM_FILE));
        }
    }

    @Override
    public List<String> getRegisteredUsers()
    {
        return permsconf.getSubNodes("users");
    }

    @Override
    public List<String> getGroupUsers(PermissionGroup group)
    {
        List<String> users = new ArrayList<>();

        for (String user : permsconf.getSubNodes("users"))
        {
            if (permsconf.getListString("users." + user + ".groups", new ArrayList<String>()).contains(group.getName()))
            {
                users.add(user);
            }
        }

        return users;
    }

    @Override
    public synchronized void saveUser(PermissionUser user, boolean savetodisk)
    {
        if (BungeePEX.getInstance().getConfig().isSaveAllUsers() ? true : !user.isNothingSpecial())
        {
            List<String> groups = new ArrayList<>();
            for (PermissionGroup g : user.getGroups())
            {
                groups.add(g.getName());
            }

            String uname = BungeePEX.getInstance().getConfig().isUseUUIDs() ? user.getUUID().toString() : user.getName();

            permsconf.setListString("users." + uname + ".groups", groups);
            permsconf.setListString("users." + uname + ".permissions", user.getExtraPerms());

            for (Map.Entry<String, Server> se : user.getServers().entrySet())
            {
                permsconf.setListString("users." + uname + ".servers." + se.getKey() + ".permissions", se.getValue().getPerms());
                permsconf.setString("users." + uname + ".servers." + se.getKey() + ".display", se.getValue().getDisplay());
                permsconf.setString("users." + uname + ".servers." + se.getKey() + ".prefix", se.getValue().getPrefix());
                permsconf.setString("users." + uname + ".servers." + se.getKey() + ".suffix", se.getValue().getSuffix());

                for (Map.Entry<String, World> we : se.getValue().getWorlds().entrySet())
                {
                    permsconf.setListString("users." + uname + ".servers." + se.getKey() + ".worlds." + we.getKey() + ".permissions", we.getValue().getPerms());
                    permsconf.setString("users." + uname + ".servers." + se.getKey() + ".worlds." + we.getKey() + ".display", we.getValue().getDisplay());
                    permsconf.setString("users." + uname + ".servers." + se.getKey() + ".worlds." + we.getKey() + ".prefix", we.getValue().getPrefix());
                    permsconf.setString("users." + uname + ".servers." + se.getKey() + ".worlds." + we.getKey() + ".suffix", we.getValue().getSuffix());
                }
            }
        }
    }

    @Override
    public synchronized void saveGroup(PermissionGroup group, boolean savetodisk)
    {
        permsconf.setListString("groups." + group.getName() + ".inheritances", group.getInheritances());
        permsconf.setListString("groups." + group.getName() + ".permissions", group.getPerms());
        permsconf.setInt("groups." + group.getName() + ".rank", group.getRank());
        permsconf.setString("groups." + group.getName() + ".ladder", group.getLadder());
        permsconf.setBool("groups." + group.getName() + ".default", group.isDefault());
        permsconf.setString("groups." + group.getName() + ".display", group.getDisplay());
        permsconf.setString("groups." + group.getName() + ".prefix", group.getPrefix());
        permsconf.setString("groups." + group.getName() + ".suffix", group.getSuffix());

        for (Map.Entry<String, Server> se : group.getServers().entrySet())
        {
            permsconf.setListString("groups." + group.getName() + ".servers." + se.getKey() + ".permissions", se.getValue().getPerms());
            permsconf.setString("groups." + group.getName() + ".servers." + se.getKey() + ".display", se.getValue().getDisplay());
            permsconf.setString("groups." + group.getName() + ".servers." + se.getKey() + ".prefix", se.getValue().getPrefix());
            permsconf.setString("groups." + group.getName() + ".servers." + se.getKey() + ".suffix", se.getValue().getSuffix());

            for (Map.Entry<String, World> we : se.getValue().getWorlds().entrySet())
            {
                permsconf.setListString("groups." + group.getName() + ".servers." + se.getKey() + ".worlds." + we.getKey() + ".permissions", we.getValue().getPerms());
                permsconf.setString("groups." + group.getName() + ".servers." + se.getKey() + ".worlds." + we.getKey() + ".display", we.getValue().getDisplay());
                permsconf.setString("groups." + group.getName() + ".servers." + se.getKey() + ".worlds." + we.getKey() + ".prefix", we.getValue().getPrefix());
                permsconf.setString("groups." + group.getName() + ".servers." + se.getKey() + ".worlds." + we.getKey() + ".suffix", we.getValue().getSuffix());
            }
        }

        if (savetodisk)
        {
            permsconf.save();
        }
    }

    @Override
    public synchronized void deleteUser(PermissionUser user)
    {
        permsconf.deleteNode("users." + (BungeePEX.getInstance().getConfig().isUseUUIDs() ? user.getUUID().toString() : user.getName()));
    }

    @Override
    public synchronized void deleteGroup(PermissionGroup group)
    {
        permsconf.deleteNode("groups." + group.getName());
    }

    @Override
    public synchronized void saveUserGroups(PermissionUser user)
    {
        List<String> savegroups = new ArrayList<>();
        for (PermissionGroup g : user.getGroups())
        {
            savegroups.add(g.getName());
        }

        permsconf.setListStringAndSave("users." + (BungeePEX.getInstance().getConfig().isUseUUIDs() ? user.getUUID().toString() : user.getName()) + ".groups", savegroups);
    }

    @Override
    public synchronized void saveUserPerms(PermissionUser user)
    {
        permsconf.setListStringAndSave("users." + (BungeePEX.getInstance().getConfig().isUseUUIDs() ? user.getUUID().toString() : user.getName()) + ".permissions", user.getExtraPerms());
    }

    @Override
    public synchronized void saveUserPerServerPerms(PermissionUser user, String server)
    {
        server = Statics.toLower(server);

        permsconf.setListStringAndSave("users." + (BungeePEX.getInstance().getConfig().isUseUUIDs() ? user.getUUID().toString() : user.getName()) + ".servers." + server + ".permissions", user.getServer(server).getPerms());
    }

    @Override
    public synchronized void saveUserPerServerWorldPerms(PermissionUser user, String server, String world)
    {
        server = Statics.toLower(server);
        world = Statics.toLower(world);

        permsconf.setListStringAndSave("users." + (BungeePEX.getInstance().getConfig().isUseUUIDs() ? user.getUUID().toString() : user.getName()) + ".servers." + server + ".worlds." + world + ".permissions", user.getServer(server).getWorld(world).getPerms());
    }

    @Override
    public synchronized void saveUserDisplay(PermissionUser user, String server, String world)
    {
        server = Statics.toLower(server);
        world = Statics.toLower(world);

        String display = user.getDisplay();
        if (server != null)
        {
            display = user.getServer(server).getDisplay();
            if (world != null)
            {
                display = user.getServer(server).getWorld(world).getDisplay();
            }
        }
        permsconf.setStringAndSave("users." + (BungeePEX.getInstance().getConfig().isUseUUIDs() ? user.getUUID().toString() : user.getName()) + (server != null ? ".servers." + server + (world != null ? ".worlds." + world : "") : "") + ".display", display);
    }

    @Override
    public synchronized void saveUserPrefix(PermissionUser user, String server, String world)
    {
        server = Statics.toLower(server);
        world = Statics.toLower(world);

        String prefix = user.getPrefix();
        if (server != null)
        {
            prefix = user.getServer(server).getPrefix();
            if (world != null)
            {
                prefix = user.getServer(server).getWorld(world).getPrefix();
            }
        }
        permsconf.setStringAndSave("users." + (BungeePEX.getInstance().getConfig().isUseUUIDs() ? user.getUUID().toString() : user.getName()) + (server != null ? ".servers." + server + (world != null ? ".worlds." + world : "") : "") + ".prefix", prefix);
    }

    @Override
    public synchronized void saveUserSuffix(PermissionUser user, String server, String world)
    {
        server = Statics.toLower(server);
        world = Statics.toLower(world);

        String suffix = user.getSuffix();
        if (server != null)
        {
            suffix = user.getServer(server).getSuffix();
            if (world != null)
            {
                suffix = user.getServer(server).getWorld(world).getSuffix();
            }
        }
        permsconf.setStringAndSave("users." + (BungeePEX.getInstance().getConfig().isUseUUIDs() ? user.getUUID().toString() : user.getName()) + (server != null ? ".servers." + server + (world != null ? ".worlds." + world : "") : "") + ".suffix", suffix);
    }

    @Override
    public synchronized void saveGroupPerms(PermissionGroup group)
    {
        permsconf.setListStringAndSave("groups." + group.getName() + ".permissions", group.getPerms());
    }

    @Override
    public synchronized void saveGroupPerServerPerms(PermissionGroup group, String server)
    {
        server = Statics.toLower(server);

        permsconf.setListStringAndSave("groups." + group.getName() + ".servers." + server + ".permissions", group.getServer(server).getPerms());
    }

    @Override
    public synchronized void saveGroupPerServerWorldPerms(PermissionGroup group, String server, String world)
    {
        server = Statics.toLower(server);
        world = Statics.toLower(world);

        permsconf.setListStringAndSave("groups." + group.getName() + ".servers." + server + ".worlds." + world + ".permissions", group.getServer(server).getWorld(world).getPerms());
    }

    @Override
    public synchronized void saveGroupInheritances(PermissionGroup group)
    {
        permsconf.setListStringAndSave("groups." + group.getName() + ".inheritances", group.getInheritances());
    }

    @Override
    public synchronized void saveGroupLadder(PermissionGroup group)
    {
        permsconf.setStringAndSave("groups." + group.getName() + ".ladder", group.getLadder());
    }

    @Override
    public synchronized void saveGroupRank(PermissionGroup group)
    {
        permsconf.setIntAndSave("groups." + group.getName() + ".rank", group.getRank());
    }

    @Override
    public synchronized void saveGroupWeight(PermissionGroup group)
    {
        permsconf.setIntAndSave("groups." + group.getName() + ".weight", group.getWeight());
    }

    @Override
    public synchronized void saveGroupDefault(PermissionGroup group)
    {
        permsconf.setBoolAndSave("groups." + group.getName() + ".default", group.isDefault());
    }

    @Override
    public synchronized void saveGroupDisplay(PermissionGroup group, String server, String world)
    {
        server = Statics.toLower(server);
        world = Statics.toLower(world);

        String display = group.getDisplay();
        if (server != null)
        {
            display = group.getServer(server).getDisplay();
            if (world != null)
            {
                display = group.getServer(server).getWorld(world).getDisplay();
            }
        }
        permsconf.setStringAndSave("groups." + group.getName() + (server != null ? ".servers." + server + (world != null ? ".worlds." + world : "") : "") + ".display", display);
    }

    @Override
    public synchronized void saveGroupPrefix(PermissionGroup group, String server, String world)
    {
        server = Statics.toLower(server);
        world = Statics.toLower(world);

        String prefix = group.getPrefix();
        if (server != null)
        {
            prefix = group.getServer(server).getPrefix();
            if (world != null)
            {
                prefix = group.getServer(server).getWorld(world).getPrefix();
            }
        }
        permsconf.setStringAndSave("groups." + group.getName() + (server != null ? ".servers." + server + (world != null ? ".worlds." + world : "") : "") + ".prefix", prefix);
    }

    @Override
    public synchronized void saveGroupSuffix(PermissionGroup group, String server, String world)
    {
        server = Statics.toLower(server);
        world = Statics.toLower(world);

        String suffix = group.getSuffix();
        if (server != null)
        {
            suffix = group.getServer(server).getSuffix();
            if (world != null)
            {
                suffix = group.getServer(server).getWorld(world).getSuffix();
            }
        }
        permsconf.setStringAndSave("groups." + group.getName() + (server != null ? ".servers." + server + (world != null ? ".worlds." + world : "") : "") + ".suffix", suffix);
    }

    @Override
    public synchronized void format(List<PermissionGroup> groups, List<PermissionUser> users, int version)
    {
        clearDatabase();
        for (int i = 0; i < groups.size(); i++)
        {
            saveGroup(groups.get(i), false);
        }
        for (int i = 0; i < users.size(); i++)
        {
            saveUser(users.get(i), false);
        }
        saveVersion(version, false);

        permsconf.save();
    }

    @Override
    public synchronized int cleanup(List<PermissionGroup> groups, List<PermissionUser> users, int version)
    {
        int deleted = 0;

        clearDatabase();
        for (int i = 0; i < groups.size(); i++)
        {
            saveGroup(groups.get(i), false);
        }
        for (int i = 0; i < users.size(); i++)
        {
            PermissionUser u = users.get(i);
            if (BungeePEX.getInstance().getConfig().isDeleteUsersOnCleanup())
            {
                //check for additional permissions and non-default groups AND onlinecheck
                if (u.isNothingSpecial()
                        && BungeePEX.getInstance().getPlugin().getPlayer(u.getName()) == null
                        && BungeePEX.getInstance().getPlugin().getPlayer(u.getUUID()) == null)
                {
                    deleted++;
                    continue;
                }
            }

            //player has to be saved
            saveUser(users.get(i), false);
        }
        saveVersion(version, false);

        permsconf.save();

        return deleted;
    }

    @Override
    public void clearDatabase()
    {
        new File(BungeePEX.getInstance().getPlugin().getPluginFolder() + permspath).delete();
        permsconf = new Config(BungeePEX.getInstance().getPlugin(), permspath);
        load();
    }

    @Override
    public void reloadGroup(PermissionGroup group)
    {
        permsconf.load();

        //load group from database
        List<String> inheritances = permsconf.getListString("groups." + group.getName() + ".inheritances", new ArrayList<String>());
        List<String> permissions = permsconf.getListString("groups." + group.getName() + ".permissions", new ArrayList<String>());
        boolean isdefault = permsconf.getBoolean("groups." + group.getName() + ".default", false);
        int rank = permsconf.getInt("groups." + group.getName() + ".rank", 1000);
        int weight = permsconf.getInt("groups." + group.getName() + ".weight", 1000);
        String ladder = permsconf.getString("groups." + group.getName() + ".ladder", "default");
        String display = permsconf.getString("groups." + group.getName() + ".display", null);
        String prefix = permsconf.getString("groups." + group.getName() + ".prefix", null);
        String suffix = permsconf.getString("groups." + group.getName() + ".suffix", null);

        //per server perms
        Map<String, Server> servers = new HashMap<>();
        for (String server : permsconf.getSubNodes("groups." + group.getName() + ".servers"))
        {
            List<String> serverperms = permsconf.getListString("groups." + group.getName() + ".servers." + server + ".permissions", new ArrayList<String>());
            String sdisplay = permsconf.getString("groups." + group.getName() + ".servers." + server + ".display", null);
            String sprefix = permsconf.getString("groups." + group.getName() + ".servers." + server + ".prefix", null);
            String ssuffix = permsconf.getString("groups." + group.getName() + ".servers." + server + ".suffix", null);

            //per server world perms
            Map<String, World> worlds = new HashMap<>();
            for (String world : permsconf.getSubNodes("groups." + group.getName() + ".servers." + server + ".worlds"))
            {
                List<String> worldperms = permsconf.getListString("groups." + group.getName() + ".servers." + server + ".worlds." + world + ".permissions", new ArrayList<String>());
                String wdisplay = permsconf.getString("groups." + group.getName() + ".servers." + server + ".worlds." + world + ".display", null);
                String wprefix = permsconf.getString("groups." + group.getName() + ".servers." + server + ".worlds." + world + ".prefix", null);
                String wsuffix = permsconf.getString("groups." + group.getName() + ".servers." + server + ".worlds." + world + ".suffix", null);

                World w = new World(Statics.toLower(world), worldperms, wdisplay, wprefix, wsuffix);
                worlds.put(Statics.toLower(world), w);
            }

            servers.put(Statics.toLower(server), new Server(Statics.toLower(server), serverperms, worlds, sdisplay, sprefix, ssuffix));
        }

        group.setInheritances(inheritances);
        group.setPerms(permissions);
        group.setIsdefault(isdefault);
        group.setRank(rank);
        group.setWeight(weight);
        group.setLadder(ladder);
        group.setDisplay(display);
        group.setPrefix(prefix);
        group.setSuffix(suffix);
        group.setServers(servers);
    }

    @Override
    public void reloadUser(PermissionUser user)
    {
        permsconf.load();

        String uname = config.isUseUUIDs() ? user.getUUID().toString() : user.getName();

        //load user from database
        List<String> sgroups = permsconf.getListString("users." + uname + ".groups", new ArrayList<String>());
        List<String> perms = permsconf.getListString("users." + uname + ".permissions", new ArrayList<String>());
        String display = permsconf.getString("users." + uname + ".display", null);
        String prefix = permsconf.getString("users." + uname + ".prefix", null);
        String suffix = permsconf.getString("users." + uname + ".suffix", null);

        List<PermissionGroup> lgroups = new ArrayList<>();
        for (String s : sgroups)
        {
            PermissionGroup g = BungeePEX.getInstance().getPermissionsManager().getGroup(s);
            if (g != null)
            {
                lgroups.add(g);
            }
        }

        //per server perms
        Map<String, Server> servers = new HashMap<>();
        for (String server : permsconf.getSubNodes("users." + uname + ".servers"))
        {
            List<String> serverperms = permsconf.getListString("users." + uname + ".servers." + server + ".permissions", new ArrayList<String>());
            String sdisplay = permsconf.getString("users." + uname + ".servers." + server + ".display", null);
            String sprefix = permsconf.getString("users." + uname + ".servers." + server + ".prefix", null);
            String ssuffix = permsconf.getString("users." + uname + ".servers." + server + ".suffix", null);

            //per server world perms
            Map<String, World> worlds = new HashMap<>();
            for (String world : permsconf.getSubNodes("users." + uname + ".servers." + server + ".worlds"))
            {
                List<String> worldperms = permsconf.getListString("users." + uname + ".servers." + server + ".worlds." + world + ".permissions", new ArrayList<String>());
                String wdisplay = permsconf.getString("users." + uname + ".servers." + server + ".worlds." + world + ".display", null);
                String wprefix = permsconf.getString("users." + uname + ".servers." + server + ".worlds." + world + ".prefix", null);
                String wsuffix = permsconf.getString("users." + uname + ".servers." + server + ".worlds." + world + ".suffix", null);

                World w = new World(Statics.toLower(world), worldperms, wdisplay, wprefix, wsuffix);
                worlds.put(Statics.toLower(world), w);
            }

            servers.put(Statics.toLower(server), new Server(Statics.toLower(server), serverperms, worlds, sdisplay, sprefix, ssuffix));
        }

        user.setGroups(lgroups);
        user.setExtraPerms(perms);
        user.setDisplay(display);
        user.setPrefix(prefix);
        user.setSuffix(suffix);
        user.setServers(servers);
    }

    @Override
    public UUID getUUID(String player)
    {
        UUID ret = null;

        for (String uuid : uuidconf.getSubNodes(""))
        {
            String p = uuidconf.getString(uuid, "");
            if (p.equalsIgnoreCase(player))
            {
                ret = UUID.fromString(uuid);
            }
        }

        return ret;
    }

    @Override
    public String getPlayerName(UUID uuid)
    {
        String ret = null;

        for (String suuid : uuidconf.getSubNodes(""))
        {
            if (suuid.equalsIgnoreCase(uuid.toString()))
            {
                ret = uuidconf.getString(suuid, "");
            }
        }

        return ret;
    }

    @Override
    public void update(UUID uuid, String player)
    {
        for (String suuid : uuidconf.getSubNodes(""))
        {
            if (suuid.equalsIgnoreCase(uuid.toString()) || uuidconf.getString(suuid, "").equalsIgnoreCase(player))
            {
                uuidconf.deleteNode(suuid);
            }
        }
        uuidconf.setStringAndSave(uuid.toString(), player);
    }

    @Override
    public Map<UUID, String> getAll()
    {
        Map<UUID, String> ret = new HashMap<>();

        for (String suuid : uuidconf.getSubNodes(""))
        {
            ret.put(UUID.fromString(suuid), uuidconf.getString(suuid, ""));
        }

        return ret;
    }

    @Override
    public void clear()
    {
        new File(BungeePEX.getInstance().getPlugin().getPluginFolder(), "/uuidplayerdb.yml").delete();
        uuidconf = new Config(BungeePEX.getInstance().getPlugin(), "/uuidplayerdb.yml");
        uuidconf.load();
    }

}
