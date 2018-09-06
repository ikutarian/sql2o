package com.okada.sql2o;

import java.sql.PreparedStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NamedParameterStatement {

    private final PreparedStatement statement;

    /**
     * key: name
     * value: columnIndexes
     */
    private final Map<String, List<Integer>> nameColumnIndexesMap = new HashMap<>();

    public NamedParameterStatement(Connection connection, String sql) throws SQLException {
        String parsedSql = parseSql(sql);
        statement = connection.prepareStatement(parsedSql);
    }

    /**
     * 处理传入的sql
     * <p>
     * 传入：INSERT INTO user (id, name, email) VALUES (:id, :name, :email)<br/>
     * 得到：INSERT INTO user (id, name, email) VALUES (?, ?, ?)
     */
    private String parseSql(String sql) {
        int length = sql.length();
        StringBuilder parsedSql = new StringBuilder();

        int columnIndex = 1;

        for (int i = 0; i < length; i++) {
            char currChar = sql.charAt(i);  // 当前字符
            int nextIndex = i + 1;  // 下一个索引

            if (currChar == ':' && nextIndex < length) {  // 当前字符为：，并且下一个索引不越界
                char nextChar = sql.charAt(nextIndex);  // 获取下一个字符
                if (Character.isJavaIdentifierStart(nextChar)) {  // 下一个字符是合法的java标识符
                    // 遍历字符串，直到下一个非合法的java标识符
                    int j = nextIndex;
                    while (j < length && Character.isJavaIdentifierPart(sql.charAt(j))) {
                        j++;
                    }
                    String name = sql.substring(i + 1, j);
                    i += name.length();  // i 位置向前进
                    parsedSql.append('?');  // 替换

                    // name - columnIndexes
                    List<Integer> columnIndexes = nameColumnIndexesMap.get(name);
                    if (columnIndexes == null) {
                        columnIndexes = new ArrayList<>();
                    }
                    columnIndexes.add(columnIndex);
                    nameColumnIndexesMap.put(name, columnIndexes);
                    columnIndex++;
                }
            } else {
                parsedSql.append(currChar);
            }
        }

        return parsedSql.toString();
    }

    public void setObject(String name, Object value) throws SQLException {
        for (int columnIndex : getColumnIndexesBy(name)) {
            if (value != null) {
                statement.setObject(columnIndex, value);
            } else {
                statement.setNull(columnIndex, Types.OTHER);
            }
        }
    }

    private List<Integer> getColumnIndexesBy(String name) {
        List<Integer> columnIndexes = nameColumnIndexesMap.get(name);
        if (columnIndexes == null) {
            throw new IllegalArgumentException("参数未找到: " + name + "，请检查sql语句");
        }
        return columnIndexes;
    }

    public ResultSet executeQuery() throws SQLException {
        return statement.executeQuery();
    }

    public int executeUpdate() throws SQLException {
        return statement.executeUpdate();
    }

    public void close() throws SQLException {
        statement.close();
    }
}
