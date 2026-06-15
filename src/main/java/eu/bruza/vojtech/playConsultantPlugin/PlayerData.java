package eu.bruza.vojtech.playConsultantPlugin;

import com.plotsquared.core.plot.PlotId;
import org.bukkit.Location;

public class PlayerData {
    private int commentsMade = 0;
    private Location lastCommentLocation = null;
    private boolean isTypingComment = false;
    private boolean receivedCreativeKey = false;
    private PlotId assignedPlotId = null;

    // Getters and Setters
    public synchronized int getCommentsMade() {
        return commentsMade;
    }

    public synchronized void setCommentsMade(int commentsMade) {
        this.commentsMade = Math.max(0, commentsMade);
    }

    public synchronized void incrementComments() {
        this.commentsMade++;
    }

    public synchronized Location getLastCommentLocation() {
        return lastCommentLocation == null ? null : lastCommentLocation.clone();
    }

    public synchronized void setLastCommentLocation(Location loc) {
        this.lastCommentLocation = loc == null ? null : loc.clone();
    }

    public synchronized boolean isTypingComment() {
        return isTypingComment;
    }

    public synchronized void setTypingComment(boolean typing) {
        this.isTypingComment = typing;
    }

    public synchronized boolean hasReceivedCreativeKey() {
        return receivedCreativeKey;
    }

    public synchronized void setReceivedCreativeKey(boolean receivedCreativeKey) {
        this.receivedCreativeKey = receivedCreativeKey;
    }

    public synchronized PlotId getAssignedPlotId() {
        return assignedPlotId;
    }

    public synchronized void setAssignedPlotId(PlotId plotId) {
        this.assignedPlotId = plotId;
    }
}
