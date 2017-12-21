package edu.dartmouth.cs.dartmouthfriendfinder;


import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.quickblox.auth.QBAuth;
import com.quickblox.auth.session.BaseService;
import com.quickblox.auth.session.QBSession;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBIncomingMessagesManager;
import com.quickblox.chat.QBRestChatService;
import com.quickblox.chat.QBSystemMessagesManager;
import com.quickblox.chat.exception.QBChatException;
import com.quickblox.chat.listeners.QBChatDialogMessageListener;
import com.quickblox.chat.listeners.QBSystemMessageListener;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.BaseServiceException;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.request.QBRequestGetBuilder;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import edu.dartmouth.cs.dartmouthfriendfinder.Adapter.DialogMessagesAdapter;
import edu.dartmouth.cs.dartmouthfriendfinder.Common.Common;
import edu.dartmouth.cs.dartmouthfriendfinder.Holder.QBDialogMessagesHolder;
import edu.dartmouth.cs.dartmouthfriendfinder.Holder.QBUnreadMessagesHolder;
import edu.dartmouth.cs.dartmouthfriendfinder.Holder.QBUsersHolder;


/**
 * Created by johnnybrady
 */

public class MessagesFragment extends Fragment implements QBSystemMessageListener, QBChatDialogMessageListener{

    private static final String TAG="MessagesFragment";

    //current Quickblox chat service
    QBChatService chatService;


    FloatingActionButton addBtn;
    ListView chatDialogs;
    TextView userTitle;

