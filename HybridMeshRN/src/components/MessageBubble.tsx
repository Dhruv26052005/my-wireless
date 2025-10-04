import React from 'react';
import {
  View,
  Text,
  StyleSheet,
} from 'react-native';
import { Message, MessageStatus } from '../models/Message';

interface MessageBubbleProps {
  message: Message;
  isOwn: boolean;
}

const MessageBubble: React.FC<MessageBubbleProps> = ({ message, isOwn }) => {
  const getStatusIcon = (status: MessageStatus): string => {
    switch (status) {
      case MessageStatus.PENDING:
        return '⏳';
      case MessageStatus.SENT:
        return '✓';
      case MessageStatus.DELIVERED:
        return '✓✓';
      case MessageStatus.FAILED:
        return '❌';
      case MessageStatus.EXPIRED:
        return '⏰';
      default:
        return '';
    }
  };

  const getStatusColor = (status: MessageStatus): string => {
    switch (status) {
      case MessageStatus.PENDING:
        return '#FF9800';
      case MessageStatus.SENT:
        return '#2196F3';
      case MessageStatus.DELIVERED:
        return '#4CAF50';
      case MessageStatus.FAILED:
        return '#F44336';
      case MessageStatus.EXPIRED:
        return '#9E9E9E';
      default:
        return '#9E9E9E';
    }
  };

  const formatTime = (timestamp: number): string => {
    const date = new Date(timestamp);
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  };

  return (
    <View style={[styles.container, isOwn ? styles.ownMessage : styles.otherMessage]}>
      <View style={[
        styles.bubble,
        isOwn ? styles.ownBubble : styles.otherBubble
      ]}>
        <Text style={[
          styles.messageText,
          isOwn ? styles.ownMessageText : styles.otherMessageText
        ]}>
          {message.content}
        </Text>
        
        {message.isEncrypted && (
          <Text style={styles.encryptedIcon}>🔒</Text>
        )}
      </View>
      
      <View style={styles.footer}>
        <Text style={styles.timestamp}>
          {formatTime(message.timestamp)}
        </Text>
        
        {isOwn && (
          <Text style={[
            styles.status,
            { color: getStatusColor(message.status) }
          ]}>
            {getStatusIcon(message.status)}
          </Text>
        )}
        
        {message.hopCount > 0 && (
          <Text style={styles.hopCount}>
            {message.hopCount} hops
          </Text>
        )}
      </View>
    </View>
  );
};

const styles = StyleSheet.create({
  container: {
    marginVertical: 4,
  },
  ownMessage: {
    alignItems: 'flex-end',
  },
  otherMessage: {
    alignItems: 'flex-start',
  },
  bubble: {
    maxWidth: '80%',
    paddingHorizontal: 16,
    paddingVertical: 12,
    borderRadius: 20,
    flexDirection: 'row',
    alignItems: 'flex-end',
  },
  ownBubble: {
    backgroundColor: '#2196F3',
    borderBottomRightRadius: 4,
  },
  otherBubble: {
    backgroundColor: '#E0E0E0',
    borderBottomLeftRadius: 4,
  },
  messageText: {
    fontSize: 16,
    lineHeight: 20,
    flex: 1,
  },
  ownMessageText: {
    color: '#fff',
  },
  otherMessageText: {
    color: '#333',
  },
  encryptedIcon: {
    fontSize: 12,
    marginLeft: 4,
  },
  footer: {
    flexDirection: 'row',
    alignItems: 'center',
    marginTop: 4,
    paddingHorizontal: 8,
  },
  timestamp: {
    fontSize: 12,
    color: '#666',
  },
  status: {
    fontSize: 12,
    marginLeft: 4,
    fontWeight: 'bold',
  },
  hopCount: {
    fontSize: 10,
    color: '#999',
    marginLeft: 4,
    fontStyle: 'italic',
  },
});

export default MessageBubble;
