const path = require('path');
require('dotenv').config({ path: path.resolve(process.cwd(), '.env') });

const ROOT_DIR = process.cwd();

module.exports = {
  port: process.env.PORT || 4000,
  host: process.env.HOST || '0.0.0.0',
  databaseFile: process.env.DB_FILE || path.join(ROOT_DIR, 'data', 'app.db'),
  embedding: {
    model: process.env.EMBEDDING_MODEL || 'local-semantic-sim',
  },
};

