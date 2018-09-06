package com.okada.sql2o;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class Query {

    private NamedParameterStatement statement;

    public Query(Sql2o sql2O, String sql) {
        try {
            statement = new NamedParameterStatement(sql2O.getConnection(), sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Query addParameter(String name, Object value) {
        try {
            statement.setObject(name, value);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return this;
    }

    public <T> List<T> executeAndFetch(Class<T> pojo) {
        List<T> result = new ArrayList<>();

        try {
            ResultSet resultSet = statement.executeQuery();
            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();

            while (resultSet.next()) {
                T obj = pojo.newInstance();

                for (int columnIndex = 1; columnIndex < resultSetMetaData.getColumnCount(); columnIndex++) {
                    String columnName = resultSetMetaData.getColumnName(columnIndex);
                    Object value = resultSet.getObject(columnName);

                    Field field = obj.getClass().getField(columnName);
                    field.set(obj, value);
                }

                result.add(obj);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            try {
                statement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return result;
    }
}
