import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String username;
    private String password;
    private boolean isOnline;
    private Set<String> contacts;
    private Set<String> groups;
    
    public User(String username, String password) {
        this.username = username;
        this.password = password;
        this.isOnline = false;
        this.contacts = new HashSet<>();
        this.groups = new HashSet<>();
    }
    
    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }
    
    public Set<String> getContacts() { return contacts; }
    public void setContacts(Set<String> contacts) { this.contacts = contacts; }
    
    public Set<String> getGroups() { return groups; }
    public void setGroups(Set<String> groups) { this.groups = groups; }
    
    public void addContact(String contactUsername) {
        this.contacts.add(contactUsername);
    }
    
    public void removeContact(String contactUsername) {
        this.contacts.remove(contactUsername);
    }
    
    public void joinGroup(String groupId) {
        this.groups.add(groupId);
    }
    
    public void leaveGroup(String groupId) {
        this.groups.remove(groupId);
    }
    
    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", isOnline=" + isOnline +
                ", contacts=" + contacts +
                ", groups=" + groups +
                '}';
    }
} 