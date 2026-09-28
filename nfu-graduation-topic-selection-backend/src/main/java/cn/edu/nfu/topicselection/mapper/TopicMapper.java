package cn.edu.nfu.topicselection.mapper;

import cn.edu.nfu.topicselection.model.entity.Topic;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 毕业设计题目数据访问层接口
 *
 * @author wobushi041
 */
public interface TopicMapper extends BaseMapper<Topic> {

    /**
     * 根据题目唯一标识加锁查询未删除的题目记录
     *
     * @param id 题目唯一标识
     * @return 题目实体信息
     */
    @Select("SELECT * FROM topic WHERE id = #{id} AND isDelete = 0 FOR UPDATE")
    Topic selectByIdForUpdate(@Param("id") Long id);

}
