package edu.dartmouth.cs.dartmouthfriendfinder;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.InflateException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.location.places.ui.SupportPlaceAutocompleteFragment;
import com.quickblox.chat.QBChatService;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.request.QBRequestGetBuilder;
import com.quickblox.customobjects.QBCustomObjects;
import com.quickblox.customobjects.model.QBCustomObject;
import com.quickblox.users.model.QBUser;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.HashMap;

import edu.dartmouth.cs.dartmouthfriendfinder.Holder.QBDialogMessagesHolder;

/**
 * Created by johnnybrady
 *
 */

public class SettingsFragment extends Fragment {

    private static final String TAG="SettingsFragment";

    private Button btn, btn2;
    private TextView userName, passWord, work;
    private EditText bio, name;
    boolean isLoggedIn;

    //the settings view
    private static View view;

    //Strings for handling UI and updating the UI
    String user, password, workLocation, currentBio;



    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {


        //remove the view from the parent view
        //makes sure to always inflate a new view
        if (view != null) {
            ViewGroup parent = (ViewGroup) view.getParent();
            if (parent != null)
                parent.removeView(view);
        }

        try {
            view = inflater.inflate(R.layout.settings, container, false);
        } catch (InflateException e) {
            Log.e("ERROR", e.getMessage() );
        }


        //buttons for handling logout and updates
        btn = (Button) view.findViewById(R.id.btn_settings);

        btn2 = (Button) view.findViewById(R.id.btn_update);

        //retrieve the current user login and password info
        user = getArguments().getString("user");
        password = getArguments().getString("password");


        //update UI for the fragment
        updateUIUserInfo();


        //get all the views
        userName = (TextView) view.findViewById(R.id.userName);
        passWord = (TextView) view.findViewById(R.id.password);
        bio = (EditText) view.findViewById(R.id.bio_input);
        bio.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                currentBio = charSequence.toString();
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
//        work = (TextView) view.findViewById(R.id.current_address_text);

        //set the view user login and password textViews
        userName.setText("username: "+user);
        passWord.setText("password: "+password);




        final QBChatService chatService = QBChatService.getInstance();

        isLoggedIn = chatService.isLoggedIn();

        //logout button onClickListener
        btn.setOnClickListener(new View.OnClickListener() {
            @Override

            //redirect to the signin page
            public void onClick(View view) {
                if(!isLoggedIn){
                    return;
                }

                //logout the current user from the chat service
                //so any user can use the same phone and log in using that device
                chatService.logout(new QBEntityCallback<Void>() {
                    @Override
                    public void onSuccess(Void aVoid, Bundle bundle) {
                        Toast.makeText(getActivity(), "You have successfully signed out", Toast.LENGTH_LONG).show();

                        //clear the list of chat dialogs in the messages tab
                        QBDialogMessagesHolder.getInstance().refreshQBDialogMessagesHolder();

                        //redirect to the sign in page
                        Intent intent = new Intent(getActivity(), SignIn.class);
                        startActivity(intent);
                        getActivity().finish();

                    }

                    @Override
                    public void onError(QBResponseException e) {
                        Log.e(TAG, ""+e.getMessage() );
                    }
                });


            }
        });


        //update button onClicklistener
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override


            public void onClick(View view) {
                updateQBUserInfo();
            }
        });


        return view;
    }

    /**
     *  Updates the server information when the user clicks the update button
     */
    private void updateQBUserInfo(){

        QBRequestGetBuilder requestBuilder = new QBRequestGetBuilder();
        requestBuilder.eq("userName", user);


        QBCustomObjects.getObjects("UserInfo", requestBuilder).performAsync(new QBEntityCallback<ArrayList<QBCustomObject>>() {
            @Override
            public void onSuccess(ArrayList<QBCustomObject> qbCustomObjects, Bundle bundle) {


                    for(QBCustomObject customObject : qbCustomObjects){

                        HashMap<String, Object> fields = new HashMap<>();
                        fields.put("userWorkLocation", workLocation);
                        fields.put("userBio", currentBio);


                        customObject.setFields(fields);

                        QBCustomObjects.updateObject(customObject, null).performAsync(new QBEntityCallback<QBCustomObject>() {
                            @Override
                            public void onSuccess(QBCustomObject qbCustomObject, Bundle bundle) {
                                Toast.makeText(getActivity().getApplicationContext(), "Updated user", Toast.LENGTH_LONG).show();

                            }

                            @Override
                            public void onError(QBResponseException e) {
                                Log.e("ERROR", e.getMessage() );
                            }


                        });
                    }

            }

            @Override
            public void onError(QBResponseException e) {
                Log.e("ERROR", e.getMessage());

            }
        });
    }


    /**
     *  Updates the settings fragment UI after an activity reload or user changes work address
     */

    private void updateUIUserInfo(){

        //server quest for getting all users with 'user' login name (only 1)
        QBRequestGetBuilder requestBuilder = new QBRequestGetBuilder();
        requestBuilder.eq("userName", user);


        QBCustomObjects.getObjects("UserInfo", requestBuilder).performAsync(new QBEntityCallback<ArrayList<QBCustomObject>>() {
            @Override
            public void onSuccess(ArrayList<QBCustomObject> qbCustomObjects, Bundle bundle) {


                //check if the only custom object workLocation returned from server is a String
                if(qbCustomObjects.get(0).get("userWorkLocation").getClass().equals(String.class)){

                    //set the users workLocation to that location
                    workLocation = (String) qbCustomObjects.get(0).get("userWorkLocation");
                }
                else{
                    workLocation = "";
                }

                if(qbCustomObjects.get(0).get("userBio").getClass().equals(String.class)){

                    //set the users currentBio to the entered bio
                    currentBio = (String) qbCustomObjects.get(0).get("userBio");
                }
                else{
                    currentBio = "No bio available.";
                }

                bio.setText(currentBio);


//                Log.d("workLocation", workLocation);

//                work.setText("Current work location: "+workLocation);

                //Google Fragment setup for handling autocomplete address search
                PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                        getActivity().getFragmentManager().findFragmentById(R.id.autocomplete_fragment);

                autocompleteFragment.setText(workLocation);


                AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                        .setTypeFilter(AutocompleteFilter.TYPE_FILTER_ADDRESS)
                        .build();
                autocompleteFragment.setFilter(typeFilter);

                autocompleteFragment.setOnPlaceSelectedListener(new PlaceSelectionListener() {
                    @Override
                    public void onPlaceSelected(Place place) {
                        Log.d("GOT PLACE", "Place: " + place.getAddress().toString());

                        //if the user selects a new work address, change workLocation for handling UI changes
                        //and change the current UI
                        workLocation = place.getAddress().toString();
//                        work.setText(place.getAddress().toString());
                    }

                    @Override
                    public void onError(Status status) {
                        Log.e("ERROR", status.toString());
                    }
                });


            }

            @Override
            public void onError(QBResponseException e) {
                Log.e("ERROR", e.getMessage());

            }
        });
    }

}
