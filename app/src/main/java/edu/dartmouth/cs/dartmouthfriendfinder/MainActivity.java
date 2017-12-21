package edu.dartmouth.cs.dartmouthfriendfinder;

import android.content.Intent;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;

import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.quickblox.chat.QBChatService;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;

import edu.dartmouth.cs.dartmouthfriendfinder.Holder.QBDialogMessagesHolder;

/**
 * Created by johnnybrady
 *  MainActivity uses PageAdapter and a ViewPager to display the three main fragments of the app
 *  (MapFragment, MessagesFragment, SettingsFragment)
 */
public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private PageAdapter mPageAdapter;
    private ViewPager mViewPager;
    private QBChatService chatService;



    private String user, password, workLocation;
    private int id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chatService = QBChatService.getInstance();

        //current user info
        Bundle extras = getIntent().getExtras();
        user = extras.getString("user");
        password = extras.getString("password");
        id = extras.getInt("id");
        workLocation = extras.getString("workLocation");




        mPageAdapter = new PageAdapter(getSupportFragmentManager());

        //set up the ViewPager with the sections adapter
        mViewPager = (ViewPager) findViewById(R.id.container);
        setUpViewPager(mViewPager);

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(mViewPager);


    }

    @Override
    /**
     * Logs out the user out if they press the back button in the MainActivity
     */
    public void onBackPressed() {

        chatService.logout(new QBEntityCallback<Void>() {
            @Override
            public void onSuccess(Void aVoid, Bundle bundle) {

                //empty the Dialog holder
                QBDialogMessagesHolder.getInstance().refreshQBDialogMessagesHolder();

                Toast.makeText(MainActivity.this, "You have been signed out", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(MainActivity.this, SignIn.class);// New activity
                startActivity(intent);
                finish();

            }

            @Override
            public void onError(QBResponseException e) {
                Log.e(TAG, ""+e.getMessage() );
            }
        });
    }

    private void setUpViewPager(ViewPager viewPager){
        PageAdapter adapter = new PageAdapter(getSupportFragmentManager());

        Bundle bundle = new Bundle();
        bundle.putString("password", password);
        bundle.putString("user", user);
        bundle.putString("workLocation", workLocation);
        bundle.putInt("id", id);


        Fragment Map = new MapFragment();
        Fragment Messages = new MessagesFragment();
        Fragment Settings = new SettingsFragment();

        //pass the current user's info to the child fragments
        Map.setArguments(bundle);
        Messages.setArguments(bundle);
        Settings.setArguments(bundle);


        adapter.addFragment(Map, "Map");
        adapter.addFragment(Messages, "Messages");
        adapter.addFragment(Settings, "Settings");
        viewPager.setAdapter(adapter);
    }


}
