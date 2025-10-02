import axios from 'axios';
import config from '../config';

const api = axios.create({
  baseURL: config.API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor (log için)
api.interceptors.request.use(
  (config) => {
    console.log(`[API Request] ${config.method.toUpperCase()} ${config.url}`);
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor (error handling)
api.interceptors.response.use(
  (response) => {
    return response;
  },
  (error) => {
    console.error('[API Error]', error.response?.data || error.message);
    return Promise.reject(error);
  }
);

// API Methods
export const apiService = {
  // Top 100 kripto para listesi
  getTop100Assets: async () => {
    try {
      const response = await api.get(config.ENDPOINTS.TOP_100);
      return response.data;
    } catch (error) {
      throw new Error('Varlık listesi yüklenemedi: ' + error.message);
    }
  },

  // Tahmin al
  getForecast: async (symbol, horizon = '1h') => {
    try {
      const response = await api.get(config.ENDPOINTS.FORECAST, {
        params: { symbol, horizon },
      });
      return response.data;
    } catch (error) {
      if (error.response?.status === 404) {
        throw new Error(`${symbol} için veri bulunamadı`);
      }
      throw new Error('Tahmin alınamadı: ' + error.message);
    }
  },

  // Performans metrikleri
  getMetrics: async (symbol, horizon = '1h') => {
    try {
      const response = await api.get(config.ENDPOINTS.METRICS, {
        params: { symbol, horizon },
      });
      return response.data;
    } catch (error) {
      throw new Error('Metrikler alınamadı: ' + error.message);
    }
  },

  // Health check
  checkHealth: async () => {
    try {
      const response = await api.get(config.ENDPOINTS.HEALTH);
      return response.data;
    } catch (error) {
      throw new Error('Sunucu yanıt vermiyor');
    }
  },
};

export default api;
