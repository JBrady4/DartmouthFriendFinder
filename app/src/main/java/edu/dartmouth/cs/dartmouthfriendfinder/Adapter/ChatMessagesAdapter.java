package edu.dartmouth.cs.dartmouthfriendfinder.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.github.library.bubbleview.BubbleTextView;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.model.QBChatMessage;

import java.util.ArrayList;

import edu.dartmouth.cs.dartmouthfriendfinder.Holder.QBUsersHolder;
import edu.dartmouth.cs.dartmouthfriendfinder.R;

/**
 * Created by johnnybrady
 *
 * Adapter for inflating the ListView of messages in the ChatMessageActivity
 */

public class ChatMessagesAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<QBChatMessage> qbChatMessages;

    public ChatMessagesAdapter(Context context,ArrayList<QBChatMessage> qbChatMessages) {

        this.context = context;
        this.qbChatMessages = qbChatMessages;
    }

    @Override
    public int getCount() {
        return qbChatMessages.size();
    }

    @Override
    public Object getItem(int i) {
        return qbChatMessages.get(i);
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

            if(qbChatMessages.get(i).getSenderId().equals(QBChatService.getInstance().getUser().getId())) {
                view = inflater.inflate(R.layout.send_message,null);
                BubbleTextView bubleTextView = (BubbleTextView)view.findViewById(R.id.message);
                bubleTextView.setText(qbChatMessages.get(i).getBody());
            }
            else{
                view = inflater.inflate(R.layout.receive_message,null);
                BubbleTextView bubleTextView = (BubbleTextView)view.findViewById(R.id.message);
                bubleTextView.setText(qbChatMessages.get(i).getBody());
                TextView textName = (TextView)view.findViewById(R.id.userId);
                textName.setText(QBUsersHolder.getInstance().getUserById(qbChatMessages.get(i).getSenderId()).getFullName());
            }
        }

        return view;

    }
}
