import React, { useState, useEffect } from 'react';
import {
  View,
  FlatList,
  StyleSheet,
  RefreshControl,
  TouchableOpacity,
} from 'react-native';
import {
  Card,
  Title,
  Paragraph,
  Searchbar,
  ActivityIndicator,
  Chip,
  Text,
  useTheme,
} from 'react-native-paper';
import { apiService } from '../services/api';

export default function AssetsScreen({ navigation }) {
  const theme = useTheme();
  const [assets, setAssets] = useState([]);
  const [filteredAssets, setFilteredAssets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');

  const fetchAssets = async () => {
    try {
      setLoading(true);
      const data = await apiService.getTop100Assets();
      setAssets(data);
      setFilteredAssets(data);
    } catch (error) {
      console.error('Error fetching assets:', error);
      alert(error.message);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchAssets();
  }, []);

  const onRefresh = () => {
    setRefreshing(true);
    fetchAssets();
  };

  const handleSearch = (query) => {
    setSearchQuery(query);
    
    if (query === '') {
      setFilteredAssets(assets);
    } else {
      const filtered = assets.filter(
        (asset) =>
          asset.symbol.toLowerCase().includes(query.toLowerCase()) ||
          asset.name.toLowerCase().includes(query.toLowerCase())
      );
      setFilteredAssets(filtered);
    }
  };

  const formatNumber = (num) => {
    if (num >= 1e9) return `$${(num / 1e9).toFixed(2)}B`;
    if (num >= 1e6) return `$${(num / 1e6).toFixed(2)}M`;
    if (num >= 1e3) return `$${(num / 1e3).toFixed(2)}K`;
    return `$${num.toFixed(2)}`;
  };

  const renderAssetCard = ({ item }) => {
    const priceChange = item.price_change_24h || 0;
    const isPositive = priceChange >= 0;

    return (
      <TouchableOpacity
        onPress={() =>
          navigation.navigate('Tahmin', { selectedSymbol: item.symbol })
        }
      >
        <Card style={styles.card}>
          <Card.Content>
            <View style={styles.cardHeader}>
              <View style={styles.rankBadge}>
                <Text style={styles.rankText}>#{item.market_cap_rank}</Text>
              </View>
              <View style={styles.symbolContainer}>
                <Title style={styles.symbol}>{item.symbol}</Title>
                <Paragraph style={styles.name}>{item.name}</Paragraph>
              </View>
              <View style={styles.priceContainer}>
                <Title style={styles.price}>
                  ${item.current_price.toLocaleString()}
                </Title>
                <Chip
                  mode="flat"
                  style={[
                    styles.changeChip,
                    { backgroundColor: isPositive ? '#4caf50' : '#f44336' },
                  ]}
                  textStyle={styles.changeText}
                >
                  {isPositive ? '+' : ''}
                  {priceChange.toFixed(2)}%
                </Chip>
              </View>
            </View>
            <View style={styles.stats}>
              <View style={styles.stat}>
                <Paragraph style={styles.statLabel}>Market Cap</Paragraph>
                <Paragraph style={styles.statValue}>
                  {formatNumber(item.market_cap)}
                </Paragraph>
              </View>
              <View style={styles.stat}>
                <Paragraph style={styles.statLabel}>24h Hacim</Paragraph>
                <Paragraph style={styles.statValue}>
                  {formatNumber(item.total_volume)}
                </Paragraph>
              </View>
            </View>
          </Card.Content>
        </Card>
      </TouchableOpacity>
    );
  };

  if (loading && assets.length === 0) {
    return (
      <View style={styles.centerContainer}>
        <ActivityIndicator size="large" color={theme.colors.primary} />
        <Text style={styles.loadingText}>Top 100 kripto yükleniyor...</Text>
      </View>
    );
  }

  return (
    <View style={styles.container}>
      <Searchbar
        placeholder="Ara (BTC, ETH, ...)"
        onChangeText={handleSearch}
        value={searchQuery}
        style={styles.searchbar}
      />
      
      <View style={styles.disclaimer}>
        <Text style={styles.disclaimerText}>
          ⚠️ Finansal tavsiye değildir. Sadece tahmin amaçlıdır.
        </Text>
      </View>

      <FlatList
        data={filteredAssets}
        renderItem={renderAssetCard}
        keyExtractor={(item) => item.symbol}
        refreshControl={
          <RefreshControl refreshing={refreshing} onRefresh={onRefresh} />
        }
        contentContainerStyle={styles.listContainer}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  centerContainer: {
    flex: 1,
    justifyContent: 'center',
    alignItems: 'center',
  },
  loadingText: {
    marginTop: 16,
    fontSize: 16,
  },
  searchbar: {
    margin: 16,
    elevation: 2,
  },
  disclaimer: {
    backgroundColor: '#fff3cd',
    padding: 12,
    marginHorizontal: 16,
    marginBottom: 8,
    borderRadius: 8,
    borderLeftWidth: 4,
    borderLeftColor: '#ff9800',
  },
  disclaimerText: {
    fontSize: 12,
    color: '#856404',
    textAlign: 'center',
  },
  listContainer: {
    padding: 16,
  },
  card: {
    marginBottom: 12,
    elevation: 2,
  },
  cardHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    marginBottom: 12,
  },
  rankBadge: {
    backgroundColor: '#6200ee',
    borderRadius: 20,
    width: 40,
    height: 40,
    justifyContent: 'center',
    alignItems: 'center',
    marginRight: 12,
  },
  rankText: {
    color: '#fff',
    fontWeight: 'bold',
    fontSize: 14,
  },
  symbolContainer: {
    flex: 1,
  },
  symbol: {
    fontSize: 18,
    fontWeight: 'bold',
    marginBottom: 0,
  },
  name: {
    fontSize: 12,
    color: '#666',
  },
  priceContainer: {
    alignItems: 'flex-end',
  },
  price: {
    fontSize: 16,
    fontWeight: 'bold',
    marginBottom: 4,
  },
  changeChip: {
    height: 24,
  },
  changeText: {
    color: '#fff',
    fontSize: 12,
    fontWeight: 'bold',
  },
  stats: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    paddingTop: 12,
    borderTopWidth: 1,
    borderTopColor: '#eee',
  },
  stat: {
    flex: 1,
  },
  statLabel: {
    fontSize: 11,
    color: '#999',
    marginBottom: 4,
  },
  statValue: {
    fontSize: 13,
    fontWeight: '600',
  },
});
