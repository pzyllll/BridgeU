const express = require('express');
const { nanoid } = require('nanoid');
const { db } = require('../db');

const router = express.Router();

router.get('/', (req, res) => {
  const stmt = db.prepare('SELECT * FROM users ORDER BY created_at DESC');
  res.json(stmt.all());
});

router.post('/', (req, res) => {
  const {
    email,
    password,
    displayName,
    nationality,
    studyingInCountry,
    institution,
    languages = [],
    interests = [],
  } = req.body;

  if (!email || !password || !displayName || !nationality || !studyingInCountry) {
    return res.status(400).json({ error: '缺少必填字段' });
  }

  try {
    const stmt = db.prepare(
      `INSERT INTO users (
        id, email, password, display_name, nationality,
        studying_in_country, institution, languages, interests
      ) VALUES (
        @id, @email, @password, @display_name, @nationality,
        @studying_in_country, @institution, @languages, @interests
      )`
    );

    const user = {
      id: nanoid(),
      email,
      password,
      display_name: displayName,
      nationality,
      studying_in_country: studyingInCountry,
      institution: institution || null,
      languages: JSON.stringify(languages),
      interests: JSON.stringify(interests),
    };

    stmt.run(user);
    res.status(201).json({ id: user.id, ...req.body });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: '创建用户失败' });
  }
});

router.get('/:id', (req, res) => {
  const stmt = db.prepare('SELECT * FROM users WHERE id = ?');
  const user = stmt.get(req.params.id);
  if (!user) return res.status(404).json({ error: '用户不存在' });
  res.json(user);
});

module.exports = router;

