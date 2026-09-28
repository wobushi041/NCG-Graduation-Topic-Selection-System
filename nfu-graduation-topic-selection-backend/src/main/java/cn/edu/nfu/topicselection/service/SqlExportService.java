package cn.edu.nfu.topicselection.service;

import java.util.List;
import java.util.Map;

/**
 * SQL 查询与报表导出服务接口
 *
 * @author wobushi041
 */
public interface SqlExportService {

    /**
     * 执行自定义 SQL 查询并返回结构化数据列表
     *
     * @param sql 待执行的 SQL 查询语句
     * @return 查询结果行数据列表
     */
    List<Map<String, Object>> executeQuery(String sql);

    /**
     * 执行自定义 SQL 查询并将结果集导出为 Excel 文件字节数组
     *
     * @param sql 待执行的 SQL 查询语句
     * @return Excel 文件二进制字节数组
     */
    byte[] exportQueryToExcel(String sql);

}
