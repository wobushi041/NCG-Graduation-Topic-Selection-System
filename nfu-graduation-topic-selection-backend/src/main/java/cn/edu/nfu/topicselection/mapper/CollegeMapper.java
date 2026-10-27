package cn.edu.nfu.topicselection.mapper;

import cn.edu.nfu.topicselection.model.entity.College;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 学院数据访问层接口
 *
 * @author wobushi041
 */
public interface CollegeMapper extends BaseMapper<College> {

    /**
     * 根据学院名称查询学院实体（包含已被逻辑删除的记录）
     *
     * @param collegeName 学院名称
     * @return 学院实体信息
     */
    @Select("SELECT * FROM college WHERE collegeName = #{collegeName} LIMIT 1")
    College selectByCollegeName(@Param("collegeName") String collegeName);

    /**
     * 恢复已被逻辑删除的学院记录（将 isDelete 重置为 0）
     *
     * @param id 学院唯一标识
     * @return 影响行数
     */
    @Update("UPDATE college SET isDelete = 0 WHERE id = #{id}")
    int restoreById(@Param("id") Long id);

}
