const express = require('express');
const { nanoid } = require('nanoid');
const { db } = require('../db');
const { semanticScore } = require('../services/nlpService');

const router = express.Router();

router.get('/', (req, res) => {
  const { q } = req.query;
  const stmt = db.prepare('SELECT * FROM posts ORDER BY created_at DESC');
  const posts = stmt.all();

  if (!q) return res.json(posts);

  const scored = posts
    .map((post) => ({
      ...post,
      score: semanticScore(q, `${post.title} ${post.body}`),
    }))
    .filter((item) => item.score > 0)
    .sort((a, b) => b.score - a.score);

  res.json(scored);
});

router.get('/:id', (req, res) => {
  const stmt = db.prepare('SELECT * FROM posts WHERE id = ?');
  const post = stmt.get(req.params.id);
  if (!post) return res.status(404).json({ error: '帖子不存在' });
  res.json(post);
});

router.post('/', (req, res) => {
  const { communityId, authorId, title, body, tags = [], category } = req.body;
  if (!communityId || !authorId || !title || !body) {
    return res.status(400).json({ error: '缺少必填字段' });
  }
  try {
    const stmt = db.prepare(
      `INSERT INTO posts (
        id, community_id, author_id, title, body, tags, category
      ) VALUES (
        @id, @community_id, @author_id, @title, @body, @tags, @category
      )`
    );
    const post = {
      id: nanoid(),
      community_id: communityId,
      author_id: authorId,
      title,
      body,
      tags: JSON.stringify(tags),
      category: category || null,
    };
    stmt.run(post);
    res.status(201).json({ id: post.id, ...req.body });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: '创建帖子失败' });
  }
});

module.exports = router;

