package com.iit.dp.dp_pos.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnect {
    private static DBConnect instance;
    private final String url = "jdbc:sqlite:pos.db";

    private DBConnect() {}

    public static DBConnect getInstance() throws SQLException {
        if (instance == null /*|| instance.getConnection().isClosed()*/) {
            instance = new DBConnect();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }
} 