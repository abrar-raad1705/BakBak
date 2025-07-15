# TCP Chat Application Backend

A comprehensive chat application backend built with Java using TCP sockets. This application supports both one-to-one private messaging and group chat functionality.

## Features

- **User Management**: User registration, login, and logout
- **One-to-One Chat**: Private messaging between users
- **Group Chat**: Create groups, join/leave groups, and send group messages
- **Real-time Communication**: TCP socket-based communication
- **Multi-threaded Server**: Handles multiple concurrent client connections
- **User Status**: Track online/offline status
- **Message Broadcasting**: Efficient message routing to recipients

## Project Structure

```
├── Message.java          # Message protocol and data structures
├── User.java            # User entity with contacts and groups
├── Group.java           # Group entity with members and admins
├── UserManager.java     # User management and authentication
├── GroupManager.java    # Group management and membership
├── ClientHandler.java   # Handles individual client connections
├── ChatServer.java      # Main TCP server
├── ChatClient.java      # Sample client for testing
└── README.md           # This file
```

## Message Types

The application supports the following message types:

- `REGISTER` - User registration
- `LOGIN` - User login
- `LOGOUT` - User logout
- `PRIVATE_MESSAGE` - One-to-one messaging
- `GROUP_MESSAGE` - Group messaging
- `CREATE_GROUP` - Create a new group
- `JOIN_GROUP` - Join an existing group
- `LEAVE_GROUP` - Leave a group
- `USER_LIST` - Get list of online users
- `GROUP_LIST` - Get list of user's groups
- `ACKNOWLEDGMENT` - Server acknowledgment
- `ERROR` - Error messages

## How to Run

### 1. Compile the Java files

```bash
javac *.java
```

### 2. Start the Server

```bash
java ChatServer
```

The server will start on port 8080 and wait for client connections.

### 3. Run Client(s)

```bash
java ChatClient
```

You can run multiple clients to test the chat functionality.

## Usage Example

### Server Output
```
Chat Server started on port 8080
Waiting for client connections...
New client connected: /127.0.0.1
User alice logged in
User bob logged in
User alice logged out
User bob disconnected
```

### Client Usage

1. **Register a new user**:
   - Choose option 1 from the main menu
   - Enter username and password

2. **Login**:
   - Choose option 2 from the main menu
   - Enter your credentials

3. **Send Private Message**:
   - After login, choose option 1 from chat menu
   - Enter recipient username and message

4. **Create Group**:
   - Choose option 3 from chat menu
   - Enter group name
   - Server will return a group ID

5. **Join Group**:
   - Choose option 4 from chat menu
   - Enter the group ID

6. **Send Group Message**:
   - Choose option 2 from chat menu
   - Enter group ID and message

## Protocol Details

### Message Structure
```java
public class Message {
    private MessageType type;
    private String sender;
    private String recipient;      // For private messages
    private String content;
    private String groupId;        // For group messages
    private LocalDateTime timestamp;
    private boolean isSuccess;
    private String errorMessage;
}
```

### Example Message Flow

1. **Registration**:
   ```
   Client -> Server: REGISTER message with username and password
   Server -> Client: ACKNOWLEDGMENT with success status
   ```

2. **Private Message**:
   ```
   Client A -> Server: PRIVATE_MESSAGE with recipient and content
   Server -> Client B: PRIVATE_MESSAGE forwarded
   Server -> Client A: ACKNOWLEDGMENT with delivery status
   ```

3. **Group Message**:
   ```
   Client A -> Server: GROUP_MESSAGE with groupId and content
   Server -> All Group Members: GROUP_MESSAGE broadcasted
   Server -> Client A: ACKNOWLEDGMENT with delivery count
   ```

## Architecture

- **ChatServer**: Main server class that accepts client connections
- **ClientHandler**: Handles individual client communication in separate threads
- **UserManager**: Singleton class managing user data and authentication
- **GroupManager**: Singleton class managing group data and membership
- **Message**: Data structure for all communication between client and server

## Thread Safety

The application uses thread-safe data structures (`ConcurrentHashMap`) to handle concurrent access from multiple client threads.

## Error Handling

The server handles various error conditions:
- Invalid credentials
- User already online
- Non-existent recipients
- Group permission errors
- Connection failures

## Extensibility

The modular design allows for easy extension:
- Add new message types
- Implement file transfer
- Add message persistence
- Implement user roles and permissions
- Add message encryption

## Testing

Run multiple client instances to test:
- Multiple users chatting simultaneously
- Group chat with multiple participants
- User registration and login
- Error handling scenarios

## Configuration

Default server configuration:
- Port: 8080
- Max concurrent clients: 100
- Host: localhost

These can be modified in the `ChatServer.java` file. 