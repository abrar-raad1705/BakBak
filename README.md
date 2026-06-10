# 💬 BakBak: A Modern OOP-Based Chat Application

**BakBak** is a real-time, multi-client chat application built using **JavaFX** and native **Java Sockets**. It is designed as an **Object-Oriented Programming (OOP) course project** to demonstrate the practical application of software engineering principles, design patterns, multi-threaded networking, and clean MVC architecture.

---

## 🎓 OOP & Software Design Showcase

This project was built from the ground up to showcase solid Object-Oriented design and robust programming practices.

### 🏛️ 1. Core OOP Principles Applied

*   **Encapsulation**: Entities such as `User`, `Group`, `Message`, and `FileMessageData` encapsulate their fields using strict private access modifiers, exposing data solely through controlled getters, setters, and logical methods. This shields internal state and guarantees state consistency.
*   **Abstraction**: Interfaces like [ProgressCallback](file:///home/raad/Desktop/BakBak/src/main/java/com/bakbak/javafx_proj_1_2/ProgressCallback.java) decouple progress tracking from the file transmission layers. Network socket communication details are abstracted away from the UI controller logic.
*   **Inheritance & Polymorphism**:
    *   Hierarchical custom styling classes inherit defaults and override states dynamically.
    *   Polymorphic event and message dispatching allows handling distinct types of communication packets seamlessly.
*   **Single Responsibility Principle (SRP)**: Each class has a single, well-defined purpose:
    *   `ChatServer` & `ClientHandler`: Server-side socket listening and packet broadcasting.
    *   `MessageStore`: Atomic, thread-safe persistence layer for message logs.
    *   `UserManager` & `GroupManager`: Entity state management, security checks, and database emulation.

### 🧩 2. Design Patterns Implemented

*   **Model-View-Controller (MVC)**: Clean architectural separation of:
    *   **Models**: Domain logic (`User`, `Group`, `Message`, `FileMessageData`).
    *   **Views**: Declarative JavaFX layouts (`ChatWindow.fxml`, `Login.fxml`, `ServerDiscovery.fxml`).
    *   **Controllers**: View controllers mapping user inputs to model behaviors (`ChatController`, `LoginController`, `ServerDiscoveryController`).
*   **Singleton Pattern**: Core system coordinators like `UserManager`, `GroupManager`, and `MessageStore` implement singletons to enforce centralized state tracking and thread-safe data persistence.
*   **Observer Pattern**: Real-time events (log-ins, log-outs, user status modifications, new messages) are broadcast dynamically to all online clients, prompting reactive UI updates.
*   **Facade / Manager Pattern**: Clean APIs hide the complexities of handling multi-threaded data transfers, background loading, and offline message queue buffers.

---

## 🚀 Key Features

### 🔐 1. Authentication & LAN Server Discovery
*   **LAN Server Discovery**: Automatically scans the Local Area Network (LAN) for active `ChatServer` instances.
*   **Secure Authentication**: Handles user registration and login with local database storage emulation.
*   **Session Management**: Maintains connection state, automatically handles unexpected server/client drops, and re-establishes sessions cleanly.

### 💬 2. Real-Time Chat & Emoji System
*   **Private Chats**: Supports instant direct messaging between users.
*   **Group Chats**: Complete group workflow supporting naming, contact selection, and admin controls.
*   **Modern Chat Bubbles**: Telegram-inspired light & dark theme styling, displaying user tags, sent/received states, and message timestamps.
*   **Interactive Emoji Panel**: Fully-featured emoji sprite sheet rendering panel with category navigation, text cursor insertion, and recent emoji tracking.

### 👥 3. Group Administration & Permissions
*   **Role Hierarchy**: Enforces permissions: **Creator/Owner** → **Admin** → **Member**.
*   **Interactive Member Popups**: Hovering over group names displays a clean popover listing group members, online status, and role badges.
*   **Administrative Actions**: Right-click context menus allow owners and admins to promote/demote members, remove them, or rename groups in real time.

### 📁 4. Robust File Sharing & Data Persistence
*   **Polymorphic File Cards**: Share all types of files (images, audio, documents) under a unified premium file-card UI.
*   **Chunk-Based File Transfer**: Custom file sender/receiver dividing files into sequential byte packets to handle large uploads smoothly without locking the main application thread.
*   **Download & Progress Bars**: Real-time file transfer progress indicators with localized downloads directory saving.
*   **Atomic Persistence**: Message history is written atomically to avoid data corruption under high concurrent loads.

---

## 🛠️ System Architecture

```
BakBak Client-Server Topology
┌──────────────┐         ┌──────────────┐         ┌──────────────┐
│  BakBak UI   │         │  ChatServer  │         │ MessageStore │
│ (JavaFX/MVC) │◄───────►│  (Sockets/   │◄───────►│ (Atomic File │
│  [Client]    │         │ Multi-Thread)│         │ Persistence) │
└──────────────┘         └──────────────┘         └──────────────┘
```

### **File Structure Directory Tree**
```
src/main/java/com/bakbak/javafx_proj_1_2/
├── ChatApplication.java          # Client entry point
├── ChatServer.java              # Multi-threaded TCP server
├── ChatClient.java              # Client-side connection coordinator
├── ClientHandler.java           # Server-side connection thread
├── Message.java                 # Message data entity
├── MessageStore.java            # Thread-safe persistent storage
├── User.java                    # User model with group list & metadata
├── UserManager.java             # User logins and session tracking
├── Group.java                   # Group model with role assertions
├── GroupManager.java            # Group details manager
├── FileChunk.java               # Sequential packet container
├── FileChunkSender.java         # Sends files in byte arrays
├── FileChunkReceiver.java       # Receives and merges file chunks
├── controller/
│   ├── ChatController.java      # Main interface handler
│   ├── LoginController.java     # User login logic
│   └── ServerDiscoveryController.java # LAN scanner controller
│
src/main/resources/com/bakbak/javafx_proj_1_2/
├── emoji_sprites/               # Emoji category maps & sprite sheets
├── fxml/                        # FXML UI Layout screens
│   ├── ChatWindow.fxml
│   ├── Login.fxml
│   └── ServerDiscovery.fxml
└── icons/                       # Navigation and file type icons
```

---

## 📥 Getting Started & Running

### **Prerequisites**
*   **Java Development Kit (JDK) 21+**
*   **Maven 3.6+**

### **How to Run**

1.  **Start the Server**:
    Run `ChatServer.java` directly in your IDE or compile it via command-line:
    ```bash
    # Starts server listener on port 12345
    java -cp target/classes com.bakbak.javafx_proj_1_2.ChatServer
    ```
2.  **Start the Client**:
    Launch the chat client application:
    ```bash
    mvn clean compile javafx:run
    ```
3.  **Establish Chat Session**:
    *   Clients automatically discover servers running on the local network.
    *   Create an account, log in, select a contact or create a group, and start chatting.

### **Building & Packaging**
```bash
# Compile and clean target folder
mvn clean compile

# Package application into a runnable JAR
mvn clean package
```

---

## 📝 Design Patterns Summary Table

| Pattern | Component | Purpose |
| :--- | :--- | :--- |
| **Model-View-Controller** | `User/Group/Message` (M), `FXML` (V), `*Controller` (C) | Decouples data representations from graphic layout and behaviors. |
| **Observer** | `ProgressCallback`, status listener broadcast loops | Automatically notifies user interface and network sockets of asynchronous tasks. |
| **Singleton** | `UserManager`, `GroupManager`, `MessageStore` | Ensures single instance state, preventing database conflicts and concurrent write errors. |
| **State Pattern** | User log states (Online / Offline / Last Seen) | Controls messaging access and updates metadata depending on socket states. |
