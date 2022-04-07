package lwq.jdbc.mysql;

import lwq.jdbc.utils.ArrayUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class JDBC {

    private String url;
    private String username;
    private String password;

    public List<Connection> cons;


    public JDBC(String path) {
        try {
            Yaml yaml = new Yaml();
            InputStream in = new FileInputStream(path);
            Map config = yaml.loadAs(in, Map.class);
            this.config(config);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void config(Map config) {
        Map datasource = (Map) config.get("datasource");
        String driver = (String) datasource.get("driver");
        try {
            Class.forName(driver);
            this.url = String.valueOf(datasource.get("url"));
            this.username = String.valueOf(datasource.get("username"));
            this.password = String.valueOf(datasource.get("password"));
            if(datasource.get("maxCount") != null){
                this.loadConnection((Integer) datasource.get("maxCount"));
            }else{
                this.loadConnection(100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadConnection(int count){
        this.cons = new ArrayList<Connection>();
        for (int i = 0; i < count; i++) {
            try {
                cons.add(DriverManager.getConnection(url, username, password));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    protected Connection getConnection(){
        synchronized (cons){
            if(cons.size() < 1){
                try {
                    cons.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return cons.remove(cons.size()-1);
        }
    }

    protected void free(Connection conn){
        synchronized (cons){
            cons.add(conn);
            cons.notify();
        }
    }

    /**
     * 查询一条记录
     * @param sql 查询语句
     * @param rClass 映射类型
     * @return 查询结果
     */
    public <R> R query(String sql, Class<R> rClass){
        R res = null;
        Connection conn = null;
        Statement statement = null;
        try {
            conn = getConnection();
            statement = conn.createStatement();
            ResultSet result = statement.executeQuery(sql);
            if(result.next()){
                res = rClass.newInstance();
                this.setFieldValue(res,result);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                statement.close();
                this.free(conn);
            }catch (Exception e){
                e.printStackTrace();
            }
            return res;
        }
    }

    /**
     * 查询多条记录
     * @param sql 查询语句
     * @param rClass 映射类型
     * @return 查询结果
     */
    public <E> List<E> queryList(String sql, Class<E> rClass){
        List<E> res = null;
        Connection conn = null;
        Statement statement = null;
        try {
            conn = getConnection();
            statement = conn.createStatement();
            ResultSet result = statement.executeQuery(sql);
            while(result.next()) {
                if(res == null){
                    res = new ArrayList<>();
                }
                E obj = rClass.newInstance();
                this.setFieldValue(obj, result);
                res.add(obj);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                statement.close();
                this.free(conn);
            }catch (Exception e){
                e.printStackTrace();
            }
            return res;
        }
    }

    /**
     * 增删改
     * @param sql sql语句
     * @return 操作成功的记录条数
     */
    public int execute(String sql){
        int res = 0;
        Connection conn = null;
        Statement statement = null;
        try {
            conn = getConnection();
            statement = conn.createStatement();
            res = statement.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }finally {
            try {
                statement.close();
                this.free(conn);
            }catch (Exception e){
                e.printStackTrace();
            }
            return res;
        }
    }

    /**
     * 插入一条数据并返回自动递增的id
     * @param sql 插入语句
     * @return 插入成功返回自动递增的id，否则返回null
     */
    public Integer insertReturnId(String sql){
        Integer res = null;
        Connection conn = null;
        Statement statement = null;
        try {
            conn = getConnection();
            statement = conn.createStatement();
            statement.executeUpdate(sql);
            ResultSet result = statement.executeQuery("select LAST_INSERT_ID() id");
            result.next();
            res = result.getInt("id");
        } catch (SQLException e) {
            e.printStackTrace();
            return 0;
        }finally {
            try {
                statement.close();
                this.free(conn);
            }catch (Exception e){
                e.printStackTrace();
            }
            return res;
        }
    }


    protected Field[] getFields(Class cls){
        if(cls == null){
            return null;
        }
        Field[] fields = cls.getDeclaredFields();
        return ArrayUtils.concat(fields,this.getFields(cls.getSuperclass()));
    }

    protected void setFieldValue(Object res, ResultSet result){
        Field[] fields = getFields(res.getClass());
        for (Field field : fields) {
            field.setAccessible(true);
            String type;
            if(field.getType() == Integer.class){
                type = "int";
            }else{
                String[] typeStr = field.getType().toString().split("\\.");
                type = typeStr[typeStr.length-1];
            }
            type = type.substring(0,1).toUpperCase()+type.substring(1);
            String methonName = "get" + type;
            try{
                Method method;
                Object value;
                if(field.getType() == char.class){
                    method = result.getClass().getDeclaredMethod("getString",String.class);
                    value = ((String)method.invoke(result, field.getName())).charAt(0);
                }else{
                    method = result.getClass().getDeclaredMethod(methonName,String.class);
                    value = method.invoke(result, field.getName());
                }
                field.set(res,value);
            }catch (Exception e){
                return;
            }
        }
    }

}
