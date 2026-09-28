package cn.edu.nfu.topicselection.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 文件控制器单元测试
 *
 * @author wobushi041
 */
class FileControllerTest {

    // 场景：测试可能触发公式执行的 CSV 单元格被正确转义
    @Test
    void csvCellsThatCouldExecuteFormulasAreEscaped() {
        assertEquals("'=SUM(A1:A2)", FileController.sanitizeCsvCell("=SUM(A1:A2)"));
        assertEquals("'  @command", FileController.sanitizeCsvCell("  @command"));
        assertEquals("普通文本", FileController.sanitizeCsvCell("普通文本"));
        assertEquals(42, FileController.sanitizeCsvCell(42));
    }

}
