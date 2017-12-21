package edu.dartmouth.cs.dartmouthfriendfinder.Holder;

import com.quickblox.chat.listeners.QBChatDialogMessageListener;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.chat.model.QBChatMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by johnnybrady on 12/14/17.
 *
 * Cache for all the chat dialogs on the Quickblox Server
 * (used for graphical purposes)
 */

public class QBDialogMessagesHolder {
    private static QBDialogMessagesHolder instance;
    private HashMap<String,QBChatDialog> qbDialogMessageHashMap;

    public static synchronized QBDialogMessagesHolder getInstance() {
        QBDialogMessagesHolder qbDialogMessagesHolder;
        synchronized (QBDialogMessagesHolder.class){
            if(instance == null ){
                instance = new QBDialogMessagesHolder();
            }
            qbDialogMessagesHolder = instance;

        }

        return qbDialogMessagesHolder;

    }

    public QBDialogMessagesHolder() { this.qbDialogMessageHashMap = new HashMap<>(); }

    public void putDialogs(List<QBChatDialog> dialogs){
        for(QBChatDialog qbChatDialog : dialogs){
            putDialog(qbChatDialog);
        }
    }

    public void putDialog(QBChatDialog qbChatDialog) {
        this.qbDialogMessageHashMap.put(qbChatDialog.getDialogId(), qbChatDialog);
    }

    public QBChatDialog getDialogMessagesById(String id){
        return (QBChatDialog)qbDialogMessageHashMap.get(id);
    }

    public List<QBChatDialog> getDialogsMessagesByIds(List<String> ids, String currentUser){
        List<QBChatDialog> dialogMessages = new ArrayList<>();

        for( String id: ids){
            QBChatDialog dialogMessage = getDialogMessagesById(id);

            if(dialogMessage != null){
                dialogMessages.add(dialogMessage);
            }
        }

        return dialogMessages;
    }

    public ArrayList<QBChatDialog> getAllDialogs(){
        ArrayList<QBChatDialog> qbMessages = new ArrayList<>();

        for( String id: qbDialogMessageHashMap.keySet()){
           qbMessages.add(qbDialogMessageHashMap.get(id));
        }
        return qbMessages;
    }

    public void refreshQBDialogMessagesHolder(){
        instance = new QBDialogMessagesHolder();
        this.qbDialogMessageHashMap = new HashMap<>();
    }
}
