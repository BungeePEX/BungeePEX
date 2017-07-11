package com.xaosia.bungeepex.platform.bukkit;

import lombok.Getter;
import com.xaosia.bungeepex.PEXConfig;
import com.xaosia.bungeepex.utils.Config;

import java.util.UUID;

@Getter
public class BukkitConfig extends PEXConfig {

    private String servername;
    private UUID serverUUID;
    private boolean allowops;
    private boolean superpermscompat;

    private boolean standalone;

    public BukkitConfig(Config config)
    {
        super(config);
    }

    @Override
    public void load()
    {
        super.load();
        servername = config.getString("servername", "servername");
        allowops = config.getBoolean("allowops", true);
        superpermscompat = config.getBoolean("superpermscompat", false);
        serverUUID = UUID.fromString(config.getString("server-uuid", UUID.randomUUID().toString()));

        standalone = config.getBoolean("standalone", false);
    }

}
