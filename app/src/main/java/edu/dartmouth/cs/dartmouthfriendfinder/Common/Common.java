package edu.dartmouth.cs.dartmouthfriendfinder.Common;

import com.quickblox.users.model.QBUser;

import java.util.List;

import edu.dartmouth.cs.dartmouthfriendfinder.Holder.QBUsersHolder;

/**
 * Created by johnnybrady
 *
 * Helper class to create the name of the chat dialog on the Quickblox server
 */

public class Common {

    public static final String DIALOG_EXTRA = "Dialogs";

    public static String createChatDialogName(List<Integer> qbUsers){
        List<QBUser> qbUsers1 = QBUsersHolder.getInstance().getUsersByIds(qbUsers);
        StringBuilder name = new StringBuilder();
        for(QBUser user : qbUsers1){
            name.append(user.getFullName()).append(" ");

            if(name.length() > 30){
                name = name.replace(30, name.length()-1,"...");
            }

        }

        return name.toString();
    }
}
