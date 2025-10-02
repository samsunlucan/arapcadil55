import React, { useState } from 'react';
import {
  View,
  ScrollView,
  StyleSheet,
} from 'react-native';
import {
  Card,
  Title,
  Paragraph,
  Button,
  TextInput,
  SegmentedButtons,
  ActivityIndicator,
  DataTable,
  Text,
  useTheme,
} from 'react-native-paper';
import { apiService } from '../services/api';
import config from '../config';

export default function MetricsScreen() {
  const theme = useTheme();
  const [symbol, setSymbol] = useState('BTC');
  const [horizon, setHorizon] = useState('1h');
  const [metrics, setMetrics] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const fetchMetrics = async () => {
    if (!symbol) {
      setError('Lütfen bir sembol girin');
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const data = await apiService.getMetrics(symbol.toUpperCase(), horizon);
      setMetrics(data);
    } catch (err) {
      setError(err.message);
      setMetrics(null);
    } finally {
      setLoading(false);
    }
  };

  const renderMetricsTable = () => {
    if (!metrics) return null;

    if (metrics.n_predictions < 5) {
      return (
        <Card style={styles.infoCard}>
          <Card.Content>
            <Text style={styles.infoText}>
              ℹ️ Bu varlık için henüz yeterli tahmin verisi yok.
              {'\n\n'}
              Tahmin sayısı: {metrics.n_predictions}
              {'\n'}
              Metrikler için en az 5 tahmin gereklidir.
            </Text>
          </Card.Content>
        </Card>
      );
    }

    const getScoreColor = (value, metricType) => {
      // MAPE için: düşük iyi
      if (metricType === 'mape') {
        if (value < 5) return '#4caf50';
        if (value < 10) return '#ff9800';
        return '#f44336';
      }
      // Accuracy için: yüksek iyi
      if (metricType === 'accuracy') {
        if (value > 0.6) return '#4caf50';
        if (value > 0.5) return '#ff9800';
        return '#f44336';
      }
      // Coverage için: hedefe yakın iyi
      if (metricType === 'coverage') {
        const diff = Math.abs(value - 0.8); // 80% hedef
        if (diff < 0.1) return '#4caf50';
        if (diff < 0.2) return '#ff9800';
        return '#f44336';
      }
      return theme.colors.primary;
    };

    return (
      <View>
        <Card style={styles.metricsCard}>
          <Card.Content>
            <Title style={styles.cardTitle}>
              {metrics.symbol} - {metrics.horizon}
            </Title>
            <Paragraph style={styles.subtitle}>
              {metrics.n_predictions} tahmin üzerinden hesaplandı
            </Paragraph>

            <DataTable>
              <DataTable.Header>
                <DataTable.Title>Metrik</DataTable.Title>
                <DataTable.Title numeric>Değer</DataTable.Title>
                <DataTable.Title numeric>Durum</DataTable.Title>
              </DataTable.Header>

              {metrics.mape !== null && (
                <DataTable.Row>
                  <DataTable.Cell>MAPE (%)</DataTable.Cell>
                  <DataTable.Cell numeric>
                    {metrics.mape?.toFixed(2) || 'N/A'}
                  </DataTable.Cell>
                  <DataTable.Cell numeric>
                    <View
                      style={[
                        styles.statusDot,
                        { backgroundColor: getScoreColor(metrics.mape, 'mape') },
                      ]}
                    />
                  </DataTable.Cell>
                </DataTable.Row>
              )}

              {metrics.rmse !== null && (
                <DataTable.Row>
                  <DataTable.Cell>RMSE</DataTable.Cell>
                  <DataTable.Cell numeric>
                    {metrics.rmse?.toFixed(2) || 'N/A'}
                  </DataTable.Cell>
                  <DataTable.Cell numeric>
                    <Text style={styles.cellText}>-</Text>
                  </DataTable.Cell>
                </DataTable.Row>
              )}

              {metrics.mae !== null && (
                <DataTable.Row>
                  <DataTable.Cell>MAE</DataTable.Cell>
                  <DataTable.Cell numeric>
                    {metrics.mae?.toFixed(2) || 'N/A'}
                  </DataTable.Cell>
                  <DataTable.Cell numeric>
                    <Text style={styles.cellText}>-</Text>
                  </DataTable.Cell>
                </DataTable.Row>
              )}

              {metrics.direction_accuracy !== null && (
                <DataTable.Row>
                  <DataTable.Cell>Yön Doğruluğu</DataTable.Cell>
                  <DataTable.Cell numeric>
                    {((metrics.direction_accuracy || 0) * 100).toFixed(1)}%
                  </DataTable.Cell>
                  <DataTable.Cell numeric>
                    <View
                      style={[
                        styles.statusDot,
                        {
                          backgroundColor: getScoreColor(
                            metrics.direction_accuracy,
                            'accuracy'
                          ),
                        },
                      ]}
                    />
                  </DataTable.Cell>
                </DataTable.Row>
              )}

              {metrics.brier_score !== null && (
                <DataTable.Row>
                  <DataTable.Cell>Brier Score</DataTable.Cell>
                  <DataTable.Cell numeric>
                    {metrics.brier_score?.toFixed(3) || 'N/A'}
                  </DataTable.Cell>
                  <DataTable.Cell numeric>
                    <Text style={styles.cellText}>-</Text>
                  </DataTable.Cell>
                </DataTable.Row>
              )}

              {metrics.coverage_80 !== null && (
                <DataTable.Row>
                  <DataTable.Cell>%80 Kapsama</DataTable.Cell>
                  <DataTable.Cell numeric>
                    {((metrics.coverage_80 || 0) * 100).toFixed(1)}%
                  </DataTable.Cell>
                  <DataTable.Cell numeric>
                    <View
                      style={[
                        styles.statusDot,
                        {
                          backgroundColor: getScoreColor(
                            metrics.coverage_80,
                            'coverage'
                          ),
                        },
                      ]}
                    />
                  </DataTable.Cell>
                </DataTable.Row>
              )}

              {metrics.coverage_95 !== null && (
                <DataTable.Row>
                  <DataTable.Cell>%95 Kapsama</DataTable.Cell>
                  <DataTable.Cell numeric>
                    {((metrics.coverage_95 || 0) * 100).toFixed(1)}%
                  </DataTable.Cell>
                  <DataTable.Cell numeric>
                    <Text style={styles.cellText}>-</Text>
                  </DataTable.Cell>
                </DataTable.Row>
              )}
            </DataTable>
          </Card.Content>
        </Card>

        <Card style={styles.infoCard}>
          <Card.Content>
            <Title style={styles.infoTitle}>Metrik Açıklamaları</Title>
            <Paragraph style={styles.infoText}>
              • <Text style={styles.bold}>MAPE:</Text> Ortalama mutlak yüzde hatası. Düşük = iyi.
              {'\n\n'}
              • <Text style={styles.bold}>RMSE/MAE:</Text> Tahmin hataları. Düşük = iyi.
              {'\n\n'}
              • <Text style={styles.bold}>Yön Doğruluğu:</Text> Yükseliş/düşüş tahminlerinin doğruluğu. Yüksek = iyi.
              {'\n\n'}
              • <Text style={styles.bold}>Brier Score:</Text> Olasılık kalibrasyonu. Düşük = iyi.
              {'\n\n'}
              • <Text style={styles.bold}>Kapsama:</Text> Gerçek değerlerin güven aralığı içinde kalma oranı. Hedefe yakın = iyi.
            </Paragraph>
          </Card.Content>
        </Card>
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
          onPress={fetchMetrics}
          loading={loading}
          disabled={loading}
          style={styles.button}
          icon="chart-bar"
        >
          Performans Metrikleri
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
          <Text style={styles.loadingText}>Metrikler yükleniyor...</Text>
        </View>
      )}

      {!loading && metrics && renderMetricsTable()}
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
  metricsCard: {
    margin: 16,
    elevation: 4,
  },
  cardTitle: {
    fontSize: 20,
    fontWeight: 'bold',
  },
  subtitle: {
    fontSize: 13,
    color: '#666',
    marginBottom: 16,
  },
  statusDot: {
    width: 12,
    height: 12,
    borderRadius: 6,
  },
  cellText: {
    fontSize: 14,
  },
  infoCard: {
    margin: 16,
    marginTop: 8,
  },
  infoTitle: {
    fontSize: 16,
    marginBottom: 12,
  },
  infoText: {
    fontSize: 13,
    lineHeight: 20,
    color: '#555',
  },
  bold: {
    fontWeight: 'bold',
  },
});