    //current user information to-be-set
    String user, password;
    int id;


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.messages, container,false);

        //retrieve current chat service
        chatService = QBChatService.getInstance();


        //get current user information
        user = getArguments().getString("user");
        password = getArguments().getString("password");
        id = getArguments().getInt("id");

        Log.d(TAG, user + " " + password + " " +id+".");



        //button for creating new chat dialogs in the MessagesFragment
        addBtn = (FloatingActionButton)view.findViewById(R.id.chatDialog_addUser);

        //start the ShowUsersActivity if pressed
        addBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean markerClicked = false;
                Intent intent = new Intent(getActivity(), ShowUsersActivity.class);
                intent.putExtra("mMarkerClicked", false);
                startActivity(intent);
            }
        });


        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {

        //create a user session if no one is logged in to a chat session
        if(!chatService.isLoggedIn()) {
            Log.d("CREATING NEW SESSION", "NEW CHAT SESSION CREATED");
            createSessionForChat();
        }


        //listview for holding all the chat dialogs
        chatDialogs = (ListView) getView().findViewById(R.id.chatDialogsList);

        //clicking on a dialog will open the ChatMessage associated
        // with that chat dialog on the server (QBChatDialog)
        chatDialogs.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                QBChatDialog qbChatDialog = (QBChatDialog) chatDialogs.getAdapter().getItem(i);
                Intent intent = new Intent (getActivity(), ChatMessage.class);
                intent.putExtra(Common.DIALOG_EXTRA, qbChatDialog);
                startActivity(intent);
            }
        });

        //load the Dialogs
        loadChatDialogs();

    }


    /**
     * Creates a new session for the user
     */
    private void createSessionForChat() {

        //progress dialog for UI purposes, stops when the user session has been created
        final ProgressDialog mDialog = new ProgressDialog(getActivity(), R.style.AppCompatAlertDialogStyle);

        mDialog.setMessage("Getting your information...");
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.show();

        //Load all users and put them in a QBUsersHolder to-be-used for UI purposes
        //(i.e. displaying the userName above a message in the ChatMessage activity)
        QBUsers.getUsers(null).performAsync(new QBEntityCallback<ArrayList<QBUser>>() {
            @Override
            public void onSuccess(ArrayList<QBUser> qbUserArrayList, Bundle bundle) {
                QBUsersHolder.getInstance().putUsers(qbUserArrayList);
            }

            @Override
            public void onError(QBResponseException e) {

            }
        });

        //instantiate a user with the current user info
        final QBUser qbUser = new QBUser(user,password);

        //create the sessions for the user
        QBAuth.createSession(qbUser).performAsync(new QBEntityCallback<QBSession>() {
            @Override
            public void onSuccess(QBSession qbSession, Bundle bundle) {
                qbUser.setId(qbSession.getUserId());
                try {
                    qbUser.setPassword(BaseService.getBaseService().getToken());
                } catch (BaseServiceException e){
                    e.printStackTrace();

                }

                //log the user into a chat service
                chatService.login(qbUser, new QBEntityCallback() {
                    @Override
                    public void onSuccess(Object o, Bundle bundle) {
                        Log.d("createSessionforChat", "Session Created ");
                        mDialog.dismiss();

                        //allow the user to be open for messages and dialogs created by other users
                        QBSystemMessagesManager qbSystemMessagesManager = QBChatService.getInstance().getSystemMessagesManager();
                        qbSystemMessagesManager.addSystemMessageListener(MessagesFragment.this);

                        QBIncomingMessagesManager qbIncomingMessagesManager = QBChatService.getInstance().getIncomingMessagesManager();
                        qbIncomingMessagesManager.addDialogMessageListener(MessagesFragment.this);

                    }

                    @Override
                    public void onError(QBResponseException e) {
                        Log.e("createSessionforChatERR", ""+e.getMessage() );

                    }
                });
            }

            @Override
            public void onError(QBResponseException e) {

            }
        });

    }

    /**
     * Loads all the chatDialogs that the user is in
     * (for UI purposes)
     */
    private void loadChatDialogs() {

        //server request for getting all the dialogs the user is in
        QBRequestGetBuilder requestBuilder = new QBRequestGetBuilder();
        requestBuilder.setLimit(500);


        QBRestChatService.getChatDialogs(null, requestBuilder).performAsync(new QBEntityCallback<ArrayList<QBChatDialog>>() {
            @Override
            public void onSuccess(ArrayList<QBChatDialog> qbChatDialogs, Bundle bundle) {

                //put all the chat Dialogs from the server into a holder that will be used for UI
                QBDialogMessagesHolder.getInstance().putDialogs(qbChatDialogs);



                //storing all the ids of the chat dialogs from the server
                final Set<String> setIds = new HashSet<>();
                for(QBChatDialog chatDialog: qbChatDialogs){
                    setIds.add(chatDialog.getDialogId());
                }

                //get all the unread messages for a given chat dialog
                QBRestChatService.getTotalUnreadMessagesCount(setIds, QBUnreadMessagesHolder.getInstance().getBundle())
                        .performAsync(new QBEntityCallback<Integer>() {
                    @Override
                    public void onSuccess(Integer integer, Bundle bundle) {
                        QBUnreadMessagesHolder.getInstance().setBundle(bundle);

                        //refresh list of dialogs in the MessagesFragment
                        DialogMessagesAdapter adapter = new DialogMessagesAdapter(getActivity().getApplicationContext(), QBDialogMessagesHolder.getInstance().getAllDialogs());
                        chatDialogs.setAdapter(adapter);
                        adapter.notifyDataSetChanged();

                    }

                    @Override
                    public void onError(QBResponseException e) {

                    }
                });

//                Log.d("loadChatDialogsSuccress", "loaded chat dialogs");
            }

            @Override
            public void onError(QBResponseException e) {
                Log.e("loadChatDialogsERROR", e.getMessage() );
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();

        //keep reloading the chatDialogs in the fragment
        loadChatDialogs();
    }

    @Override
    /**
     * For QBSystemMessagesManager
     */
    public void processMessage(QBChatMessage qbChatMessage) {

        //retrieve the dialog that received a chat message and update it's UI in the MessagesFragment
        QBRestChatService.getChatDialogById(qbChatMessage.getBody()).performAsync(new QBEntityCallback<QBChatDialog>() {
            @Override
            public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                // add the current dialog to our list of DialogMessages (the holder)
                QBDialogMessagesHolder.getInstance().putDialog(qbChatDialog);

                //retrieve all our dialogs in our holder
                ArrayList<QBChatDialog> source = QBDialogMessagesHolder.getInstance().getAllDialogs();

                //display all our dialogs in our holder
                DialogMessagesAdapter adapters = new DialogMessagesAdapter(getActivity().getApplicationContext(), source);
                chatDialogs.setAdapter(adapters);
                adapters.notifyDataSetChanged();
            }

            @Override
            public void onError(QBResponseException e) {

            }
        });

    }

    @Override
    /**
     * For QBSystemMessagesManager
     */
    public void processError(QBChatException e, QBChatMessage qbChatMessage) {
        Log.e("ErrorMessagesFragment", e.getMessage());
    }

    @Override
    /**
     * For QBIncomingMessagesManager
     */
    public void processMessage(String s, QBChatMessage qbChatMessage, Integer integer) {
        //when we get a new dialog, reload the UI of our list of chat dialogs
        loadChatDialogs();
    }

    @Override
    /**
     * For QBIncomingMessagesManager
     */
    public void processError(String s, QBChatException e, QBChatMessage qbChatMessage, Integer integer) {

    }
}
