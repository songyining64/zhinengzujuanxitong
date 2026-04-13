package com.example.exam.module.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.common.api.ErrorCode;
import com.example.exam.common.enums.RoleEnum;
import com.example.exam.common.exception.BizException;
import com.example.exam.common.security.SecurityHelper;
import com.example.exam.module.course.dto.CourseCreateRequest;
import com.example.exam.module.course.dto.CourseStudentVO;
import com.example.exam.module.course.dto.CourseUpdateRequest;
import com.example.exam.module.course.entity.Course;
import com.example.exam.module.course.entity.CourseStudent;
import com.example.exam.module.course.mapper.CourseMapper;
import com.example.exam.module.course.mapper.CourseStudentMapper;
import com.example.exam.module.course.service.CoursePermissionService;
import com.example.exam.module.course.service.CourseService;
import com.example.exam.module.system.entity.User;
import com.example.exam.module.system.mapper.UserMapper;
import com.example.exam.module.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseServiceImpl implements CourseService {

    private final CourseMapper courseMapper;
    private final CourseStudentMapper courseStudentMapper;
    private final UserService userService;
    private final UserMapper userMapper;
    private final CoursePermissionService coursePermissionService;

    private User me() {
        User u = userService.findByUsername(SecurityHelper.requireUsername());
        if (u == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "用户不存在");
        }
        return u;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Course create(CourseCreateRequest req) {
        User u = me();
        if (!RoleEnum.ADMIN.name().equals(u.getRole()) && !RoleEnum.TEACHER.name().equals(u.getRole())) {
            throw new BizException(ErrorCode.FORBIDDEN, "仅教师或管理员可创建课程");
        }
        Long teacherId;
        if (RoleEnum.ADMIN.name().equals(u.getRole())) {
            if (req.getTeacherId() == null) {
                throw new BizException(ErrorCode.BAD_REQUEST, "管理员创建课程时请指定授课教师 teacherId");
            }
            User assignee = userMapper.selectById(req.getTeacherId());
            if (assignee == null || !RoleEnum.TEACHER.name().equals(assignee.getRole())) {
                throw new BizException(ErrorCode.BAD_REQUEST, "请选择有效的教师用户作为授课教师");
            }
            teacherId = req.getTeacherId();
        } else {
            teacherId = u.getId();
        }
        Course c = new Course();
        c.setTeacherId(teacherId);
        c.setName(req.getName());
        c.setDescription(req.getDescription());
        c.setCode(req.getCode());
        c.setStatus(1);
        c.setCreateTime(LocalDateTime.now());
        c.setUpdateTime(LocalDateTime.now());
        courseMapper.insert(c);
        return c;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Course update(Long id, CourseUpdateRequest req) {
        coursePermissionService.assertCourseManageable(id);
        Course c = courseMapper.selectById(id);
        if (c == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "课程不存在");
        }
        if (req.getName() != null) {
            c.setName(req.getName());
        }
        if (req.getDescription() != null) {
            c.setDescription(req.getDescription());
        }
        if (req.getCode() != null) {
            c.setCode(req.getCode());
        }
        if (req.getStatus() != null) {
            c.setStatus(req.getStatus());
        }
        c.setUpdateTime(LocalDateTime.now());
        courseMapper.updateById(c);
        return c;
    }

    @Override
    public Course get(Long id) {
        coursePermissionService.assertCourseReadable(id);
        Course c = courseMapper.selectById(id);
        if (c == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "课程不存在");
        }
        return c;
    }

    @Override
    public Page<Course> pageForCurrentUser(long page, long size, String keyword) {
        User u = me();
        LambdaQueryWrapper<Course> w = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            w.and(q -> q.like(Course::getName, keyword).or().like(Course::getCode, keyword));
        }
        if (RoleEnum.ADMIN.name().equals(u.getRole())) {
            // list all
        } else if (RoleEnum.TEACHER.name().equals(u.getRole())) {
            w.eq(Course::getTeacherId, u.getId());
        } else if (RoleEnum.STUDENT.name().equals(u.getRole())) {
            List<Long> ids = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                            .eq(CourseStudent::getStudentId, u.getId())
                            .eq(CourseStudent::getStatus, 1)).stream()
                    .map(CourseStudent::getCourseId)
                    .distinct()
                    .toList();
            if (ids.isEmpty()) {
                return new Page<>(page, size, 0);
            }
            w.in(Course::getId, ids);
        }
        Page<Course> p = new Page<>(page, size);
        courseMapper.selectPage(p, w.orderByDesc(Course::getCreateTime));
        return p;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addStudent(Long courseId, Long studentId) {
        coursePermissionService.assertCourseManageable(courseId);
        User student = userMapper.selectById(studentId);
        if (student == null || !RoleEnum.STUDENT.name().equals(student.getRole())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "无效的学生用户");
        }
        CourseStudent cs = courseStudentMapper.selectOne(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, courseId)
                .eq(CourseStudent::getStudentId, studentId));
        if (cs != null) {
            cs.setStatus(1);
            courseStudentMapper.updateById(cs);
            return;
        }
        CourseStudent row = new CourseStudent();
        row.setCourseId(courseId);
        row.setStudentId(studentId);
        row.setStatus(1);
        row.setCreateTime(LocalDateTime.now());
        courseStudentMapper.insert(row);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeStudent(Long courseId, Long studentId) {
        coursePermissionService.assertCourseManageable(courseId);
        CourseStudent cs = courseStudentMapper.selectOne(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, courseId)
                .eq(CourseStudent::getStudentId, studentId));
        if (cs != null) {
            cs.setStatus(0);
            courseStudentMapper.updateById(cs);
        }
    }

    @Override
    public List<CourseStudentVO> listStudents(Long courseId) {
        coursePermissionService.assertCourseReadable(courseId);
        List<CourseStudent> list = courseStudentMapper.selectList(new LambdaQueryWrapper<CourseStudent>()
                .eq(CourseStudent::getCourseId, courseId)
                .eq(CourseStudent::getStatus, 1));
        List<CourseStudentVO> vos = new ArrayList<>();
        for (CourseStudent cs : list) {
            User su = userMapper.selectById(cs.getStudentId());
            CourseStudentVO vo = new CourseStudentVO();
            vo.setId(cs.getId());
            vo.setStudentId(cs.getStudentId());
            vo.setStatus(cs.getStatus());
            vo.setJoinTime(cs.getCreateTime());
            if (su != null) {
                vo.setUsername(su.getUsername());
                vo.setRealName(su.getRealName());
            }
            vos.add(vo);
        }
        return vos;
    }
}
