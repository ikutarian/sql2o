package com.okada.sql2o;

import java.util.List;

public class Main {

    public static void main(String[] args) {
        Sql2o sql2o = new Sql2o("jdbc:mysql://127.0.0.1:8889/mario_sample", "root", "root");
        List<User> users = sql2o.createQuery("select * from t_user where name is :name")
                .addParameter("name", null)
                .executeAndFetch(User.class);
        System.out.println(users);
    }
}
