import { useState } from 'react';
import { askQuestion } from '../api';

const NlpAssistant = () => {
  const [question, setQuestion] = useState('');
  const [answer, setAnswer] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleAsk = async () => {
    if (!question.trim()) return;
    setLoading(true);
    try {
      const data = await askQuestion({ question });
      setAnswer(data);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card" style={{ maxWidth: '800px', margin: '0 auto', boxShadow: '8px 8px 0px rgba(0,0,0,1)' }}>
      <h2 className="section-title">🤖 AI Assistant</h2>
      <div className="flex" style={{ marginBottom: '1rem', alignItems: 'flex-start' }}>
        <textarea
          className="input"
          style={{ flex: 1, minHeight: '80px' }}
          placeholder="Ask a question, e.g. Where to eat cheap in Bangkok? How to find Thai friends in Shanghai?"
          value={question}
          onChange={(e) => setQuestion(e.target.value)}
        />
        <button className="btn btn-primary" onClick={handleAsk} style={{ height: '80px' }}>
          {loading ? 'Generating...' : 'Ask'}
        </button>
      </div>
      {answer && (
        <div style={{ background: '#f9f9f9', border: '2px solid #333', padding: '1rem' }}>
          <div style={{ borderLeft: '4px solid #2563eb', paddingLeft: '1rem', marginBottom: '1rem' }}>
            <p style={{ whiteSpace: 'pre-line', fontFamily: 'Georgia, serif', lineHeight: 1.7 }}>
              {answer.answer}
            </p>
          </div>
          <div style={{ borderTop: '2px dashed #333', paddingTop: '1rem' }}>
            <strong style={{ fontSize: '0.875rem' }}>📚 Reference Posts:</strong>
            <ul style={{ margin: '0.5rem 0', paddingLeft: '1.5rem' }}>
              {answer.references.map((ref) => (
                <li key={ref.id} style={{ fontSize: '0.875rem', marginBottom: '4px' }}>
                  {ref.title}
                  <span style={{ color: '#7c3aed', marginLeft: '0.5rem' }}>(Score {ref.score.toFixed(2)})</span>
                </li>
              ))}
            </ul>
          </div>
        </div>
      )}
    </div>
  );
};

export default NlpAssistant;
