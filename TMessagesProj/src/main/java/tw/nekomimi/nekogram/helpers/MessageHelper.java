package tw.nekomimi.nekogram.helpers;

import org.json.JSONException;
import org.json.JSONObject;

import org.telegram.messenger.BaseController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.Utilities;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.util.Log;

public class MessageHelper extends BaseController {

    public MessageHelper(int num) {
        super(num);
    }
    
    public static ArrayList<TLRPC.MessageEntity> checkBlockedEntities(MessageObject messageObject, ArrayList<TLRPC.MessageEntity> original) {
        if (messageObject.shouldBlockMessage() && messageObject.messageOwner.message != null) {
            ArrayList<TLRPC.MessageEntity> entities = new ArrayList<>(original);
            var spoiler = new TLRPC.TL_messageEntitySpoiler();
            spoiler.offset = 0;
            spoiler.length = messageObject.messageOwner.message.length();
            entities.add(spoiler);
            var quote = new TLRPC.TL_messageEntityBlockquote();
            quote.offset = 0;
            quote.length = messageObject.messageOwner.message.length();
            quote.collapsed = true;
            entities.add(quote);
            return entities;
        } else {
            return original;
        }
    }

    public static ArrayList<TLRPC.MessageEntity> checkBlockedEntities(MessageObject messageObject) {
        return checkBlockedEntities(messageObject, messageObject.messageOwner.entities);
    }

    public static ArrayList<TLRPC.MessageEntity> checkBlockedUserEntities(MessageObject messageObject) {
        return checkBlockedEntities(messageObject);
    }

    public static boolean shouldBlockMessage(MessageObject message) {
        if (message.messageOwner == null || message.storyItem != null) {
            return false;
        }
        if (!SettingsHelper.hideBlockedUserMessages()) {
            return false;
        }
        if (SettingsHelper.treatChannelMessagesAsBlocked()) {
            if (isFromExternalChannel(message)) {
                return true;
            }
        }
        if (isUserBlocked(message.currentAccount, message.getFromChatId())) {
            return true;
        }
        if (message.messageOwner.fwd_from == null || message.messageOwner.fwd_from.from_id == null) {
            return false;
        }
        return isUserBlocked(message.currentAccount, MessageObject.getPeerId(message.messageOwner.fwd_from.from_id));
    }

    private static boolean isUserBlocked(int currentAccount, long id) {
        var messagesController = MessagesController.getInstance(currentAccount);
        var userFull = messagesController.getUserFull(id);
        return (userFull != null && userFull.blocked) || messagesController.blockePeers.indexOfKey(id) >= 0;
    }

    private static boolean isFromExternalChannel(MessageObject messageObject) {
        if (messageObject.isFromUser()) {
            return false;
        }

        long senderChannelId = messageObject.messageOwner.from_id.channel_id;
        Log.d("MessageHelper", "Sender Channel ID: " + senderChannelId + ", Dialog ID: " + messageObject.getDialogId());
        if (senderChannelId == 0) {
            return false; 
        }

        final TLRPC.ChatFull currentChat = MessagesController.getInstance(messageObject.currentAccount).getChatFull(-messageObject.getDialogId());
        Log.d("MessageHelper", "Current Chat Id: " + (currentChat != null ? currentChat.id : "N/A"));
        if (currentChat == null || currentChat.linked_chat_id == null) {
            return false; 
        }

        long linkedChannelId = currentChat.linked_chat_id;
        Log.d("MessageHelper", "Linked Channel ID: " + linkedChannelId);
        if (senderChannelId == linkedChannelId) {
            return false;
        }

        return true;
    }
}