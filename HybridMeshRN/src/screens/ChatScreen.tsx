import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  TextInput,
  TouchableOpacity,
  FlatList,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
} from 'react-native';
import { Device } from '../models/Device';
import { Message, createMessage, MessageStatus } from '../models/Message';
import MessageBubble from '../components/MessageBubble';

interface ChatScreenProps {
  navigation: any;
  route: {
    params: {
      device: Device;
    };
  };
}

const ChatScreen: React.FC<ChatScreenProps> = ({ navigation, route }) => {
  const { device } = route.params;
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputText, setInputText] = useState('');

  useEffect(() => {
    // Load demo messages
    const demoMessages: Message[] = [
      createMessage({
        content: 'Hello! This is a demo message.',
        senderId: 'me',
        receiverId: device.id,
        isEncrypted: true,
        status: MessageStatus.DELIVERED,
      }),
      createMessage({
        content: 'Hi there! How are you?',
        senderId: device.id,
        receiverId: 'me',
        isEncrypted: true,
        status: MessageStatus.DELIVERED,
      }),
    ];
    setMessages(demoMessages);
  }, [device.id]);

  const sendMessage = () => {
    if (inputText.trim() === '') return;

    const newMessage = createMessage({
      content: inputText.trim(),
      senderId: 'me',
      receiverId: device.id,
      isEncrypted: true,
      status: MessageStatus.PENDING,
    });

    setMessages(prev => [...prev, newMessage]);
    setInputText('');

    // Simulate message sending
    setTimeout(() => {
      setMessages(prev => 
        prev.map(msg => 
          msg.id === newMessage.id 
            ? { ...msg, status: MessageStatus.SENT }
            : msg
        )
      );
    }, 1000);
  };

  const renderMessage = ({ item }: { item: Message }) => (
    <MessageBubble message={item} isOwn={item.senderId === 'me'} />
  );

  return (
    <KeyboardAvoidingView 
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : 'height'}
    >
      <View style={styles.header}>
        <Text style={styles.deviceName}>{device.name}</Text>
        <Text style={styles.deviceStatus}>
          {device.isOnline ? 'Online' : 'Offline'} • {device.signalStrength} dBm
        </Text>
      </View>

      <FlatList
        data={messages}
        renderItem={renderMessage}
        keyExtractor={(item) => item.id}
        style={styles.messagesList}
        contentContainerStyle={styles.messagesContainer}
      />

      <View style={styles.inputContainer}>
        <TextInput
          style={styles.textInput}
          value={inputText}
          onChangeText={setInputText}
          placeholder="Type a message..."
          multiline
          maxLength={500}
        />
        <TouchableOpacity
          style={[styles.sendButton, inputText.trim() === '' && styles.sendButtonDisabled]}
          onPress={sendMessage}
          disabled={inputText.trim() === ''}
        >
          <Text style={styles.sendButtonText}>Send</Text>
        </TouchableOpacity>
      </View>
    </KeyboardAvoidingView>
  );
};

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: '#f5f5f5',
  },
  header: {
    backgroundColor: '#2196F3',
    padding: 16,
    paddingTop: 50,
  },
  deviceName: {
    fontSize: 18,
    fontWeight: 'bold',
    color: '#fff',
  },
  deviceStatus: {
    fontSize: 14,
    color: '#e3f2fd',
    marginTop: 4,
  },
  messagesList: {
    flex: 1,
  },
  messagesContainer: {
    padding: 16,
  },
  inputContainer: {
    flexDirection: 'row',
    padding: 16,
    backgroundColor: '#fff',
    borderTopWidth: 1,
    borderTopColor: '#e0e0e0',
  },
  textInput: {
    flex: 1,
    borderWidth: 1,
    borderColor: '#e0e0e0',
    borderRadius: 20,
    paddingHorizontal: 16,
    paddingVertical: 8,
    marginRight: 8,
    maxHeight: 100,
  },
  sendButton: {
    backgroundColor: '#2196F3',
    paddingHorizontal: 20,
    paddingVertical: 8,
    borderRadius: 20,
    justifyContent: 'center',
  },
  sendButtonDisabled: {
    backgroundColor: '#ccc',
  },
  sendButtonText: {
    color: '#fff',
    fontWeight: 'bold',
  },
});

export default ChatScreen;
