package com.xaosia.bungeepex.backends.mysql;

import com.xaosia.bungeepex.BungeePEX;
import com.xaosia.bungeepex.backends.MySQLBackEnd;
import com.xaosia.bungeepex.data.StorageBackend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

public class PermsAdapter
{

    public void createTable()
    {
        if (!MySQLBackEnd.getStorageBackend().tableExists(MySQLBackEnd.PERMISSIONS_TABLE))
        {
            String t = "CREATE TABLE `" + MySQLBackEnd.PERMISSIONS_TABLE + "` ("
                    + "`id` INT( 64 ) NOT NULL AUTO_INCREMENT PRIMARY KEY ,"
                    + "`name` VARCHAR( 64 ) NOT NULL ,"
                    + "`type` TINYINT( 2 ) NOT NULL ,"
                    + "`key` VARCHAR( 256 ) NOT NULL, "
                    + "`value` VARCHAR( 256 ) NOT NULL, "
                    + "`server` VARCHAR( 64 ), "
                    + "`world` VARCHAR( 64 ) "
                    + ") ENGINE = MYISAM ;";
            MySQLBackEnd.getStorageBackend().runQuery(t);
        }
    }

    public List<String> getGroups()
    {
        List<String> groups = new ArrayList<>();
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet res = null;
        try
        {
            connection = MySQLBackEnd.getStorageBackend().getPoolManager().getConnection();
            stmt = connection.prepareStatement("SELECT DISTINCT  `name` FROM `" + MySQLBackEnd.PERMISSIONS_TABLE + "` WHERE `type` = ? ORDER BY id ASC");
            stmt.setInt(1, EntityType.Group.getCode());
            res = stmt.executeQuery();
            while (res.next())
            {
                String name = StorageBackend.unescape(res.getString("name"));
                groups.add(name);
            }
        }
        catch (Exception e)
        {
            BungeePEX.getInstance().getDebug().log(e);
        }
        finally
        {
            MySQLBackEnd.getStorageBackend().getPoolManager().close(connection, stmt, res);
        }

        return groups;
    }

    public List<String> getUsers()
    {
        List<String> groups = new ArrayList<>();
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet res = null;
        try
        {
            connection = MySQLBackEnd.getStorageBackend().getPoolManager().getConnection();
            stmt = connection.prepareStatement("SELECT DISTINCT `name` FROM `" + MySQLBackEnd.PERMISSIONS_TABLE + "` WHERE `type` = ?  ORDER BY id ASC");
            stmt.setInt(1, EntityType.User.getCode());
            res = stmt.executeQuery();
            while (res.next())
            {
                String name = StorageBackend.unescape(res.getString("name"));
                groups.add(name);
            }
        }
        catch (Exception e)
        {
            BungeePEX.getInstance().getDebug().log(e);
        }
        finally
        {
            MySQLBackEnd.getStorageBackend().getPoolManager().close(connection, stmt, res);
        }

        return groups;
    }

    private MysqlPermEntity getEntity(String name, EntityType type)
    {
        MysqlPermEntity mpe = null;
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet res = null;
        try
        {
            connection = MySQLBackEnd.getStorageBackend().getPoolManager().getConnection();
            stmt = connection.prepareStatement("SELECT `name`,`type`,`key`,`value`,`server`,`world` FROM `" + MySQLBackEnd.PERMISSIONS_TABLE + "` " +
                    "WHERE `type` = ? AND `name` = ? ORDER BY id ASC");
            stmt.setInt(1, type.getCode());
            stmt.setString(2, StorageBackend.escape(name));
            res = stmt.executeQuery();
            mpe = new MysqlPermEntity(res);
        }
        catch (Exception e)
        {
            BungeePEX.getInstance().getDebug().log(e);
        }
        finally
        {
            MySQLBackEnd.getStorageBackend().getPoolManager().close(connection, stmt, res);
        }

        return mpe;
    }

    public MysqlPermEntity getGroup(String name)
    {
        return getEntity(name, EntityType.Group);
    }

    public MysqlPermEntity getUser(String name)
    {
        return getEntity(name, EntityType.User);
    }

    public MysqlPermEntity getVersion()
    {
        return getEntity("version", EntityType.Version);
    }

    public boolean isInBD(String name, EntityType type)
    {
        boolean found = false;
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet res = null;
        try
        {
            connection = MySQLBackEnd.getStorageBackend().getPoolManager().getConnection();
            stmt = connection.prepareStatement("SELECT DISTINCT `name` FROM `" + MySQLBackEnd.PERMISSIONS_TABLE + "` WHERE `name` = ? AND `type`= ? ORDER BY id ASC");
            stmt.setString(1, StorageBackend.escape(name));
            stmt.setInt(2, type.getCode());
            res = stmt.executeQuery();
            if (res.next())
            {
                found = true;
            }
        }
        catch (Exception e)
        {
            BungeePEX.getInstance().getDebug().log(e);
            found = false;
        }
        finally
        {
            MySQLBackEnd.getStorageBackend().getPoolManager().close(connection, stmt, res);
        }

        return found;
    }

