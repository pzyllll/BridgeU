const express = require('express');
const { db } = require('../db');
const { answerQuestion } = require('../services/nlpService');

const router = express.Router();

router.post('/qa', (req, res) => {
  const { question } = req.body;
  if (!question) return res.status(400).json({ error: 'question 必填' });

  const posts = db.prepare('SELECT id, title, body FROM posts ORDER BY created_at DESC LIMIT 50').all();
  const result = answerQuestion(question, posts);
  res.json(result);
});

module.exports = router;

