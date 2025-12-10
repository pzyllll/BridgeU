package com.globalbuddy.bootstrap;

import com.globalbuddy.model.AppUser;
import com.globalbuddy.model.Community;
import com.globalbuddy.model.CommunityPost;
import com.globalbuddy.model.News;
import com.globalbuddy.repository.AppUserRepository;
import com.globalbuddy.repository.CommunityPostRepository;
import com.globalbuddy.repository.CommunityRepository;
import com.globalbuddy.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final CommunityRepository communityRepository;
    private final CommunityPostRepository postRepository;
    private final NewsRepository newsRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }
        
        // Seed admin user
        AppUser admin = AppUser.createAdmin("admin", "admin@globalbuddy.com", passwordEncoder.encode("admin123"), "管理员");
        admin.setNationality("China");
        admin.setStudyingInCountry("China");
        admin.setInstitution("GlobalBuddy");
        admin.setLanguages(Arrays.asList("zh", "en"));
        admin.setInterests(Arrays.asList("管理", "审核"));
        userRepository.save(admin);
        
        // Seed users
        AppUser user1 = AppUser.create("lihua", "lihua@example.com", passwordEncoder.encode("password123"), "李华");
        user1.setNationality("China");
        user1.setStudyingInCountry("Thailand");
        user1.setInstitution("Chulalongkorn University");
        user1.setLanguages(Arrays.asList("zh", "en"));
        user1.setInterests(Arrays.asList("美食", "旅游"));
        AppUser user2 = AppUser.create("emily", "emily@example.com", passwordEncoder.encode("password123"), "Emily");
        user2.setNationality("UK");
        user2.setStudyingInCountry("South Korea");
        user2.setInstitution("Yonsei University");
        user2.setLanguages(Arrays.asList("en", "ko"));
        user2.setInterests(Arrays.asList("语言交换", "咖啡厅"));
        AppUser user3 = AppUser.create("somchai", "somchai@example.com", passwordEncoder.encode("password123"), "Somchai");
        user3.setNationality("Thailand");
        user3.setStudyingInCountry("China");
        user3.setInstitution("Fudan University");
        user3.setLanguages(Arrays.asList("th", "zh"));
        user3.setInterests(Arrays.asList("科技", "创业"));

        userRepository.saveAll(Arrays.asList(user1, user2, user3));

        // Seed communities
        Community c1 = Community.create("中国人在泰国留学", "分享签证、租房、美食攻略等信息", "Thailand", "zh");
        c1.setTags(Arrays.asList("签证", "美食", "租房"));
        c1.setCreatedBy(user1);
        
        Community c2 = Community.create("英国人在韩国留学", "学校申请、课程选择、生活分享", "South Korea", "en");
        c2.setTags(Arrays.asList("课程", "生活", "语言"));
        c2.setCreatedBy(user2);
        
        Community c3 = Community.create("泰国人在中国留学", "适应中国生活、二手交易、语言互助", "China", "zh");
        c3.setTags(Arrays.asList("二手", "语言", "互助"));
        c3.setCreatedBy(user3);

        communityRepository.saveAll(Arrays.asList(c1, c2, c3));

        // Seed posts
        CommunityPost p1 = new CommunityPost();
        p1.setId(java.util.UUID.randomUUID().toString());
        p1.setCommunity(c1);
        p1.setAuthor(user1);
        p1.setTitle("曼谷租房攻略");
        p1.setBody("推荐在 BTS 线附近找公寓，注意提前准备押金，和房东确认水电费用。");
        p1.setTags(Arrays.asList("租房", "曼谷"));
        p1.setCategory("生活");
        p1.setStatus(CommunityPost.Status.APPROVED);
        
        CommunityPost p2 = new CommunityPost();
        p2.setId(java.util.UUID.randomUUID().toString());
        p2.setCommunity(c2);
        p2.setAuthor(user2);
        p2.setTitle("延世大学选课技巧");
        p2.setBody("热门课程要提前卡位，建议关注选课开放时间，并提前准备好备选计划。");
        p2.setTags(Arrays.asList("课程", "选课"));
        p2.setCategory("学习");
        p2.setStatus(CommunityPost.Status.APPROVED);
        
        CommunityPost p3 = new CommunityPost();
        p3.setId(java.util.UUID.randomUUID().toString());
        p3.setCommunity(c3);
        p3.setAuthor(user3);
        p3.setTitle("上海哪里吃泰餐");
        p3.setBody("静安寺附近有很多泰国餐厅，想家时可以去吃，味道很接近家乡。");
        p3.setTags(Arrays.asList("美食", "生活"));
        p3.setCategory("社交");
        p3.setStatus(CommunityPost.Status.APPROVED);
        
        // 待审核的帖子
        CommunityPost p4 = new CommunityPost();
        p4.setId(java.util.UUID.randomUUID().toString());
        p4.setCommunity(c1);
        p4.setAuthor(user1);
        p4.setTitle("泰国留学签证申请教程");
        p4.setBody("详细介绍如何申请泰国学生签证，包括所需材料和流程。");
        p4.setTags(Arrays.asList("签证", "留学"));
        p4.setCategory("生活");
        p4.setStatus(CommunityPost.Status.PENDING_REVIEW);
        p4.setAiResult("待审核");
        p4.setAiConfidence(0.4);
        
        CommunityPost p5 = new CommunityPost();
        p5.setId(java.util.UUID.randomUUID().toString());
        p5.setCommunity(c2);
        p5.setAuthor(user2);
        p5.setTitle("韩国二手物品交易");
        p5.setBody("有人需要二手家具吗？低价转让，联系方式: xxx");
        p5.setTags(Arrays.asList("二手", "交易"));
        p5.setCategory("生活");
        p5.setStatus(CommunityPost.Status.PENDING_REVIEW);
        p5.setAiResult("AI 无法确定内容是否合规");
        p5.setAiConfidence(0.35);

        postRepository.saveAll(Arrays.asList(p1, p2, p3, p4, p5));

        // 创建一些测试用的清迈大学新闻（如果数据库中没有清迈大学新闻）
        List<News> existingCmuNews = newsRepository.findBySource("清迈大学 (CMU)");
        if (existingCmuNews == null || existingCmuNews.isEmpty()) {
            System.out.println("创建测试用的清迈大学新闻数据...");
            
            News cmuNews1 = News.builder()
                    .title("清迈大学举办国际学生交流活动")
                    .originalUrl("https://www.cmu.ac.th/en/news/international-student-exchange")
                    .source("清迈大学 (CMU)")
                    .summary("清迈大学近期举办了大型国际学生交流活动，吸引了来自世界各地的留学生参与。")
                    .originalContent("清迈大学（CMU）于本月举办了盛大的国际学生交流活动。此次活动旨在促进不同文化背景的学生之间的交流与理解。活动包括文化展示、学术研讨会、校园参观等多个环节，为国际学生提供了深入了解泰国文化和清迈大学学术环境的机会。")
                    .createTime(new Date())
                    .publishDate(new Date())
                    .build();
            
            News cmuNews2 = News.builder()
                    .title("清迈大学新增留学生奖学金项目")
                    .originalUrl("https://www.cmu.ac.th/en/news/new-scholarship-program")
                    .source("清迈大学 (CMU)")
                    .summary("清迈大学宣布推出新的留学生奖学金项目，为优秀国际学生提供学费减免和生活补助。")
                    .originalContent("清迈大学近日宣布启动新的国际学生奖学金项目。该项目面向全球优秀学生，提供全额或部分学费减免，以及每月生活补助。申请者需要具备优秀的学术成绩和语言能力。奖学金覆盖本科、硕士和博士等多个学位层次。")
                    .createTime(new Date())
                    .publishDate(new Date())
                    .build();
            
            News cmuNews3 = News.builder()
                    .title("清迈大学与多所国际大学签署合作协议")
                    .originalUrl("https://www.cmu.ac.th/en/news/international-partnerships")
                    .source("清迈大学 (CMU)")
                    .summary("清迈大学与来自中国、韩国、日本等国家的多所知名大学签署了学术交流合作协议。")
                    .originalContent("清迈大学在推进国际化进程中取得重要进展，与多所国际知名大学签署了合作协议。这些协议将促进学生交换、教师互访、联合研究等多个领域的合作。合作院校包括中国的清华大学、韩国的首尔大学、日本的东京大学等。")
                    .createTime(new Date())
                    .publishDate(new Date())
                    .build();
            
            newsRepository.saveAll(Arrays.asList(cmuNews1, cmuNews2, cmuNews3));
            System.out.println("已创建 3 条测试用的清迈大学新闻");
        }
    }
}

