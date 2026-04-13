package com.example.exam.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.exam.common.enums.QuestionReviewStatusEnum;
import com.example.exam.common.enums.QuestionTypeEnum;
import com.example.exam.common.enums.RoleEnum;
import com.example.exam.module.course.dto.CourseCreateRequest;
import com.example.exam.module.course.entity.Course;
import com.example.exam.module.course.mapper.CourseMapper;
import com.example.exam.module.course.service.CourseService;
import com.example.exam.module.exam.dto.ExamCreateRequest;
import com.example.exam.module.exam.entity.Exam;
import com.example.exam.module.exam.service.ExamService;
import com.example.exam.module.paper.dto.PaperManualRequest;
import com.example.exam.module.paper.entity.Paper;
import com.example.exam.module.paper.service.PaperService;
import com.example.exam.module.question.dto.KnowledgePointCreateRequest;
import com.example.exam.module.question.dto.QuestionCreateRequest;
import com.example.exam.module.question.entity.KnowledgePoint;
import com.example.exam.module.question.entity.Question;
import com.example.exam.module.question.service.KnowledgePointService;
import com.example.exam.module.question.service.QuestionService;
import com.example.exam.module.system.entity.User;
import com.example.exam.module.system.mapper.UserMapper;
import com.example.exam.module.system.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 空库时写入演示账号与一门完整课程（知识点、试题、试卷、已发布考试），便于联调与验收。
 */
@Service
@RequiredArgsConstructor
public class DemoDataInitializerService {

    private static final String DEMO_TEACHER = "teacher1";
    private static final String DEMO_STUDENT = "student1";

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final UserService userService;
    private final CourseMapper courseMapper;
    private final CourseService courseService;
    private final KnowledgePointService knowledgePointService;
    private final QuestionService questionService;
    private final PaperService paperService;
    private final ExamService examService;
    private final UserDetailsService userDetailsService;

    @Transactional(rollbackFor = Exception.class)
    public void ensureDemoUsers() {
        if (userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getUsername, "admin")) > 0) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        insertUser("admin", "系统管理员", RoleEnum.ADMIN.name(), now);
        insertUser(DEMO_TEACHER, "演示教师", RoleEnum.TEACHER.name(), now);
        insertUser(DEMO_STUDENT, "演示学生", RoleEnum.STUDENT.name(), now);
    }

    private void insertUser(String username, String realName, String role, LocalDateTime now) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode("123456"));
        u.setRealName(realName);
        u.setRole(role);
        u.setStatus(1);
        u.setCreateTime(now);
        u.setUpdateTime(now);
        userService.saveUser(u);
    }

    @Transactional(rollbackFor = Exception.class)
    public void ensureDemoCourseChain() {
        if (courseMapper.selectCount(new LambdaQueryWrapper<>()) > 0) {
            return;
        }
        User teacher = userService.findByUsername(DEMO_TEACHER);
        User student = userService.findByUsername(DEMO_STUDENT);
        if (teacher == null || student == null) {
            return;
        }
        UserDetails details = userDetailsService.loadUserByUsername(DEMO_TEACHER);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(details, details.getPassword(), details.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
        try {
            CourseCreateRequest cc = new CourseCreateRequest();
            cc.setName("Java 程序设计（演示）");
            cc.setCode("DEMO-JAVA-101");
            cc.setDescription("演示用课程：含知识点、题库、试卷与一场已发布的考试。默认账号 teacher1 / student1，密码均为 123456。");
            Course course = courseService.create(cc);
            courseService.addStudent(course.getId(), student.getId());

            KnowledgePointCreateRequest k1 = new KnowledgePointCreateRequest();
            k1.setCourseId(course.getId());
            k1.setName("第一章 语言基础");
            k1.setSortOrder(1);
            KnowledgePoint kpRoot = knowledgePointService.create(k1);

            KnowledgePointCreateRequest k2 = new KnowledgePointCreateRequest();
            k2.setCourseId(course.getId());
            k2.setParentId(kpRoot.getId());
            k2.setName("1.1 数据类型与变量");
            k2.setSortOrder(1);
            KnowledgePoint kpLeaf = knowledgePointService.create(k2);

            Question q1 = createQuestion(course.getId(), kpLeaf.getId(), QuestionTypeEnum.SINGLE.name(),
                    "下列关于 Java 的描述，正确的是？",
                    "[\"Java 是编译型语言，与 C 一样直接生成机器码\",\"Java 源码编译为字节码，由 JVM 执行\",\"Java 仅能在 Windows 上运行\",\"Java 不支持面向对象\"]",
                    "B", "字节码 + JVM 是 Java 平台无关的核心。");

            Question q2 = createQuestion(course.getId(), kpLeaf.getId(), QuestionTypeEnum.MULTIPLE.name(),
                    "以下属于 Java 基本数据类型的有？（多选）",
                    "[\"int\",\"String\",\"boolean\",\"double\"]",
                    "A,C,D", "String 是引用类型。");

            Question q3 = createQuestion(course.getId(), kpLeaf.getId(), QuestionTypeEnum.TRUE_FALSE.name(),
                    "Java 中 int 与 Integer 在使用上可以完全等同，没有任何区别。",
                    "[\"正确\",\"错误\"]",
                    "B", "int 为基本类型，Integer 为包装类。");

            Question q4 = createQuestion(course.getId(), kpLeaf.getId(), QuestionTypeEnum.FILL.name(),
                    "Java 入口方法签名为 public static void main(String[] ___ )",
                    null,
                    "args", "参数名为约定俗成的 args。");

            PaperManualRequest pm = new PaperManualRequest();
            pm.setCourseId(course.getId());
            pm.setTitle("期中测验（演示卷）");
            List<PaperManualRequest.PaperManualItem> items = new ArrayList<>();
            items.add(item(q1.getId(), 25));
            items.add(item(q2.getId(), 25));
            items.add(item(q3.getId(), 25));
            items.add(item(q4.getId(), 25));
            pm.setItems(items);
            Paper paper = paperService.createManual(pm);

            ExamCreateRequest er = new ExamCreateRequest();
            er.setCourseId(course.getId());
            er.setPaperId(paper.getId());
            er.setTitle("2026 春季期中考试（演示）");
            er.setDescription("演示考试，开始时间已设为过去，结束时间为一个月后，便于随时进入答题。");
            er.setStartTime(LocalDateTime.now().minusDays(1));
            er.setEndTime(LocalDateTime.now().plusDays(30));
            er.setDurationMinutes(90);
            er.setPassScore(new BigDecimal("60"));
            er.setShuffleQuestions(true);
            er.setShuffleOptions(true);
            er.setSwitchBlurLimit(5);
            Exam exam = examService.create(er);
            examService.publish(exam.getId());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private static PaperManualRequest.PaperManualItem item(Long qid, int score) {
        PaperManualRequest.PaperManualItem it = new PaperManualRequest.PaperManualItem();
        it.setQuestionId(qid);
        it.setScore(new BigDecimal(score));
        return it;
    }

    private Question createQuestion(Long courseId, Long kpId, String type, String stem, String optionsJson,
                                    String answer, String analysis) {
        QuestionCreateRequest req = new QuestionCreateRequest();
        req.setCourseId(courseId);
        req.setKnowledgePointId(kpId);
        req.setType(type);
        req.setStem(stem);
        req.setOptionsJson(optionsJson);
        req.setAnswer(answer);
        req.setAnalysis(analysis);
        req.setScoreDefault(new BigDecimal("10"));
        req.setDifficulty(2);
        req.setReviewStatus(QuestionReviewStatusEnum.PUBLISHED.name());
        return questionService.create(req);
    }
}
