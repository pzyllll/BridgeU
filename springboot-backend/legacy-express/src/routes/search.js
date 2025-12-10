const express = require('express');
const { db } = require('../db');
const { semanticScore } = require('../services/nlpService');

const router = express.Router();

router.get('/', (req, res) => {
  const { q } = req.query;
  if (!q) return res.status(400).json({ error: '缺少搜索关键词 q' });

  const postStmt = db.prepare('SELECT * FROM posts');
  const communityStmt = db.prepare('SELECT * FROM communities');

  const posts = postStmt.all().map((post) => ({
    ...post,
    score: semanticScore(q, `${post.title} ${post.body}`),
  }));
  const communities = communityStmt.all().map((community) => ({
    ...community,
    score: semanticScore(q, `${community.title} ${community.description}`),
  }));

  res.json({
    query: q,
    posts: posts.filter((item) => item.score > 0).sort((a, b) => b.score - a.score).slice(0, 10),
    communities: communities
      .filter((item) => item.score > 0)
      .sort((a, b) => b.score - a.score)
      .slice(0, 10),
  });
});

module.exports = router;

