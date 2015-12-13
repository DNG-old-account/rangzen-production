package org.denovogroup.rangzen.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.RadioGroup;
import android.widget.Toast;

import org.denovogroup.rangzen.R;
import org.denovogroup.rangzen.backend.*;
import org.denovogroup.rangzen.backend.SecurityManager;
import org.denovogroup.rangzen.objects.BetaUser;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by Liran on 12/13/2015.
 */
public class BetaFragment extends Fragment {

    List<BetaUser> betaUsers;
    BetaUser selectedUser;
    int serialNumber = 1;
    RadioGroup radioGroup;
    Button selectSerial;

    AsyncTask task = null;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.beta_fragment, container, false);

        if(betaUsers == null){
            getBetaUsers();
        }

        selectSerial = (Button) v.findViewById(R.id.serial_number);
        selectSerial.setText(String.valueOf(serialNumber));
        selectSerial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Device serial number");
                builder.setMessage("select unique serial number");

                final NumberPicker numberPicker = new NumberPicker(getActivity());
                numberPicker.setMinValue(1);
                numberPicker.setMaxValue(15);
                numberPicker.setValue(serialNumber);

                builder.setView(numberPicker);
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        serialNumber = numberPicker.getValue();
                        selectSerial.setText(String.valueOf(serialNumber));
                        dialog.dismiss();
                    }
                });
                builder.show();
            }
        });

        radioGroup = (RadioGroup) v.findViewById(R.id.message_number);

        v.findViewById(R.id.select_user_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(betaUsers != null){
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle("Select beta user");

                    final ListView dialogView = new ListView(getActivity());

                    builder.setView(dialogView);
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    AlertDialog dialog = builder.create();
                    dialog.setOnShowListener(new DialogInterface.OnShowListener() {
                        @Override
                        public void onShow(final DialogInterface dialog) {
                            dialogView.setAdapter(new ArrayAdapter<>(getActivity() , android.R.layout.simple_dropdown_item_1line, betaUsers));
                            dialogView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    selectedUser = betaUsers.get(position);
                                    ((Button)getView().findViewById(R.id.select_user_button)).setText(selectedUser.getName());
                                    dialog.dismiss();
                                }
                            });
                        }
                    });
                    dialog.show();
                }
            }
        });

        v.findViewById(R.id.setup_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(selectedUser != null){

                    final int radioId = radioGroup.getCheckedRadioButtonId();
                    int messagesToPush = 0;

                    switch (radioId){
                        case R.id.radio_push_10:
                            messagesToPush = 10;
                            break;
                        case R.id.radio_push_50:
                            messagesToPush = 50;
                            break;
                    }

                    final int messageOffset = serialNumber-1;

                    if((messageOffset+1) * messagesToPush > MESSAGES.length){
                        Toast.makeText(getActivity(),"Too many devices, pushing "+messagesToPush+" messages support up to "+Math.floor(MESSAGES.length/messagesToPush)+" devices",Toast.LENGTH_LONG).show();
                        return;
                    }

                    final int finalMessagesToPush = messagesToPush;
                    task = new AsyncTask() {

                        AlertDialog progdialog;

                        @Override
                        protected void onPreExecute() {
                            super.onPreExecute();

                            progdialog = new AlertDialog.Builder(getActivity())
                                    .setTitle("Setting up test")
                                    .setMessage("please wait")
                                    .setCancelable(false)
                                    .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (task != null) task.cancel(true);
                                            dialog.dismiss();
                                        }
                                    })
                                    .show();
                        }

                        @Override
                        protected void onPostExecute(Object o) {
                            super.onPostExecute(o);
                            progdialog.dismiss();
                            Toast.makeText(getActivity(), "Test ready", Toast.LENGTH_SHORT).show();
                            task = null;
                        }

                        @Override
                        protected Object doInBackground(Object[] params) {
                            MessageStore mStore = MessageStore.getInstance(getActivity());
                            FriendStore fStore = FriendStore.getInstance(getActivity());

                            //cleanup
                            mStore.purgeStore();
                            fStore.purgeStore();

                            SecurityManager.setCurrentPseudonym(getActivity(), selectedUser.getName());
                            for(String friend : selectedUser.getFriends()) {
                                fStore.addFriend(friend,friend,FriendStore.ADDED_VIA_QR, null);
                            }

                            Random random = new Random();

                            for(int i = finalMessagesToPush * messageOffset;
                                i < Math.min(MESSAGES.length, finalMessagesToPush * (1+messageOffset));
                                i++){

                                mStore.addMessage(
                                        Utils.makeTextSafeForSQL(MESSAGES[i]),
                                        random.nextDouble(),
                                        0,
                                        selectedUser.getName(),
                                        System.currentTimeMillis(),
                                        true,
                                        -1,
                                        null);

                            }
                            return null;
                        }
                    };
                    task.execute();
                }

            }
        });

        return v;
    }

    private void getBetaUsers(){

        AsyncTask<Void,Void,Void> task = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                betaUsers = new ArrayList<>();

                try {
                    JSONArray users = new JSONArray(SOCIAL_GRAPH);

                    for(int i=0; i<users.length();i++){
                        JSONObject userJson = (JSONObject) users.get(i);

                        String usersString = userJson.getString("friends");
                        usersString = usersString.substring(usersString.indexOf("[")+1,usersString.indexOf("]"));

                        String[] friends = usersString.split(",");

                        BetaUser user = new BetaUser(
                                userJson.getInt("position"),
                                userJson.getString("pseudonym"),
                                friends
                        );

                        betaUsers.add(user);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return null;
            }
        };
        task.execute();
    }

    private static final String SOCIAL_GRAPH = "[{\"position\":0,\"pseudonym\":\"Nicole\",\"friends\":\"[Sara, Sandra, Michelle, Carol, Brenda, Annie, Jeremy, Sean, Fred, Tina, Shawn, Anthony, Marie, Bruce, Joseph, Nicholas, Peter, Randy, Anne, Henry, Alice, Stephen, Mildred, Frances, Brian, Keith, Gloria, Carolyn, Jerry, Rose, Paula, Russell, Ralph, Debra, Andrew, Wayne, William, Donna, Norma, Evelyn, Larry, Samuel, Roy, Carl, Judith, Kenneth, James, Ryan, Christopher, Julia, Margaret, Angela, Gerald, Michael, Walter, Ruby, Jennifer, Kathy, Donald, Roger, Gregory, Rachel, Scott, Joshua, Louise, Heather, Joyce, Arthur, Cynthia, Steve, Deborah, Dorothy, Sarah]\"},{\"position\":1,\"pseudonym\":\"Sara\",\"friends\":\"[Nicole, Fred]\"},{\"position\":2,\"pseudonym\":\"Sandra\",\"friends\":\"[Nicole, Bruce, Randy]\"},{\"position\":3,\"pseudonym\":\"Michelle\",\"friends\":\"[Nicole, James, Deborah]\"},{\"position\":4,\"pseudonym\":\"Carol\",\"friends\":\"[Nicole, Tina, Marie, Mildred, Frances, Brian, Gloria, Jerry, Ralph, Julia, Carol, Peter, Rachel, Larry, Joyce, Arthur]\"},{\"position\":5,\"pseudonym\":\"Brenda\",\"friends\":\"[Nicole, Walter]\"},{\"position\":6,\"pseudonym\":\"Annie\",\"friends\":\"[Nicole, Andrew, Stephen, Fred, Louise, Steve, Deborah]\"},{\"position\":7,\"pseudonym\":\"Jeremy\",\"friends\":\"[Nicole, Marie, Anne, Brian, Julia, William, Gregory]\"},{\"position\":8,\"pseudonym\":\"Sean\",\"friends\":\"[Nicole, Peter, Brian, Bruce, Debra, Kenneth, Angela, Walter, Steve]\"},{\"position\":9,\"pseudonym\":\"Fred\",\"friends\":\"[Nicole, Marie, Randy, Anne, Mildred, Louise, Stephen, Roy]\"},{\"position\":10,\"pseudonym\":\"Tina\",\"friends\":\"[Nicole, Carol, Wayne, Sean, Deborah]\"},{\"position\":11,\"pseudonym\":\"Shawn\",\"friends\":\"[Nicole, Nicholas]\"},{\"position\":12,\"pseudonym\":\"Anthony\",\"friends\":\"[Nicole, Ralph, Margaret, Gregory, Scott, Fred, Louise, Walter, Dorothy]\"},{\"position\":13,\"pseudonym\":\"Marie\",\"friends\":\"[Nicole, Carol, Jeremy, Fred, Norma]\"},{\"position\":14,\"pseudonym\":\"Bruce\",\"friends\":\"[Nicole, Sandra, Gloria, Frances, Rachel]\"},{\"position\":15,\"pseudonym\":\"Joseph\",\"friends\":\"[Nicole, Sean, Norma]\"},{\"position\":16,\"pseudonym\":\"Nicholas\",\"friends\":\"[Nicole, Shawn, Roy, Annie, Stephen, Heather, Deborah]\"},{\"position\":17,\"pseudonym\":\"Peter\",\"friends\":\"[Nicole, Sean, Angela, Gerald, Deborah]\"},{\"position\":18,\"pseudonym\":\"Randy\",\"friends\":\"[Nicole, Sandra, Fred, Brenda, Nicholas, Stephen, Anne, Mildred, Roy]\"},{\"position\":19,\"pseudonym\":\"Anne\",\"friends\":\"[Nicole, Jeremy, Sara, Judith, Margaret, Stephen, Brian]\"},{\"position\":20,\"pseudonym\":\"Henry\",\"friends\":\"[Nicole, Sean, Steve]\"},{\"position\":21,\"pseudonym\":\"Alice\",\"friends\":\"[Nicole, Anne, Frances]\"},{\"position\":22,\"pseudonym\":\"Stephen\",\"friends\":\"[Nicole, Randy, Fred, Donna, Frances, Donald, Gregory, Mildred, Stephen, Larry, Heather]\"},{\"position\":23,\"pseudonym\":\"Mildred\",\"friends\":\"[Nicole, Carol, Michelle, Andrew, Ryan, Michael, Debra]\"},{\"position\":24,\"pseudonym\":\"Frances\",\"friends\":\"[Nicole, Carol, Alice, Jeremy, Bruce, Angela]\"},{\"position\":25,\"pseudonym\":\"Brian\",\"friends\":\"[Nicole, Carol, Jeremy, Sean, Anne, Paula, Bruce, Russell, James]\"},{\"position\":26,\"pseudonym\":\"Shawn\",\"friends\":\"[]\"},{\"position\":27,\"pseudonym\":\"Keith\",\"friends\":\"[Nicole, Anne, Henry, Ryan, Kathy]\"},{\"position\":28,\"pseudonym\":\"Gloria\",\"friends\":\"[Nicole, Carol, Bruce, Tina, Norma, Kenneth, James, Michael, Gregory, Joshua, Walter, Debra]\"},{\"position\":29,\"pseudonym\":\"Carolyn\",\"friends\":\"[Nicole, Ralph, Debra, Evelyn, Anne, Peter, Donald]\"},{\"position\":30,\"pseudonym\":\"Jerry\",\"friends\":\"[Nicole, Carol, Joseph, Keith, Larry]\"},{\"position\":31,\"pseudonym\":\"Rose\",\"friends\":\"[Nicole, Brian, Paula, Wayne, Rachel, Deborah]\"},{\"position\":32,\"pseudonym\":\"Paula\",\"friends\":\"[Nicole, Brian, Rose, Debra]\"},{\"position\":33,\"pseudonym\":\"Bruce\",\"friends\":\"[Sean, Frances, Brian]\"},{\"position\":34,\"pseudonym\":\"Russell\",\"friends\":\"[Nicole, Brian, Tina, Gerald]\"},{\"position\":35,\"pseudonym\":\"Ralph\",\"friends\":\"[Nicole, Carol, Anthony, Carolyn, Judith, Frances, Steve, Brian]\"},{\"position\":36,\"pseudonym\":\"Debra\",\"friends\":\"[Nicole, Sean, Carolyn, Paula, Randy, Carl, Norma, Arthur, Debra]\"},{\"position\":37,\"pseudonym\":\"Andrew\",\"friends\":\"[Nicole, Annie, Mildred, Rose, Margaret]\"},{\"position\":38,\"pseudonym\":\"Mildred\",\"friends\":\"[Sean, Christopher, Gerald]\"},{\"position\":39,\"pseudonym\":\"Wayne\",\"friends\":\"[Nicole, Tina, Rose, Brian, Norma, Mildred, Sean, Ryan, Arthur]\"},{\"position\":40,\"pseudonym\":\"William\",\"friends\":\"[Nicole, Carol, Wayne]\"},{\"position\":41,\"pseudonym\":\"Donna\",\"friends\":\"[Nicole, Stephen, Sandra, Brian, Fred, Roy, Debra]\"},{\"position\":42,\"pseudonym\":\"Norma\",\"friends\":\"[Nicole, Gloria, Wayne, Brenda, Tina, Carolyn, Jerry, Carl, Rachel, Fred]\"},{\"position\":43,\"pseudonym\":\"Mildred\",\"friends\":\"[Wayne, Joseph, Donald, Arthur, Deborah]\"},{\"position\":44,\"pseudonym\":\"Evelyn\",\"friends\":\"[Nicole, Carolyn, Jerry, James, Julia]\"},{\"position\":45,\"pseudonym\":\"Larry\",\"friends\":\"[Nicole, Jerry, Bruce]\"},{\"position\":46,\"pseudonym\":\"Samuel\",\"friends\":\"[Nicole, Rose]\"},{\"position\":47,\"pseudonym\":\"Anne\",\"friends\":\"[Fred, Randy, Carolyn, Anne, Henry, Judith, Christopher, Gerald, Michael, Norma, Arthur]\"},{\"position\":48,\"pseudonym\":\"Roy\",\"friends\":\"[Nicole, Nicholas, Michelle, Debra, Brian]\"},{\"position\":49,\"pseudonym\":\"Carl\",\"friends\":\"[Nicole, Debra, Norma]\"},{\"position\":50,\"pseudonym\":\"Judith\",\"friends\":\"[Nicole, Mildred, Norma, Sean, Louise]\"},{\"position\":51,\"pseudonym\":\"Kenneth\",\"friends\":\"[Nicole, Sean, Gloria, Joseph]\"},{\"position\":52,\"pseudonym\":\"Judith\",\"friends\":\"[Anne, Ralph, Anthony, Christopher, Stephen, Arthur]\"},{\"position\":53,\"pseudonym\":\"Sean\",\"friends\":\"[Tina, Henry, Wayne, Judith, Fred, Samuel, Margaret, Heather]\"},{\"position\":54,\"pseudonym\":\"James\",\"friends\":\"[Nicole, Michelle, Brian, Gloria, Evelyn, Joseph, Carolyn, Rose, Kenneth, Peter, Louise, Arthur, Steve]\"},{\"position\":55,\"pseudonym\":\"Annie\",\"friends\":\"[Nicholas, Rose, Frances]\"},{\"position\":56,\"pseudonym\":\"Ryan\",\"friends\":\"[Nicole, Mildred, Keith, Wayne, Marie, Carol, Peter]\"},{\"position\":57,\"pseudonym\":\"Christopher\",\"friends\":\"[Nicole, Mildred, Anne, Judith]\"},{\"position\":58,\"pseudonym\":\"Julia\",\"friends\":\"[Nicole, Carol, Jeremy, Evelyn, Sandra, Sarah]\"},{\"position\":59,\"pseudonym\":\"Joseph\",\"friends\":\"[Gregory]\"},{\"position\":60,\"pseudonym\":\"Margaret\",\"friends\":\"[Nicole, Anthony, Anne, Andrew, Sean, Carol]\"},{\"position\":61,\"pseudonym\":\"Carol\",\"friends\":\"[Carol, Ryan, Margaret, Marie, Gregory, Sandra, Carolyn]\"},{\"position\":62,\"pseudonym\":\"Angela\",\"friends\":\"[Nicole, Sean, Peter, Frances, Brenda, Nicholas, Ruby, Arthur, Brian]\"},{\"position\":63,\"pseudonym\":\"Peter\",\"friends\":\"[Carol, Carolyn, James, Ryan, Larry, Margaret, Walter]\"},{\"position\":64,\"pseudonym\":\"Gerald\",\"friends\":\"[Nicole, Peter, Russell, Mildred, Anne, Christopher]\"},{\"position\":65,\"pseudonym\":\"Michael\",\"friends\":\"[Nicole, Mildred, Gloria, Anne, Anthony, Gregory, Joyce, Brian]\"},{\"position\":66,\"pseudonym\":\"Walter\",\"friends\":\"[Nicole, Brenda, Sean, Fred, Shawn, James, Margaret, Carol, Ruby, Arthur]\"},{\"position\":67,\"pseudonym\":\"Ruby\",\"friends\":\"[Nicole, Angela, Walter, Wayne, William]\"},{\"position\":68,\"pseudonym\":\"Frances\",\"friends\":\"[Bruce, Stephen, Ralph, Annie, Carolyn, Norma, Dorothy]\"},{\"position\":69,\"pseudonym\":\"Jennifer\",\"friends\":\"[Nicole, Fred, Debra, Scott]\"},{\"position\":70,\"pseudonym\":\"William\",\"friends\":\"[Jeremy, Shawn, Peter, Gerald, Louise]\"},{\"position\":71,\"pseudonym\":\"Kathy\",\"friends\":\"[Nicole, Keith, Jeremy, Jennifer]\"},{\"position\":72,\"pseudonym\":\"Donald\",\"friends\":\"[Nicole, Stephen, Carolyn, Mildred, Keith, Rose, Larry, Carl, Angela, Jennifer]\"},{\"position\":73,\"pseudonym\":\"Roger\",\"friends\":\"[Nicole, Tina, Shawn, Debra]\"},{\"position\":74,\"pseudonym\":\"Gregory\",\"friends\":\"[Nicole, Jeremy, Anthony, Stephen, Gloria, Joseph, Carol, Michael, Fred, Andrew, Deborah]\"},{\"position\":75,\"pseudonym\":\"Sandra\",\"friends\":\"[Julia, Carol, Sara, Frances, Carolyn, William, Roy, Stephen]\"},{\"position\":76,\"pseudonym\":\"Carolyn\",\"friends\":\"[Carol, Frances, Margaret, Michael, Arthur, Steve]\"},{\"position\":77,\"pseudonym\":\"Rachel\",\"friends\":\"[Nicole, Carol, Bruce, Rose, Norma, Joseph]\"},{\"position\":78,\"pseudonym\":\"Stephen\",\"friends\":\"[Annie, Anne, Judith, Sandra, Ralph, Mildred, Wayne, Roy]\"},{\"position\":79,\"pseudonym\":\"Scott\",\"friends\":\"[Nicole, Anthony, Jennifer, Sean, Mildred, Paula, Ryan]\"},{\"position\":80,\"pseudonym\":\"Fred\",\"friends\":\"[Sara, Annie, Anthony, Donna, Norma, Joseph, Mildred, Brian, Roy, Judith, Frances, Carolyn, Larry, Arthur, Steve]\"},{\"position\":81,\"pseudonym\":\"Joshua\",\"friends\":\"[Nicole, Gloria]\"},{\"position\":82,\"pseudonym\":\"Mildred\",\"friends\":\"[Fred, Randy, Stephen, Frances, Roger]\"},{\"position\":83,\"pseudonym\":\"Louise\",\"friends\":\"[Nicole, Annie, Fred, Anthony, Judith, James, William, Rachel, Larry]\"},{\"position\":84,\"pseudonym\":\"Stephen\",\"friends\":\"[Fred, Nicholas, Stephen, Jennifer]\"},{\"position\":85,\"pseudonym\":\"Larry\",\"friends\":\"[Carol, Stephen, Fred, Louise, Sara, Bruce, Carl, Judith, Joshua]\"},{\"position\":86,\"pseudonym\":\"Heather\",\"friends\":\"[Nicole, Nicholas, Stephen, Sean, Carol, Gloria, Carolyn, Christopher, Roger]\"},{\"position\":87,\"pseudonym\":\"Walter\",\"friends\":\"[Anthony, Gloria, Peter, Alice]\"},{\"position\":88,\"pseudonym\":\"Roy\",\"friends\":\"[Fred, Randy, Donna, Carolyn, Mildred, Peter, Brian]\"},{\"position\":89,\"pseudonym\":\"Joyce\",\"friends\":\"[Nicole, Carol, Michael, Samuel, Cynthia]\"},{\"position\":90,\"pseudonym\":\"Arthur\",\"friends\":\"[Nicole, Mildred, James, Walter, Fred, Judith]\"},{\"position\":91,\"pseudonym\":\"Norma\",\"friends\":\"[Marie, Joseph, Debra, Anne, Frances, Carolyn]\"},{\"position\":92,\"pseudonym\":\"Cynthia\",\"friends\":\"[Nicole, Joyce, Gregory]\"},{\"position\":93,\"pseudonym\":\"Arthur\",\"friends\":\"[Carol, Debra, Wayne, Anne, Judith, Angela, Carolyn, Nicole, Stephen, Larry]\"},{\"position\":94,\"pseudonym\":\"Steve\",\"friends\":\"[Nicole, Annie, Sean, Henry, Ralph, James, Carolyn, Fred, Mildred, Bruce, Julia, Peter, Kathy, Larry]\"},{\"position\":95,\"pseudonym\":\"Deborah\",\"friends\":\"[Nicole, Michelle, Annie, Tina, Nicholas, Peter, Rose, Mildred, Gregory, Keith, Ryan]\"},{\"position\":96,\"pseudonym\":\"Debra\",\"friends\":\"[Mildred, Gloria, Debra, Donna, Roy, Roger, Marie, Annie, Angela, Frances, Sarah]\"},{\"position\":97,\"pseudonym\":\"Dorothy\",\"friends\":\"[Nicole, Anthony, Frances, Larry, Judith]\"},{\"position\":98,\"pseudonym\":\"Sarah\",\"friends\":\"[Nicole, Julia, Debra, Sandra, Gloria, Roger]\"},{\"position\":99,\"pseudonym\":\"Brian\",\"friends\":\"[Anne, Ralph, Roy, Angela, Michael, Annie, Jeremy, Anthony, Norma]\"}]";

    private static final String[] MESSAGES = new String[]{
            "If you like tuna and tomato sauce- try combining the two. It’s really not as bad as it sounds.",
            "A glittering gem is not enough.",
            "Italy is my favorite country; in fact, I plan to spend two weeks there next year.",
            "The shooter says goodbye to his love.",
            "She folded her handkerchief neatly.",
            "I would have gotten the promotion, but my attendance wasn’t good enough.",
            "Should we start class now, or should we wait for everyone to get here?",
            "The clock within this blog and the clock on my laptop are 1 hour different from each other.",
            "The old apple revels in its authority.",
            "The mysterious diary records the voice.",
            "Wow, does that work?",
            "A song can make or ruin a person’s day if they let it get to them.",
            "Please wait outside of the house.",
            "Is it free?",
            "Don't step on the broken glass.",
            "Two seats were vacant.",
            "The lake is a long way from here.",
            "He turned in the research paper on Friday; otherwise, he would have not passed the class.",
            "What was the person thinking when they discovered cow’s milk was fine for human consumption… and why did they do it in the first place!?",
            "I hear that Nancy is very pretty.",
            "Yeah, I think it's a good environment for learning English.",
            "Wednesday is hump day, but has anyone asked the camel if he’s happy about it?",
            "I want more detailed information.",
            "The stranger officiates the meal.",
            "I'd rather be a bird than a fish.",
            "She borrowed the book from him many years ago and hasn't yet returned it.",
            "Writing a list of random sentences is harder than I initially thought it would be.",
            "Sometimes, all you need to do is completely make an ass of yourself and laugh it off to realise that life isn’t so bad after all.",
            "I checked to make sure that he was still alive.",
            "She did her best to help him.",
            "I really want to go to work, but I am too sick to drive.",
            "The memory we used to share is no longer coherent.",
            "I am never at home on Sundays.",
            "If the Easter Bunny and the Tooth Fairy had babies would they take your teeth and leave chocolate for you?",
            "I want to buy a onesie… but know it won’t suit me.",
            "She did not cheat on the test, for it was not the right thing to do.",
            "If I don’t like something, I’ll stay away from it.",
            "We have never been to Asia, nor have we visited Africa.",
            "When I was little I had a car door slammed shut on my hand. I still remember it quite vividly.",
            "Christmas is coming.",
            "He told us a very exciting adventure story.",
            "I was very proud of my nickname throughout high school but today- I couldn’t be any different to what my nickname was.",
            "Joe made the sugar cookies; Susan decorated them.",
            "If Purple People Eaters are real… where do they find purple people to eat?",
            "Last Friday in three week’s time I saw a spotted striped blue worm shake hands with a legless lizard.",
            "Cats are good pets, for they are clean and are not noisy.",
            "We need to rent a room for our party.",
            "Where do random thoughts come from?",
            "The sky is clear; the stars are twinkling.",
            "The quick brown fox jumps over the lazy dog.",
            "Sometimes it is better to just walk away from things and go back to them later when you’re in a better frame of mind.",
            "She works two jobs to make ends meet; at least, that was her reason for not having time to join us.",
            "The book is in front of the table.",
            "A purple pig and a green donkey flew a kite in the middle of the night and ended up sunburnt.",
            "Check back tomorrow; I will see if the book has arrived.",
            "Everyone was busy, so I went to the movie alone.",
            "She advised him to come back at once.",
            "He said he was not there yesterday; however, many people saw him there.",
            "Sixty-Four comes asking for bread.",
            "I currently have 4 windows open up… and I don’t know why.",
            "He didn’t want to go to the dentist, yet he went anyway.",
            "I love eating toasted cheese and tuna sandwiches.",
            "The waves were crashing on the shore; it was a lovely sight.",
            "Mary plays the piano.",
            "Let me help you with your baggage.",
            "She wrote him a long letter, but he didn't read it.",
            "Abstraction is often one floor above you.",
            "Tom got a small piece of pie.",
            "I often see the time 11:11 or 12:34 on clocks.",
            "They got there early, and they got really good seats.",
            "I will never be this young again. Ever. Oh damn… I just got older.",
            "I think I will buy the red car, or I will lease the blue one.",
            "She only paints with bold colors; she does not like pastels.",
            "We have a lot of rain in June.",
            "How was the math test?",
            "He ran out of money, so he had to stop playing poker.",
            "She was too short to see over the fence.",
            "I am counting my calories, yet I really want dessert.",
            "This is a Japanese doll.",
            "Someone I know recently combined Maple Syrup & buttered Popcorn thinking it would taste like caramel popcorn. It didn’t and they don’t recommend anyone else do it either.",
            "My Mum tries to be cool by saying that she likes all the same things that I do.",
            "Hurry!",
            "It was getting dark, and we weren’t there yet.",
            "Lets all be unique together until we realise we are all the same.",
            "Rock music approaches at high velocity.",
            "This is the last random sentence I will be writing and I am going to stop mid-sent",
            "I am happy to take your donation; any amount will be greatly appreciated.",
            "She always speaks to him in a loud voice.",
            "There were white out conditions in the town; subsequently, the roads were impassable.",
            "There was no ice cream in the freezer, nor did they have money to go to the store.",
            "Malls are great places to shop; I can find everything I need under one roof.",
            "The body may perhaps compensates for the loss of a true metaphysics.",
            "The river stole the gods.",
            "We need to rent a room for our party.",
            "The mysterious diary records the voice.",
            "Italy is my favorite country; in fact, I plan to spend two weeks there next year.",
            "Is it free?",
            "Two seats were vacant.",
            "There was no ice cream in the freezer, nor did they have money to go to the store.",
            "Abstraction is often one floor above you.",
            "If I don’t like something, I’ll stay away from it.",
            "I will never be this young again. Ever. Oh damn… I just got older.",
            "I hear that Nancy is very pretty.",
            "He ran out of money, so he had to stop playing poker.",
            "Wow, does that work?",
            "When I was little I had a car door slammed shut on my hand. I still remember it quite vividly.",
            "I checked to make sure that he was still alive.",
            "She did her best to help him.",
            "Sixty-Four comes asking for bread.",
            "Everyone was busy, so I went to the movie alone.",
            "I'd rather be a bird than a fish.",
            "There were white out conditions in the town; subsequently, the roads were impassable.",
            "The body may perhaps compensates for the loss of a true metaphysics.",
            "I currently have 4 windows open up… and I don’t know why.",
            "The lake is a long way from here.",
            "He told us a very exciting adventure story.",
            "She borrowed the book from him many years ago and hasn't yet returned it.",
            "The old apple revels in its authority.",
            "I want to buy a onesie… but know it won’t suit me.",
            "The clock within this blog and the clock on my laptop are 1 hour different from each other.",
            "This is the last random sentence I will be writing and I am going to stop mid-sent",
            "The river stole the gods.",
            "She only paints with bold colors; she does not like pastels.",
            "Christmas is coming.",
            "What was the person thinking when they discovered cow’s milk was fine for human consumption… and why did they do it in the first place!?",
            "The quick brown fox jumps over the lazy dog.",
            "We have a lot of rain in June.",
            "I would have gotten the promotion, but my attendance wasn’t good enough.",
            "The shooter says goodbye to his love.",
            "Let me help you with your baggage.",
            "She did not cheat on the test, for it was not the right thing to do.",
            "She always speaks to him in a loud voice.",
            "He said he was not there yesterday; however, many people saw him there.",
            "How was the math test?",
            "If you like tuna and tomato sauce- try combining the two. It’s really not as bad as it sounds.",
            "Hurry!",
            "The stranger officiates the meal.",
            "I am counting my calories, yet I really want dessert.",
            "We have never been to Asia, nor have we visited Africa.",
            "Sometimes, all you need to do is completely make an ass of yourself and laugh it off to realise that life isn’t so bad after all.",
            "She works two jobs to make ends meet; at least, that was her reason for not having time to join us.",
            "Lets all be unique together until we realise we are all the same.",
            "I think I will buy the red car, or I will lease the blue one.",
            "Don't step on the broken glass.",
            "Where do random thoughts come from?",
            "She advised him to come back at once.",
            "I want more detailed information.",
            "Please wait outside of the house.",
            "Sometimes it is better to just walk away from things and go back to them later when you’re in a better frame of mind.",
            "I am happy to take your donation; any amount will be greatly appreciated.",
            "Mary plays the piano.",
            "The book is in front of the table.",
            "They got there early, and they got really good seats.",
            "I was very proud of my nickname throughout high school but today- I couldn’t be any different to what my nickname was.",
            "A purple pig and a green donkey flew a kite in the middle of the night and ended up sunburnt.",
            "I really want to go to work, but I am too sick to drive.",
            "The memory we used to share is no longer coherent.",
            "Joe made the sugar cookies; Susan decorated them.",
            "I often see the time 11:11 or 12:34 on clocks.",
            "Writing a list of random sentences is harder than I initially thought it would be.",
            "He didn’t want to go to the dentist, yet he went anyway.",
            "Rock music approaches at high velocity.",
            "Tom got a small piece of pie.",
            "Last Friday in three week’s time I saw a spotted striped blue worm shake hands with a legless lizard.",
            "If the Easter Bunny and the Tooth Fairy had babies would they take your teeth and leave chocolate for you?",
            "The sky is clear; the stars are twinkling.",
            "Should we start class now, or should we wait for everyone to get here?",
            "My Mum tries to be cool by saying that she likes all the same things that I do.",
            "The waves were crashing on the shore; it was a lovely sight.",
            "Wednesday is hump day, but has anyone asked the camel if he’s happy about it?",
            "I love eating toasted cheese and tuna sandwiches.",
            "Someone I know recently combined Maple Syrup & buttered Popcorn thinking it would taste like caramel popcorn. It didn’t and they don’t recommend anyone else do it either.",
            "She wrote him a long letter, but he didn't read it.",
            "Check back tomorrow; I will see if the book has arrived.",
            "I am never at home on Sundays.",
            "He turned in the research paper on Friday; otherwise, he would have not passed the class.",
            "Yeah, I think it's a good environment for learning English.",
            "Cats are good pets, for they are clean and are not noisy.",
            "A glittering gem is not enough.",
            "She folded her handkerchief neatly.",
            "This is a Japanese doll.",
            "Malls are great places to shop; I can find everything I need under one roof.",
            "If Purple People Eaters are real… where do they find purple people to eat?",
            "A song can make or ruin a person’s day if they let it get to them.",
            "She was too short to see over the fence.",
            "It was getting dark, and we weren’t there yet.",
            "How was the math test?",
            "The waves were crashing on the shore; it was a lovely sight.",
            "There was no ice cream in the freezer, nor did they have money to go to the store.",
            "I love eating toasted cheese and tuna sandwiches.",
            "The quick brown fox jumps over the lazy dog.",
            "The river stole the gods.",
            "Two seats were vacant.",
            "I want to buy a onesie… but know it won’t suit me.",
            "This is the last random sentence I will be writing and I am going to stop mid-sent",
            "Mary plays the piano.",
            "Please wait outside of the house.",
            "We have a lot of rain in June.",
            "I want more detailed information.",
            "The body may perhaps compensates for the loss of a true metaphysics.",
            "Lets all be unique together until we realise we are all the same.",
            "We need to rent a room for our party.",
            "I was very proud of my nickname throughout high school but today- I couldn’t be any different to what my nickname was.",
            "I think I will buy the red car, or I will lease the blue one.",
            "The clock within this blog and the clock on my laptop are 1 hour different from each other.",
            "He told us a very exciting adventure story.",
            "Yeah, I think it's a good environment for learning English.",
            "She works two jobs to make ends meet; at least, that was her reason for not having time to join us.",
            "The stranger officiates the meal.",
            "The old apple revels in its authority.",
            "If Purple People Eaters are real… where do they find purple people to eat?",
            "Joe made the sugar cookies; Susan decorated them.",
            "She borrowed the book from him many years ago and hasn't yet returned it.",
            "Check back tomorrow; I will see if the book has arrived.",
            "He didn’t want to go to the dentist, yet he went anyway.",
            "A glittering gem is not enough.",
            "I hear that Nancy is very pretty.",
            "Christmas is coming.",
            "If I don’t like something, I’ll stay away from it.",
            "The memory we used to share is no longer coherent.",
            "I'd rather be a bird than a fish.",
            "It was getting dark, and we weren’t there yet.",
            "I checked to make sure that he was still alive.",
            "She always speaks to him in a loud voice.",
            "The lake is a long way from here.",
            "She was too short to see over the fence.",
            "Is it free?",
            "If you like tuna and tomato sauce- try combining the two. It’s really not as bad as it sounds.",
            "Last Friday in three week’s time I saw a spotted striped blue worm shake hands with a legless lizard.",
            "Rock music approaches at high velocity.",
            "If the Easter Bunny and the Tooth Fairy had babies would they take your teeth and leave chocolate for you?",
            "He turned in the research paper on Friday; otherwise, he would have not passed the class.",
            "Don't step on the broken glass.",
            "I am never at home on Sundays.",
            "Should we start class now, or should we wait for everyone to get here?",
            "Italy is my favorite country; in fact, I plan to spend two weeks there next year.",
            "She did not cheat on the test, for it was not the right thing to do.",
            "I currently have 4 windows open up… and I don’t know why.",
            "Sometimes, all you need to do is completely make an ass of yourself and laugh it off to realise that life isn’t so bad after all.",
            "My Mum tries to be cool by saying that she likes all the same things that I do.",
            "I really want to go to work, but I am too sick to drive.",
            "I often see the time 11:11 or 12:34 on clocks.",
            "There were white out conditions in the town; subsequently, the roads were impassable.",
            "She advised him to come back at once.",
            "I would have gotten the promotion, but my attendance wasn’t good enough.",
            "Hurry!",
            "Tom got a small piece of pie.",
            "Sometimes it is better to just walk away from things and go back to them later when you’re in a better frame of mind.",
            "When I was little I had a car door slammed shut on my hand. I still remember it quite vividly.",
            "Let me help you with your baggage.",
            "What was the person thinking when they discovered cow’s milk was fine for human consumption… and why did they do it in the first place!?",
            "I will never be this young again. Ever. Oh damn… I just got older.",
            "He ran out of money, so he had to stop playing poker.",
            "Malls are great places to shop; I can find everything I need under one roof.",
            "The mysterious diary records the voice.",
            "She did her best to help him.",
            "Wednesday is hump day, but has anyone asked the camel if he’s happy about it?",
            "Abstraction is often one floor above you.",
            "Wow, does that work?",
            "Where do random thoughts come from?",
            "A purple pig and a green donkey flew a kite in the middle of the night and ended up sunburnt.",
            "Cats are good pets, for they are clean and are not noisy.",
            "The book is in front of the table.",
            "She folded her handkerchief neatly.",
            "Sixty-Four comes asking for bread.",
            "She only paints with bold colors; she does not like pastels.",
            "They got there early, and they got really good seats.",
            "The shooter says goodbye to his love.",
            "Everyone was busy, so I went to the movie alone.",
            "The sky is clear; the stars are twinkling.",
            "I am counting my calories, yet I really want dessert.",
            "We have never been to Asia, nor have we visited Africa.",
            "He said he was not there yesterday; however, many people saw him there.",
            "A song can make or ruin a person’s day if they let it get to them.",
            "She wrote him a long letter, but he didn't read it.",
            "This is a Japanese doll.",
            "I am happy to take your donation; any amount will be greatly appreciated.",
            "Someone I know recently combined Maple Syrup & buttered Popcorn thinking it would taste like caramel popcorn. It didn’t and they don’t recommend anyone else do it either.",
            "Writing a list of random sentences is harder than I initially thought it would be.",
            "The book is in front of the table.",
            "Rock music approaches at high velocity.",
            "Let me help you with your baggage.",
            "Sometimes it is better to just walk away from things and go back to them later when you’re in a better frame of mind.",
            "I hear that Nancy is very pretty.",
            "I think I will buy the red car, or I will lease the blue one.",
            "Should we start class now, or should we wait for everyone to get here?",
            "She did her best to help him.",
            "I will never be this young again. Ever. Oh damn… I just got older.",
            "Sixty-Four comes asking for bread.",
            "This is the last random sentence I will be writing and I am going to stop mid-sent",
            "She always speaks to him in a loud voice.",
            "He turned in the research paper on Friday; otherwise, he would have not passed the class.",
            "She only paints with bold colors; she does not like pastels.",
            "The waves were crashing on the shore; it was a lovely sight.",
            "There was no ice cream in the freezer, nor did they have money to go to the store.",
            "A glittering gem is not enough.",
            "He ran out of money, so he had to stop playing poker.",
            "Someone I know recently combined Maple Syrup & buttered Popcorn thinking it would taste like caramel popcorn. It didn’t and they don’t recommend anyone else do it either.",
            "I am never at home on Sundays.",
            "Last Friday in three week’s time I saw a spotted striped blue worm shake hands with a legless lizard.",
            "The lake is a long way from here.",
            "If you like tuna and tomato sauce- try combining the two. It’s really not as bad as it sounds.",
            "Italy is my favorite country; in fact, I plan to spend two weeks there next year.",
            "My Mum tries to be cool by saying that she likes all the same things that I do.",
            "I checked to make sure that he was still alive.",
            "Malls are great places to shop; I can find everything I need under one roof.",
            "Two seats were vacant.",
            "I'd rather be a bird than a fish.",
            "She advised him to come back at once.",
            "We have never been to Asia, nor have we visited Africa.",
            "Everyone was busy, so I went to the movie alone.",
            "How was the math test?",
            "Hurry!",
            "Writing a list of random sentences is harder than I initially thought it would be.",
            "Abstraction is often one floor above you.",
            "The stranger officiates the meal.",
            "A song can make or ruin a person’s day if they let it get to them.",
            "It was getting dark, and we weren’t there yet.",
            "I often see the time 11:11 or 12:34 on clocks.",
            "The mysterious diary records the voice.",
            "Lets all be unique together until we realise we are all the same.",
            "The sky is clear; the stars are twinkling.",
            "I really want to go to work, but I am too sick to drive.",
            "I love eating toasted cheese and tuna sandwiches.",
            "She works two jobs to make ends meet; at least, that was her reason for not having time to join us.",
            "This is a Japanese doll.",
            "She wrote him a long letter, but he didn't read it.",
            "Where do random thoughts come from?",
            "The memory we used to share is no longer coherent.",
            "Please wait outside of the house.",
            "When I was little I had a car door slammed shut on my hand. I still remember it quite vividly.",
            "We need to rent a room for our party.",
            "The quick brown fox jumps over the lazy dog.",
            "Check back tomorrow; I will see if the book has arrived.",
            "I would have gotten the promotion, but my attendance wasn’t good enough.",
            "Is it free?",
            "Mary plays the piano.",
            "If the Easter Bunny and the Tooth Fairy had babies would they take your teeth and leave chocolate for you?",
            "Christmas is coming.",
            "A purple pig and a green donkey flew a kite in the middle of the night and ended up sunburnt.",
            "If I don’t like something, I’ll stay away from it.",
            "The old apple revels in its authority.",
            "The body may perhaps compensates for the loss of a true metaphysics.",
            "Yeah, I think it's a good environment for learning English.",
            "What was the person thinking when they discovered cow’s milk was fine for human consumption… and why did they do it in the first place!?",
            "The river stole the gods.",
            "She was too short to see over the fence.",
            "She borrowed the book from him many years ago and hasn't yet returned it.",
            "The shooter says goodbye to his love.",
            "The clock within this blog and the clock on my laptop are 1 hour different from each other.",
            "They got there early, and they got really good seats.",
            "Cats are good pets, for they are clean and are not noisy.",
            "We have a lot of rain in June.",
            "Joe made the sugar cookies; Susan decorated them.",
            "Sometimes, all you need to do is completely make an ass of yourself and laugh it off to realise that life isn’t so bad after all.",
            "Wednesday is hump day, but has anyone asked the camel if he’s happy about it?",
            "I was very proud of my nickname throughout high school but today- I couldn’t be any different to what my nickname was.",
            "He didn’t want to go to the dentist, yet he went anyway.",
            "If Purple People Eaters are real… where do they find purple people to eat?",
            "She folded her handkerchief neatly.",
            "Tom got a small piece of pie.",
            "I want to buy a onesie… but know it won’t suit me.",
            "He said he was not there yesterday; however, many people saw him there.",
            "I want more detailed information.",
            "He told us a very exciting adventure story.",
            "She did not cheat on the test, for it was not the right thing to do.",
            "Wow, does that work?",
            "Don't step on the broken glass.",
            "I am happy to take your donation; any amount will be greatly appreciated.",
            "There were white out conditions in the town; subsequently, the roads were impassable.",
            "I currently have 4 windows open up… and I don’t know why.",
            "I am counting my calories, yet I really want dessert.",
            "I often see the time 11:11 or 12:34 on clocks.",
            "Let me help you with your baggage.",
            "Wednesday is hump day, but has anyone asked the camel if he’s happy about it?",
            "She did her best to help him.",
            "He ran out of money, so he had to stop playing poker.",
            "I want to buy a onesie… but know it won’t suit me.",
            "We have a lot of rain in June.",
            "The book is in front of the table.",
            "If I don’t like something, I’ll stay away from it.",
            "She advised him to come back at once.",
            "He said he was not there yesterday; however, many people saw him there.",
            "The old apple revels in its authority.",
            "We need to rent a room for our party.",
            "Lets all be unique together until we realise we are all the same.",
            "Sometimes it is better to just walk away from things and go back to them later when you’re in a better frame of mind.",
            "There were white out conditions in the town; subsequently, the roads were impassable.",
            "The body may perhaps compensates for the loss of a true metaphysics.",
            "I am happy to take your donation; any amount will be greatly appreciated.",
            "She did not cheat on the test, for it was not the right thing to do.",
            "Yeah, I think it's a good environment for learning English.",
            "My Mum tries to be cool by saying that she likes all the same things that I do.",
            "Italy is my favorite country; in fact, I plan to spend two weeks there next year.",
            "Sometimes, all you need to do is completely make an ass of yourself and laugh it off to realise that life isn’t so bad after all.",
            "Wow, does that work?",
            "What was the person thinking when they discovered cow’s milk was fine for human consumption… and why did they do it in the first place!?",
            "Cats are good pets, for they are clean and are not noisy.",
            "Sixty-Four comes asking for bread.",
            "If you like tuna and tomato sauce- try combining the two. It’s really not as bad as it sounds.",
            "Everyone was busy, so I went to the movie alone.",
            "Where do random thoughts come from?",
            "I would have gotten the promotion, but my attendance wasn’t good enough.",
            "Last Friday in three week’s time I saw a spotted striped blue worm shake hands with a legless lizard.",
            "The clock within this blog and the clock on my laptop are 1 hour different from each other.",
            "She wrote him a long letter, but he didn't read it.",
            "Tom got a small piece of pie.",
            "The stranger officiates the meal.",
            "I think I will buy the red car, or I will lease the blue one.",
            "We have never been to Asia, nor have we visited Africa.",
            "Don't step on the broken glass.",
            "This is the last random sentence I will be writing and I am going to stop mid-sent",
            "The mysterious diary records the voice.",
            "A song can make or ruin a person’s day if they let it get to them.",
            "I was very proud of my nickname throughout high school but today- I couldn’t be any different to what my nickname was.",
            "I checked to make sure that he was still alive.",
            "Rock music approaches at high velocity.",
            "Two seats were vacant.",
            "Writing a list of random sentences is harder than I initially thought it would be.",
            "I'd rather be a bird than a fish.",
            "He turned in the research paper on Friday; otherwise, he would have not passed the class.",
            "Please wait outside of the house.",
            "I currently have 4 windows open up… and I don’t know why.",
            "The lake is a long way from here.",
            "Mary plays the piano.",
            "Joe made the sugar cookies; Susan decorated them.",
            "It was getting dark, and we weren’t there yet.",
            "He didn’t want to go to the dentist, yet he went anyway.",
            "Malls are great places to shop; I can find everything I need under one roof.",
            "She always speaks to him in a loud voice.",
            "Should we start class now, or should we wait for everyone to get here?",
            "I love eating toasted cheese and tuna sandwiches.",
            "If the Easter Bunny and the Tooth Fairy had babies would they take your teeth and leave chocolate for you?",
            "The river stole the gods.",
            "A glittering gem is not enough.",
            "How was the math test?",
            "He told us a very exciting adventure story.",
            "She borrowed the book from him many years ago and hasn't yet returned it.",
            "A purple pig and a green donkey flew a kite in the middle of the night and ended up sunburnt.",
            "The shooter says goodbye to his love.",
            "This is a Japanese doll.",
            "I am counting my calories, yet I really want dessert.",
            "There was no ice cream in the freezer, nor did they have money to go to the store.",
            "Abstraction is often one floor above you.",
            "She was too short to see over the fence.",
            "Check back tomorrow; I will see if the book has arrived.",
            "I will never be this young again. Ever. Oh damn… I just got older.",
            "I really want to go to work, but I am too sick to drive.",
            "Is it free?",
            "She only paints with bold colors; she does not like pastels.",
            "She works two jobs to make ends meet; at least, that was her reason for not having time to join us.",
            "I want more detailed information.",
            "The memory we used to share is no longer coherent.",
            "The quick brown fox jumps over the lazy dog.",
            "When I was little I had a car door slammed shut on my hand. I still remember it quite vividly.",
            "I hear that Nancy is very pretty.",
            "The waves were crashing on the shore; it was a lovely sight.",
            "Hurry!",
            "They got there early, and they got really good seats.",
            "If Purple People Eaters are real… where do they find purple people to eat?",
            "Someone I know recently combined Maple Syrup & buttered Popcorn thinking it would taste like caramel popcorn. It didn’t and they don’t recommend anyone else do it either.",
            "I am never at home on Sundays.",
            "The sky is clear; the stars are twinkling.",
            "She folded her handkerchief neatly.",
            "Christmas is coming.",
            "The river stole the gods.",
            "I will never be this young again. Ever. Oh damn… I just got older.",
            "Sometimes it is better to just walk away from things and go back to them later when you’re in a better frame of mind.",
            "She borrowed the book from him many years ago and hasn't yet returned it.",
            "The book is in front of the table.",
            "The waves were crashing on the shore; it was a lovely sight.",
            "The mysterious diary records the voice.",
            "Christmas is coming.",
            "He told us a very exciting adventure story.",
            "Tom got a small piece of pie.",
            "The shooter says goodbye to his love.",
            "Rock music approaches at high velocity.",
            "Sometimes, all you need to do is completely make an ass of yourself and laugh it off to realise that life isn’t so bad after all.",
            "Mary plays the piano.",
            "Everyone was busy, so I went to the movie alone.",
            "Two seats were vacant.",
            "She folded her handkerchief neatly.",
            "They got there early, and they got really good seats.",
            "The clock within this blog and the clock on my laptop are 1 hour different from each other.",
            "We have never been to Asia, nor have we visited Africa.",
            "She did not cheat on the test, for it was not the right thing to do.",
            "Is it free?",
            "There was no ice cream in the freezer, nor did they have money to go to the store.",
            "I would have gotten the promotion, but my attendance wasn’t good enough.",
            "We have a lot of rain in June.",
            "I am never at home on Sundays.",
            "If you like tuna and tomato sauce- try combining the two. It’s really not as bad as it sounds.",
            "The stranger officiates the meal.",
            "Don't step on the broken glass.",
            "He turned in the research paper on Friday; otherwise, he would have not passed the class.",
            "Someone I know recently combined Maple Syrup & buttered Popcorn thinking it would taste like caramel popcorn. It didn’t and they don’t recommend anyone else do it either.",
            "Yeah, I think it's a good environment for learning English.",
            "I want to buy a onesie… but know it won’t suit me.",
            "Joe made the sugar cookies; Susan decorated them.",
            "The quick brown fox jumps over the lazy dog.",
            "Abstraction is often one floor above you.",
            "This is a Japanese doll.",
            "She advised him to come back at once.",
            "I'd rather be a bird than a fish.",
            "What was the person thinking when they discovered cow’s milk was fine for human consumption… and why did they do it in the first place!?",
            "There were white out conditions in the town; subsequently, the roads were impassable.",
            "If the Easter Bunny and the Tooth Fairy had babies would they take your teeth and leave chocolate for you?",
            "The memory we used to share is no longer coherent.",
            "Let me help you with your baggage.",
            "Wow, does that work?",
            "She only paints with bold colors; she does not like pastels.",
            "He ran out of money, so he had to stop playing poker.",
            "I am happy to take your donation; any amount will be greatly appreciated.",
            "This is the last random sentence I will be writing and I am going to stop mid-sent",
            "I love eating toasted cheese and tuna sandwiches.",
            "Italy is my favorite country; in fact, I plan to spend two weeks there next year.",
            "Check back tomorrow; I will see if the book has arrived.",
            "Malls are great places to shop; I can find everything I need under one roof.",
            "I want more detailed information.",
            "The body may perhaps compensates for the loss of a true metaphysics.",
            "Lets all be unique together until we realise we are all the same.",
            "I was very proud of my nickname throughout high school but today- I couldn’t be any different to what my nickname was.",
            "Please wait outside of the house.",
            "Where do random thoughts come from?",
            "The old apple revels in its authority.",
            "A song can make or ruin a person’s day if they let it get to them.",
            "Writing a list of random sentences is harder than I initially thought it would be.",
            "When I was little I had a car door slammed shut on my hand. I still remember it quite vividly.",
            "She always speaks to him in a loud voice.",
            "He didn’t want to go to the dentist, yet he went anyway.",
            "How was the math test?",
            "I often see the time 11:11 or 12:34 on clocks.",
            "The sky is clear; the stars are twinkling.",
            "If I don’t like something, I’ll stay away from it.",
            "I think I will buy the red car, or I will lease the blue one.",
            "She works two jobs to make ends meet; at least, that was her reason for not having time to join us.",
            "A glittering gem is not enough.",
            "I currently have 4 windows open up… and I don’t know why.",
            "She was too short to see over the fence.",
            "I am counting my calories, yet I really want dessert.",
            "She wrote him a long letter, but he didn't read it.",
            "She did her best to help him.",
            "Should we start class now, or should we wait for everyone to get here?",
            "If Purple People Eaters are real… where do they find purple people to eat?",
            "Hurry!",
            "Cats are good pets, for they are clean and are not noisy.",
            "A purple pig and a green donkey flew a kite in the middle of the night and ended up sunburnt.",
            "Wednesday is hump day, but has anyone asked the camel if he’s happy about it?",
            "Sixty-Four comes asking for bread.",
            "I checked to make sure that he was still alive.",
            "He said he was not there yesterday; however, many people saw him there.",
            "The lake is a long way from here.",
            "We need to rent a room for our party.",
            "It was getting dark, and we weren’t there yet.",
            "I really want to go to work, but I am too sick to drive.",
            "I hear that Nancy is very pretty.",
            "Last Friday in three week’s time I saw a spotted striped blue worm shake hands with a legless lizard.",
            "My Mum tries to be cool by saying that she likes all the same things that I do.",
            "Yeah, I think it's a good environment for learning English.",
            "If the Easter Bunny and the Tooth Fairy had babies would they take your teeth and leave chocolate for you?",
            "Should we start class now, or should we wait for everyone to get here?",
            "Check back tomorrow; I will see if the book has arrived.",
            "She wrote him a long letter, but he didn't read it.",
            "I will never be this young again. Ever. Oh damn… I just got older.",
            "I'd rather be a bird than a fish.",
            "The stranger officiates the meal.",
            "The body may perhaps compensates for the loss of a true metaphysics.",
            "She did not cheat on the test, for it was not the right thing to do.",
            "Wow, does that work?",
            "What was the person thinking when they discovered cow’s milk was fine for human consumption… and why did they do it in the first place!?",
            "The river stole the gods.",
            "Abstraction is often one floor above you.",
            "Writing a list of random sentences is harder than I initially thought it would be.",
            "The clock within this blog and the clock on my laptop are 1 hour different from each other.",
            "The book is in front of the table.",
            "Wednesday is hump day, but has anyone asked the camel if he’s happy about it?",
            "He didn’t want to go to the dentist, yet he went anyway.",
            "How was the math test?",
            "I am counting my calories, yet I really want dessert.",
            "The lake is a long way from here.",
            "I am never at home on Sundays.",
            "He told us a very exciting adventure story.",
            "Christmas is coming.",
            "Rock music approaches at high velocity.",
            "She always speaks to him in a loud voice.",
            "I checked to make sure that he was still alive.",
            "I want more detailed information.",
            "If I don’t like something, I’ll stay away from it.",
            "My Mum tries to be cool by saying that she likes all the same things that I do.",
            "Sixty-Four comes asking for bread.",
            "Mary plays the piano.",
            "When I was little I had a car door slammed shut on my hand. I still remember it quite vividly.",
            "I love eating toasted cheese and tuna sandwiches.",
            "There was no ice cream in the freezer, nor did they have money to go to the store.",
            "Two seats were vacant.",
            "A song can make or ruin a person’s day if they let it get to them.",
            "Everyone was busy, so I went to the movie alone.",
            "Lets all be unique together until we realise we are all the same.",
            "I am happy to take your donation; any amount will be greatly appreciated.",
            "A glittering gem is not enough.",
            "Where do random thoughts come from?",
            "She works two jobs to make ends meet; at least, that was her reason for not having time to join us.",
            "Joe made the sugar cookies; Susan decorated them.",
            "The waves were crashing on the shore; it was a lovely sight.",
            "She folded her handkerchief neatly.",
            "The old apple revels in its authority.",
            "We have never been to Asia, nor have we visited Africa.",
            "She only paints with bold colors; she does not like pastels.",
            "I want to buy a onesie… but know it won’t suit me.",
            "I would have gotten the promotion, but my attendance wasn’t good enough.",
            "The quick brown fox jumps over the lazy dog.",
            "Sometimes it is better to just walk away from things and go back to them later when you’re in a better frame of mind.",
            "She borrowed the book from him many years ago and hasn't yet returned it.",
            "Tom got a small piece of pie.",
            "She did her best to help him.",
            "She was too short to see over the fence.",
            "They got there early, and they got really good seats.",
            "If Purple People Eaters are real… where do they find purple people to eat?",
            "If you like tuna and tomato sauce- try combining the two. It’s really not as bad as it sounds.",
            "I think I will buy the red car, or I will lease the blue one.",
            "Last Friday in three week’s time I saw a spotted striped blue worm shake hands with a legless lizard.",
            "Italy is my favorite country; in fact, I plan to spend two weeks there next year.",
            "She advised him to come back at once.",
            "The sky is clear; the stars are twinkling.",
            "Don't step on the broken glass.",
            "He said he was not there yesterday; however, many people saw him there.",
            "Please wait outside of the house.",
            "It was getting dark, and we weren’t there yet.",
            "I often see the time 11:11 or 12:34 on clocks.",
            "There were white out conditions in the town; subsequently, the roads were impassable.",
            "He turned in the research paper on Friday; otherwise, he would have not passed the class.",
            "I hear that Nancy is very pretty.",
            "We have a lot of rain in June.",
            "Malls are great places to shop; I can find everything I need under one roof.",
            "I currently have 4 windows open up… and I don’t know why.",
            "He ran out of money, so he had to stop playing poker.",
            "The memory we used to share is no longer coherent.",
            "This is a Japanese doll.",
            "The mysterious diary records the voice.",
            "I was very proud of my nickname throughout high school but today- I couldn’t be any different to what my nickname was.",
            "Hurry!",
            "Cats are good pets, for they are clean and are not noisy.",
            "The shooter says goodbye to his love.",
            "A purple pig and a green donkey flew a kite in the middle of the night and ended up sunburnt.",
            "This is the last random sentence I will be writing and I am going to stop mid-sent",
            "We need to rent a room for our party.",
            "Someone I know recently combined Maple Syrup & buttered Popcorn thinking it would taste like caramel popcorn. It didn’t and they don’t recommend anyone else do it either.",
            "Is it free?",
            "Sometimes, all you need to do is completely make an ass of yourself and laugh it off to realise that life isn’t so bad after all.",
            "Let me help you with your baggage.",
            "I really want to go to work, but I am too sick to drive.",
            "If the Easter Bunny and the Tooth Fairy had babies would they take your teeth and leave chocolate for you?",
            "He ran out of money, so he had to stop playing poker.",
            "Tom got a small piece of pie.",
            "When I was little I had a car door slammed shut on my hand. I still remember it quite vividly.",
            "Christmas is coming.",
            "Everyone was busy, so I went to the movie alone.",
            "Wow, does that work?",
            "She wrote him a long letter, but he didn't read it.",
            "He said he was not there yesterday; however, many people saw him there.",
            "The shooter says goodbye to his love.",
            "Lets all be unique together until we realise we are all the same.",
            "I hear that Nancy is very pretty.",
            "He didn’t want to go to the dentist, yet he went anyway.",
            "There was no ice cream in the freezer, nor did they have money to go to the store.",
            "I am counting my calories, yet I really want dessert.",
            "The river stole the gods.",
            "Writing a list of random sentences is harder than I initially thought it would be.",
            "Where do random thoughts come from?",
            "Yeah, I think it's a good environment for learning English.",
            "We have never been to Asia, nor have we visited Africa.",
            "Sixty-Four comes asking for bread.",
            "I currently have 4 windows open up… and I don’t know why.",
            "What was the person thinking when they discovered cow’s milk was fine for human consumption… and why did they do it in the first place!?",
            "Is it free?",
            "I would have gotten the promotion, but my attendance wasn’t good enough.",
            "I am never at home on Sundays.",
            "She was too short to see over the fence.",
            "I really want to go to work, but I am too sick to drive.",
            "Joe made the sugar cookies; Susan decorated them.",
            "This is the last random sentence I will be writing and I am going to stop mid-sent",
            "She did not cheat on the test, for it was not the right thing to do.",
            "I'd rather be a bird than a fish.",
            "Please wait outside of the house.",
            "If you like tuna and tomato sauce- try combining the two. It’s really not as bad as it sounds.",
            "It was getting dark, and we weren’t there yet.",
            "The clock within this blog and the clock on my laptop are 1 hour different from each other.",
            "My Mum tries to be cool by saying that she likes all the same things that I do.",
            "Mary plays the piano.",
            "She works two jobs to make ends meet; at least, that was her reason for not having time to join us.",
            "Rock music approaches at high velocity.",
            "Italy is my favorite country; in fact, I plan to spend two weeks there next year.",
            "She folded her handkerchief neatly.",
            "Sometimes, all you need to do is completely make an ass of yourself and laugh it off to realise that life isn’t so bad after all.",
            "She only paints with bold colors; she does not like pastels.",
            "If Purple People Eaters are real… where do they find purple people to eat?",
            "She advised him to come back at once.",
            "We have a lot of rain in June.",
            "Two seats were vacant.",
            "Someone I know recently combined Maple Syrup & buttered Popcorn thinking it would taste like caramel popcorn. It didn’t and they don’t recommend anyone else do it either.",
            "Hurry!",
            "The quick brown fox jumps over the lazy dog.",
            "Cats are good pets, for they are clean and are not noisy.",
            "The waves were crashing on the shore; it was a lovely sight.",
            "They got there early, and they got really good seats.",
            "The mysterious diary records the voice.",
            "How was the math test?",
            "Sometimes it is better to just walk away from things and go back to them later when you’re in a better frame of mind.",
            "The stranger officiates the meal.",
            "I am happy to take your donation; any amount will be greatly appreciated.",
            "I want to buy a onesie… but know it won’t suit me.",
            "The book is in front of the table.",
            "The body may perhaps compensates for the loss of a true metaphysics.",
            "I will never be this young again. Ever. Oh damn… I just got older.",
            "A glittering gem is not enough.",
            "I think I will buy the red car, or I will lease the blue one.",
            "Check back tomorrow; I will see if the book has arrived.",
            "This is a Japanese doll.",
            "The lake is a long way from here.",
            "He turned in the research paper on Friday; otherwise, he would have not passed the class.",
            "A purple pig and a green donkey flew a kite in the middle of the night and ended up sunburnt.",
            "Don't step on the broken glass.",
            "Malls are great places to shop; I can find everything I need under one roof.",
            "I love eating toasted cheese and tuna sandwiches.",
            "I was very proud of my nickname throughout high school but today- I couldn’t be any different to what my nickname was.",
            "Abstraction is often one floor above you.",
            "Let me help you with your baggage.",
            "He told us a very exciting adventure story.",
            "We need to rent a room for our party.",
            "She borrowed the book from him many years ago and hasn't yet returned it.",
            "Should we start class now, or should we wait for everyone to get here?",
            "I want more detailed information.",
            "She did her best to help him.",
            "She always speaks to him in a loud voice.",
            "I checked to make sure that he was still alive.",
            "The memory we used to share is no longer coherent.",
            "A song can make or ruin a person’s day if they let it get to them.",
            "If I don’t like something, I’ll stay away from it.",
            "There were white out conditions in the town; subsequently, the roads were impassable.",
            "I often see the time 11:11 or 12:34 on clocks.",
            "Wednesday is hump day, but has anyone asked the camel if he’s happy about it?",
            "Last Friday in three week’s time I saw a spotted striped blue worm shake hands with a legless lizard.",
            "The old apple revels in its authority.",
            "The sky is clear; the stars are twinkling.",
            "The waves were crashing on the shore; it was a lovely sight.",
            "He ran out of money, so he had to stop playing poker.",
            "Sometimes, all you need to do is completely make an ass of yourself and laugh it off to realise that life isn’t so bad after all.",
            "Rock music approaches at high velocity.",
            "Tom got a small piece of pie.",
            "She borrowed the book from him many years ago and hasn't yet returned it."
    };

}