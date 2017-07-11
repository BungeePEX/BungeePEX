package com.xaosia.bungeepex;

import java.util.Locale;

import java.util.Locale;

import com.xaosia.bungeepex.backends.BackEndType;
import com.xaosia.bungeepex.utils.Lang;
import lombok.Getter;
import lombok.Setter;
import com.xaosia.bungeepex.utils.Config;



@Getter
public class PEXConfig {

    protected Config config;

    //perms
    private boolean useUUIDs;
    private boolean useRegexPerms;
    private boolean groupPermission;

    //db
    private BackEndType backEndType;
    //private UUIDPlayerDBType UUIDPlayerDBType;
    private String tablePrefix;
    private int fetcherCooldown;
    private boolean saveAllUsers;
    private boolean deleteUsersOnCleanup;

    //fancy ingame
    private boolean notifyPromote;
    private boolean notifyDemote;
    private boolean tabComplete;
    private Locale locale;
    private boolean terminatePrefixReset;
    private boolean terminateSuffixReset;
    private boolean terminatePrefixSpace;
    private boolean terminateSuffixSpace;

    //tmp at runtime
    @Setter
    private boolean debug;

    //cleanup
    private int cleanupInterval;
    private int cleanupThreshold;

    //other
    private boolean asyncCommands;

    public PEXConfig(Config config)
    {
        this.config = config;
    }

    public void load()
    {
        config.load();

        //perms, use UUIDs by default
        useUUIDs = config.getBoolean("useUUIDs", true); //todo: force uuid usage?
        useRegexPerms = config.getBoolean("useregexperms", false);
        groupPermission = config.getBoolean("grouppermission", true);

        //db
        backEndType = config.getEnumValue("backendtype", BackEndType.YAML);
        //UUIDPlayerDBType = config.getEnumValue("uuidplayerdb", UUIDPlayerDBType.YAML);
        tablePrefix = config.getString("tablePrefix", "bungeeperms_");
        fetcherCooldown = config.getInt("uuidfetcher.cooldown", 3000);
        saveAllUsers = config.getBoolean("saveAllUsers", true);
        deleteUsersOnCleanup = config.getBoolean("deleteUsersOnCleanup", false);

        //fancy ingame
        notifyPromote = config.getBoolean("notify.promote", false);
        notifyDemote = config.getBoolean("notify.demote", false);
        tabComplete = config.getBoolean("tabcomplete", false);
        locale = Locale.forLanguageTag(config.getString("locale", Statics.localeString(new Locale("en", "US"))));
        terminatePrefixReset = config.getBoolean("terminate.prefix.reset", true);
        terminateSuffixReset = config.getBoolean("terminate.suffix.reset", true);
        terminatePrefixSpace = config.getBoolean("terminate.prefix.space", true);
        terminateSuffixSpace = config.getBoolean("terminate.suffix.space", true);

        //cleanup
        cleanupInterval = config.getInt("cleanup.interval", 30 * 60);
        cleanupThreshold = config.getInt("cleanup.threshold", 10 * 60);

        //other
        asyncCommands = config.getBoolean("async-commands", true);

        //validate();
    }

    /*public void validate()
    {
        if(useUUIDs && UUIDPlayerDBType == UUIDPlayerDBType.None)
        {
            BungeePEX.getLogger().warning(Lang.translate(Lang.MessageType.MISCONFIGURATION) + ": " + Lang.translate(Lang.MessageType.MISCONFIG_USEUUID_NONE_UUID_DB));
        }
    }*/

    public void setUseUUIDs(boolean useUUIDs)
    {
        this.useUUIDs = useUUIDs;
        config.setBool("useUUIDs", useUUIDs);
        config.save();
    }

    /*public void setUUIDPlayerDB(UUIDPlayerDBType type)
    {
        this.UUIDPlayerDBType = type;
        config.setEnumValue("uuidplayerdb", type);
        config.save();
    }*/

    public void setBackendType(BackEndType type)
    {
        this.backEndType = type;
        config.setEnumValue("backendtype", type);
        config.save();
    }

}
