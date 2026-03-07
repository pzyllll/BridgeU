const { nanoid } = require('nanoid');
const { db, initSchema } = require('../db');

const run = () => {
  initSchema();
  db.exec(`
    DELETE FROM messages;
    DELETE FROM conversations;
    DELETE FROM posts;
    DELETE FROM memberships;
    DELETE FROM communities;
    DELETE FROM users;
  `);

  const users = [
    {
      id: nanoid(),
      email: 'lihua@example.com',
      password: 'password123',
      display_name: '李华',
      nationality: 'China',
      studying_in_country: 'Thailand',
      institution: 'Chulalongkorn University',
      languages: JSON.stringify(['zh', 'en']),
      interests: JSON.stringify(['美食', '旅游']),
    },
    {
      id: nanoid(),
      email: 'emily@example.com',
      password: 'password123',
      display_name: 'Emily',
      nationality: 'UK',
      studying_in_country: 'South Korea',
      institution: 'Yonsei University',
      languages: JSON.stringify(['en', 'ko']),
      interests: JSON.stringify(['语言交换', '咖啡厅']),
    },
    {
      id: nanoid(),
      email: 'somchai@example.com',
      password: 'password123',
      display_name: 'Somchai',
      nationality: 'Thailand',
      studying_in_country: 'China',
      institution: 'Fudan University',
      languages: JSON.stringify(['th', 'zh']),
      interests: JSON.stringify(['科技', '创业']),
    },
  ];

  const insertUser = db.prepare(`
    INSERT INTO users (
      id, email, password, display_name, nationality,
      studying_in_country, institution, languages, interests
    ) VALUES (
      @id, @email, @password, @display_name, @nationality,
      @studying_in_country, @institution, @languages, @interests
    )
  `);
  users.forEach((user) => insertUser.run(user));

  const communities = [
    {
      id: nanoid(),
      title: '中国人在泰国留学',
      description: '分享签证、租房、美食攻略等信息',
      country: 'Thailand',
      language: 'zh',
      tags: JSON.stringify(['签证', '美食', '租房']),
      created_by: users[0].id,
    },
    {
      id: nanoid(),
      title: '英国人在韩国留学',
      description: '学校申请、课程选择、生活分享',
      country: 'South Korea',
      language: 'en',
      tags: JSON.stringify(['课程', '生活', '语言']),
      created_by: users[1].id,
    },
    {
      id: nanoid(),
      title: '泰国人在中国留学',
      description: '适应中国生活、二手交易、语言互助',
      country: 'China',
      language: 'zh',
      tags: JSON.stringify(['二手', '语言', '互助']),
      created_by: users[2].id,
    },
  ];
  const insertCommunity = db.prepare(`
    INSERT INTO communities (
      id, title, description, country, language, tags, created_by
    ) VALUES (
      @id, @title, @description, @country, @language, @tags, @created_by
    )
  `);
  communities.forEach((community) => insertCommunity.run(community));

  const posts = [
    {
      id: nanoid(),
      community_id: communities[0].id,
      author_id: users[0].id,
      title: '曼谷租房攻略',
      body: '推荐在 BTS 线附近找公寓，注意提前准备押金，和房东确认水电费用。',
      tags: JSON.stringify(['租房', '曼谷']),
      category: '生活',
    },
    {
      id: nanoid(),
      community_id: communities[1].id,
      author_id: users[1].id,
      title: '延世大学选课技巧',
      body: '热门课程要抢先注册，建议提前收藏课程。语言课和专业课都要合理搭配。',
      tags: JSON.stringify(['课程', '选课']),
      category: '学习',
    },
    {
      id: nanoid(),
      community_id: communities[2].id,
      author_id: users[2].id,
      title: '上海哪里吃泰餐',
      body: '静安寺附近有很多泰国餐厅，想家时可以去吃。也欢迎大家一起组局做饭！',
      tags: JSON.stringify(['美食', '聚会']),
      category: '社交',
    },
  ];
  const insertPost = db.prepare(`
    INSERT INTO posts (
      id, community_id, author_id, title, body, tags, category
    ) VALUES (
      @id, @community_id, @author_id, @title, @body, @tags, @category
    )
  `);
  posts.forEach((post) => insertPost.run(post));

  console.log(`Seed 完成：${users.length} 位用户，${communities.length} 个社区，${posts.length} 条帖子。`);
};

run();

