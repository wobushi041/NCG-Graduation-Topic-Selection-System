package cn.com.edtechhub.worktopicselection.service.impl;

import cn.com.edtechhub.worktopicselection.mapper.StudentTopicSelectionMapper;
import cn.com.edtechhub.worktopicselection.model.entity.StudentTopicSelection;
import cn.com.edtechhub.worktopicselection.service.StudentTopicSelectionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 学生选题关联业务服务实现类
 *
 * @author wobushi041
 */
@Service
@Transactional
public class StudentTopicSelectionServiceImpl extends ServiceImpl<StudentTopicSelectionMapper, StudentTopicSelection>
        implements StudentTopicSelectionService {

}
