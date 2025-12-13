package edu.ptithcm.network.packet;

import edu.ptithcm.model.Peer;
import edu.ptithcm.security.Signable;

import java.util.List;

public class SyncMetadataResponsePayload implements Signable {
    public static class GroupConversationInfo{
        private final String groupId;
        private final String groupName;
        private final long clock;
        private final List<Peer> participants;

        public GroupConversationInfo(String groupId, String groupName, long clock, List<Peer> participants) {
            this.groupId = groupId;
            this.groupName = groupName;
            this.clock = clock;
            this.participants = participants;
        }

        public String getGroupId() {
            return groupId;
        }

        public String getGroupName() {
            return groupName;
        }

        public long getClock() {
            return clock;
        }

        public List<Peer> getParticipants() {
            return participants;
        }
    }

    private final String senderId;
    private final long directChatClock;
    private final List<GroupConversationInfo> groupConversationInfoList;

    private final long timestamp;
    private String signature;

    public SyncMetadataResponsePayload(String senderId, long directChatClock, List<GroupConversationInfo> groupConversationInfoList) {
        this.senderId = senderId;
        this.directChatClock = directChatClock;
        this.groupConversationInfoList = groupConversationInfoList;
        this.timestamp = System.currentTimeMillis();
    }

    public String getSenderId() {
        return senderId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getDirectChatClock() {
        return directChatClock;
    }

    public List<GroupConversationInfo> getGroupConversationInfoList() {
        return groupConversationInfoList;
    }

    @Override
    public String getSignableData() {
        return "";
    }

    @Override
    public String getSignature() {
        return signature;
    }

    @Override
    public void setSignature(String signature) {
        this.signature = signature;
    }
}
