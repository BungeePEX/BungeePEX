package com.xaosia.bungeepex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.xaosia.bungeepex.backends.migrate.Migrate2MySQL;
import com.xaosia.bungeepex.backends.migrate.Migrate2YAML;
import com.xaosia.bungeepex.backends.migrate.Migrator;
import com.xaosia.bungeepex.utils.Lang;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import com.xaosia.bungeepex.utils.Debug;
import com.xaosia.bungeepex.utils.Lang.MessageType;
import com.xaosia.bungeepex.backends.BackEnd;
import com.xaosia.bungeepex.backends.BackEndType;
import com.xaosia.bungeepex.backends.MySQLBackEnd;
import com.xaosia.bungeepex.backends.YAMLBackEnd;
import com.xaosia.bungeepex.platform.PlatformPlugin;
import com.xaosia.bungeepex.platform.Sender;
import com.xaosia.bungeepex.utils.ConcurrentList;

public class PermissionsManager {

    private final PlatformPlugin plugin;
    private final PEXConfig config;
    private final Debug debug;
    private boolean enabled = false;

    @Getter
    @Setter
    private BackEnd backEnd;

    private List<PermissionGroup> groups;
    private List<PermissionUser> users;
    private int permsversion;

    private final ReadWriteLock grouplock = new ReentrantReadWriteLock();
    @Getter
    private final ReadWriteLock userlock = new ReentrantReadWriteLock();

    public PermissionsManager(PlatformPlugin plugin, PEXConfig config, Debug debug)
    {
        this.plugin = plugin;
        this.config = config;
        this.debug = debug;

        //config
        loadConfig();

        //perms
        loadPerms();
    }

    /**
     * Loads the configuration of the plugin from the config.yml file.
     */
    public final void loadConfig() {
        config.load();
        BackEndType bet = config.getBackEndType();
        switch (bet) {
            case YAML:
                backEnd = new YAMLBackEnd();
                break;
            case MySQL:
                backEnd = new MySQLBackEnd(config);
                break;
            default:
                break;
        }
    }


    /**
     * (Re)loads the all groups and online players from file/table.
     */
    public final void loadPerms()
    {
        BungeePEX.getLogger().info(Lang.translate(MessageType.PERMISSIONS_LOADING));

        //load database
        backEnd.load();

        grouplock.writeLock().lock();
        try
        {
            groups = backEnd.loadGroups();
        }
        finally
        {
            grouplock.writeLock().unlock();
        }

        userlock.writeLock().lock();
        try
        {
            users = new ConcurrentList<>();
        }
        finally
        {
            userlock.writeLock().unlock();
        }

        //load permsversion
        permsversion = backEnd.loadVersion();

        BungeePEX.getLogger().info(Lang.translate(MessageType.PERMISSIONS_LOADED));
    }

    /**
     * Enables the permissions manager.
     */
    public void enable()
    {
        if (enabled)
        {
            return;
        }

        //load online players; allows reload
        for (Sender s : BungeePEX.getInstance().getPlugin().getPlayers())
        {
            PermissionUser user;
            if (config.isUseUUIDs())
            {
                user = getUser(s.getUUID());
                if (user == null)
                {
                    createTempUser(s.getName(), s.getUUID());
                }
            }
            else
            {
                user = getUser(s.getName());
                if (user == null)
                {
                    createTempUser(s.getName(), null);
                }
            }

            //call event
            BungeePEX.getInstance().getEventDispatcher().dispatchUserChangeEvent(user);
        }

        enabled = true;
    }

    /**
     * Disables the permissions manager.
     */
    public void disable()
    {
        if (!enabled)
        {
            return;
        }
        userlock.writeLock().lock();
        try
        {
            users.clear();
        }
        finally
        {
            userlock.writeLock().unlock();
        }
        enabled = false;
    }

    /**
     * Reloads the config and permissions.
     */
    public void reload()
    {
        disable();

        //config
        loadConfig();

        //perms
        loadPerms();

        enable();

        //call event
        BungeePEX.getInstance().getEventDispatcher().dispatchReloadedEvent();
    }

