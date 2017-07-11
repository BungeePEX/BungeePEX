package com.xaosia.bungeepex.platform;

public interface PluginMessageSender {

    public void sendPluginMessage(String target, String channel, String msg);

}
