package edu.dartmouth.cs.dartmouthfriendfinder;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import com.quickblox.chat.QBChat;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBRestChatService;
import com.quickblox.chat.QBSystemMessagesManager;
import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.chat.model.QBChatMessage;
import com.quickblox.chat.model.QBDialogType;
import com.quickblox.chat.utils.DialogUtils;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.request.QBPagedRequestBuilder;
import com.quickblox.core.request.QBRequestGetBuilder;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import org.jivesoftware.smack.SmackException;

import java.lang.reflect.Array;
import java.util.ArrayList;

import edu.dartmouth.cs.dartmouthfriendfinder.Adapter.ShowUsersAdapter;
import edu.dartmouth.cs.dartmouthfriendfinder.Common.Common;
import edu.dartmouth.cs.dartmouthfriendfinder.Holder.QBUsersHolder;

/**
 * Created by johnnybrady
 * Shows a list of users that are on the Quickblox server
 *
 * (after you click the Floating Action button to create a new chat Dialog in the MessagesFragment or
 * you click a marker on the map)
 */

public class ShowUsersActivity extends AppCompatActivity {

    ListView showUsers;
    Button btnCreateChat;
    ArrayList<QBChatDialog> chatDialogs;

    //handles if a user clicked on a user on the map and decided to start a chat
    boolean markerClicked;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_users);

        //get check if the user clicked a marker on the map to start a chat
        markerClicked = getIntent().getBooleanExtra("mMarkerClicked", false);

        //if the user clicked on a user on the map
        if(markerClicked){
            //show the activity with only that user listed
            retrieveOneUser(getIntent().getStringExtra("userName"));
        }
        else{
            //retrieve all other users because the only other way to get this activity is when a user
            //clicks the Floating Action button in the messagesFragment
            retrieveAllUsers();
        }



        //get the ListView to display the users
        showUsers = (ListView) findViewById(R.id.showUsers);
        showUsers.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        //button to create a ChatMessage
        btnCreateChat = (Button) findViewById(R.id.createChatButton);

        btnCreateChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d("COUNT_create", String.valueOf(showUsers.getCheckedItemPositions().size()));
