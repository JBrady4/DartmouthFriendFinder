package edu.dartmouth.cs.dartmouthfriendfinder.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.amulyakhare.textdrawable.TextDrawable;
import com.amulyakhare.textdrawable.util.ColorGenerator;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.model.QBChatDialog;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;

import edu.dartmouth.cs.dartmouthfriendfinder.Holder.QBUnreadMessagesHolder;
import edu.dartmouth.cs.dartmouthfriendfinder.R;

/**
 * Created by johnnybrady
 *
 * Adapter for inflating the ListView of Chat Dialogs in the MessagesFragment
 */

public class DialogMessagesAdapter extends BaseAdapter {

    private Context context;
    private ArrayList<QBChatDialog> qbChatDialogs;


    public DialogMessagesAdapter(Context context, ArrayList<QBChatDialog> qbChatDialogs) {
        this.context = context;
        this.qbChatDialogs = qbChatDialogs;
    }

    @Override
    public int getCount() {
        return qbChatDialogs.size();
    }

    public ArrayList<QBChatDialog> getQBChatDialogs(){
        return this.qbChatDialogs;
    }

    @Override
    public Object getItem(int i) {
        return qbChatDialogs.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }



    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {

        View view = convertView;
        if(view == null){
            LayoutInflater inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            view = inflater.inflate(R.layout.chat_dialogs,null);

            TextView textTitle, textMessage;
            ImageView imageView, imageUnread;

            textMessage = (TextView)view.findViewById(R.id.chat_dialogs_message);
            textTitle = (TextView)view.findViewById(R.id.chat_dialogs_title);
            imageView = (ImageView) view.findViewById(R.id.chatDialog_image);
            imageUnread = (ImageView) view.findViewById(R.id.image_unread);

            textTitle.setText(qbChatDialogs.get(i).getName());

            textMessage.setText(qbChatDialogs.get(i).getLastMessage());



            TextDrawable.IBuilder builder = TextDrawable.builder().beginConfig().withBorder(8).endConfig().roundRect(10);

            //Get first character from Chat DialogTitle for creating chat Dialog image
            TextDrawable drawable = builder.build(textTitle.getText().toString().substring(0,1).toUpperCase(), Color.rgb((int) (Math.random()*255),(int) (Math.random()*255),(int) (Math.random()*255)));

            imageView.setImageDrawable(drawable);

            //set Message unread count
            TextDrawable.IBuilder unreadBuilder = TextDrawable.builder().beginConfig().withBorder(4).endConfig().round();
            int unread_count = QBUnreadMessagesHolder.getInstance().getBundle().getInt(qbChatDialogs.get(i).getDialogId());

            if(unread_count >0){
                TextDrawable unread_drawable = unreadBuilder.build(String.valueOf(unread_count), Color.RED);
                imageUnread.setImageDrawable(unread_drawable);
            }

        }

        return view;
    }
}
