package edu.dartmouth.cs.dartmouthfriendfinder;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.quickblox.auth.QBAuth;
import com.quickblox.auth.session.QBSession;
import com.quickblox.chat.QBChatService;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import edu.dartmouth.cs.dartmouthfriendfinder.Holder.QBDialogMessagesHolder;

/**
 *  Created by johnnybrady
 */
public class SignUp extends AppCompatActivity {

    Button btnSignUp, btnCancel;
    EditText edtUser, edtPassword, edtFullName;
    String workLocation;


    //current Quickblox chat service
    QBChatService chatService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signup);

        //get current chatService
        chatService = QBChatService.getInstance();

        //create a new user session
        registerSession();

        //get activity views
        btnSignUp = (Button) findViewById(R.id.signup_buttonSignUp);
        btnCancel = (Button) findViewById(R.id.cancel_button);

        edtPassword = (EditText) findViewById(R.id.main_editPasswordSignUp);
        edtUser = (EditText) findViewById(R.id.main_editLoginSignUp);
        edtFullName = (EditText) findViewById(R.id.main_editFullNameSignUp);


        //Google Fragment setup for handling autocomplete address search
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.place_autocomplete_fragment);

        AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                .setTypeFilter(AutocompleteFilter.TYPE_FILTER_ADDRESS)
                .build();
        autocompleteFragment.setFilter(typeFilter);

        autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
            @Override
            public void onPlaceSelected(Place place) {
                Log.d("GOT PLACE", "Place: " + place.getAddress().toString());
                workLocation = place.getAddress().toString();
            }

            @Override
            public void onError(Status status) {
                Log.e("ERROR", status.toString());
            }
        });



        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });

        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //check if all fields are filled
                if(edtPassword != null && edtFullName !=null && edtUser!=null && workLocation != null){
                    final String user = edtUser.getText().toString();
                    final String passwd = edtPassword.getText().toString();

                    //instantiate a Quickblox user
                    QBUser qbUser = new QBUser(user, passwd);

                    qbUser.setFullName(edtFullName.getText().toString());

                    //signup the user and add the user the Quickblox list of users
                    QBUsers.signUp(qbUser).performAsync(new QBEntityCallback<QBUser>() {
                        @Override
                        public void onSuccess(QBUser qbUser, Bundle bundle) {
                            Toast.makeText(getBaseContext(), "Sign Up successful", Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(SignUp.this, MainActivity.class);

                            //user info to be passed to the MainActivity
                            intent.putExtra("user", user);
                            intent.putExtra("password", passwd);
                            intent.putExtra("id", qbUser.getId());
                            intent.putExtra("workLocation", workLocation);

                            //logout of the current chat service
                            // if you just logged out of an existing user to create a new one
                            if (chatService.isLoggedIn()) {
                                chatService.logout(new QBEntityCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid, Bundle bundle) {
                                        Log.d("signuplogout", "logged out of chat session before creating new one ");
                                    }

                                    @Override
                                    public void onError(QBResponseException e) {
                                        Log.e("signuplogout", "" + e.getMessage());
                                    }
                                });
                            }

                            startActivity(intent);
                        }

                        @Override
                        public void onError(QBResponseException e) {
                            Toast.makeText(getBaseContext(), "" + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                }
                else{
                    Toast.makeText(getBaseContext(), "Fill out all fields", Toast.LENGTH_LONG).show();
                }
            }
        });

    }


    /**
     * Creating a new user Quickblox session for chat dialogs
     */
    private void registerSession() {
        QBAuth.createSession().performAsync(new QBEntityCallback<QBSession>() {
            @Override
            public void onSuccess(QBSession qbSession, Bundle bundle) {

            }

            @Override
            public void onError(QBResponseException e) {
                Log.e("ERROR", e.getMessage());
            }
        });
    }

}
