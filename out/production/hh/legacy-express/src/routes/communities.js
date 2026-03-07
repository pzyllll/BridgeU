const express = require('express');
const { nanoid } = require('nanoid');
const { db } = require('../db');

const router = express.Router();

router.get('/', (req, res) => {
  const { country, language } = req.query;
  let sql = 'SELECT * FROM communities';
  const params = [];
  const conditions = [];

  if (country) {
    conditions.push('country = ?');
    params.push(country);
  }
  if (language) {
    conditions.push('(language = ? OR language IS NULL)');
    params.push(language);
  }
  if (conditions.length) {
    sql += ` WHERE ${conditions.join(' AND ')}`;
  }
  sql += ' ORDER BY created_at DESC';

  const stmt = db.prepare(sql);
  res.json(stmt.all(...params));
});

router.post('/', (req, res) => {
  const { title, description, country, language, tags = [], createdBy } = req.body;
  if (!title || !country) {
    return res.status(400).json({ error: '标题和国家必填' });
  }
  try {
    const community = {
      id: nanoid(),
      title,
      description: description || '',
      country,
      language: language || null,
      tags: JSON.stringify(tags),
      created_by: createdBy || null,
    };
    const stmt = db.prepare(
      `INSERT INTO communities (
        id, title, description, country, language, tags, created_by
      ) VALUES (@id, @title, @description, @country, @language, @tags, @created_by)`
    );
    stmt.run(community);
    res.status(201).json({ id: community.id, ...req.body });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: '创建社区失败' });
  }
});

router.get('/:id/posts', (req, res) => {
  const stmt = db.prepare('SELECT * FROM posts WHERE community_id = ? ORDER BY created_at DESC');
  res.json(stmt.all(req.params.id));
});

router.post('/:id/posts', (req, res) => {
  const { title, body, authorId, tags = [], category } = req.body;
  if (!title || !body || !authorId) {
    return res.status(400).json({ error: '标题、内容、作者必填' });
  }
  const communityId = req.params.id;
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

