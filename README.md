# 💬 **JavaFX Chat Application**

A modern, feature-rich chat application built with JavaFX, supporting real-time messaging, group chats, emoji support, and comprehensive user management.

## 🚀 **Current Features**

### **🔐 Authentication & Connection**
- **Server Discovery**: Automatic LAN server scanning with dynamic window resizing
- **User Login/Registration**: Secure user authentication with persistent sessions
- **Reconnection Support**: Automatic reconnection after logout without returning to server discovery
- **Session Management**: Maintains user sessions and handles disconnections gracefully

### **💬 Private Messaging**
- **Real-time Messaging**: Instant private message delivery between users
- **Message History**: Persistent message storage and retrieval from chat_data/messages/
- **Emoji Support**: Image-based emoji system with 50+ PNG emojis
  - Interactive emoji picker with popup interface
  - Recent emoji tracking (last 20 used)
  - Search functionality for emojis
  - Mixed text and emoji content using TextFlow + ImageView
- **Message Display**: Modern chat bubbles with timestamps positioned at bottom-right
- **Typing Interface**: Responsive text input with emoji insertion at cursor position

### **👥 Group Chat System**
#### **Group Creation & Management**
- **New Group Workflow**: 
  1. Group name input dialog
  2. Member selection popup with search functionality
  3. Real-time group creation with server-side processing
- **Group Information Display**:
  - Member count and online status ("X members, Y online")
  - Interactive hover popups showing all members with roles
  - Visual role indicators (owner/admin/member)
  - Online/offline status for each member

#### **Group Administration**
- **Hierarchy System**: Owner → Admin → Member permissions
- **Member Management**:
  - Right-click context menus for member actions
  - Promote/demote admin privileges (owner only)
  - Remove members from group (admin/owner only)
  - Add new members via 3-dot menu
- **Group Settings** (via 3-dot menu):
  - Change group name (admin/owner only with real-time updates)
  - Add members with contact selection interface
  - Leave group option (except owner)

### **📋 Contact Management**
- **Smart Contact Loading**: Automatically loads contacts from message history
- **Last Message Display**: Shows most recent message from conversations
- **Unread Message Tracking**: Bold text for unread messages, normal when viewed
- **Contact Sorting**: Prioritizes unread messages, then groups, then users
- **Real-time Status Updates**: Live online/offline status with "last seen" tracking

### **🎨 User Interface**
- **Modern Design**: Clean, WhatsApp-inspired interface with proper spacing
- **Responsive Layout**: Minimum window size constraints prevent UI breaking
- **Interactive Elements**:
  - Hover effects on buttons and emojis
  - Right-click context menus for advanced actions
  - Popup-based emoji picker (attached to main window)
  - Member management popups with scrollable lists

### **⚙️ Settings & Preferences**
- **Settings Menu**: Accessible via gear icon in sidebar
- **Group Creation**: "New Group" option with full workflow
- **Dark Mode Toggle**: UI theme switching (🌙 button)
- **Logout Functionality**: Clean session termination

### **📁 Data Persistence**
- **Message Storage**: Atomic file operations for message persistence
- **Recent Emojis**: Local storage of frequently used emojis
- **User Preferences**: Persistent settings and session data
- **Group Information**: Server-side group member and admin data

## 🛠 **Backend Architecture**

### **Core Components**
- **ChatServer.java**: Main server handling client connections and message routing
- **ChatClient.java**: Client-side connection management and message handling
- **MessageStore.java**: Persistent message storage with atomic write operations
- **UserManager.java**: User authentication, status tracking, and session management
- **GroupManager.java**: Comprehensive group management with role-based permissions

### **Data Models**
- **User.java**: User entity with groups, online status, and last seen tracking
- **Group.java**: Group entity with members, admins, creator, and permission methods
- **Message.java**: Message entity supporting private, group, and system messages
- **ClientHandler.java**: Server-side client session management

### **Message Types**
```java
enum MessageType {
    PRIVATE_MESSAGE,     // Direct user-to-user messages
    GROUP_MESSAGE,       // Group chat messages
    LOGIN, LOGOUT,       // Authentication events
    USER_LIST,           // Online user requests/responses
    USER_STATUS_UPDATE,  // Real-time status broadcasts
    CREATE_GROUP,        // Group creation requests
    JOIN_GROUP,          // Add members to groups
    LEAVE_GROUP,         // Remove members or leave groups
    GROUP_LIST          // Group information requests
}
```

