import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  TouchableOpacity,
  ScrollView,
  TextInput,
  Modal,
  Alert,
} from 'react-native';
import { Ionicons } from '@expo/vector-icons';
import AsyncStorage from '@react-native-async-storage/async-storage';
import * as Haptics from 'expo-haptics';

interface WorkoutPreset {
  id: string;
  name: string;
  workTime: number;
  restTime: number;
  rounds: number;
  createdAt: string;
}

export default function PresetsScreen() {
  const [presets, setPresets] = useState<WorkoutPreset[]>([]);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [newPreset, setNewPreset] = useState({
    name: '',
    workTime: 30,
    restTime: 10,
    rounds: 5,
  });

  useEffect(() => {
    loadPresets();
  }, []);

  const loadPresets = async () => {
    try {
      const saved = await AsyncStorage.getItem('workoutPresets');
      if (saved) {
        setPresets(JSON.parse(saved));
      } else {
        // Add default presets
        const defaultPresets: WorkoutPreset[] = [
          {
            id: '1',
            name: 'Quick HIIT',
            workTime: 20,
            restTime: 10,
            rounds: 8,
            createdAt: new Date().toISOString(),
          },
          {
            id: '2',
            name: 'Tabata',
            workTime: 20,
            restTime: 10,
            rounds: 8,
            createdAt: new Date().toISOString(),
          },
          {
            id: '3',
            name: 'Strength Training',
            workTime: 45,
            restTime: 15,
            rounds: 6,
            createdAt: new Date().toISOString(),
          },
          {
            id: '4',
            name: 'Cardio Blast',
            workTime: 60,
            restTime: 20,
            rounds: 10,
            createdAt: new Date().toISOString(),
          },
        ];
        await AsyncStorage.setItem('workoutPresets', JSON.stringify(defaultPresets));
        setPresets(defaultPresets);
      }
    } catch (error) {
      console.error('Error loading presets:', error);
    }
  };

  const savePresets = async (updatedPresets: WorkoutPreset[]) => {
    try {
      await AsyncStorage.setItem('workoutPresets', JSON.stringify(updatedPresets));
      setPresets(updatedPresets);
    } catch (error) {
      console.error('Error saving presets:', error);
    }
  };

  const createPreset = async () => {
    if (!newPreset.name.trim()) {
      Alert.alert('Error', 'Please enter a preset name');
      return;
    }

    const preset: WorkoutPreset = {
      id: Date.now().toString(),
      name: newPreset.name,
      workTime: newPreset.workTime,
      restTime: newPreset.restTime,
      rounds: newPreset.rounds,
      createdAt: new Date().toISOString(),
    };

    const updatedPresets = [preset, ...presets];
    await savePresets(updatedPresets);
    setShowCreateModal(false);
    setNewPreset({ name: '', workTime: 30, restTime: 10, rounds: 5 });
    Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
  };

  const deletePreset = (id: string) => {
    Alert.alert('Delete Preset', 'Are you sure you want to delete this preset?', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          const updatedPresets = presets.filter((p) => p.id !== id);
          await savePresets(updatedPresets);
          Haptics.notificationAsync(Haptics.NotificationFeedbackType.Warning);
        },
      },
    ]);
  };

  const applyPreset = async (preset: WorkoutPreset) => {
    try {
      await AsyncStorage.setItem(
        'lastWorkoutSettings',
        JSON.stringify({
          workTime: preset.workTime,
          restTime: preset.restTime,
          rounds: preset.rounds,
        })
      );
      Alert.alert('Success', `"${preset.name}" has been applied to the timer!`);
      Haptics.notificationAsync(Haptics.NotificationFeedbackType.Success);
    } catch (error) {
      console.error('Error applying preset:', error);
    }
  };

  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={styles.title}>Workout Presets</Text>
        <TouchableOpacity
          style={styles.addButton}
          onPress={() => setShowCreateModal(true)}
        >
          <Ionicons name="add-circle" size={32} color="#00D9FF" />
        </TouchableOpacity>
      </View>

      <ScrollView style={styles.presetsList}>
        {presets.length === 0 ? (
          <View style={styles.emptyState}>
            <Ionicons name="clipboard-outline" size={64} color="#444" />
            <Text style={styles.emptyText}>No presets yet</Text>
            <Text style={styles.emptySubtext}>Tap + to create your first preset</Text>
          </View>
        ) : (
          presets.map((preset) => (
            <View key={preset.id} style={styles.presetCard}>
              <View style={styles.presetHeader}>
                <Text style={styles.presetName}>{preset.name}</Text>
                <TouchableOpacity onPress={() => deletePreset(preset.id)}>
                  <Ionicons name="trash-outline" size={24} color="#FF3B30" />
                </TouchableOpacity>
              </View>

              <View style={styles.presetDetails}>
                <View style={styles.detailItem}>
                  <Ionicons name="fitness" size={20} color="#00D9FF" />
                  <Text style={styles.detailText}>{preset.workTime}s work</Text>
                </View>
                <View style={styles.detailItem}>
                  <Ionicons name="pause-circle" size={20} color="#FF9500" />
                  <Text style={styles.detailText}>{preset.restTime}s rest</Text>
                </View>
                <View style={styles.detailItem}>
                  <Ionicons name="repeat" size={20} color="#4CD964" />
                  <Text style={styles.detailText}>{preset.rounds} rounds</Text>
                </View>
              </View>

              <TouchableOpacity
                style={styles.applyButton}
                onPress={() => applyPreset(preset)}
              >
                <Text style={styles.applyButtonText}>Use This Preset</Text>
                <Ionicons name="arrow-forward" size={20} color="#000" />
              </TouchableOpacity>
            </View>
          ))
        )}
      </ScrollView>

      <Modal
        visible={showCreateModal}
        animationType="slide"
        transparent={true}
        onRequestClose={() => setShowCreateModal(false)}
      >
        <View style={styles.modalOverlay}>
          <View style={styles.modalContent}>
            <View style={styles.modalHeader}>
              <Text style={styles.modalTitle}>Create Preset</Text>
              <TouchableOpacity onPress={() => setShowCreateModal(false)}>
                <Ionicons name="close" size={28} color="#fff" />
              </TouchableOpacity>
            </View>

            <View style={styles.inputContainer}>
              <Text style={styles.inputLabel}>Preset Name</Text>
              <TextInput
                style={styles.input}
                placeholder="e.g. Morning HIIT"
                placeholderTextColor="#666"
                value={newPreset.name}
                onChangeText={(text) => setNewPreset({ ...newPreset, name: text })}
              />
            </View>

            <View style={styles.inputContainer}>
              <Text style={styles.inputLabel}>Work Time (seconds)</Text>
              <TextInput
                style={styles.input}
                keyboardType="number-pad"
                value={newPreset.workTime.toString()}
                onChangeText={(text) =>
                  setNewPreset({ ...newPreset, workTime: parseInt(text) || 0 })
                }
              />
            </View>

            <View style={styles.inputContainer}>
              <Text style={styles.inputLabel}>Rest Time (seconds)</Text>
              <TextInput
                style={styles.input}
                keyboardType="number-pad"
                value={newPreset.restTime.toString()}
                onChangeText={(text) =>
                  setNewPreset({ ...newPreset, restTime: parseInt(text) || 0 })
                }
              />
            </View>

            <View style={styles.inputContainer}>
              <Text style={styles.inputLabel}>Number of Rounds</Text>
              <TextInput
                style={styles.input}
                keyboardType="number-pad"
                value={newPreset.rounds.toString()}
                onChangeText={(text) =>
                  setNewPreset({ ...newPreset, rounds: parseInt(text) || 0 })
                }
              />
            </View>

            <TouchableOpacity style={styles.createButton} onPress={createPreset}>
              <Text style={styles.createButtonText}>Create Preset</Text>
            </TouchableOpacity>
          </View>
        </View>
      </Modal>
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
  addButton: {
    padding: 8,
  },
  presetsList: {
    flex: 1,
    padding: 20,
  },
  emptyState: {
    alignItems: 'center',
    justifyContent: 'center',
    paddingTop: 100,
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
  },
  presetCard: {
    backgroundColor: '#1a1a1a',
    borderRadius: 16,
    padding: 20,
    marginBottom: 16,
    borderWidth: 1,
    borderColor: '#333',
  },
  presetHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 16,
  },
  presetName: {
    fontSize: 20,
    fontWeight: 'bold',
    color: '#fff',
  },
  presetDetails: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginBottom: 16,
  },
  detailItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  detailText: {
    fontSize: 14,
    color: '#ccc',
  },
  applyButton: {
    backgroundColor: '#00D9FF',
    borderRadius: 12,
    padding: 14,
    flexDirection: 'row',
    justifyContent: 'center',
    alignItems: 'center',
    gap: 8,
  },
  applyButtonText: {
    fontSize: 16,
    fontWeight: 'bold',
    color: '#000',
  },
  modalOverlay: {
    flex: 1,
    backgroundColor: 'rgba(0, 0, 0, 0.8)',
    justifyContent: 'center',
    padding: 20,
  },
  modalContent: {
    backgroundColor: '#1a1a1a',
    borderRadius: 20,
    padding: 24,
  },
  modalHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 24,
  },
  modalTitle: {
    fontSize: 24,
    fontWeight: 'bold',
    color: '#fff',
  },
  inputContainer: {
    marginBottom: 20,
  },
  inputLabel: {
    fontSize: 16,
    color: '#fff',
    marginBottom: 8,
  },
  input: {
    backgroundColor: '#2a2a2a',
    borderRadius: 12,
    padding: 16,
    fontSize: 16,
    color: '#fff',
    borderWidth: 1,
    borderColor: '#333',
  },
  createButton: {
    backgroundColor: '#00D9FF',
    borderRadius: 12,
    padding: 16,
    alignItems: 'center',
    marginTop: 8,
  },
  createButtonText: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#000',
  },
});
