package com.xaosia.bungeepex.data;

import com.xaosia.bungeepex.BungeePEX;
import com.xaosia.bungeepex.utils.DatabaseCredentials;
import com.xaosia.bungeepex.data.connection.ConnectionPoolManager;

import java.sql.*;
import java.util.logging.Level;

public class StorageBackend {

    private ConnectionPoolManager poolManager;

    public StorageBackend(DatabaseCredentials credentials) {
        this.poolManager = new ConnectionPoolManager(credentials);
        this.createTables();
    }

    public ConnectionPoolManager getPoolManager() {
        return this.poolManager;
    }

    public void closeConnections() {
        this.poolManager.closePool();
    }

    private synchronized void createTables() {
        BungeePEX.getInstance().getPlugin().doAsync(new Runnable() {
            @Override
            public void run() {
                Connection connection = null;

                try {

                    connection = poolManager.getConnection();

                    connection.prepareStatement("CREATE TABLE IF NOT EXISTS `permissions` (" +
                            "`id` BIGINT NOT NULL AUTO_INCREMENT, " +
                            "`name` varchar(255) NOT NULL," +
                            "`type` TINYINT(2) NOT NULL," +
                            "`key` varchar(255) NOT NULL," +
                            "`value` varchar(255) NOT NULL," +
                            "`server` varchar(64) DEFAULT NULL," +
                            "`world` varchar(64) DEFAULT NULL," +
                            "PRIMARY KEY (`id`)," +
                            ") ENGINE = InnoDB;").executeUpdate();

                    connection.prepareStatement("CREATE TABLE IF NOT EXISTS `uuidplayer` (" +
                            "`id` BIGINT NOT NULL AUTO_INCREMENT, " +
                            "`uuid` varchar(64) NOT NULL," +
                            "`player` varchar(32) DEFAULT NULL," +
                            "PRIMARY KEY (`id`)," +
                            "KEY `uuid_index` (`uuid`)" +
                            "KEY `player_index` (`player`)" +
                            ") ENGINE = InnoDB;").executeUpdate();

                } catch (SQLException e) {
                    if (!e.getMessage().contains("already exists")) {
                        BungeePEX.getLogger().log(Level.SEVERE, "Failed createTables");
                        e.printStackTrace();
                    }
                } finally {
                    poolManager.close(connection, null, null);
                }
            }
        });
    }

    public boolean tableExists(String table)
    {
        Connection connection = null;
        ResultSet rs = null;

        try {
            connection = poolManager.getConnection();
            DatabaseMetaData metaData = connection.getMetaData();
            rs = metaData.getTables(null, null, table, null);
            if (rs.next()) {
                return rs.getRow() == 1;
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        } finally {
            poolManager.close(connection, null, rs);
        }

        return false;
    }

    public synchronized void runQuery(String query)
    {
        BungeePEX.getInstance().getPlugin().doAsync(new Runnable() {
            @Override
            public void run() {
                Connection connection = null;
                Statement stmt = null;

                try {
                    connection = poolManager.getConnection();
                    stmt = connection.createStatement();
                    stmt.execute(query);
                } catch (SQLException ex) {
                    BungeePEX.getLogger().log(Level.SEVERE, "Could not run sql query", ex);
                } finally {
                    poolManager.close(connection, stmt, null);
                }

            }
        });
    }


    public static String escape(String s)
    {
        if (s == null)
        {
            return null;
        }
        String ret = s;
        ret = ret.replaceAll("\\\\", "\\\\\\\\");
        ret = ret.replaceAll("\\n", "\\\\n");
        ret = ret.replaceAll("\\r", "\\\\r");
        ret = ret.replaceAll("\\t", "\\\\t");
        ret = ret.replaceAll("\\00", "\\\\0");
        ret = ret.replaceAll("'", "\\\\'");
        ret = ret.replaceAll("\\\"", "\\\\\"");
        return ret;
    }

    public static String unescape(String s)
    {
        if (s == null)
        {
            return null;
        }
        String ret = s;
        ret = ret.replaceAll("\\\\n", "\\n");
        ret = ret.replaceAll("\\\\r", "\\r");
        ret = ret.replaceAll("\\\\t", "\\t");
        ret = ret.replaceAll("\\\\0", "\\00");
        ret = ret.replaceAll("\\\\'", "'");
        ret = ret.replaceAll("\\\\\"", "\\\"");
        ret = ret.replaceAll("\\\\\\\\", "\\\\");
        return ret;
    }


}