    /**
     * Validates all loaded groups and users and fixes invalid objects.
     */
    public synchronized void validateUsersGroups()
    {
        grouplock.readLock().lock();
        try
        {
            for (PermissionGroup group : groups)
            {
                List<String> inheritances = group.getInheritances();
                for (int j = 0; j < inheritances.size(); j++)
                {
                    if (getGroup(inheritances.get(j)) == null)
                    {
                        inheritances.remove(j);
                        j--;
                    }
                }
                backEnd.saveGroupInheritances(group);
            }

            //perms recalc and bukkit perms update
            //do this in 2 seperate loops to keep validation clean
            for (PermissionGroup g : groups)
            {
                g.recalcPerms();

                //send bukkit update info
                BungeePEX.getInstance().getNetworkNotifier().reloadGroup(g, null);
            }
        }
        finally
        {
            grouplock.readLock().unlock();
        }

        userlock.readLock().lock();
        try
        {
            for (PermissionUser u : users)
            {
                for (int j = 0; j < u.getGroups().size(); j++)
                {
                    if (getGroup(u.getGroups().get(j).getName()) == null)
                    {
                        u.getGroups().remove(j);
                        j--;
                    }
                }
                backEnd.saveUserGroups(u);
            }

            //perms recalc and bukkit perms update
            //do this in 2 seperate loops to keep validation clean
            for (PermissionUser u : users)
            {
                u.recalcPerms();

                //send bukkit update info
                BungeePEX.getInstance().getNetworkNotifier().reloadUser(u, null);
            }
        }
        finally
        {
            userlock.readLock().unlock();
        }

        //user groups check - backEnd
        List<PermissionUser> backendusers = backEnd.loadUsers();
        for (PermissionUser u : backendusers)
        {
            for (int j = 0; j < u.getGroups().size(); j++)
            {
                if (getGroup(u.getGroups().get(j).getName()) == null)
                {
                    u.getGroups().remove(j);
                    j--;
                }
            }
            backEnd.saveUserGroups(u);
        }
    }

    /**
     * Get the group of the player with the highesst rank. Do not to be confused with the rank property. The higher the rank the smaller the rank property. (1 is highest rank; 1000 is a low rank)
     *
     * @param player the user to get the main group of
     * @return the main group of the user (highest rank)
     * @throws NullPointerException if player is null
     */
    public synchronized PermissionGroup getMainGroup(PermissionUser player)
    {
        if (player == null)
        {
            throw new NullPointerException("player is null");
        }
        if (player.getGroups().isEmpty())
        {
            return null;
        }
        PermissionGroup ret = player.getGroups().get(0);
        for (int i = 1; i < player.getGroups().size(); i++)
        {
            if (player.getGroups().get(i).getWeight() < ret.getWeight())
            {
                ret = player.getGroups().get(i);
            }
        }
        return ret;
    }

    /**
     * Gets the next (higher) group in the same ladder.
     *
     * @param group the group to get the next group of
     * @return the next group in the same ladder or null if the group has no next group
     * @throws IllegalArgumentException if the group ladder does not exist (anymore)
     */
    public synchronized PermissionGroup getNextGroup(PermissionGroup group)
    {
        List<PermissionGroup> laddergroups = getLadderGroups(group.getLadder());

        for (int i = 0; i < laddergroups.size(); i++)
        {
            if (laddergroups.get(i).getRank() == group.getRank())
            {
                if (i + 1 < laddergroups.size())
                {
                    return laddergroups.get(i + 1);
                }
                else
                {
                    return null;
                }
            }
        }
        throw new IllegalArgumentException("group ladder does not exist (anymore)");
    }

    /**
     * Gets the previous (lower) group in the same ladder.
     *
     * @param group the group to get the previous group of
     * @return the previous group in the same ladder or null if the group has no previous group
     * @throws IllegalArgumentException if the group ladder does not exist (anymore)
     */
    public synchronized PermissionGroup getPreviousGroup(PermissionGroup group)
    {
        List<PermissionGroup> laddergroups = getLadderGroups(group.getLadder());

        for (int i = 0; i < laddergroups.size(); i++)
        {
            if (laddergroups.get(i).getRank() == group.getRank())
            {
                if (i > 0)
                {
                    return laddergroups.get(i - 1);
                }
                else
                {
                    return null;
                }
            }
        }
        throw new IllegalArgumentException("group ladder does not exist (anymore)");
    }

    /**
     * Gets all groups of the given ladder.
     *
     * @param ladder the ladder of the groups to get
     * @return a sorted list of all matched groups
     */
    public synchronized List<PermissionGroup> getLadderGroups(String ladder)
    {
        List<PermissionGroup> ret = new ArrayList<>();

        grouplock.readLock().lock();
        try
        {
            for (PermissionGroup g : groups)
            {
                if (g.getLadder().equalsIgnoreCase(ladder))
                {
                    ret.add(g);
                }
            }
        }
        finally
        {
            grouplock.readLock().unlock();
        }

        Collections.sort(ret);

        return ret;
    }

    /**
     * Gets a list of all existing ladders.
     *
     * @return a list of all ladders
     */
    public synchronized List<String> getLadders()
    {
        List<String> ret = new ArrayList<>();

        grouplock.readLock().lock();
        try
        {
            for (PermissionGroup g : groups)
            {
                if (!Statics.listContains(ret, g.getLadder()))
                {
                    ret.add(g.getLadder());
                }
            }
        }
        finally
        {
            grouplock.readLock().unlock();
        }

        return ret;
    }

    /**
     * Gets a list of all groups that are marked as default and given to all users by default.
     *
     * @return a list of default groups
     */
    public synchronized List<PermissionGroup> getDefaultGroups()
    {
        List<PermissionGroup> ret = new ArrayList<>();
        grouplock.readLock().lock();
        try
        {
            for (PermissionGroup g : groups)
            {
                if (g.isDefault())
                {
                    ret.add(g);
                }
            }
        }
        finally
        {
            grouplock.readLock().unlock();
        }
        return ret;
    }

