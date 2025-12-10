const naturalSynonyms = {
  吃饭: ['吃饭', '用餐', '就餐', '餐馆', '餐饮', '烹饪', '饭堂'],
  租房: ['租房', '住宿', '公寓', '房源', '宿舍'],
  课程: ['课程', '课表', '课堂', '教学', '选课'],
  签证: ['签证', '移民', '入境', '海关', '居留证'],
  二手: ['二手', '闲置', '转卖', '交易'],
};

const tokenize = (text = '') =>
  text
    .toLowerCase()
    .replace(/[^\p{L}\p{N}\s]/gu, ' ')
    .split(/\s+/)
    .filter(Boolean);

const getSemanticBag = (text = '') => {
  const tokens = tokenize(text);
  const extended = new Set(tokens);

  tokens.forEach((token) => {
    Object.values(naturalSynonyms).forEach((synonyms) => {
      if (synonyms.includes(token)) {
        synonyms.forEach((candidate) => extended.add(candidate));
      }
    });
  });

  return Array.from(extended);
};

const semanticScore = (query, target) => {
  const queryTokens = getSemanticBag(query);
  const targetTokens = getSemanticBag(target);

  const overlap = queryTokens.filter((token) => targetTokens.includes(token));
  const score = overlap.length / Math.max(1, queryTokens.length);
  return score;
};

const summarizePosts = (posts = []) => {
  if (!posts.length) return '暂无可用答案。';
  const highlights = posts
    .slice(0, 3)
    .map(
      (post, index) =>
        `${index + 1}. ${post.title}：${post.body.slice(0, 120)}${post.body.length > 120 ? '...' : ''}`
    )
    .join('\n');
  return `以下是社区的热门建议：\n${highlights}`;
};

const answerQuestion = (question, posts) => {
  const scored = posts
    .map((post) => ({
      ...post,
      score: semanticScore(question, `${post.title} ${post.body}`),
    }))
    .sort((a, b) => b.score - a.score);

  const top = scored.filter((item) => item.score > 0).slice(0, 3);
  const summary = summarizePosts(top.length ? top : posts.slice(0, 3));
  return {
    answer: summary,
    references: top.map(({ id, title, score }) => ({ id, title, score })),
  };
};

module.exports = {
  semanticScore,
  summarizePosts,
  answerQuestion,
};

