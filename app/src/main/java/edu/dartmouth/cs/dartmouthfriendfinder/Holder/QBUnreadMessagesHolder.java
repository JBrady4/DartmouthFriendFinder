package edu.dartmouth.cs.dartmouthfriendfinder.Holder;

import android.os.Bundle;

/**
 * Created by johnnybrady on 12/15/17.
 *
 * Cache for all unread Messages of a chat dialog on the Quickblox Server
 * (used for graphical purposes)
 */

public class QBUnreadMessagesHolder {
    private static QBUnreadMessagesHolder instance ;
    private Bundle bundle;

    public static synchronized QBUnreadMessagesHolder getInstance(){
        QBUnreadMessagesHolder qbUnreadMessagesHolder;
        synchronized (QBUnreadMessagesHolder.class) {
            if (instance == null) {
                instance = new QBUnreadMessagesHolder();
            }
            qbUnreadMessagesHolder = instance;
        }

       return qbUnreadMessagesHolder;
    }

    private QBUnreadMessagesHolder(){
        bundle = new Bundle();
    }

    public void setBundle(Bundle bundle){
        this.bundle = bundle;
    }

    public Bundle getBundle(){
        return this.bundle;
    }

    public int getUnreadMessagesByDialogId(String id){
        return this.bundle.getInt(id);
    }

}
