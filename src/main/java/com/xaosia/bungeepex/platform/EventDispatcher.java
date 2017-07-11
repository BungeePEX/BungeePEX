package com.xaosia.bungeepex.platform;

import com.xaosia.bungeepex.PermissionGroup;
import com.xaosia.bungeepex.PermissionUser;
public interface EventDispatcher {

    public void dispatchReloadedEvent();
    public void dispatchGroupChangeEvent(PermissionGroup group);
    public void dispatchUserChangeEvent(PermissionUser user);

}
