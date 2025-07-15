import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class UserManager {
    private static UserManager instance;
    private Map<String, User> users;
    private Map<String, ClientHandler> onlineUsers;
    
    private UserManager() {
        this.users = new ConcurrentHashMap<>();
        this.onlineUsers = new ConcurrentHashMap<>();
    }
    
    public static synchronized UserManager getInstance() {
        if (instance == null) {
            instance = new UserManager();
        }
        return instance;
    }
    
    public boolean registerUser(String username, String password) {
        if (users.containsKey(username)) {
            return false; // User already exists
        }
        
        User newUser = new User(username, password);
        users.put(username, newUser);
        return true;
    }
    
    public boolean loginUser(String username, String password, ClientHandler clientHandler) {
        User user = users.get(username);
        if (user == null || !user.getPassword().equals(password)) {
            return false; // Invalid credentials
        }
        
        if (onlineUsers.containsKey(username)) {
            return false; // User already online
        }
        
        user.setOnline(true);
        onlineUsers.put(username, clientHandler);
        return true;
    }
    
    public void logoutUser(String username) {
        User user = users.get(username);
        if (user != null) {
            user.setOnline(false);
            onlineUsers.remove(username);
        }
    }
    
    public boolean isUserOnline(String username) {
        return onlineUsers.containsKey(username);
    }
    
    public ClientHandler getClientHandler(String username) {
        return onlineUsers.get(username);
    }
    
    public User getUser(String username) {
        return users.get(username);
    }
    
    public Set<String> getOnlineUsers() {
        return new HashSet<>(onlineUsers.keySet());
    }
    
    public Set<String> getAllUsers() {
        return new HashSet<>(users.keySet());
    }
    
    public boolean userExists(String username) {
        return users.containsKey(username);
    }
    
    public void addContact(String username, String contactUsername) {
        User user = users.get(username);
        if (user != null && users.containsKey(contactUsername)) {
            user.addContact(contactUsername);
        }
    }
    
    public void removeContact(String username, String contactUsername) {
        User user = users.get(username);
        if (user != null) {
            user.removeContact(contactUsername);
        }
    }
    
    public Set<String> getUserContacts(String username) {
        User user = users.get(username);
        return user != null ? user.getContacts() : new HashSet<>();
    }
} 