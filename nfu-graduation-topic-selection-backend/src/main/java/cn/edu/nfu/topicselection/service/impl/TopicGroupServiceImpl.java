package cn.edu.nfu.topicselection.service.impl;

import cn.edu.nfu.topicselection.mapper.TopicGroupMapper;
import cn.edu.nfu.topicselection.model.entity.TopicGroup;
import cn.edu.nfu.topicselection.service.TopicGroupService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 选题组业务服务实现类
 *
 * @author wobushi041
 */
@Service
public class TopicGroupServiceImpl extends ServiceImpl<TopicGroupMapper, TopicGroup>
        implements TopicGroupService {

}
