// API Configuration
// Geliştirme için localhost, production için sunucu IP'niz
const API_BASE_URL = __DEV__ 
  ? 'http://10.0.2.2:8000'  // Android emulator için
  : 'https://your-server.com';  // Production

export default {
  API_BASE_URL,
  
  ENDPOINTS: {
    TOP_100: '/assets/top100',
    FORECAST: '/forecast',
    METRICS: '/metrics',
    HEALTH: '/health',
  },
  
  HORIZONS: [
    { value: '15m', label: '15 Dakika' },
    { value: '1h', label: '1 Saat' },
    { value: '4h', label: '4 Saat' },
    { value: '1d', label: '1 Gün' },
  ],
  
  REFRESH_INTERVAL: 60000, // 1 dakika
};
