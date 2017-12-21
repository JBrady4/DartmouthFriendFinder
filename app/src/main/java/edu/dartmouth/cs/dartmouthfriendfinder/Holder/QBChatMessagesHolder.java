package edu.dartmouth.cs.dartmouthfriendfinder.Holder;

import com.quickblox.chat.model.QBChatMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by johnnybrady on 12/14/17.
 *
 * Cache for messages of a chat dialog on the Quickblox Server
 * (used for graphical purposes)
 */

public class QBChatMessagesHolder  {

    private static QBChatMessagesHolder instance;
    private HashMap<String,ArrayList<QBChatMessage>> qbChatMessageArray;

    public static synchronized QBChatMessagesHolder getInstance() {
        QBChatMessagesHolder qbChatMessagesHolder;
        synchronized (QBChatMessagesHolder.class){
            if(instance == null ){
                instance = new QBChatMessagesHolder();
            }
            qbChatMessagesHolder = instance;
        }

        return qbChatMessagesHolder;

    }

    private QBChatMessagesHolder(){
        this.qbChatMessageArray = new HashMap<>();
    }

    public void putMessages(String dialogId, ArrayList<QBChatMessage> qbChatMessages){
        this.qbChatMessageArray.put(dialogId, qbChatMessages);
    }

    public void putMessage(String dialogId, QBChatMessage qbChatMessage){
        List<QBChatMessage> result = (List)this.qbChatMessageArray.get(dialogId);
        result.add(qbChatMessage);
        ArrayList<QBChatMessage> messageAdded = new ArrayList(result.size());
        messageAdded.addAll(result);
        putMessages(dialogId,messageAdded);



    }

    public ArrayList<QBChatMessage> getChatMessagesByDialogId(String dialogId){
        return (ArrayList<QBChatMessage>)this.qbChatMessageArray.get(dialogId);
    }

}
