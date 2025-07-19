# JavaFX Chat Application - Complete Documentation

## Table of Contents
1. [Project Overview](#project-overview)
2. [Architecture](#architecture)
3. [Directory Structure](#directory-structure)
4. [Core Components](#core-components)
5. [Message Protocol](#message-protocol)
6. [Data Persistence](#data-persistence)
7. [User Interface](#user-interface)
8. [Features](#features)
9. [Setup and Installation](#setup-and-installation)
10. [Usage Guide](#usage-guide)
11. [API Reference](#api-reference)
12. [File Formats](#file-formats)
13. [Troubleshooting](#troubleshooting)

## Project Overview

### Description
A modern, real-time chat application built with JavaFX that supports private messaging, user search, conversation management, and persistent message storage. The application works without requiring a database, using file-based persistence for all data storage.

### Key Features
- **Real-time Private Messaging**: Send and receive messages instantly
- **User Search & Discovery**: Find other users by searching their usernames
- **Offline Message Support**: Messages sent to offline users are queued and delivered when they return
- **Persistent Chat History**: All conversations are saved and preserved across app restarts
- **Modern UI**: Beautiful, responsive interface with dark mode support
- **File-based Storage**: No database required - all data stored in organized text files

### Technologies Used
- **JavaFX 21**: UI framework and application platform
- **Java 21**: Programming language and runtime
- **Maven**: Build tool and dependency management
- **TCP Sockets**: Network communication between client and server
- **File I/O**: Data persistence using text files

## Architecture

### System Architecture
The application follows a **client-server architecture** with the following components:

```
┌─────────────────┐    TCP Socket    ┌─────────────────┐
│   JavaFX Client │ ◄──────────────► │   Chat Server   │
│                 │                  │                 │
│ • UI Components │                  │ • ClientHandler │
│ • Chat Controller│                  │ • UserManager   │
│ • Message Client│                  │ • MessageStore  │
└─────────────────┘                  └─────────────────┘
                                               │
                                               ▼
                                    ┌─────────────────┐
                                    │  File Storage   │
                                    │                 │
                                    │ • User Data     │
                                    │ • Message History│
                                    │ • Offline Queue │
                                    └─────────────────┘
```

### Design Patterns Used
- **Singleton Pattern**: UserManager, GroupManager, MessageStore
- **Observer Pattern**: MessageListener interface for real-time updates
- **MVC Pattern**: Clear separation between UI (View), business logic (Controller), and data (Model)
- **Factory Pattern**: Custom ListCell factories for UI components

## Directory Structure

```
JavaFX Project 1-2/
├── src/main/
│   ├── java/com/bakbak/javafx_proj_1_2/
│   │   ├── ChatApplication.java          # Main JavaFX Application
│   │   ├── ChatClient.java               # Client-side network communication
│   │   ├── ChatController.java           # Main UI controller
│   │   ├── LoginController.java          # Login screen controller
│   │   ├── ChatServer.java               # Server main class
│   │   ├── ClientHandler.java            # Handles individual client connections
│   │   ├── Message.java                  # Message data structure and protocol
│   │   ├── MessageListener.java          # Interface for message handling
│   │   ├── MessageStore.java             # File-based message persistence
│   │   ├── User.java                     # User entity class
│   │   ├── UserManager.java              # User management and authentication
│   │   ├── Group.java                    # Group entity (for future group chat)
│   │   ├── GroupManager.java             # Group management (for future use)
│   │   ├── Conversation.java             # Conversation data structure
│   │   ├── ConversationManager.java      # Manages conversations and history
│   │   ├── ConversationCell.java         # Custom UI cell for conversation list
│   │   └── SearchResultCell.java         # Custom UI cell for search results
│   └── resources/com/bakbak/javafx_proj_1_2/
│       ├── chat.fxml                     # Main chat interface layout
│       ├── login.fxml                    # Login screen layout
│       └── style.css                     # Application styles and themes
├── chat_data/                            # Data persistence directory (auto-created)
│   ├── messages/                         # User message history files
│   ├── offline_queue/                    # Queued messages for offline users
│   └── users.txt                         # User registration data
├── target/                               # Maven build output
├── pom.xml                               # Maven configuration
└── README.md                             # Basic project information
```

## Core Components

### 1. Server-Side Components

#### ChatServer.java
- **Purpose**: Main server application entry point
- **Responsibilities**: 
  - Accepts client connections on port 8080
  - Creates ClientHandler threads for each connection
  - Manages server lifecycle

#### ClientHandler.java
- **Purpose**: Handles individual client connections
- **Responsibilities**:
  - Processes incoming messages from clients
  - Routes messages to appropriate handlers
  - Manages user authentication and session state
  - Coordinates with UserManager and MessageStore

#### UserManager.java
- **Purpose**: Manages user accounts and online status
- **Responsibilities**:
  - User registration and authentication
  - Tracks online/offline status
  - Persists user data to files
  - Manages user contacts and relationships

#### MessageStore.java
- **Purpose**: Handles all message persistence and offline queuing
- **Responsibilities**:
  - Stores messages in organized file structure
  - Manages offline message queues
  - Provides conversation history retrieval
  - Handles message serialization/deserialization

### 2. Client-Side Components

#### ChatApplication.java
- **Purpose**: Main JavaFX application entry point
- **Responsibilities**:
  - Initializes the application
  - Manages scene transitions (login → chat)
  - Establishes server connection

#### ChatController.java
- **Purpose**: Main UI controller for the chat interface
- **Responsibilities**:
  - Handles user interactions in the chat window
  - Manages conversation display and switching
  - Coordinates with ConversationManager for local state
  - Implements search functionality and dark mode

#### LoginController.java
- **Purpose**: Handles login and registration UI
- **Responsibilities**:
  - User authentication interface
  - Registration form handling
  - Connection status management

#### ConversationManager.java
- **Purpose**: Client-side conversation state management
- **Responsibilities**:
  - Maintains local conversation history
  - Manages unread message counts
  - Coordinates conversation UI updates

### 3. Data Models

#### Message.java
- **Purpose**: Core message data structure and protocol definition
- **Properties**:
  - Message type (LOGIN, PRIVATE_MESSAGE, etc.)
  - Sender and recipient information
  - Content and timestamp
  - Success/error status

#### User.java
- **Purpose**: User entity representation
- **Properties**:
  - Username and password
  - Online status
  - Contact list
  - Group memberships

#### Conversation.java
- **Purpose**: Represents a conversation between two users
- **Properties**:
  - Contact username
  - Message history
  - Unread count
  - Last activity timestamp

## Message Protocol

### Message Types
The application uses a comprehensive message protocol for client-server communication:

| Message Type | Direction | Purpose |
|--------------|-----------|---------|
| `REGISTER` | Client → Server | User registration |
| `LOGIN` | Client → Server | User authentication |
| `LOGOUT` | Client → Server | User logout |
| `PRIVATE_MESSAGE` | Client ↔ Server | Send/receive private messages |
| `USER_SEARCH` | Client → Server | Search for users |
| `SYNC_HISTORY` | Client → Server | Request conversation history |
| `OFFLINE_MESSAGES` | Server → Client | Notification of offline message delivery |
| `MESSAGE_HISTORY` | Server → Client | Historical message data |
| `ACKNOWLEDGMENT` | Server → Client | Operation success/failure |
| `ERROR` | Server → Client | Error notifications |

### Message Flow Examples

#### User Registration
```
1. Client → Server: REGISTER message with username/password
2. Server → Client: ACKNOWLEDGMENT with success/failure status
```

#### Private Message (Online Recipient)
```
1. Client A → Server: PRIVATE_MESSAGE with recipient and content
2. Server: Stores message in MessageStore
3. Server → Client B: PRIVATE_MESSAGE (immediate delivery)
4. Server → Client A: ACKNOWLEDGMENT (delivery confirmation)
```

#### Private Message (Offline Recipient)
```
1. Client A → Server: PRIVATE_MESSAGE with recipient and content
2. Server: Stores message in MessageStore
3. Server: Queues message in offline queue
4. Server → Client A: ACKNOWLEDGMENT (queued notification)
5. [Later] Client B logs in
6. Server → Client B: Delivers queued messages
7. Server → Client B: OFFLINE_MESSAGES notification
```

## Data Persistence

### File-Based Storage System
The application uses a sophisticated file-based persistence system without requiring a database:

#### Directory Structure
```
chat_data/
├── users.txt                    # User accounts (username|password)
├── messages/
│   ├── alice.txt               # Alice's complete message history
│   ├── bob.txt                 # Bob's complete message history
│   └── charlie.txt             # Charlie's complete message history
└── offline_queue/
    ├── alice.txt               # Messages waiting for Alice
    └── bob.txt                 # Messages waiting for Bob
```

#### Message Storage Format
Messages are stored in pipe-delimited format:
```
MESSAGE_TYPE|SENDER|RECIPIENT|CONTENT|GROUP_ID|TIMESTAMP|SUCCESS|ERROR_MESSAGE
```

Example:
```
PRIVATE_MESSAGE|alice|bob|Hello Bob!||2025-07-18T02:15:30|true|
PRIVATE_MESSAGE|bob|alice|Hi Alice, how are you?||2025-07-18T02:16:45|true|
```

#### User Storage Format
Users are stored in simple format:
```
username|password
alice|password123
bob|mypassword
charlie|secret456
```

### Data Persistence Features
- **Automatic Directory Creation**: Server creates necessary directories on startup
- **Thread-Safe Operations**: Concurrent access protected with synchronized methods
- **Error Handling**: Graceful handling of file I/O errors
- **Cross-Platform Compatibility**: Works on Windows, macOS, and Linux

## User Interface

### Modern Design Philosophy
The UI follows modern chat application design principles:
- **Clean, Minimal Interface**: Focus on content, not chrome
- **Intuitive Navigation**: Familiar patterns from popular messengers
- **Responsive Design**: Adapts to different window sizes
- **Accessibility**: Clear visual hierarchy and readable fonts

### UI Components

#### Login Screen (`login.fxml`)
- Gradient header with app branding
- Clean form design with rounded input fields
- Primary and secondary button styles
- Loading indicators and status messages

#### Main Chat Interface (`chat.fxml`)
- **Left Sidebar**: 
  - User profile header with search functionality
  - Conversation list with custom cells
  - New chat and group buttons
- **Main Chat Area**:
  - Chat header with contact info
  - Scrollable message container
  - Message input with send button

#### Custom UI Components
- **ConversationCell**: Rich conversation display with avatars, previews, timestamps, and unread badges
- **SearchResultCell**: User search results with online status indicators
- **Message Bubbles**: Styled message containers with sender/recipient distinction

### Styling and Themes

#### Light Theme (Default)
- Clean white backgrounds
- Blue accent colors (#4299e1)
- Subtle shadows and borders
- Professional color scheme

#### Dark Theme
- Dark backgrounds (#1a202c, #2d3748)
- Maintained blue accents
- High contrast for readability
- Eye-friendly for low-light use

#### CSS Architecture
- **Modular Styles**: Separate sections for different components
- **Theme Support**: Complete dark mode implementation
- **Custom Properties**: Consistent color and spacing variables
- **Responsive Elements**: Hover states and transitions

## Features

### 1. User Management
- **Registration**: Create new user accounts with username/password
- **Authentication**: Secure login with credential validation
- **Persistence**: User accounts saved across server restarts
- **Online Status**: Real-time tracking of user availability

### 2. Real-Time Messaging
- **Instant Delivery**: Messages appear immediately for online users
- **Typing Indicators**: Visual feedback during message composition
- **Message Status**: Delivery confirmations and error handling
- **Rich Text Support**: Emoji and text formatting

### 3. User Discovery
- **Real-Time Search**: Find users as you type
- **Status Indicators**: See who's online/offline
- **Beautiful Results**: Rich search interface with avatars
- **Quick Actions**: Double-click to start conversations

### 4. Conversation Management
- **Persistent History**: All conversations saved permanently
- **Unread Tracking**: Visual indicators for new messages
- **Last Activity**: Timestamps for recent conversations
- **Message Previews**: See latest message content in conversation list

### 5. Offline Support
- **Message Queuing**: Messages sent to offline users are queued
- **Automatic Delivery**: Queued messages delivered on login
- **Delivery Notifications**: Users notified of offline message count
- **Reliable Storage**: Messages never lost, even during outages

### 6. Modern UI/UX
- **Dark Mode**: Complete theme switching
- **Responsive Design**: Adapts to window resizing
- **Smooth Animations**: Polished interactions and transitions
- **Keyboard Shortcuts**: Enter to send, ESC to cancel, etc.

## Setup and Installation

### Prerequisites
- **Java 21 or higher**
- **Maven 3.6 or higher**
- **JavaFX 21** (included via Maven dependencies)

### Installation Steps

1. **Clone the Repository**
```bash
git clone <repository-url>
cd "JavaFX Project 1-2"
```

2. **Build the Project**
```bash
mvn clean compile
```

3. **Start the Server**
```bash
java -cp target/classes com.bakbak.javafx_proj_1_2.ChatServer
```

4. **Start the Client** (in a new terminal)
```bash
mvn javafx:run
```

### Configuration
- **Server Port**: Default port 8080 (configurable in ChatServer.java)
- **Data Directory**: `chat_data/` (auto-created in project root)
- **Max Connections**: 100 concurrent clients (configurable)

## Usage Guide

### Getting Started

1. **First Time Setup**
   - Start the server first
   - Launch the client application
   - Create a new account using "Create Account" button
   - Log in with your credentials

2. **Finding Other Users**
   - Type in the search bar to find other users
   - Users appear with online/offline status
   - Double-click any user to start a conversation

3. **Sending Messages**
   - Select a conversation from the list
   - Type your message in the input field
   - Press Enter or click the send button
   - Messages appear immediately for online users

4. **Managing Conversations**
   - All conversations appear in the left sidebar
   - Unread messages show with red badges
   - Click any conversation to open it
   - Message history is preserved across sessions

### Advanced Features

#### Dark Mode
1. Click the settings (⚙) button in the top-right
2. Toggle "Dark Mode" checkbox
3. Theme changes immediately

#### Offline Messaging
1. Send messages to any user (online or offline)
2. Offline messages are queued automatically
3. Recipients receive them when they log in
4. You'll see delivery status in acknowledgments

#### Search and Discovery
1. Type partial usernames in the search bar
2. Results update in real-time
3. Online users show green indicators
4. Offline users show red indicators

## API Reference

### Server-Side APIs

#### UserManager
```java
public boolean registerUser(String username, String password)
public boolean loginUser(String username, String password, ClientHandler handler)
public void logoutUser(String username)
public boolean isUserOnline(String username)
public Set<String> getAllUsers()
```

#### MessageStore
```java
public void storeMessage(Message message)
public void queueOfflineMessage(String recipient, Message message)
public List<Message> getOfflineMessages(String username)
public List<Message> getConversationHistory(String user1, String user2)
```

#### ClientHandler
```java
public void sendMessage(Message message)
private void handlePrivateMessage(Message message, UserManager userManager, MessageStore messageStore)
private void handleUserSearch(Message message, UserManager userManager)
```

### Client-Side APIs

#### ChatClient
```java
public void connect() throws IOException
public void sendMessage(Message message) throws IOException
public void setMessageListener(MessageListener listener)
public void disconnect()
```

#### ConversationManager
```java
public Conversation getOrCreateConversation(String contactUsername)
public void addMessage(Message message)
public List<Conversation> getAllConversations()
public void markConversationAsRead(String contactUsername)
```

#### ChatController
```java
private void handleUserSearch(String searchQuery)
private void openConversation(Conversation conversation)
private void handleIncomingMessage(Message message)
private void toggleDarkMode(boolean enable)
```

## File Formats

### Message File Format
Each line represents one message:
```
MESSAGE_TYPE|SENDER|RECIPIENT|CONTENT|GROUP_ID|TIMESTAMP|SUCCESS|ERROR_MESSAGE
```

**Field Descriptions:**
- `MESSAGE_TYPE`: Enum value (PRIVATE_MESSAGE, etc.)
- `SENDER`: Username of message sender
- `RECIPIENT`: Username of message recipient
- `CONTENT`: Message text content
- `GROUP_ID`: Group identifier (empty for private messages)
- `TIMESTAMP`: ISO LocalDateTime format
- `SUCCESS`: Boolean success status
- `ERROR_MESSAGE`: Error description (empty if successful)

### User File Format
Each line represents one user:
```
username|password
```

### Directory Structure Rules
- User message files: `chat_data/messages/{username}.txt`
- Offline queue files: `chat_data/offline_queue/{username}.txt`
- User registry: `chat_data/users.txt`

## Troubleshooting

### Common Issues

#### "Connection refused" Error
**Problem**: Client cannot connect to server
**Solutions**:
1. Ensure server is running first
2. Check if port 8080 is available
3. Verify firewall settings
4. Check server startup logs for errors

#### CSS Parsing Warnings
**Problem**: JavaFX CSS warnings in console
**Cause**: Some CSS properties may not be fully supported
**Impact**: Visual appearance only, functionality unaffected
**Solution**: Warnings can be safely ignored

#### "Module javafx.controls not found"
**Problem**: JavaFX runtime not found when running directly with java
**Solution**: Use `mvn javafx:run` instead of direct java execution

#### Messages Not Persisting
**Problem**: Message history lost after restart
**Causes**:
1. Insufficient file permissions
2. Disk space issues
3. MessageStore initialization errors
**Solutions**:
1. Check chat_data directory permissions
2. Verify available disk space
3. Check server console for MessageStore errors

#### Users Cannot Register
**Problem**: Registration fails repeatedly
**Causes**:
1. Username already exists
2. File write permissions
3. Server connection issues
**Solutions**:
1. Try a different username
2. Check chat_data directory permissions
3. Verify server connection

### Debug Mode
To enable detailed logging:
1. Add debug prints in ClientHandler
2. Monitor server console output
3. Check file creation in chat_data directory

### Performance Considerations
- **File I/O**: Each message write is synchronous (reliable but slower)
- **Memory Usage**: All user data loaded in memory for fast access
- **Concurrent Access**: Thread-safe operations may cause brief delays
- **Scalability**: Suitable for small to medium user bases (< 1000 users)

### Limitations
- **No Message Encryption**: Messages stored in plain text
- **No File Attachments**: Text-only messaging
- **No Group Chat UI**: Backend supports groups, UI pending
- **No Message Search**: Cannot search within conversation history
- **No Message Editing**: Sent messages cannot be modified

## Future Enhancements

### Planned Features
1. **Message Encryption**: End-to-end encryption for security
2. **File Sharing**: Support for image and document attachments
3. **Group Chat UI**: Complete group messaging interface
4. **Message Search**: Search within conversation history
5. **Push Notifications**: System notifications for new messages
6. **User Profiles**: Avatar images and status messages
7. **Message Reactions**: Emoji reactions to messages
8. **Voice Messages**: Audio message support

### Technical Improvements
1. **Database Migration**: Optional database support for larger deployments
2. **Message Compression**: Reduce file storage requirements
3. **Network Optimization**: Message batching and compression
4. **Mobile Support**: Cross-platform mobile applications
5. **Web Interface**: Browser-based client option

---

**Documentation Version**: 1.0  
**Last Updated**: July 18, 2025  
**Project Version**: 1.0-SNAPSHOT 