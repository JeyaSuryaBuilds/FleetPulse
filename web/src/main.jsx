import React from 'react';
import { createRoot } from 'react-dom/client';
import './styles/index.css';
import { FleetPulseMotion } from './design-system/motion.js';
import App from './App.jsx';

createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <FleetPulseMotion><App /></FleetPulseMotion>
  </React.StrictMode>,
);
