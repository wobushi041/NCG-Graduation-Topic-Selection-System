package cn.edu.nfu.topicselection.service.impl;

import cn.edu.nfu.topicselection.mapper.StudentTopicSelectionMapper;
import cn.edu.nfu.topicselection.model.entity.StudentTopicSelection;
import cn.edu.nfu.topicselection.service.StudentTopicSelectionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * 学生选题关联业务服务实现类
 *
 * @author wobushi041
 */
@Service
public class StudentTopicSelectionServiceImpl extends ServiceImpl<StudentTopicSelectionMapper, StudentTopicSelection>
        implements StudentTopicSelectionService {

}