    /**
     * Gets a group by its name.
     *
     * @param groupname the name of the group to get
     * @return the found group if any or null
     */
    public synchronized PermissionGroup getGroup(String groupname)
    {
        if (groupname == null)
        {
            return null;
        }

        grouplock.readLock().lock();
        try
        {
            for (PermissionGroup g : groups)
            {
                if (g.getName().equalsIgnoreCase(groupname))
                {
                    // this is java runtime convention ...
                    // finally will always be executed
                    // grouplock.readLock().unlock();
                    return g;
                }
            }
        }
        finally
        {
            grouplock.readLock().unlock();
        }
        return null;
    }

    /**
     * Gets a user by its name. If the user is not loaded it will be loaded.
     *
     * @param usernameoruuid the name or the UUID of the user to get
     * @return the found user or null if it does not exist
     */
    public synchronized PermissionUser getUser(String usernameoruuid)
    {
        return getUser(usernameoruuid, true);
    }

    /**
     * Gets a user by its name. If the user is not loaded it will be loaded if loadfromdb is true.
     *
     * @param usernameoruuid the name or the UUID of the user to get
     * @param loadfromdb whether or not to load the user from the database if not already loaded
     * @return the found user or null if it does not exist
     */
    public synchronized PermissionUser getUser(String usernameoruuid, boolean loadfromdb)
    {
        if (usernameoruuid == null)
        {
            return null;
        }

        UUID uuid = Statics.parseUUID(usernameoruuid);
        if (config.isUseUUIDs() && uuid != null)
        {
            return getUser(uuid);
        }

        userlock.readLock().lock();
        try
        {
            for (PermissionUser u : users)
            {
                if (u.getName().equalsIgnoreCase(usernameoruuid))
                {
                    // this is java runtime convention ...
                    // finally will always be executed
                    // userlock.readLock().unlock();
                    return u;
                }
            }
        }
        finally
        {
            userlock.readLock().unlock();
        }

        //load user from database
        if (loadfromdb)
        {
            PermissionUser u = null;
            if (config.isUseUUIDs())
            {
                if (uuid == null)
                {
                    uuid = backEnd.getUUID(usernameoruuid);
                }
                if (uuid != null)
                {
                    u = backEnd.loadUser(uuid);
                }
            }
            else
            {
                u = backEnd.loadUser(usernameoruuid);
            }
            if (u != null)
            {
                addUserToCache(u);
                return u;
            }
        }

        return null;
    }

    /**
     * Gets a user by its UUID. If the user is not loaded it will be loaded.
     *
     * @param uuid the uuid of the user to get
     * @return the found user or null if it does not exist
     */
    public synchronized PermissionUser getUser(UUID uuid)
    {
        return getUser(uuid, true);
    }

    /**
     * Gets a user by its UUID. If the user is not loaded it will be loaded if loadfromdb is true.
     *
     * @param uuid the uuid of the user to get
     * @param loadfromdb whether or not to load the user from the database if not already loaded
     * @return the found user or null if it does not exist
     */
    public synchronized PermissionUser getUser(UUID uuid, boolean loadfromdb)
    {
        if (uuid == null)
        {
            return null;
        }

        userlock.readLock().lock();
        try
        {
            for (PermissionUser u : users)
            {
                if (u.getUUID().equals(uuid))
                {
                    // this is java runtime convention ...
                    // finally will always be executed
                    // userlock.readLock().unlock();
                    return u;
                }
            }
        }
        finally
        {
            userlock.readLock().unlock();
        }

        //load user from database
        if (loadfromdb)
        {
            PermissionUser u = backEnd.loadUser(uuid);
            if (u != null)
            {
                addUserToCache(u);
                return u;
            }
        }

        return null;
    }

    public PermissionUser createTempUser(String playername, UUID uuid)
    {
        List<PermissionGroup> groups = getDefaultGroups();
        PermissionUser u = new PermissionUser(playername, uuid, groups, new ArrayList<String>(), new HashMap<String, Server>(), null, null, null);
        addUserToCache(u);

        return u;
    }

    /**
     * Gets an unmodifiable list of all groups
     *
     * @return an unmodifiable list of all groups
     */
    public List<PermissionGroup> getGroups()
    {
        List<PermissionGroup> l;
        grouplock.readLock().lock();
        try
        {
            l = Collections.unmodifiableList(groups);
        }
        finally
        {
            grouplock.readLock().unlock();
        }
        return l;
    }

    /**
     * Gets an unmodifiable list of all loaded users
     *
     * @return an unmodifiable list of all loaded users
     */
    public List<PermissionUser> getUsers()
    {
        List<PermissionUser> l;
        userlock.readLock().lock();
        try
        {
            l = Collections.unmodifiableList(users);
        }
        finally
        {
            userlock.readLock().unlock();
        }
        return l;
    }

