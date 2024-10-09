package tw.nekomimi.nekogram.helpers;

import org.telegram.messenger.MessagesController;
import org.telegram.messenger.BaseController;

public class SettingsHelper extends BaseController {

    public SettingsHelper(int num) {
        super(num);
    }

    public static boolean hideBlockedUserMessages() {
        return MessagesController.getGlobalMainSettings().getBoolean("hideBlockedUserMsgs", false);
    }

    public static boolean hideSponsoredMessages() {
        return MessagesController.getGlobalMainSettings().getBoolean("hideSponsoredMessages", false);
    }
    
    public static boolean hideStories() {
        return MessagesController.getGlobalMainSettings().getBoolean("hideStories", false);
    }
}