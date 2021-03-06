package nl.tudelft.cs4160.trustchain_android.inbox;

import java.io.Serializable;
import java.util.Objects;

import nl.tudelft.cs4160.trustchain_android.peer.Peer;

public class InboxItem implements Serializable {
    private Peer peer;
    private int halfBlockCount;

    /**
     * Inbox item constructor
     * @param peer the peer which this inboxitem will be about
     * @param halfBlockCount the number of the unread blocks
     */
    public InboxItem(Peer peer, int halfBlockCount) {
        this.peer = peer;
        this.halfBlockCount = halfBlockCount;
    }

    /**
     * return the amount of unread blocks
     * @return
     */
    public int getAmountUnread() {
        return halfBlockCount;
    }

    public Peer getPeer(){
       return peer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InboxItem inboxItem = (InboxItem) o;
        return halfBlockCount == inboxItem.halfBlockCount &&
                Objects.equals(peer, inboxItem.peer);
    }

    @Override
    public int hashCode() {
        return Objects.hash(peer, halfBlockCount);
    }
}