    /**
     * Gets a list of all users
     *
     * @return a list of all users
     */
    public List<String> getRegisteredUsers()
    {
        return backEnd.getRegisteredUsers();
    }

    /**
     * Gets a list of all user which are in the given group
     *
     * @param group the group
     * @return a list of all user which are in the given group
     */
    public List<String> getGroupUsers(PermissionGroup group)
    {
        return backEnd.getGroupUsers(group);
    }

    /**
     * Deletes a user from cache and database.
     *
     * @param user the user to delete
     */
    public synchronized void deleteUser(PermissionUser user)
    {
        //cache
        removeUserFromCache(user);

        //database
        backEnd.deleteUser(user);

        //send bukkit update infoif(useUUIDs)
        BungeePEX.getInstance().getNetworkNotifier().deleteUser(user, null);

        //call event
        BungeePEX.getInstance().getEventDispatcher().dispatchUserChangeEvent(user);
    }

    /**
     * Deletes a user from cache and database and validates all groups and users.
     *
     * @param group the group the remove
     */
    public synchronized void deleteGroup(PermissionGroup group)
    {
        //cache
        removeGroupFromCache(group);

        //database
        backEnd.deleteGroup(group);

        //group validation
        validateUsersGroups();

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().deleteGroup(group, null);

        //call event
        BungeePEX.getInstance().getEventDispatcher().dispatchGroupChangeEvent(group);
    }

    /**
     * Adds a user to cache and database.
     *
     * @param user the user to add
     */
    public synchronized void addUser(PermissionUser user)
    {
        //cache
        addUserToCache(user);

        //database
        backEnd.saveUser(user, true);

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadUser(user, null);

        //call event
        BungeePEX.getInstance().getEventDispatcher().dispatchUserChangeEvent(user);
    }

    /**
     * Adds a group to cache and database.
     *
     * @param group the group to add
     */
    public synchronized void addGroup(PermissionGroup group)
    {
        grouplock.writeLock().lock();
        try
        {
            groups.add(group);
            Collections.sort(groups);
        }
        finally
        {
            grouplock.writeLock().unlock();
        }

        //database
        backEnd.saveGroup(group, true);

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadGroup(group, null);

        //call event
        BungeePEX.getInstance().getEventDispatcher().dispatchGroupChangeEvent(group);
    }

    //database and permission operations
    /**
     * Formats the permissions backEnd.
     */
    public void format()
    {
        backEnd.format(backEnd.loadGroups(), backEnd.loadUsers(), permsversion);
        backEnd.load();
        BungeePEX.getInstance().getNetworkNotifier().reloadAll(null);
    }

    /**
     * Cleans the permissions backEnd and wipes 0815 users.
     *
     * @return the number of deleted users
     */
    public int cleanup()
    {
        int res = backEnd.cleanup(backEnd.loadGroups(), backEnd.loadUsers(), permsversion);
        backEnd.load();
        BungeePEX.getInstance().getNetworkNotifier().reloadAll(null);
        return res;
    }

    /**
     * Adds the given group to the user.
     *
     * @param user the user to add the group to
     * @param group the group to add to the user
     */
    public void addUserGroup(PermissionUser user, PermissionGroup group)
    {
        //cache
        user.getGroups().add(group);
        Collections.sort(user.getGroups());

        //database
        backEnd.saveUserGroups(user);

        //recalc perms
        user.recalcPerms();

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadUser(user, null);
    }

    /**
     * Removes the given group from the user.
     *
     * @param user the user to remove the group from
     * @param group the group to remove from the user
     */
    public void removeUserGroup(PermissionUser user, PermissionGroup group)
    {
        //cache
        user.getGroups().remove(group);
        Collections.sort(user.getGroups());

        //database
        backEnd.saveUserGroups(user);

        //recalc perms
        user.recalcPerms();

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadUser(user, null);
    }

    /**
     * Adds a permission to the user.
     *
     * @param user the user to add the permission to
     * @param perm the permission to add to the user
     */
    public void addUserPerm(PermissionUser user, String perm)
    {
        //cache
        user.getExtraPerms().add(Statics.toLower(perm));

        //database
        backEnd.saveUserPerms(user);

        //recalc perms
        user.recalcPerms();

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadUser(user, null);
    }

    /**
     * Removes a permission from the user.
     *
     * @param user the user to remove the permission from
     * @param perm the permission to remove from the user
     */
    public void removeUserPerm(PermissionUser user, String perm)
    {
        //cache
        user.getExtraPerms().remove(Statics.toLower(perm));

        //database
        backEnd.saveUserPerms(user);

        //recalc perms
        user.recalcPerms();

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadUser(user, null);
    }

