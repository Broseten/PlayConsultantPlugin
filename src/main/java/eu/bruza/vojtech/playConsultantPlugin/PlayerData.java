package eu.bruza.vojtech.playConsultantPlugin;

import org.bukkit.Location;

public class PlayerData {
    private int commentsMade = 0;
    private Location lastCommentLocation = null;
    private boolean isTypingComment = false;

    // Getters and Setters
    public int getCommentsMade() {
        return commentsMade;
    }

    public void incrementComments() {
        this.commentsMade++;
    }

    public Location getLastCommentLocation() {
        return lastCommentLocation;
    }

    public void setLastCommentLocation(Location loc) {
        this.lastCommentLocation = loc;
    }

    public boolean isTypingComment() {
        return isTypingComment;
    }

    public void setTypingComment(boolean typing) {
        this.isTypingComment = typing;
    }
}

