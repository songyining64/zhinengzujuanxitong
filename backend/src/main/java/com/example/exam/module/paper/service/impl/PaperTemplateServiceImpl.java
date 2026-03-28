package com.example.exam.module.paper.service.impl;

import com.example.exam.common.api.ErrorCode;
import com.example.exam.common.exception.BizException;
import com.example.exam.common.security.SecurityHelper;
import com.example.exam.module.course.service.CoursePermissionService;
import com.example.exam.module.paper.dto.PaperAutoGenRequest;
import com.example.exam.module.paper.dto.PaperTemplateSaveRequest;
import com.example.exam.module.paper.entity.PaperTemplate;
import com.example.exam.module.paper.mapper.PaperTemplateMapper;
import com.example.exam.module.paper.service.PaperTemplateService;
import com.example.exam.module.system.entity.User;
import com.example.exam.module.system.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaperTemplateServiceImpl implements PaperTemplateService {

    private final PaperTemplateMapper paperTemplateMapper;
    private final CoursePermissionService coursePermissionService;
    private final UserService userService;
    private final ObjectMapper objectMapper;

    private User me() {
        User u = userService.findByUsername(SecurityHelper.requireUsername());
        if (u == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        return u;
    }

    private void validateRules(PaperAutoGenRequest rules) {
        if (rules.getCountByType() == null || rules.getCountByType().isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请配置各题型数量");
        }
        boolean randomPool = Boolean.TRUE.equals(rules.getRandomPool());
        if (!randomPool && (rules.getKnowledgePointIds() == null || rules.getKnowledgePointIds().isEmpty())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "请指定知识点或开启随机全课程抽题");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaperTemplate create(PaperTemplateSaveRequest req) {
        coursePermissionService.assertCourseManageable(req.getCourseId());
        PaperAutoGenRequest rules = req.getRules();
        rules.setCourseId(req.getCourseId());
        validateRules(rules);
        if (rules.getTitle() == null || rules.getTitle().isBlank()) {
            rules.setTitle("TEMPLATE");
        }
        PaperTemplate t = new PaperTemplate();
        t.setCourseId(req.getCourseId());
        t.setName(req.getName());
        try {
            rules.setTitle(null);
            t.setRulesJson(objectMapper.writeValueAsString(rules));
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "规则序列化失败");
        }
        t.setCreatorId(me().getId());
        t.setCreateTime(LocalDateTime.now());
        t.setUpdateTime(LocalDateTime.now());
        paperTemplateMapper.insert(t);
        return paperTemplateMapper.selectById(t.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaperTemplate update(Long id, PaperTemplateSaveRequest req) {
        PaperTemplate existing = paperTemplateMapper.selectById(id);
        if (existing == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "模板不存在");
        }
        coursePermissionService.assertCourseManageable(existing.getCourseId());
        if (!existing.getCourseId().equals(req.getCourseId())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "不可修改课程");
        }
        PaperAutoGenRequest rules = req.getRules();
        rules.setCourseId(req.getCourseId());
        validateRules(rules);
        existing.setName(req.getName());
        try {
            rules.setTitle(null);
            existing.setRulesJson(objectMapper.writeValueAsString(rules));
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "规则序列化失败");
        }
        existing.setUpdateTime(LocalDateTime.now());
        paperTemplateMapper.updateById(existing);
        return paperTemplateMapper.selectById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        PaperTemplate t = paperTemplateMapper.selectById(id);
        if (t == null) {
            return;
        }
        coursePermissionService.assertCourseManageable(t.getCourseId());
        paperTemplateMapper.deleteById(id);
    }

    @Override
    public List<PaperTemplate> listByCourse(Long courseId) {
        coursePermissionService.assertCourseReadable(courseId);
        return paperTemplateMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PaperTemplate>()
                        .eq(PaperTemplate::getCourseId, courseId)
                        .orderByDesc(PaperTemplate::getUpdateTime));
    }

    @Override
    public PaperTemplate get(Long id) {
        PaperTemplate t = paperTemplateMapper.selectById(id);
        if (t == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "模板不存在");
        }
        coursePermissionService.assertCourseReadable(t.getCourseId());
        return t;
    }
}
