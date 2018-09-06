## 使用方法

准备一张用户表 `t_user`

```sql
CREATE TABLE `t_user` (
  `id` int(10) NOT NULL AUTO_INCREMENT,
  `name` varchar(50),
  `age` tinyint(4),
  `birthday` datetime,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;
```

然后插入几条数据，其中一条数据没有用户名

```sql
INSERT INTO t_user (name, age, birthday) VALUES ('jack', 1, '1991-01-01 01:01:01');
INSERT INTO t_user (name, age, birthday) VALUES ('rose', 2, '1992-02-02 02:02:02');
INSERT INTO t_user (age, birthday) VALUES (3, '1993-03-03 03:03:03');
```

接着是操作数据库的代码。连接数据库，查询用户名不存在的用户

```java
public class Main {

    public static void main(String[] args) {
        Sql2o sql2o = new Sql2o("jdbc:mysql://127.0.0.1:8889/mario_sample", "root", "root");
        List<User> users = sql2o.createQuery("select * from t_user where name is :name")
                .addParameter("name", null)
                .executeAndFetch(User.class);
        System.out.println(users);
    }
}
```

输出

```
[User{id=3, name='null', age=3, birthday=null}]
```

## 实现难点

用户只需要传入 sql 语句

```sql
select * from t_user where name is :name
```

其中参数使用类似于 `:name` 的表示。然后是指定参数的值，只需要调用 `addParameter` 方法即可

```java
addParameter("name", null)
```

接着就是查询操作，将查询结果映射到指定的 POJO

```java
executeAndFetch(User.class);
```

以上操作合起来，代码如下

```java
List<User> users = sql2o.createQuery("select * from t_user where name is :name")
                .addParameter("name", null)
                .executeAndFetch(User.class);
```

要实现这样的效果，有三个难点：

1. `select * from t_user where name is :name` 转换成 `select * from t_user where name is ?`
2. 把参数，比如 `name` 填充到 `?` 中
3. 查询结果映射到指定的 POJO

下面来详细说明这些功能要如何实现

## SQL转换

遍历 sql 字符串，遇到 `:` 字符，就进行判断和转换成 `?`。实现代码如下

```java
private String parseSql(String sql) {
    int length = sql.length();
    StringBuilder parsedSql = new StringBuilder();

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
            }
        } else {
            parsedSql.append(currChar);
        }
    }

    return parsedSql.toString();
}
```

## 参数填充

经过上一步的“SQL转换”之后，将 `select * from t_user where name is :name` 转换成 `select * from t_user where name is ?`。接下来就是把参数填充到 `?` 占位符中

可以利用 `Map` 这样的数据结构，以参数名 `name` 为 key，以 `columnIndexes` 数组为 value，在进行“SQL转换”遍历 sql 字符串串时，得到 `<name, columnIndexes>` 的数据

```java
private String parseSql(String sql) {
    // name - columnIndexes
    List<Integer> columnIndexes = nameColumnIndexesMap.get(name);
    if (columnIndexes == null) {
        columnIndexes = new ArrayList<>();
    }
    columnIndexes.add(columnIndex);
    nameColumnIndexesMap.put(name, columnIndexes);
    columnIndex++;
}
```

用户调用 `addParameter`时，`addParameter` 内部调用 `setObject` 方法把参数填充进去

```java
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
```

## 查询结果映射

其实就是把 `ResultSet` 的结果映射到 POJO，利用到的技术是反射。核心代码如下

```java
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
```

目前这段代码有很大的缺陷，只能支持属性为 `public` 的 POJO，比如

```java
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
```

这个问题我需要再学学反射的知识之后，再接下来的版本解决