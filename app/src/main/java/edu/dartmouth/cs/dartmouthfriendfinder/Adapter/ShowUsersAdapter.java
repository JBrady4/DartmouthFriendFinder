package edu.dartmouth.cs.dartmouthfriendfinder.Adapter;

import android.content.Context;
import android.support.annotation.StyleRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.quickblox.chat.model.QBChatDialog;
import com.quickblox.users.model.QBUser;

import java.util.ArrayList;

import edu.dartmouth.cs.dartmouthfriendfinder.R;

/**
 * Created by johnnybrady
 *
 * Adapter for inflating the ListView of users in the ShowUserActivity
 */

public class ShowUsersAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<QBUser> qbUserArrayList;

    public ShowUsersAdapter(Context context, ArrayList<QBUser> qbUserArrayList) {
        this.context = context;
        this.qbUserArrayList = qbUserArrayList;
    }

    @Override
    public int getCount() {
        return qbUserArrayList.size();
    }

    @Override
    public Object getItem(int i) {
        return qbUserArrayList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        View view = convertView;
        if(convertView == null){
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            view = inflater.inflate(android.R.layout.simple_list_item_multiple_choice, null);
            TextView textView = (TextView) view.findViewById(android.R.id.text1);
            textView.setTextSize(16);
            textView.setText(qbUserArrayList.get(i).getLogin());
        }

        return view;
    }
}
