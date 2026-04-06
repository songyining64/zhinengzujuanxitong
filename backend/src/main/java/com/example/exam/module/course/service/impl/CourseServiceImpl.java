package com.example.exam.module.course.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.exam.common.api.ErrorCode;
import com.example.exam.common.enums.RoleEnum;
import com.example.exam.common.exception.BizException;
import com.example.exam.common.security.SecurityHelper;
import com.example.exam.module.course.dto.CourseCreateRequest;
import com.example.exam.module.course.dto.CourseVO;
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
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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
        if (RoleEnum.TEACHER.name().equals(u.getRole())) {
            teacherId = u.getId();
        } else {
            if (req.getTeacherId() != null) {
                assertAssignableInstructor(req.getTeacherId());
                teacherId = req.getTeacherId();
            } else {
                teacherId = u.getId();
            }
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
        if (req.getTeacherId() != null) {
            User u = me();
            if (!RoleEnum.ADMIN.name().equals(u.getRole())) {
                throw new BizException(ErrorCode.FORBIDDEN, "仅管理员可修改授课教师");
            }
            assertAssignableInstructor(req.getTeacherId());
            c.setTeacherId(req.getTeacherId());
        }
        c.setUpdateTime(LocalDateTime.now());
        courseMapper.updateById(c);
        return c;
    }

    private void assertAssignableInstructor(Long userId) {
        User t = userMapper.selectById(userId);
        if (t == null) {
            throw new BizException(ErrorCode.BAD_REQUEST, "授课教师用户不存在");
        }
        if (!RoleEnum.TEACHER.name().equals(t.getRole()) && !RoleEnum.ADMIN.name().equals(t.getRole())) {
            throw new BizException(ErrorCode.BAD_REQUEST, "授课教师须为教师或管理员角色");
        }
        if (t.getStatus() == null || t.getStatus() != 1) {
            throw new BizException(ErrorCode.BAD_REQUEST, "该账号已禁用，不可设为授课教师");
        }
    }

    private String resolveTeacherName(User t) {
        if (t == null) return null;
        if (t.getRealName() != null && !t.getRealName().isBlank()) {
            return t.getRealName();
        }
        if (t.getUsername() != null && !t.getUsername().isBlank()) {
            return t.getUsername();
        }
        // 理论上不太会走到这里，但兜底避免前端拿到空值
        return String.valueOf(t.getId());
    }

    @Override
    public CourseVO get(Long id) {
        coursePermissionService.assertCourseReadable(id);
        Course c = courseMapper.selectById(id);
        if (c == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "课程不存在");
        }
        CourseVO vo = new CourseVO();
        vo.setId(c.getId());
        vo.setTeacherId(c.getTeacherId());
        vo.setTeacherName(resolveTeacherName(userMapper.selectById(c.getTeacherId())));
        vo.setName(c.getName());
        vo.setDescription(c.getDescription());
        vo.setCode(c.getCode());
        vo.setStatus(c.getStatus());
        vo.setCreateTime(c.getCreateTime());
        vo.setUpdateTime(c.getUpdateTime());
        return vo;
    }

    @Override
    public Page<CourseVO> pageForCurrentUser(long page, long size, String keyword) {
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
        List<Course> courses = p.getRecords();
        List<Long> teacherIds = courses.stream()
                .map(Course::getTeacherId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<User> teachers = teacherIds.isEmpty() ? List.of() : userMapper.selectBatchIds(teacherIds);
        Map<Long, User> teacherMap = teachers.stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<CourseVO> vos = courses.stream().map(c -> {
            CourseVO vo = new CourseVO();
            vo.setId(c.getId());
            vo.setTeacherId(c.getTeacherId());
            // 理论上 teacherMap 一定能命中，但为避免序列化/映射异常导致 teacherName 为空，这里做兜底查询
            User teacher = teacherMap.get(c.getTeacherId());
            if (teacher == null && c.getTeacherId() != null) {
                teacher = userMapper.selectById(c.getTeacherId());
            }
            vo.setTeacherName(resolveTeacherName(teacher));
            vo.setName(c.getName());
            vo.setDescription(c.getDescription());
            vo.setCode(c.getCode());
            vo.setStatus(c.getStatus());
            vo.setCreateTime(c.getCreateTime());
            vo.setUpdateTime(c.getUpdateTime());
            return vo;
        }).toList();

        Page<CourseVO> pv = new Page<>(p.getCurrent(), p.getSize());
        pv.setTotal(p.getTotal());
        pv.setRecords(vos);
        return pv;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        coursePermissionService.assertCourseManageable(id);
        // 这里直接物理删除；课程相关业务（知识点/试题/试卷）若需要级联需再做约束/清理。
        courseMapper.deleteById(id);
    }
}
