package cn.edu.nfu.topicselection.mapper;

import cn.edu.nfu.topicselection.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户数据访问层接口
 *
 * @author wobushi041
 */
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户唯一标识加锁查询未删除的用户记录
     *
     * @param id 用户唯一标识
     * @return 用户实体信息
     */
    @Select("SELECT * FROM `user` WHERE id = #{id} AND isDelete = 0 FOR UPDATE")
    User selectByIdForUpdate(@Param("id") Long id);

}
