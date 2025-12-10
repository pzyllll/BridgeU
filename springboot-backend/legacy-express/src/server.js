const express = require('express');
const cors = require('cors');
const config = require('./config');
const { initSchema } = require('./db');

const userRouter = require('./routes/users');
const communityRouter = require('./routes/communities');
const postRouter = require('./routes/posts');
const searchRouter = require('./routes/search');
const nlpRouter = require('./routes/nlp');

initSchema();

const app = express();
app.use(cors());
app.use(express.json());

app.get('/health', (req, res) => {
  res.json({ status: 'ok', timestamp: Date.now() });
});

app.use('/api/users', userRouter);
app.use('/api/communities', communityRouter);
app.use('/api/posts', postRouter);
app.use('/api/search', searchRouter);
app.use('/api/nlp', nlpRouter);

app.use((err, req, res, next) => {
  console.error(err);
  res.status(500).json({ error: '服务器异常' });
});

app.listen(config.port, config.host, () => {
  console.log(`API server listening on http://${config.host}:${config.port}`);
});

