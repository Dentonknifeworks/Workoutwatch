import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  Alert,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import AsyncStorage from '@react-native-async-storage/async-storage';

interface WorkoutHistory {
  date: string;
  rounds: number;
  totalRounds: number;
  workTime: number;
  restTime: number;
}

export default function HistoryScreen() {
  const [history, setHistory] = useState<WorkoutHistory[]>([]);
  const [stats, setStats] = useState({
    totalWorkouts: 0,
    totalRounds: 0,
    totalMinutes: 0,
  });

  useEffect(() => {
    loadHistory();
  }, []);

  const loadHistory = async () => {
    try {
      const saved = await AsyncStorage.getItem('workoutHistory');
      if (saved) {
        const historyData: WorkoutHistory[] = JSON.parse(saved);
        setHistory(historyData);
        calculateStats(historyData);
      }
    } catch (error) {
      console.error('Error loading history:', error);
    }
  };

  const calculateStats = (historyData: WorkoutHistory[]) => {
    const totalWorkouts = historyData.length;
    const totalRounds = historyData.reduce((sum, workout) => sum + workout.rounds, 0);
    const totalMinutes = historyData.reduce(
      (sum, workout) =>
        sum + Math.floor((workout.rounds * (workout.workTime + workout.restTime)) / 60),
      0
    );

    setStats({ totalWorkouts, totalRounds, totalMinutes });
  };

  const clearHistory = () => {
    Alert.alert(
      'Clear History',
      'Are you sure you want to delete all workout history? This cannot be undone.',
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Clear All',
          style: 'destructive',
          onPress: async () => {
            await AsyncStorage.removeItem('workoutHistory');
            setHistory([]);
            setStats({ totalWorkouts: 0, totalRounds: 0, totalMinutes: 0 });
          },
        },
      ]
    );
  };

  const formatDate = (dateString: string) => {
    const date = new Date(dateString);
    const today = new Date();
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);

    if (date.toDateString() === today.toDateString()) {
      return 'Today';
    } else if (date.toDateString() === yesterday.toDateString()) {
      return 'Yesterday';
    } else {
      return date.toLocaleDateString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
      });
    }
  };

  const formatTime = (dateString: string) => {
    const date = new Date(dateString);
    return date.toLocaleTimeString('en-US', {
      hour: 'numeric',
      minute: '2-digit',
    });
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Workout History</Text>
        {history.length > 0 && (
          <TouchableOpacity onPress={clearHistory}>
            <Ionicons name="trash-outline" size={24} color="#FF3B30" />
          </TouchableOpacity>
        )}
      </View>

      <View style={styles.statsContainer}>
        <View style={styles.statCard}>
          <Ionicons name="trophy" size={32} color="#FFD60A" />
          <Text style={styles.statValue}>{stats.totalWorkouts}</Text>
          <Text style={styles.statLabel}>Workouts</Text>
        </View>
        <View style={styles.statCard}>
          <Ionicons name="repeat" size={32} color="#00D9FF" />
          <Text style={styles.statValue}>{stats.totalRounds}</Text>
          <Text style={styles.statLabel}>Rounds</Text>
        </View>
        <View style={styles.statCard}>
          <Ionicons name="time" size={32} color="#4CD964" />
          <Text style={styles.statValue}>{stats.totalMinutes}</Text>
          <Text style={styles.statLabel}>Minutes</Text>
        </View>
      </View>

      <ScrollView style={styles.historyList}>
        {history.length === 0 ? (
          <View style={styles.emptyState}>
            <Ionicons name="bar-chart-outline" size={64} color="#444" />
            <Text style={styles.emptyText}>No workout history</Text>
            <Text style={styles.emptySubtext}>
              Complete your first workout to see it here
            </Text>
          </View>
        ) : (
          history.map((workout, index) => (
            <View key={index} style={styles.historyCard}>
              <View style={styles.historyHeader}>
                <View style={styles.dateContainer}>
                  <Ionicons name="calendar" size={20} color="#00D9FF" />
                  <Text style={styles.dateText}>{formatDate(workout.date)}</Text>
                </View>
                <Text style={styles.timeText}>{formatTime(workout.date)}</Text>
              </View>

              <View style={styles.historyDetails}>
                <View style={styles.historyDetailItem}>
                  <Text style={styles.detailLabel}>Rounds Completed</Text>
                  <Text style={styles.detailValue}>
                    {workout.rounds} / {workout.totalRounds}
                  </Text>
                </View>
                <View style={styles.historyDetailItem}>
                  <Text style={styles.detailLabel}>Work / Rest</Text>
                  <Text style={styles.detailValue}>
                    {workout.workTime}s / {workout.restTime}s
                  </Text>
                </View>
              </View>

              {workout.rounds >= workout.totalRounds && (
                <View style={styles.completeBadge}>
                  <Ionicons name="checkmark-circle" size={16} color="#4CD964" />
                  <Text style={styles.completeText}>Completed</Text>
                </View>
              )}
            </View>
          ))
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#0c0c0c',
  },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: 20,
    paddingTop: 40,
  },
  title: {
    fontSize: 32,
    fontWeight: 'bold',
    color: '#fff',
  },
  statsContainer: {
    flexDirection: 'row',
    padding: 20,
    gap: 12,
  },
  statCard: {
    flex: 1,
    backgroundColor: '#1a1a1a',
    borderRadius: 16,
    padding: 16,
    alignItems: 'center',
    borderWidth: 1,
    borderColor: '#333',
  },
  statValue: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#fff',
    marginTop: 8,
  },
  statLabel: {
    fontSize: 12,
    color: '#888',
    marginTop: 4,
  },
  historyList: {
    flex: 1,
    padding: 20,
  },
  emptyState: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingTop: 60,
  },
  emptyText: {
    fontSize: 20,
    color: '#666',
    marginTop: 16,
  },
  emptySubtext: {
    fontSize: 16,
    color: '#444',
    marginTop: 8,
    textAlign: 'center',
  },
  historyCard: {
    backgroundColor: '#1a1a1a',
    borderRadius: 16,
    padding: 20,
    marginBottom: 16,
    borderWidth: 1,
    borderColor: '#333',
  },
  historyHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  dateContainer: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  dateText: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#fff',
  },
  timeText: {
    fontSize: 14,
    color: '#888',
  },
  historyDetails: {
    gap: 12,
  },
  historyDetailItem: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  detailLabel: {
    fontSize: 14,
    color: '#888',
  },
  detailValue: {
    fontSize: 16,
    fontWeight: '600',
    color: '#fff',
  },
  completeBadge: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
    marginTop: 12,
    paddingTop: 12,
    borderTopWidth: 1,
    borderTopColor: '#333',
  },
  completeText: {
    fontSize: 14,
    color: '#4CD964',
    fontWeight: '600',
  },
});
