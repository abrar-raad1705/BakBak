package com.bakbak.javafx_proj_1_2;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

public class GroupManager {
    private static GroupManager instance;
    private Map<String, Group> groups;

    private GroupManager() {
        this.groups = new ConcurrentHashMap<>();
    }

    public static synchronized GroupManager getInstance() {
        if (instance == null) {
            instance = new GroupManager();
        }
        return instance;
    }

    public String createGroup(String groupName, String creator) {
        String groupId = UUID.randomUUID().toString();
        Group newGroup = new Group(groupId, groupName, creator);
        groups.put(groupId, newGroup);

        UserManager userManager = UserManager.getInstance();
        User user = userManager.getUser(creator);
        if (user != null) {
            user.joinGroup(groupId);
        }
        return groupId;
    }

    public boolean joinGroup(String groupId, String username) {
        Group group = groups.get(groupId);
        if (group == null) {
            return false;
        }

        UserManager userManager = UserManager.getInstance();
        User user = userManager.getUser(username);
        /*
         * if (user == null) {
         * return false; // User doesn't exist
         * }
         */
        group.addMember(username);
        user.joinGroup(groupId);
        return true;
    }

    public boolean leaveGroup(String groupId, String username) {
        Group group = groups.get(groupId);
        /*
         * if (group == null || !group.isMember(username)) {
         * return false;
         * }
         */
        if (group.getCreator().equals(username)) {
            return false;
        }
        group.removeMember(username);

        UserManager userManager = UserManager.getInstance();
        User user = userManager.getUser(username);
        if (user != null) {
            user.leaveGroup(groupId);
        }
        return true;
    }

    public boolean deleteGroup(String groupId, String username) {
        Group group = groups.get(groupId);
        if (/* group == null || */!group.getCreator().equals(username)) {
            return false;
        }

        UserManager userManager = UserManager.getInstance();
        for (String member : group.getMembers()) {
            User user = userManager.getUser(member);
            if (user != null) {
                user.leaveGroup(groupId);
            }
        }

        groups.remove(groupId);
        return true;
    }

    public Group getGroup(String groupId) {
        return groups.get(groupId);
    }

    public Set<String> getGroupMembers(String groupId) {
        Group group = groups.get(groupId);
        return group != null ? group.getMembers() : new HashSet<>();
    }

    public Set<String> getUserGroups(String username) {
        UserManager userManager = UserManager.getInstance();
        User user = userManager.getUser(username);
        return user != null ? user.getGroups() : new HashSet<>();
    }

    public Set<String> getAllGroups() {
        return new HashSet<>(groups.keySet());
    }

    public boolean isGroupMember(String groupId, String username) {
        Group group = groups.get(groupId);
        return group != null && group.isMember(username);
    }

    public boolean isGroupAdmin(String groupId, String username) {
        Group group = groups.get(groupId);
        return group != null && group.isAdmin(username);
    }

    public boolean addAdmin(String groupId, String adminUsername, String newAdminUsername) {
        Group group = groups.get(groupId);
        if (!group.isAdmin(adminUsername)) {
            return false;
        }

        if (!group.isMember(newAdminUsername)) {
            return false;
        }

        group.addAdmin(newAdminUsername);
        return true;
    }

    public boolean removeAdmin(String groupId, String adminUsername, String targetUsername) {
        Group group = groups.get(groupId);
        if (!group.isAdmin(adminUsername)) {
            return false;
        }

        // !As only creator should be able to remove other admins
        if (!group.getCreator().equals(adminUsername)) {
            return false;
        }

        group.removeAdmin(targetUsername);
        return true;
    }

    public boolean removeMember(String groupId, String adminUsername, String targetMember){
        Group group = groups.get(groupId);
        if(!group.isAdmin(adminUsername)){
            return false;
        }

        group.removeMember(targetMember);
        return true;
    }

    public boolean addMember(String groupId, String memberUsername, String targetMember){
        Group group = groups.get(groupId);
        if(!group.isMember(memberUsername)){
            return false;
        }

        group.addMember(targetMember);
        return true;
    }
}