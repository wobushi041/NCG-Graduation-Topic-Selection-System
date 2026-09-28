package cn.edu.nfu.topicselection.mapper;

import cn.edu.nfu.topicselection.model.entity.StudentTopicSelection;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 学生选题关联数据访问层接口
 *
 * @author wobushi041
 */
public interface StudentTopicSelectionMapper extends BaseMapper<StudentTopicSelection> {

    /**
     * 根据学生账号加锁查询未删除的选题记录列表
     *
     * @param userAccount 学生账号
     * @return 学生选题关联记录列表
     */
    @Select("SELECT * FROM student_topic_selection " +
            "WHERE userAccount = #{userAccount} AND isDelete = 0 FOR UPDATE")
    List<StudentTopicSelection> selectByUserForUpdate(@Param("userAccount") String userAccount);

    /**
     * 根据学生账号与题目标识加锁查询未删除的选题记录
     *
     * @param userAccount 学生账号
     * @param topicId     题目唯一标识
     * @return 学生选题关联记录
     */
    @Select("SELECT * FROM student_topic_selection " +
            "WHERE userAccount = #{userAccount} AND topicId = #{topicId} AND isDelete = 0 FOR UPDATE")
    StudentTopicSelection selectByUserAndTopicForUpdate(
            @Param("userAccount") String userAccount,
            @Param("topicId") Long topicId
    );

}
