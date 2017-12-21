package edu.dartmouth.cs.dartmouthfriendfinder;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.quickblox.auth.session.QBSettings;
import com.quickblox.chat.QBChatService;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.ServiceZone;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.request.QBRequestGetBuilder;
import com.quickblox.customobjects.QBCustomObjects;
import com.quickblox.customobjects.model.QBCustomObject;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import java.util.ArrayList;

/**
 * Created by johnnybrady
 */

public class SignIn extends AppCompatActivity{

    //required info for connecting to Quickblox server
    static final String APP_ID = "66092";
    static final String AUTH_KEY = "jMqyhdXZ953N7m3";
    static final String AUTH_SECRET = "6gjnvM3MOKf5cRg";
    static final String ACCOUNT_KEY = "rW_YryhFB8PJQCyZLY2M";


    //intent to be sent to MainActivity after user logs in
    Intent intent;

    Button btnLogin, btnSignUp;
    EditText edtUser, edtPassword;

    @Override
    protected void onCreate( Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.signin);

        //connect to Quickblox server
        intializeFramework();


        //retrieve views for signin page
        btnLogin = (Button) findViewById(R.id.login_buttonSignIn);
        btnSignUp = (Button) findViewById(R.id.signup_buttonSignIn);

        edtPassword = (EditText) findViewById(R.id.main_editPasswordSignIn);
        edtUser = (EditText) findViewById(R.id.main_editLoginSignIn);


        //SignUp button onClickListener for starting signup activity
        btnSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(SignIn.this, SignUp.class));
            }
        });


        //Login button onClickListener for handling successful/unsuccessful login attempts
        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //get user inputs
               final String user = edtUser.getText().toString();
               final String passwd = edtPassword.getText().toString();


                //instantiate the Quickblox user
               QBUser qbUser = new QBUser(user, passwd);

                //sign user into server
                QBUsers.signIn(qbUser).performAsync(new QBEntityCallback<QBUser>() {

                    @Override
                    public void onSuccess(QBUser qbUser, Bundle bundle) {
//                        Toast.makeText(getBaseContext(), "Login Successful", Toast.LENGTH_LONG).show();
                        intent = new Intent(SignIn.this, MainActivity.class);

                        // user info to be passed to the MainActivity
                        intent.putExtra("id", qbUser.getId());
                        intent.putExtra("user", user);
                        intent.putExtra("password",passwd);


                        //requestBuilder for retrieving the user's workLocation by using the user's ID
                        // (needs to be passed to MainActivity)
                        QBRequestGetBuilder requestGetBuilder = new QBRequestGetBuilder();
                        requestGetBuilder.eq("userID", qbUser.getId());

                        //the request
                        QBCustomObjects.getObjects("UserInfo", requestGetBuilder).performAsync(new QBEntityCallback<ArrayList<QBCustomObject>>() {
                            @Override
                            public void onSuccess(ArrayList<QBCustomObject> qbCustomObjects, Bundle bundle) {

                                //check if the returned object for userWorkLocation is a String
                                if(qbCustomObjects.get(0).get("userWorkLocation").getClass().equals(String.class)){

                                    //put the user's workLocation in the intent to be sent to the MainActivity
                                    intent.putExtra("workLocation", (String) qbCustomObjects.get(0).get("userWorkLocation"));
                                }
                                else{
                                    //the user doesn't have a String for the work, meaning nothing exists in the DB for it
                                    //make workLocation an empty String
                                    intent.putExtra("workLocation", "");
                                }

                                startActivity(intent);
                                finish();
                            }

                            @Override
                            public void onError(QBResponseException e) {

                            }
                        });




                    }

                    @Override
                    public void onError(QBResponseException e) {
                        Toast.makeText(getBaseContext(), "Check your login credentials and internet connection", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }

    /**
     * Connects to Quickblox server
     */
    private void intializeFramework() {
        QBSettings.getInstance().init(getApplicationContext(),APP_ID,AUTH_KEY,AUTH_SECRET);
        QBSettings.getInstance().setAccountKey(ACCOUNT_KEY);

    }
}
