package com.xaosia.bungeepex.backends;

import java.util.List;
import java.util.UUID;
import com.xaosia.bungeepex.PermissionGroup;
import com.xaosia.bungeepex.PermissionUser;

public interface BackEnd
{

    public BackEndType getType();

    public void load();

    public List<PermissionGroup> loadGroups();

    public List<PermissionUser> loadUsers();

    public PermissionGroup loadGroup(String group);

    public PermissionUser loadUser(String user);

    public PermissionUser loadUser(UUID user);

    public int loadVersion();

    public void saveVersion(int version, boolean savetodisk);

    public boolean isUserInDatabase(PermissionUser user);

    public List<String> getRegisteredUsers();

    public List<String> getGroupUsers(PermissionGroup group);

    public void reloadGroup(PermissionGroup group);

    public void reloadUser(PermissionUser user);

    public void saveUser(PermissionUser user, boolean savetodisk);

    public void saveGroup(PermissionGroup group, boolean savetodisk);

    public void deleteUser(PermissionUser user);

    public void deleteGroup(PermissionGroup group);

    public void saveUserGroups(PermissionUser user);

    public void saveUserPerms(PermissionUser user);

    public void saveUserPerServerPerms(PermissionUser user, String server);

    public void saveUserPerServerWorldPerms(PermissionUser user, String server, String world);

    public void saveUserDisplay(PermissionUser user, String server, String world);

    public void saveUserPrefix(PermissionUser user, String server, String world);

    public void saveUserSuffix(PermissionUser user, String server, String world);

    public void saveGroupPerms(PermissionGroup group);

    public void saveGroupPerServerPerms(PermissionGroup group, String server);

    public void saveGroupPerServerWorldPerms(PermissionGroup group, String server, String world);

    public void saveGroupInheritances(PermissionGroup group);

    public void saveGroupRank(PermissionGroup group);

    public void saveGroupWeight(PermissionGroup group);

    public void saveGroupLadder(PermissionGroup group);

    public void saveGroupDefault(PermissionGroup group);

    public void saveGroupDisplay(PermissionGroup group, String server, String world);

    public void saveGroupPrefix(PermissionGroup group, String server, String world);

    public void saveGroupSuffix(PermissionGroup group, String server, String world);

    public int cleanup(List<PermissionGroup> groups, List<PermissionUser> users, int version);

    public void format(List<PermissionGroup> groups, List<PermissionUser> users, int version);

    public void clearDatabase();
}