    public synchronized void deleteEntity(String name, EntityType type)
    {
        BungeePEX.getInstance().getPlugin().doAsync(new Runnable() {
            @Override
            public void run() {

                Connection connection = null;
                PreparedStatement stmt = null;

                try {
                    connection = MySQLBackEnd.getStorageBackend().getPoolManager().getConnection();
                    stmt = connection.prepareStatement("DELETE FROM `" + MySQLBackEnd.PERMISSIONS_TABLE + "` WHERE `name` = ? AND `type` = ?");
                    stmt.setString(1, StorageBackend.escape(name));
                    stmt.setInt(2, type.getCode());
                } catch (SQLException e) {
                    BungeePEX.getLogger().log(Level.SEVERE, "Failed to delete Entity", e);
                } finally {
                    MySQLBackEnd.getStorageBackend().getPoolManager().close(connection, stmt, null);
                }

            }
        });
    }

    public synchronized void saveData(String name, EntityType type, String key, List<ValueEntry> values)
    {
        BungeePEX.getInstance().getPlugin().doAsync(new Runnable() {
            @Override
            public void run() {

                Connection connection = null;
                PreparedStatement stmt = null;

                try {
                    connection = MySQLBackEnd.getStorageBackend().getPoolManager().getConnection();
                    stmt = connection.prepareStatement("DELETE FROM `" + MySQLBackEnd.PERMISSIONS_TABLE + "` WHERE `name` = ? AND `type` = ? AND `key` = ?");
                    stmt.setString(1, StorageBackend.escape(name));
                    stmt.setInt(2, type.getCode());
                    stmt.setString(3, StorageBackend.escape(key));

                } catch (SQLException e) {
                    BungeePEX.getLogger().log(Level.SEVERE, "Failed to delete Entity", e);
                } finally {
                    MySQLBackEnd.getStorageBackend().getPoolManager().close(connection, stmt, null);

                    //add valuse
                    PermsAdapter.this.doSaveData(name, type, key, values);
                }

            }
        });
    }

    public void saveData(String name, EntityType type, String key, List<ValueEntry> values, String server, String world)
    {
        BungeePEX.getInstance().getPlugin().doAsync(new Runnable() {
            @Override
            public void run() {

                Connection connection = null;
                PreparedStatement stmt = null;

                String delq = "DELETE FROM `" + MySQLBackEnd.PERMISSIONS_TABLE + "` WHERE `name` = ? AND `type` = ? AND `key` = ? AND";
                if (server == null)
                {
                    delq += "`server` IS NULL";
                }
                else
                {
                    delq += "`server`='" + StorageBackend.escape(server) + "'";
                }
                delq += " AND ";
                if (world == null)
                {
                    delq += "`world` IS NULL";
                }
                else
                {
                    delq += "`world`='" + StorageBackend.escape(world) + "'";
                }

                try {
                    connection = MySQLBackEnd.getStorageBackend().getPoolManager().getConnection();
                    stmt = connection.prepareStatement(delq);
                    stmt.setString(1, StorageBackend.escape(name));
                    stmt.setInt(2, type.getCode());
                    stmt.setString(3, StorageBackend.escape(key));

                    PermsAdapter.this.doSaveData(name, type, key, values);

                } catch (SQLException e) {
                    BungeePEX.getLogger().log(Level.SEVERE, "Failed to delete Entity", e);
                } finally {
                    MySQLBackEnd.getStorageBackend().getPoolManager().close(connection, stmt, null);

                    //add values
                    PermsAdapter.this.doSaveData(name, type, key, values);
                }

            }
        });

    }

    private void doSaveData(String name, EntityType type, String key, List<ValueEntry> values)
    {
        for (ValueEntry val : values)
        {
            if (val.getValue() == null)
            {
                continue;
            }
            String insq = "INSERT INTO `" + MySQLBackEnd.PERMISSIONS_TABLE + "` (`name`,`type`,`key`,`value`,`server`,`world`) VALUES"
                    + "('" + StorageBackend.escape(name) + "'," + type.getCode() + ",'" + StorageBackend.escape(key) + "','" + StorageBackend.escape(val.getValue()) + "',";
            if (val.getServer() == null)
            {
                insq += "null,null";
            }
            else
            {
                insq += "'" + StorageBackend.escape(val.getServer()) + "',";
                if (val.getWorld() == null)
                {
                    insq += "null";
                }
                else
                {
                    insq += "'" + StorageBackend.escape(val.getWorld()) + "'";
                }
            }

            insq += ")";
            MySQLBackEnd.getStorageBackend().runQuery(insq);
        }
    }

    public List<String> getGroupUsers(String group)
    {
        List<String> groups = new ArrayList<>();
        Connection connection = null;
        PreparedStatement stmt = null;
        ResultSet res = null;
        try
        {
            connection = MySQLBackEnd.getStorageBackend().getPoolManager().getConnection();
            stmt = connection.prepareStatement("SELECT DISTINCT `name` FROM `" + MySQLBackEnd.PERMISSIONS_TABLE + "` WHERE `type` = ? AND `key` = 'groups' AND `value` = ? ORDER BY id ASC");
            stmt.setInt(1, EntityType.User.getCode());
            stmt.setString(2, StorageBackend.escape(group));
            res = stmt.executeQuery();
            while (res.next())
            {
                String name = StorageBackend.unescape(res.getString("name"));
                groups.add(name);
            }
        }
        catch (SQLException e)
        {
            BungeePEX.getInstance().getDebug().log(e);
        }
        finally
        {
            MySQLBackEnd.getStorageBackend().getPoolManager().close(connection, stmt, res);
        }

        return groups;
    }

    public void clearTable(String table)
    {
        MySQLBackEnd.getStorageBackend().runQuery("TRUNCATE `" + table + "`");
    }

}
