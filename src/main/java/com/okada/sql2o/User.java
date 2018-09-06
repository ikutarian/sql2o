package com.okada.sql2o;

import java.util.Date;

public class User {

    public int id;
    public String name;
    public int age;
    public Date birthday;

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                ", birthday=" + birthday +
                '}';
    }
}
