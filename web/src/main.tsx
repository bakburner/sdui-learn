import React from 'react';
import ReactDOM from 'react-dom/client';
import './nba-tokens.css';
import App from './App';
import { initializeColorSchemePreference } from './utils/ColorTokenResolver';

initializeColorSchemePreference();

// Add spinner animation
const style = document.createElement('style');
style.textContent = `
  @keyframes spin {
    to { transform: rotate(360deg); }
  }
  @keyframes sduiTabContentFade {
    from { opacity: 0.35; }
    to { opacity: 1; }
  }
  @keyframes toastIn {
    from { opacity: 0; }
    to { opacity: 1; }
  }
`;
document.head.appendChild(style);

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