### **File Structure**
```
src/main/java/com/bakbak/javafx_proj_1_2/
├── ChatApplication.java        # Main application entry point
├── ChatServer.java            # Server implementation
├── ChatClient.java            # Client connection management
├── ClientHandler.java         # Server-side client handling
├── Message.java               # Message data model
├── MessageStore.java          # Message persistence layer
├── User.java                  # User data model
├── UserManager.java           # User management logic
├── Group.java                 # Group data model
├── GroupManager.java          # Group management logic
└── controller/
    ├── ChatController.java    # Main chat interface controller
    ├── LoginController.java   # Login interface controller
    └── ServerDiscoveryController.java  # Server discovery controller

src/main/resources/com/bakbak/javafx_proj_1_2/
├── emojis/                    # 50+ PNG emoji files
└── fxml/
    ├── ChatWindow.fxml        # Main chat interface
    ├── Login.fxml             # Login screen
    └── ServerDiscovery.fxml   # Server discovery screen

chat_data/
├── messages/                  # User message history
│   ├── username1.txt
│   ├── username2.txt
│   └── ...
├── offline_queue/             # Offline message queue
├── users.txt                  # User database with last seen
└── recent_emojis.txt          # Recent emoji preferences
```

## 🚀 **How to Run**

### **Prerequisites**
- Java 21+ with JavaFX support
- Maven 3.6+

### **Running the Application**
1. **Start the Server**: Run `ChatServer.java` first
2. **Start Client(s)**: Run `ChatApplication.java` for each client
3. **Server Discovery**: Clients will automatically scan for LAN servers
4. **Login/Register**: Create account or login with existing credentials

**⚠️ Important**: Always start `ChatServer.java` before `ChatApplication.java`

### **Maven Commands**
```bash
# Clean and compile
mvn clean compile

# Run with JavaFX (if configured)
mvn clean javafx:run

# Package application
mvn clean package
```

## 🔮 **Future Prospects & Planned Features**

### **🔒 Enhanced Security**
- **End-to-End Encryption**: Message encryption for private conversations
- **User Authentication**: OAuth integration (Google, GitHub, etc.)
- **Admin Panel**: Server administration interface with user management
- **Rate Limiting**: Message throttling and spam prevention

### **📱 Advanced Messaging**
- **File Sharing**: Document, image, and media file support
- **Voice Messages**: Audio recording and playback
- **Message Reactions**: Like, love, laugh emoji reactions
- **Message Editing**: Edit sent messages with history tracking
- **Message Search**: Full-text search across conversation history
- **Message Threading**: Reply to specific messages with threading

### **👥 Enhanced Group Features**
- **Group Permissions**: Custom role creation and fine-grained permissions
- **Group Categories**: Organize groups by categories/folders
- **Group Voice/Video**: Voice and video calling for groups
- **Group Announcements**: Admin-only announcement channels
- **Group Templates**: Pre-configured group settings for common use cases

### **🎨 UI/UX Improvements**
- **Themes**: Multiple theme options (Dark, Light, Custom)
- **Customization**: User-defined color schemes and fonts
- **Animations**: Smooth transitions and micro-interactions
- **Accessibility**: Screen reader support and keyboard navigation
- **Mobile Layout**: Responsive design for different screen sizes

### **🌐 Connectivity & Sync**
- **Multi-Device Sync**: Synchronize conversations across devices
- **Cloud Storage**: Optional cloud backup for message history
- **Offline Mode**: Enhanced offline message queuing and sync
- **Push Notifications**: Desktop notifications for new messages
- **Web Interface**: Browser-based chat client

### **📊 Analytics & Monitoring**
- **Server Metrics**: Connection statistics and performance monitoring
- **User Analytics**: Activity tracking and engagement metrics
- **Message Analytics**: Popular emojis, message frequency, etc.
- **Health Checks**: Automated system health monitoring

### **🔧 Technical Enhancements**
- **Database Integration**: PostgreSQL/MySQL for scalable data storage
- **Microservices**: Split into authentication, messaging, and group services
- **Load Balancing**: Support for multiple server instances
- **Docker Support**: Containerized deployment
- **REST API**: HTTP API for external integrations
- **WebSocket Support**: Alternative to socket-based communication

### **🎯 Advanced Features**
- **Bot Framework**: Create and integrate chatbots
- **Webhooks**: External service integrations
- **Plugins**: Extensible plugin system for custom features
- **Translation**: Real-time message translation
- **Screen Sharing**: Share screen during conversations
- **Polls & Surveys**: Interactive voting within groups

## 📝 **Architecture Notes**

### **Design Patterns Used**
- **Singleton Pattern**: UserManager, GroupManager instances
- **Observer Pattern**: Real-time status updates and message broadcasting
- **MVC Pattern**: Separation of controllers, models, and FXML views
- **Factory Pattern**: Message creation and handling

### **Performance Considerations**
- **Atomic File Operations**: Prevents data corruption during high-frequency message storage
- **Concurrent Collections**: Thread-safe data structures for multi-client handling
- **Lazy Loading**: Emoji images loaded on-demand
- **Message Batching**: Efficient message processing and storage

### **Scalability Features**
- **Thread-Safe Operations**: ConcurrentHashMap usage for shared data
- **Modular Design**: Easy to extract components into separate services
- **Configurable Settings**: Easily adjustable parameters for different deployment sizes
- **Resource Management**: Proper cleanup and resource disposal

---

## 📞 **Contact & Support**

For questions, bug reports, or feature requests, please open an issue in this repository.

**Project Status**: ✅ **Active Development**  
**Version**: 2.0.0  
**Last Updated**: January 2025
