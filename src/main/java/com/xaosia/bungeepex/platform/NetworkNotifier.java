package com.xaosia.bungeepex.platform;

import com.xaosia.bungeepex.PermissionUser;
import com.xaosia.bungeepex.PermissionGroup;

public interface NetworkNotifier {

    public void deleteUser(PermissionUser user, String origin);

    public void deleteGroup(PermissionGroup group, String origin);

    public void reloadUser(PermissionUser user, String origin);

    public void reloadGroup(PermissionGroup group, String origin);

    public void reloadUsers(String origin);

    public void reloadGroups(String origin);

    public void reloadAll(String origin);

}