    /**
     * Adds a permission to the user on the given server.
     *
     * @param user the user to add the permission to
     * @param server the server to add the permission on
     * @param perm the permission to add to the user
     */
    public void addUserPerServerPerm(PermissionUser user, String server, String perm)
    {
        //cache
        Server srv = user.getServer(server);
        srv.getPerms().add(Statics.toLower(perm));

        //database
        backEnd.saveUserPerServerPerms(user, Statics.toLower(server));

        //recalc perms
        user.recalcPerms(Statics.toLower(server));

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadUser(user, null);
    }

    /**
     * Removes a permission from the user on the given server.
     *
     * @param user the user to remove the permission from
     * @param server the server to remove the permission from
     * @param perm the permission to remove from the user
     */
    public void removeUserPerServerPerm(PermissionUser user, String server, String perm)
    {
        //cache
        Server srv = user.getServer(server);
        srv.getPerms().remove(Statics.toLower(perm));

        //database
        backEnd.saveUserPerServerPerms(user, Statics.toLower(server));

        //recalc perms
        user.recalcPerms(server);

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadUser(user, null);
    }

    /**
     * Adds a permission to the user on the given server in the given world.
     *
     * @param user the user to add the permission to
     * @param server the server to add the permission on
     * @param world the world to add the permission in
     * @param perm the permission to add to the user
     */
    public void addUserPerServerWorldPerm(PermissionUser user, String server, String world, String perm)
    {
        //cache
        Server srv = user.getServer(server);
        World w = srv.getWorld(world);
        w.getPerms().add(Statics.toLower(perm));

        //database
        backEnd.saveUserPerServerWorldPerms(user, Statics.toLower(server), Statics.toLower(world));

        //recalc perms
        user.recalcPerms(server, world);

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadUser(user, null);
    }

    /**
     * Removes a permission from the user on the given server.
     *
     * @param user the user to remove the permission from
     * @param server the server to remove the permission from
     * @param world the world to remove the permission from
     * @param perm the permission to remove from the user
     */
    public void removeUserPerServerWorldPerm(PermissionUser user, String server, String world, String perm)
    {
        //cache
        Server srv = user.getServer(server);
        World w = srv.getWorld(world);
        w.getPerms().remove(Statics.toLower(perm));

        //database
        backEnd.saveUserPerServerWorldPerms(user, Statics.toLower(server), Statics.toLower(world));

        //recalc perms
        user.recalcPerms(server, world);

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadUser(user, null);
    }

    /**
     * Sets the displayname of the group
     *
     * @param user
     * @param display
     * @param server
     * @param world
     */
    public void setUserDisplay(PermissionUser user, String display, String server, String world)
    {
        //cache
        if (server == null)
        {
            user.setDisplay(display);
        }
        else if (world == null)
        {
            user.getServer(server).setDisplay(display);
        }
        else
        {
            user.getServer(server).getWorld(world).setDisplay(display);
        }

        //database
        backEnd.saveUserDisplay(user, Statics.toLower(server), Statics.toLower(world));

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadUser(user, null);

        //call event
        BungeePEX.getInstance().getEventDispatcher().dispatchUserChangeEvent(user);
    }

    /**
     * Sets the prefix for the group.
     *
     * @param user
     * @param prefix
     * @param server
     * @param world
     */
    public void setUserPrefix(PermissionUser user, String prefix, String server, String world)
    {
        //cache
        if (server == null)
        {
            user.setPrefix(prefix);
        }
        else if (world == null)
        {
            user.getServer(server).setPrefix(prefix);
        }
        else
        {
            user.getServer(server).getWorld(world).setPrefix(prefix);
        }

        //database
        backEnd.saveUserPrefix(user, Statics.toLower(server), Statics.toLower(world));

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadUser(user, null);

        //call event
        BungeePEX.getInstance().getEventDispatcher().dispatchUserChangeEvent(user);
    }

    /**
     * Sets the suffix for the group.
     *
     * @param user
     * @param suffix
     * @param server
     * @param world
     */
    public void setUserSuffix(PermissionUser user, String suffix, String server, String world)
    {
        //cache
        if (server == null)
        {
            user.setSuffix(suffix);
        }
        else if (world == null)
        {
            user.getServer(server).setSuffix(suffix);
        }
        else
        {
            user.getServer(server).getWorld(world).setSuffix(suffix);
        }

        //database
        backEnd.saveUserSuffix(user, Statics.toLower(server), Statics.toLower(world));

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadUser(user, null);

        //call event
        BungeePEX.getInstance().getEventDispatcher().dispatchUserChangeEvent(user);
    }

    /**
     * Adds the permission to the group.
     *
     * @param group the group
     * @param perm the permission to add
     */
    public void addGroupPerm(PermissionGroup group, String perm)
    {
        //cache
        group.getPerms().add(Statics.toLower(perm));

        //database
        backEnd.saveGroupPerms(group);

        //recalc perms
        for (PermissionGroup g : groups)
        {
            g.recalcPerms();
        }
        for (PermissionUser u : users)
        {
            u.recalcPerms();
        }

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadGroup(group, null);
    }

