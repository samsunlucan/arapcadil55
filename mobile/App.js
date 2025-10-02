import React, { useState, useEffect } from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { Provider as PaperProvider, MD3LightTheme } from 'react-native-paper';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import Icon from 'react-native-vector-icons/MaterialCommunityIcons';

import AssetsScreen from './src/screens/AssetsScreen';
import ForecastScreen from './src/screens/ForecastScreen';
import MetricsScreen from './src/screens/MetricsScreen';

const Tab = createBottomTabNavigator();

const theme = {
  ...MD3LightTheme,
  colors: {
    ...MD3LightTheme.colors,
    primary: '#6200ee',
    secondary: '#03dac6',
    background: '#f5f5f5',
  },
};

export default function App() {
  return (
    <SafeAreaProvider>
      <PaperProvider theme={theme}>
        <NavigationContainer>
          <Tab.Navigator
            screenOptions={({ route }) => ({
              tabBarIcon: ({ focused, color, size }) => {
                let iconName;

                if (route.name === 'Varlıklar') {
                  iconName = focused ? 'bitcoin' : 'bitcoin';
                } else if (route.name === 'Tahmin') {
                  iconName = focused ? 'chart-line' : 'chart-line-variant';
                } else if (route.name === 'Performans') {
                  iconName = focused ? 'chart-bar' : 'chart-bar';
                }

                return <Icon name={iconName} size={size} color={color} />;
              },
              tabBarActiveTintColor: theme.colors.primary,
              tabBarInactiveTintColor: 'gray',
              headerStyle: {
                backgroundColor: theme.colors.primary,
              },
              headerTintColor: '#fff',
              headerTitleStyle: {
                fontWeight: 'bold',
              },
            })}
          >
            <Tab.Screen 
              name="Varlıklar" 
              component={AssetsScreen}
              options={{ title: 'Top 100 Kripto' }}
            />
            <Tab.Screen 
              name="Tahmin" 
              component={ForecastScreen}
              options={{ title: 'Tahmin Al' }}
            />
            <Tab.Screen 
              name="Performans" 
              component={MetricsScreen}
              options={{ title: 'Model Performansı' }}
            />
          </Tab.Navigator>
        </NavigationContainer>
      </PaperProvider>
    </SafeAreaProvider>
  );
}
