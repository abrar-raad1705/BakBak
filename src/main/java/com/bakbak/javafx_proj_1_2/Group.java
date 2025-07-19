package com.bakbak.javafx_proj_1_2;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class Group implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String groupId;
    private String groupName;
    private String creator;
    private Set<String> members;
    private Set<String> admins;
    
    public Group(String groupId, String groupName, String creator) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.creator = creator;
        this.members = new HashSet<>();
        this.admins = new HashSet<>();
        
        // Creator is automatically a member and admin
        this.members.add(creator);
        this.admins.add(creator);
    }
    
    // Getters and Setters
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    
    public String getCreator() { return creator; }
    public void setCreator(String creator) { this.creator = creator; }
    
    public Set<String> getMembers() { return members; }
    public void setMembers(Set<String> members) { this.members = members; }
    
    public Set<String> getAdmins() { return admins; }
    public void setAdmins(Set<String> admins) { this.admins = admins; }
    
    public void addMember(String username) {
        this.members.add(username);
    }
    
    public void removeMember(String username) {
        this.members.remove(username);
        this.admins.remove(username); // Remove from admins too if they were admin
    }
    
    public void addAdmin(String username) {
        if (this.members.contains(username)) {
            this.admins.add(username);
        }
    }
    
    public void removeAdmin(String username) {
        // Creator cannot be removed as admin
        if (!username.equals(creator)) {
            this.admins.remove(username);
        }
    }
    
    public boolean isMember(String username) {
        return this.members.contains(username);
    }
    
    public boolean isAdmin(String username) {
        return this.admins.contains(username);
    }
    
    @Override
    public String toString() {
        return "Group{" +
                "groupId='" + groupId + '\'' +
                ", groupName='" + groupName + '\'' +
                ", creator='" + creator + '\'' +
                ", members=" + members +
                ", admins=" + admins +
                '}';
    }
} 