    /**
     * Removes the permission from the group.
     *
     * @param group the group
     * @param perm the permission to remove
     */
    public void removeGroupPerm(PermissionGroup group, String perm)
    {
        //cache
        group.getPerms().remove(Statics.toLower(perm));

        //database
        backEnd.saveGroupPerms(group);

        //recalc perms
        for (PermissionGroup g : groups)
        {
            g.recalcPerms();
        }
        for (PermissionUser u : users)
        {
            u.recalcPerms();
        }

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadGroup(group, null);
    }

    /**
     * Adds the permission to the group on the given server.
     *
     * @param group the group
     * @param server the server
     * @param perm the permission to add
     */
    public void addGroupPerServerPerm(PermissionGroup group, String server, String perm)
    {
        //cache
        Server srv = group.getServer(server);
        srv.getPerms().add(Statics.toLower(perm));

        //database
        backEnd.saveGroupPerServerPerms(group, Statics.toLower(server));

        //recalc perms
        for (PermissionGroup g : groups)
        {
            g.recalcPerms(server);
        }
        for (PermissionUser u : users)
        {
            u.recalcPerms();
        }

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadGroup(group, null);
    }

    /**
     * Removes the permission from the group on the given server.
     *
     * @param group the group
     * @param server the server
     * @param perm the permission to remove
     */
    public void removeGroupPerServerPerm(PermissionGroup group, String server, String perm)
    {
        //cache
        Server srv = group.getServer(server);

        srv.getPerms().remove(Statics.toLower(perm));

        //database
        backEnd.saveGroupPerServerPerms(group, Statics.toLower(server));

        //recalc perms
        for (PermissionGroup g : groups)
        {
            g.recalcPerms(server);
        }
        for (PermissionUser u : users)
        {
            u.recalcPerms();
        }

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadGroup(group, null);
    }

    /**
     * Adds the permission to the group on the given server and world.
     *
     * @param group the group
     * @param server the server
     * @param world the world
     * @param perm the permission to add
     */
    public void addGroupPerServerWorldPerm(PermissionGroup group, String server, String world, String perm)
    {
        //cache
        Server srv = group.getServer(server);
        World w = srv.getWorld(world);
        w.getPerms().add(Statics.toLower(perm));

        //database
        backEnd.saveGroupPerServerWorldPerms(group, Statics.toLower(server), Statics.toLower(world));

        //recalc perms
        for (PermissionGroup g : groups)
        {
            g.recalcPerms(server, world);
        }
        for (PermissionUser u : users)
        {
            u.recalcPerms();
        }

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadGroup(group, null);
    }

    /**
     * Removes the permission from the group on the given server and world.
     *
     * @param group the group
     * @param server the server
     * @param world the world
     * @param perm the permission to remove
     */
    public void removeGroupPerServerWorldPerm(PermissionGroup group, String server, String world, String perm)
    {
        //cache
        Server srv = group.getServer(server);
        World w = srv.getWorld(world);
        w.getPerms().remove(Statics.toLower(perm));

        //database
        backEnd.saveGroupPerServerWorldPerms(group, Statics.toLower(server), Statics.toLower(world));

        //recalc perms
        for (PermissionGroup g : groups)
        {
            g.recalcPerms(server, world);
        }
        for (PermissionUser u : users)
        {
            u.recalcPerms();
        }

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadGroup(group, null);
    }

    /**
     * Adds the toadd group to the group as inheritance
     *
     * @param group the group which should inherit
     * @param toadd the group which should be inherited
     */
    public void addGroupInheritance(PermissionGroup group, PermissionGroup toadd)
    {
        //cache
        group.getInheritances().add(toadd.getName());
        Collections.sort(group.getInheritances());

        //database
        backEnd.saveGroupInheritances(group);

        //recalc perms
        for (PermissionGroup g : groups)
        {
            g.recalcPerms();
        }
        for (PermissionUser u : users)
        {
            u.recalcPerms();
        }

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadGroup(group, null);
    }

    /**
     * Removes the toremove group from the group as inheritance
     *
     * @param group the group which should no longer inherit
     * @param toremove the group which should no longer be inherited
     */
    public void removeGroupInheritance(PermissionGroup group, PermissionGroup toremove)
    {
        //cache
        group.getInheritances().remove(toremove.getName());
        Collections.sort(group.getInheritances());

        //database
        backEnd.saveGroupInheritances(group);

        //recalc perms
        for (PermissionGroup g : groups)
        {
            g.recalcPerms();
        }
        for (PermissionUser u : users)
        {
            u.recalcPerms();
        }

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadGroup(group, null);
    }

    /**
     * Set the ladder for the group.
     *
     * @param group
     * @param ladder
     */
    public void ladderGroup(PermissionGroup group, String ladder)
    {
        //cache
        group.setLadder(ladder);

        //database
        backEnd.saveGroupLadder(group);

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadGroup(group, null);

        //call event
        BungeePEX.getInstance().getEventDispatcher().dispatchGroupChangeEvent(group);
    }

