import React, { useState, useEffect } from 'react';
import {
  View,
  ScrollView,
  StyleSheet,
  Dimensions,
} from 'react-native';
import {
  Card,
  Title,
  Paragraph,
  Button,
  TextInput,
  SegmentedButtons,
  ActivityIndicator,
  Chip,
  Text,
  useTheme,
  ProgressBar,
} from 'react-native-paper';
import { apiService } from '../services/api';
import config from '../config';

const { width } = Dimensions.get('window');

export default function ForecastScreen({ route }) {
  const theme = useTheme();
  const [symbol, setSymbol] = useState(route.params?.selectedSymbol || 'BTC');
  const [horizon, setHorizon] = useState('1h');
  const [forecast, setForecast] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchForecast = async () => {
    if (!symbol) {
      setError('Lütfen bir sembol girin');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const data = await apiService.getForecast(symbol.toUpperCase(), horizon);
      setForecast(data);
    } catch (err) {
      setError(err.message);
      setForecast(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (route.params?.selectedSymbol) {
      setSymbol(route.params.selectedSymbol);
    }
  }, [route.params?.selectedSymbol]);

  const renderForecastCard = () => {
    if (!forecast) return null;

    const currentPrice = forecast.point_forecast / (1 + 0.01); // Approximation
    const change = forecast.point_forecast - currentPrice;
    const changePct = (change / currentPrice) * 100;
    const isUp = changePct > 0;

    return (
      <View>
        {/* Ana Tahmin */}
        <Card style={styles.forecastCard}>
          <Card.Content>
            <View style={styles.forecastHeader}>
              <Title style={styles.forecastSymbol}>{forecast.symbol}</Title>
              <Chip
                mode="flat"
                style={[
                  styles.horizonChip,
                  { backgroundColor: theme.colors.primary },
                ]}
                textStyle={{ color: '#fff' }}
              >
                {forecast.horizon}
              </Chip>
            </View>

            <View style={styles.priceSection}>
              <Paragraph style={styles.label}>Tahmin Edilen Fiyat</Paragraph>
              <Title style={styles.forecastPrice}>
                ${forecast.point_forecast.toLocaleString(undefined, {
                  minimumFractionDigits: 2,
                  maximumFractionDigits: 2,
                })}
              </Title>
              <Chip
                mode="flat"
                style={[
                  styles.changeChip,
                  { backgroundColor: isUp ? '#4caf50' : '#f44336' },
                ]}
                icon={isUp ? 'arrow-up' : 'arrow-down'}
                textStyle={{ color: '#fff', fontWeight: 'bold' }}
              >
                {isUp ? '+' : ''}
                {changePct.toFixed(2)}%
              </Chip>
            </View>

            <View style={styles.directionSection}>
              <Paragraph style={styles.label}>Yükseliş Olasılığı</Paragraph>
              <View style={styles.probabilityBar}>
                <ProgressBar
                  progress={forecast.direction_prob_up}
                  color={forecast.direction_prob_up > 0.5 ? '#4caf50' : '#f44336'}
                  style={styles.progressBar}
                />
                <Text style={styles.probabilityText}>
                  {(forecast.direction_prob_up * 100).toFixed(1)}%
                </Text>
              </View>
            </View>

            <View style={styles.confidenceSection}>
              <Paragraph style={styles.label}>Güven Skoru</Paragraph>
              <View style={styles.probabilityBar}>
                <ProgressBar
                  progress={forecast.confidence}
                  color={theme.colors.secondary}
                  style={styles.progressBar}
                />
                <Text style={styles.probabilityText}>
                  {(forecast.confidence * 100).toFixed(1)}%
                </Text>
              </View>
            </View>
          </Card.Content>
        </Card>

        {/* Güven Aralıkları */}
        <Card style={styles.intervalCard}>
          <Card.Content>
            <Title style={styles.sectionTitle}>Güven Aralıkları</Title>
            
            <View style={styles.intervalRow}>
              <Text style={styles.intervalLabel}>%80 Aralık:</Text>
              <Text style={styles.intervalValue}>
                ${forecast.interval_80[0].toFixed(2)} - ${forecast.interval_80[1].toFixed(2)}
              </Text>
            </View>

            <View style={styles.intervalRow}>
              <Text style={styles.intervalLabel}>%95 Aralık:</Text>
              <Text style={styles.intervalValue}>
                ${forecast.interval_95[0].toFixed(2)} - ${forecast.interval_95[1].toFixed(2)}
              </Text>
            </View>
          </Card.Content>
        </Card>

        {/* Analiz */}
        <Card style={styles.analysisCard}>
          <Card.Content>
            <Title style={styles.sectionTitle}>Analiz</Title>
            <Paragraph style={styles.rationale}>{forecast.rationale}</Paragraph>
            
            <View style={styles.featuresSection}>
              <Paragraph style={styles.featuresLabel}>
                Kullanılan Göstergeler:
              </Paragraph>
              <View style={styles.featureChips}>
                {forecast.features_used.map((feature, index) => (
                  <Chip key={index} style={styles.featureChip} mode="outlined">
                    {feature}
                  </Chip>
                ))}
              </View>
            </View>

            {forecast.learning_action !== 'no_update' && (
              <View style={styles.learningBadge}>
                <Text style={styles.learningText}>
                  🔄 Model Durumu: {forecast.learning_action === 'retrain_needed' 
                    ? 'Yeniden eğitim gerekli' 
                    : 'Ağırlıklar güncelleniyor'}
                </Text>
              </View>
            )}
          </Card.Content>
        </Card>

        {/* Timestamp */}
        <Text style={styles.timestamp}>
          Tahmin Zamanı: {new Date(forecast.timestamp).toLocaleString('tr-TR')}
        </Text>
      </View>
    );
  };

  return (
    <ScrollView style={styles.container}>
      <View style={styles.inputSection}>
        <TextInput
          label="Kripto Sembolü"
          value={symbol}
          onChangeText={setSymbol}
          mode="outlined"
          style={styles.input}
          placeholder="BTC, ETH, SOL..."
          autoCapitalize="characters"
        />

        <SegmentedButtons
          value={horizon}
          onValueChange={setHorizon}
          buttons={config.HORIZONS.map((h) => ({
            value: h.value,
            label: h.label,
          }))}
          style={styles.segmented}
        />

        <Button
          mode="contained"
          onPress={fetchForecast}
          loading={loading}
          disabled={loading}
          style={styles.button}
          icon="chart-line"
        >
          Tahmin Al
        </Button>
      </View>

      {error && (
        <Card style={[styles.errorCard, { backgroundColor: '#ffebee' }]}>
          <Card.Content>
            <Text style={{ color: '#c62828' }}>❌ {error}</Text>
          </Card.Content>
        </Card>
      )}

      {loading && (
        <View style={styles.loadingContainer}>
          <ActivityIndicator size="large" color={theme.colors.primary} />
          <Text style={styles.loadingText}>
            {symbol} için tahmin hesaplanıyor...
          </Text>
        </View>
      )}

      {!loading && forecast && renderForecastCard()}
      
      <View style={styles.disclaimer}>
        <Text style={styles.disclaimerText}>
          ⚠️ UYARI: Bu tahminler olasılıksaldır ve kesin değildir. 
          Kripto para piyasaları son derece volatildir. 
          Yatırım kararlarınızı kendi araştırmanıza dayandırın. 
          Bu uygulama finansal tavsiye SAĞLAMAZ.
        </Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  inputSection: {
    padding: 16,
    backgroundColor: '#fff',
    elevation: 2,
  },
  input: {
    marginBottom: 16,
  },
  segmented: {
    marginBottom: 16,
  },
  button: {
    paddingVertical: 4,
  },
  errorCard: {
    margin: 16,
  },
  loadingContainer: {
    padding: 40,
    alignItems: 'center',
  },
  loadingText: {
    marginTop: 16,
    fontSize: 14,
    color: '#666',
  },
  forecastCard: {
    margin: 16,
    marginBottom: 8,
    elevation: 4,
  },
  forecastHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  forecastSymbol: {
    fontSize: 24,
    fontWeight: 'bold',
  },
  horizonChip: {
    height: 32,
  },
  priceSection: {
    alignItems: 'center',
    paddingVertical: 16,
    borderBottomWidth: 1,
    borderBottomColor: '#eee',
  },
  label: {
    fontSize: 12,
    color: '#666',
    marginBottom: 4,
  },
  forecastPrice: {
    fontSize: 32,
    fontWeight: 'bold',
    marginVertical: 8,
  },
  changeChip: {
    marginTop: 8,
  },
  directionSection: {
    marginTop: 16,
  },
  confidenceSection: {
    marginTop: 12,
  },
  probabilityBar: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 8,
  },
  progressBar: {
    flex: 1,
    height: 8,
    borderRadius: 4,
  },
  probabilityText: {
    marginLeft: 12,
    fontWeight: 'bold',
    fontSize: 14,
    minWidth: 50,
  },
  intervalCard: {
    marginHorizontal: 16,
    marginBottom: 8,
    elevation: 2,
  },
  sectionTitle: {
    fontSize: 16,
    marginBottom: 12,
  },
  intervalRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingVertical: 8,
    borderBottomWidth: 1,
    borderBottomColor: '#f0f0f0',
  },
  intervalLabel: {
    fontSize: 14,
    color: '#666',
  },
  intervalValue: {
    fontSize: 14,
    fontWeight: '600',
  },
  analysisCard: {
    marginHorizontal: 16,
    marginBottom: 8,
    elevation: 2,
  },
  rationale: {
    fontSize: 14,
    lineHeight: 20,
    marginBottom: 16,
  },
  featuresSection: {
    marginTop: 8,
  },
  featuresLabel: {
    fontSize: 12,
    color: '#666',
    marginBottom: 8,
  },
  featureChips: {
    flexDirection: 'row',
    flexWrap: 'wrap',
  },
  featureChip: {
    marginRight: 8,
    marginBottom: 8,
  },
  learningBadge: {
    backgroundColor: '#e3f2fd',
    padding: 12,
    borderRadius: 8,
    marginTop: 12,
  },
  learningText: {
    fontSize: 12,
    color: '#1976d2',
  },
  timestamp: {
    textAlign: 'center',
    fontSize: 11,
    color: '#999',
    marginVertical: 8,
  },
  disclaimer: {
    backgroundColor: '#fff3cd',
    padding: 16,
    margin: 16,
    borderRadius: 8,
    borderLeftWidth: 4,
    borderLeftColor: '#ff9800',
  },
  disclaimerText: {
    fontSize: 12,
    color: '#856404',
    lineHeight: 18,
  },
});
