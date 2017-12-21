package edu.dartmouth.cs.dartmouthfriendfinder;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBIncomingMessagesManager;
import com.quickblox.chat.QBRestChatService;
import com.quickblox.chat.exception.QBChatException;
import com.quickblox.chat.listeners.QBChatDialogMessageListener;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.chat.model.QBDialogType;
import com.quickblox.chat.request.QBMessageGetBuilder;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smackx.muc.DiscussionHistory;

import java.util.ArrayList;

import edu.dartmouth.cs.dartmouthfriendfinder.Adapter.ChatMessagesAdapter;
import edu.dartmouth.cs.dartmouthfriendfinder.Common.Common;
import edu.dartmouth.cs.dartmouthfriendfinder.Holder.QBChatMessagesHolder;
import edu.dartmouth.cs.dartmouthfriendfinder.R;

/**
 * Created by johnnybrady
 * Displays the server messages with nice graphical interface
 */

public class ChatMessage extends AppCompatActivity implements QBChatDialogMessageListener{

    QBChatDialog qbChatDialog;
    ListView chatMessages;
    ImageButton submitButton;
    EditText inputText;

    ChatMessagesAdapter chatMessagesAdapter;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_message);

        //initialize views
        submitButton = (ImageButton)findViewById(R.id.sendButton);
        inputText = (EditText)findViewById(R.id.inputContent);
        chatMessages = (ListView)findViewById(R.id.allMessages);

        initChatDialogs();

        retrieveMessage();

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                QBChatMessage chatMessage = new QBChatMessage();
                chatMessage.setBody(inputText.getText().toString());
                chatMessage.setSenderId(QBChatService.getInstance().getUser().getId());
                chatMessage.setSaveToHistory(true);

                try {
                    qbChatDialog.sendMessage(chatMessage);
                } catch (SmackException.NotConnectedException e){
                    e.printStackTrace();
                }

               //private chat
                if(qbChatDialog.getType() == QBDialogType.PRIVATE){

                    QBChatMessagesHolder.getInstance().putMessage(qbChatDialog.getDialogId(),chatMessage);
                    ArrayList<QBChatMessage> messages = QBChatMessagesHolder.getInstance().getChatMessagesByDialogId(qbChatDialog.getDialogId());

                    chatMessagesAdapter = new ChatMessagesAdapter(getBaseContext(), messages);
                    chatMessages.setAdapter(chatMessagesAdapter);
                    chatMessagesAdapter.notifyDataSetChanged();


                }

                //Remove Text from edit text
                inputText.setText("");
                inputText.setFocusable(true);
            }

        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        qbChatDialog.removeMessageListrener(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        qbChatDialog.removeMessageListrener(this);
    }


    private void initChatDialogs() {
        //retrieve the chat dialog on the server that corresponds to the one the user clicked in the MessagesFragment
        qbChatDialog = (QBChatDialog)getIntent().getSerializableExtra(Common.DIALOG_EXTRA);
        qbChatDialog.initForChat(QBChatService.getInstance());

        //create listener for incoming messages
        QBIncomingMessagesManager incomingMessagesManager = QBChatService.getInstance().getIncomingMessagesManager();
        incomingMessagesManager.addDialogMessageListener(new QBChatDialogMessageListener() {
            @Override
            public void processMessage(String s, QBChatMessage qbChatMessage, Integer integer) {

            }

            @Override
            public void processError(String s, QBChatException e, QBChatMessage qbChatMessage, Integer integer) {

            }
        });

        //before chatting in dialog, join the dialog
        if(qbChatDialog.getType() == QBDialogType.PUBLIC_GROUP || qbChatDialog.getType() == QBDialogType.GROUP){
            DiscussionHistory discussionHistory = new DiscussionHistory();
            discussionHistory.setMaxStanzas(0);


            qbChatDialog.join(discussionHistory, new QBEntityCallback() {
                @Override
                public void onSuccess(Object o, Bundle bundle) {

                }

                @Override
                public void onError(QBResponseException e) {
                    Log.e("initChatDialogGroup", e.getMessage() );
                }
            });
        }

        qbChatDialog.addMessageListener(this);

    }

    private void retrieveMessage() {
        QBMessageGetBuilder messageGetBuilder = new QBMessageGetBuilder();
        messageGetBuilder.setLimit(300); //limit 300 messages

        if(qbChatDialog != null ){
            QBRestChatService.getDialogMessages(qbChatDialog,messageGetBuilder).performAsync(new QBEntityCallback<ArrayList<QBChatMessage>>() {
                @Override
                public void onSuccess(ArrayList<QBChatMessage> qbChatMessages, Bundle bundle) {
                    //put messages into ChatMessages holder so we can display these messages from the server
                    QBChatMessagesHolder.getInstance().putMessages(qbChatDialog.getDialogId(),qbChatMessages);


                    //set the adapter to the list view to display the messages
                    chatMessagesAdapter = new ChatMessagesAdapter(getBaseContext(), qbChatMessages);
                    chatMessages.setAdapter(chatMessagesAdapter);
                    chatMessagesAdapter.notifyDataSetChanged();

                }

                @Override
                public void onError(QBResponseException e) {

                }
            });
        }
    }


    @Override
    public void processMessage(String s, QBChatMessage qbChatMessage, Integer integer) {
        QBChatMessagesHolder.getInstance().putMessage(qbChatMessage.getDialogId(),qbChatMessage);

        //get all the messages in our cache
        ArrayList<QBChatMessage> messages = QBChatMessagesHolder.getInstance().getChatMessagesByDialogId(qbChatMessage.getDialogId());

        //display these messages
        chatMessagesAdapter = new ChatMessagesAdapter(getBaseContext(), messages);
        chatMessages.setAdapter(chatMessagesAdapter);
        chatMessagesAdapter.notifyDataSetChanged();
    }

    @Override
    public void processError(String s, QBChatException e, QBChatMessage qbChatMessage, Integer integer) {
        Log.e("processError: "+s, e.getMessage() );
    }
}
