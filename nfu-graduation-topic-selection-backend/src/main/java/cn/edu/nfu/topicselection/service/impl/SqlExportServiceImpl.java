package cn.edu.nfu.topicselection.service.impl;

import cn.edu.nfu.topicselection.exception.CodeBindMessageEnums;
import cn.edu.nfu.topicselection.service.SqlExportService;
import cn.edu.nfu.topicselection.utils.ThrowUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.io.ByteArrayOutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * SQL 查询与报表导出服务实现类
 *
 * @author wobushi041
 */
@Service
public class SqlExportServiceImpl implements SqlExportService {

    /**
     * 注入数据源依赖
     */
    @Resource
    private DataSource dataSource;

    /**
     * 从 DataSource 获取原生 JDBC 连接并执行 SQL 查询，通过 ResultSetMetaData 提取列标签与值封装为有序 Map 列表
     *
     * @param sql 待执行的 SQL 查询语句
     * @return 查询结果行数据列表
     */
    @Override
    public List<Map<String, Object>> executeQuery(String sql) {

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            List<Map<String, Object>> result = new ArrayList<>();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                result.add(row);
            }
            return result;

        } catch (SQLException e) {
            ThrowUtils.throwIf(true, CodeBindMessageEnums.SYSTEM_ERROR, "执行查询失败: " + e.getMessage());
            return null;
        }
    }

    /**
     * 调用 executeQuery 获取结果集并通过 Apache POI SXSSFWorkbook 流式写入内存字节流生成全部为文本格式的 Excel 数据
     *
     * @param sql 待执行的 SQL 查询语句
     * @return Excel 文件二进制字节数组
     */
    @Override
    public byte[] exportQueryToExcel(String sql) {
        List<Map<String, Object>> rows = executeQuery(sql);

        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("result");

            // 定义文本格式
            CellStyle textStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            textStyle.setDataFormat(format.getFormat("@")); // @ 表示文本格式

            if (!rows.isEmpty()) {
                // 写表头
                Row header = sheet.createRow(0);
                int colIdx = 0;
                for (String col : rows.get(0).keySet()) {
                    Cell cell = header.createCell(colIdx++);
                    cell.setCellStyle(textStyle);
                    cell.setCellValue(col);
                }

                // 写数据
                int rowIdx = 1;
                for (Map<String, Object> rowData : rows) {
                    Row row = sheet.createRow(rowIdx++);
                    int c = 0;
                    for (Object value : rowData.values()) {
                        Cell cell = row.createCell(c++);
                        cell.setCellStyle(textStyle); // 全部当文本
                        cell.setCellValue(value == null ? "" : value.toString());
                    }
                }
            } else {
                Row row = sheet.createRow(0);
                Cell cell = row.createCell(0);
                cell.setCellStyle(textStyle);
                cell.setCellValue("无数据");
            }

            workbook.write(baos);
            workbook.dispose();
            return baos.toByteArray();
        } catch (Exception e) {
            ThrowUtils.throwIf(true, CodeBindMessageEnums.SYSTEM_ERROR, "生成 Excel 失败: " + e.getMessage());
            return null;
        }
    }

}