//
                SparseBooleanArray array = showUsers.getCheckedItemPositions();
                int checked = 0;

                //get the number of checkedUsers
                for(int i = 0; i<array.size(); i++){
                    if(array.valueAt(i)){
                        checked++;
                    }
                }

                Log.d("COUNT_create", "checked: "+String.valueOf(checked));

                //check how many users were checked when the current user chose to create a chat
                if(checked== 1 ){
                    createPrivateChat(showUsers.getCheckedItemPositions());
                } else if (checked >1){
                    createGroupChat(showUsers.getCheckedItemPositions());
                }
                else{
                    Toast.makeText(ShowUsersActivity.this, "Select friends to chat", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    /**
     * Create a group chat if multiple users are selected
     * @param checkedItemPositions
     */
    private void createGroupChat(SparseBooleanArray checkedItemPositions) {

        //dialog for UI purposes, dismissed when the chat group is created
        final ProgressDialog mDialog = new ProgressDialog(ShowUsersActivity.this, R.style.AppCompatAlertDialogStyle);
        mDialog.setMessage("Creating group chat...");
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.show();


        int countChoice = showUsers.getCount();
        //holds the list of ids of the occupants in group chat to-be-created
        ArrayList<Integer> occupantIdsList = new ArrayList<>();

        //iterate through the users that were checked and put their ids in the occupantIdsList
        for( int i = 0; i < countChoice;i++){

            if(checkedItemPositions.get(i)){
                QBUser user = (QBUser) showUsers.getItemAtPosition(i);

                occupantIdsList.add(user.getId());
            }
        }
        //add the creator of the chat to the list (for creating the name of the group)
        occupantIdsList.add(QBChatService.getInstance().getUser().getId());

        //instantiate a Chat Dialog (group chat to-be-displayed in the MessagesFragment)
        QBChatDialog dialog = DialogUtils.buildDialog(Common.createChatDialogName(occupantIdsList), QBDialogType.GROUP, occupantIdsList);

        //create the Dialog on the Quickblox server
        QBRestChatService.createChatDialog(dialog).performAsync(new QBEntityCallback<QBChatDialog>() {
            @Override
            public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                mDialog.dismiss();

//                Toast.makeText(getBaseContext(), "Created group chat dialog successfully", Toast.LENGTH_LONG).show();

                //send system message to recipient Id user
                QBSystemMessagesManager qbSystemMessagesManager = QBChatService.getInstance().getSystemMessagesManager();
                QBChatMessage qbChatMessage = new QBChatMessage();
                qbChatMessage.setBody(qbChatDialog.getDialogId());
                for (int i =0; i<qbChatDialog.getOccupants().size();i++){
                    //send the message to all the users on the server
                    qbChatMessage.setRecipientId(qbChatDialog.getOccupants().get(i));

                    try{
                        qbSystemMessagesManager.sendSystemMessage(qbChatMessage);
                    } catch (SmackException.NotConnectedException e){
                        e.printStackTrace();
                    }
                }


                finish();
            }

            @Override
            public void onError(QBResponseException e) {
                Log.e("ERROR", e.getMessage() );
            }
        });
    }

    /**
     * Create a private chat if only one user is clicked in the list
     * @param checkedItemPositions
     */
    private void createPrivateChat(SparseBooleanArray checkedItemPositions) {
        //dialog for UI purposes, dismissed when the private group is created
        final ProgressDialog mDialog = new ProgressDialog(ShowUsersActivity.this, R.style.AppCompatAlertDialogStyle);
        mDialog.setMessage("Creating private chat...");
        mDialog.setCanceledOnTouchOutside(false);
        mDialog.show();

        int countChoice = showUsers.getCount();

        for( int i = 0; i < countChoice;i++){

            //find the checked user
            if(checkedItemPositions.get(i)){

                //instantiate the user
                final QBUser user = (QBUser) showUsers.getItemAtPosition(i);

                //create a private chat dialog between current user and checked user
                QBChatDialog dialog = DialogUtils.buildPrivateDialog(user.getId());


                //create chat dialog on server
                QBRestChatService.createChatDialog(dialog).performAsync(new QBEntityCallback<QBChatDialog>() {
                    @Override
                    public void onSuccess(QBChatDialog qbChatDialog, Bundle bundle) {
                        mDialog.dismiss();
//                        Toast.makeText(getBaseContext(), "Created private chat dialog successfully", Toast.LENGTH_LONG).show();

                        //send system message to recipient Id user
                        QBSystemMessagesManager qbSystemMessagesManager = QBChatService.getInstance().getSystemMessagesManager();
                        QBChatMessage qbChatMessage = new QBChatMessage();
                        qbChatMessage.setRecipientId(user.getId());
                        qbChatMessage.setBody(qbChatDialog.getDialogId());

                        try{
                            qbSystemMessagesManager.sendSystemMessage(qbChatMessage);
                        } catch (SmackException.NotConnectedException e){
                            e.printStackTrace();
                        }

                        finish();
                    }

                    @Override
                    public void onError(QBResponseException e) {
                        Log.e("ERROR", e.getMessage() );
                    }
                });

            }
        }




    }

    /**
     * Fetches all the users on the Quickblox server
     */
    private void retrieveAllUsers() {

        QBUsers.getUsers(null).performAsync(new QBEntityCallback<ArrayList<QBUser>>() {
            @Override
            public void onSuccess(ArrayList<QBUser> qbUsers, Bundle bundle) {

                //add the users to the cache for UI purposes
                QBUsersHolder.getInstance().putUsers(qbUsers);

                ArrayList<QBUser> qbUserWithoutCurrent = new ArrayList<QBUser>();

                //List every user in the activity except the current user
                //(don't want to start chat dialog with self)
                for(QBUser user : qbUsers){
                    if(!user.getLogin().equals(QBChatService.getInstance().getUser().getLogin())){
                        qbUserWithoutCurrent.add(user);
                    }
                }

                //instantiate the adapter for showing all the users in the activity
                ShowUsersAdapter adapter = new ShowUsersAdapter(getBaseContext(),qbUserWithoutCurrent);
                showUsers.setAdapter(adapter);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(QBResponseException e) {
                Log.e("ERROR",e.getMessage() );
            }
        });
    }

    /**
     * Fetches the specific user from Quickblox server
     * @param userName
     */
    private void retrieveOneUser(String userName){

        QBUsers.getUserByLogin(userName).performAsync(new QBEntityCallback<QBUser>() {
            @Override
            public void onSuccess(QBUser qbUser, Bundle bundle) {
                ArrayList<QBUser> qbUserList = new ArrayList<>();
                qbUserList.add(qbUser);

                //adapter will only display the singular user
                ShowUsersAdapter adapter = new ShowUsersAdapter(getBaseContext(),qbUserList);
                showUsers.setAdapter(adapter);
                adapter.notifyDataSetChanged();

            }

            @Override
            public void onError(QBResponseException e) {

            }
        });
    }


}