    /**
     * Sets the rank for the group.
     *
     * @param group
     * @param rank
     */
    public void rankGroup(PermissionGroup group, int rank)
    {
        //cache
        group.setRank(rank);
        Collections.sort(groups);

        //database
        backEnd.saveGroupRank(group);

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadGroup(group, null);

        //call event
        BungeePEX.getInstance().getEventDispatcher().dispatchGroupChangeEvent(group);
    }

    /**
     * Sets the weight for the group.
     *
     * @param group
     * @param weight
     */
    public void weightGroup(PermissionGroup group, int weight)
    {
        //cache
        group.setWeight(weight);
        Collections.sort(groups);

        //database
        backEnd.saveGroupWeight(group);

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadGroup(group, null);

        //call event
        BungeePEX.getInstance().getEventDispatcher().dispatchGroupChangeEvent(group);
    }

    /**
     * Sets if the the group is a default group.
     *
     * @param group
     * @param isdefault
     */
    public void setGroupDefault(PermissionGroup group, boolean isdefault)
    {
        //cache
        group.setIsdefault(isdefault);

        //database
        backEnd.saveGroupDefault(group);

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadGroup(group, null);

        //call event
        BungeePEX.getInstance().getEventDispatcher().dispatchGroupChangeEvent(group);
    }

    /**
     * Sets the displayname of the group
     *
     * @param group
     * @param display
     * @param server
     * @param world
     */
    public void setGroupDisplay(PermissionGroup group, String display, String server, String world)
    {
        //cache
        if (server == null)
        {
            group.setDisplay(display);
        }
        else if (world == null)
        {
            group.getServer(server).setDisplay(display);
        }
        else
        {
            group.getServer(server).getWorld(world).setDisplay(display);
        }

        //database
        backEnd.saveGroupDisplay(group, server, world);

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadGroup(group, null);

        //call event
        BungeePEX.getInstance().getEventDispatcher().dispatchGroupChangeEvent(group);
    }

    /**
     * Sets the prefix for the group.
     *
     * @param group
     * @param prefix
     * @param server
     * @param world
     */
    public void setGroupPrefix(PermissionGroup group, String prefix, String server, String world)
    {
        //cache
        if (server == null)
        {
            group.setPrefix(prefix);
        }
        else if (world == null)
        {
            group.getServer(server).setPrefix(prefix);
        }
        else
        {
            group.getServer(server).getWorld(world).setPrefix(prefix);
        }

        //database
        backEnd.saveGroupPrefix(group, server, world);

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadGroup(group, null);

        //call event
        BungeePEX.getInstance().getEventDispatcher().dispatchGroupChangeEvent(group);
    }

    /**
     * Sets the suffix for the group.
     *
     * @param group
     * @param suffix
     * @param server
     * @param world
     */
    public void setGroupSuffix(PermissionGroup group, String suffix, String server, String world)
    {
        //cache
        if (server == null)
        {
            group.setSuffix(suffix);
        }
        else if (world == null)
        {
            group.getServer(server).setSuffix(suffix);
        }
        else
        {
            group.getServer(server).getWorld(world).setSuffix(suffix);
        }

        //database
        backEnd.saveGroupSuffix(group, server, world);

        //send bukkit update info
        BungeePEX.getInstance().getNetworkNotifier().reloadGroup(group, null);

        //call event
        BungeePEX.getInstance().getEventDispatcher().dispatchGroupChangeEvent(group);
    }

    /**
     * Migrates the permissions to the given backnd type.
     *
     * @param bet the backEnd type to migrate to
     */
    public synchronized void migrateBackEnd(BackEndType bet)
    {
        Migrator migrator = null;
        switch (bet)
        {
            case MySQL:
                migrator = new Migrate2MySQL(config, debug);
                break;
            case YAML:
                migrator = new Migrate2YAML(config);
                break;
            default:
                throw new UnsupportedOperationException("bet = " + bet.name());
        }

        migrator.migrate(backEnd.loadGroups(), backEnd.loadUsers(), permsversion);

        backEnd.load();
    }

    /**
     * Converts the permissions database to use UUIDs for player identification.
     *
     * @param uuids a map of player names and their corresponding UUIDs
     */
    public void migrateUseUUID(Map<String, UUID> uuids)
    {
        List<PermissionGroup> groups = backEnd.loadGroups();
        List<PermissionUser> users = backEnd.loadUsers();
        int version = backEnd.loadVersion();
        BungeePEX.getInstance().getConfig().setUseUUIDs(true);

        backEnd.clearDatabase();
        for (PermissionGroup g : groups)
        {
            backEnd.saveGroup(g, false);
        }
        for (PermissionUser u : users)
        {
            UUID uuid = uuids.get(u.getName());
            if (uuid != null)
            {
                u.setUUID(uuid);
                backEnd.saveUser(u, false);
            }
        }
        backEnd.saveVersion(version, true);
    }

