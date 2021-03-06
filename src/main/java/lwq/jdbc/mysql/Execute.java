package lwq.jdbc.mysql;

import java.util.List;

public interface Execute {


    /**
     * 查询一条记录
     * @param sql 查询语句
     * @param rClass 映射类型
     * @return 查询结果
     */
    <R> R query(String sql, Class<R> rClass) throws Exception;

    /**
     * 查询多条记录
     * @param sql 查询语句
     * @param rClass 映射类型
     * @return 查询结果
     */
    <E> List<E> queryList(String sql, Class<E> rClass) throws Exception;

    /**
     * 增删改
     * @param sql sql语句
     * @return 操作成功的记录条数
     */
    int execute(String sql) throws Exception;

    /**
     * 插入一条数据并返回自动递增的id
     * @param sql 插入语句
     * @return 插入成功返回自动递增的id，否则返回null
     */
    Integer insertReturnId(String sql) throws Exception;


    /**
     * 分页查询
     * @param sql 查询sql
     * @param rClass 映射类型
     * @param current 当前页
     * @param size 每页条数
     * @return Page对象
     */
    <E extends Entity> Page<E> getPage(String sql, Class rClass, int current, int size) throws Exception;

}
