package com.okada.sql2o;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Sql2o {

    private String url;
    private String user;
    private String password;
    private Connection connection;

    public Sql2o(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
        createConnection();
    }

    private void createConnection() {
        try {
            this.connection = DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public Query createQuery(String sql) {
        return new Query(this, sql);
    }
}