    /**
     * Converts the permissions database to use player names for player identification.
     *
     * @param playernames a map of UUIDs and their corresponding player names
     */
    public void migrateUsePlayerNames(Map<UUID, String> playernames)
    {
        List<PermissionGroup> groups = backEnd.loadGroups();
        List<PermissionUser> users = backEnd.loadUsers();
        int version = backEnd.loadVersion();
        BungeePEX.getInstance().getConfig().setUseUUIDs(false);

        backEnd.clearDatabase();
        for (PermissionGroup g : groups)
        {
            backEnd.saveGroup(g, false);
        }
        for (PermissionUser u : users)
        {
            String playername = playernames.get(u.getUUID());
            if (playername != null)
            {
                u.setName(playername);
                backEnd.saveUser(u, false);
            }
        }
        backEnd.saveVersion(version, true);
    }

    /**
     * Converts the backend of the database holding the UUIDs and their corresponding player names to the new backend.
     *
     * @param type the new backend type
     */
    public void migrateUUIDPlayerDB()
    {
        Map<UUID, String> map = backEnd.getAll();

        /*switch (type)
        {
            case None:
                UUIDPlayerDB = new NoneUUIDPlayerDB();
                break;
            case YAML:
                UUIDPlayerDB = new YAMLUUIDPlayerDB();
                break;
            case MySQL:
                UUIDPlayerDB = new MySQLUUIDPlayerDB();
                break;
            default:
                throw new UnsupportedOperationException("type = " + type);
        }
        BungeePerms.getInstance().getConfig().setUUIDPlayerDB(UUIDPlayerDB.getType());*/
        backEnd.clear();

        for (Map.Entry<UUID, String> e : map.entrySet())
        {
            backEnd.update(e.getKey(), e.getValue());
        }
    }

    //internal functions
    public void reloadUser(String user)
    {
        PermissionUser u = getUser(user);
        if (u == null)
        {
            debug.log("User " + user + " not found!!!");
            return;
        }
        backEnd.reloadUser(u);
        u.recalcPerms();
    }

    public void reloadUser(UUID uuid)
    {
        PermissionUser u = getUser(uuid);
        if (u == null)
        {
            debug.log("User " + uuid + " not found!!!");
            return;
        }
        backEnd.reloadUser(u);
        u.recalcPerms();
    }

    public void reloadGroup(String group)
    {
        PermissionGroup g = getGroup(group);
        if (g == null)
        {
            debug.log("Group " + group + " not found!!!");
            return;
        }

        boolean holdread = false;
        grouplock.writeLock().lock();
        try
        {
            backEnd.reloadGroup(g);
            Collections.sort(groups);

            grouplock.readLock().lock();
            holdread = true;
        }
        finally
        {
            grouplock.writeLock().unlock();
        }
        try
        {
            for (PermissionGroup gr : groups)
            {
                gr.recalcPerms();
            }
        }
        finally
        {
            if (holdread)
            {
                grouplock.readLock().unlock();
            }
        }

        userlock.readLock().lock();
        try
        {
            for (PermissionUser u : users)
            {
                u.recalcPerms();
            }
        }
        finally
        {
            userlock.readLock().unlock();
        }
    }

    public void reloadUsers()
    {
        userlock.readLock().lock();
        try
        {
            for (PermissionUser u : users)
            {
                backEnd.reloadUser(u);
                u.recalcPerms();
            }
        }
        finally
        {
            userlock.readLock().unlock();
        }
    }

    public void reloadGroups()
    {
        boolean holdread = false;
        grouplock.writeLock().lock();
        try
        {
            for (PermissionGroup g : groups)
            {
                backEnd.reloadGroup(g);
            }
            Collections.sort(groups);

            grouplock.readLock().lock();
            holdread = true;
        }
        finally
        {
            grouplock.writeLock().unlock();
        }
        try
        {
            for (PermissionGroup g : groups)
            {
                g.recalcPerms();
            }
        }
        finally
        {
            if (holdread)
            {
                grouplock.readLock().unlock();
            }
        }

        userlock.readLock().lock();
        try
        {
            for (PermissionUser u : users)
            {
                u.recalcPerms();
            }
        }
        finally
        {
            userlock.readLock().unlock();
        }
    }

    public void addUserToCache(PermissionUser u)
    {
        userlock.writeLock().lock();
        try
        {
            users.add(u);
        }
        finally
        {
            userlock.writeLock().unlock();
        }
    }

    public void removeUserFromCache(PermissionUser u)
    {
        userlock.writeLock().lock();
        try
        {
            users.remove(u);
        }
        finally
        {
            userlock.writeLock().unlock();
        }
    }

    public void addGroupToCache(PermissionGroup g)
    {
        grouplock.writeLock().lock();
        try
        {
            groups.add(g);
        }
        finally
        {
            grouplock.writeLock().unlock();
        }
    }

    public void removeGroupFromCache(PermissionGroup g)
    {
        grouplock.writeLock().lock();
        try
        {
            groups.remove(g);
        }
        finally
        {
            grouplock.writeLock().unlock();
        }
    }

}
