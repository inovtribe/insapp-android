package fr.insapp.insapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.util.Linkify;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.github.clans.fab.FloatingActionButton;
import com.github.clans.fab.FloatingActionMenu;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import fr.insapp.insapp.http.AsyncResponse;
import fr.insapp.insapp.http.HttpDelete;
import fr.insapp.insapp.http.HttpGet;
import fr.insapp.insapp.http.HttpPost;
import fr.insapp.insapp.models.Club;
import fr.insapp.insapp.models.Event;
import fr.insapp.insapp.models.Notification;
import fr.insapp.insapp.models.User;
import fr.insapp.insapp.utility.Utils;

/**
 * Created by thomas on 05/12/2016.
 */

public class EventActivity extends AppCompatActivity {

    private Toolbar toolbar;
    private RelativeLayout relativeLayout;
    private ImageView header_image_event;
    private ImageView clubImageView;
    private TextView clubTextView;
    private ImageView participantsImageView;
    private TextView participantsTextView;
    private ImageView dateImageView;
    private TextView dateTextView;
    private TextView descriptionTextView;

    private Event event;

    private FloatingActionMenu floatingActionMenu;
    private FloatingActionButton floatingActionButton1, floatingActionButton2;

    private boolean userParticipates = false;

    private Notification notification = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event);

        Intent intent = getIntent();
        this.event = intent.getParcelableExtra("event");

        this.relativeLayout = (RelativeLayout) findViewById(R.id.event_info);
        this.header_image_event = (ImageView) findViewById(R.id.header_image_event);
        this.clubImageView = (ImageView) findViewById(R.id.event_club_icon);
        this.clubTextView = (TextView) findViewById(R.id.event_club_text);
        this.participantsImageView = (ImageView) findViewById(R.id.event_participants_icon);
        this.participantsTextView = (TextView) findViewById(R.id.event_participants_text);
        this.dateImageView = (ImageView) findViewById(R.id.event_date_icon);
        this.dateTextView = (TextView) findViewById(R.id.event_date_text);
        this.descriptionTextView = (TextView) findViewById(R.id.event_desc);

        final LinearLayout participantsLayout = (LinearLayout) findViewById(R.id.event_participants_layout);
        participantsLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(getBaseContext(), UsersActivity.class).putExtra("users", event.getParticipants()));
            }
        });

        // toolbar

        this.toolbar = (Toolbar) findViewById(R.id.toolbar_event);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // if we come from an android notification
        if (this.event == null) {
            notification = intent.getParcelableExtra("notification");

            if(HttpGet.credentials != null)
                onActivityResult(PostActivity.NOTIFICATION_MESSAGE, RESULT_OK, null);
            else
                startActivityForResult(new Intent(getApplicationContext(), LoginActivity.class), PostActivity.NOTIFICATION_MESSAGE);
        }
        else
            generateEvent();
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // Add your code here

        if(requestCode == PostActivity.NOTIFICATION_MESSAGE){
            if (resultCode == RESULT_OK){

                HttpGet request = new HttpGet(new AsyncResponse() {
                    @Override
                    public void processFinish(String output) {
                        try {
                            event = new Event(new JSONObject(output));

                            generateEvent();

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
                request.execute(HttpGet.ROOTEVENT + "/" + notification.getContent() + "?token=" + HttpGet.credentials.getSessionToken());

            }
        }
    }

    public void generateEvent() {

        // collapsing toolbar

        final CollapsingToolbarLayout collapsingToolbar = (CollapsingToolbarLayout) findViewById(R.id.collapsing_toolbar_event);
        AppBarLayout appBarLayout = (AppBarLayout) findViewById(R.id.appbar_event);
        appBarLayout.addOnOffsetChangedListener(new AppBarLayout.OnOffsetChangedListener() {
            boolean isShow = true;
            int scrollRange = -1;

            @Override
            public void onOffsetChanged(AppBarLayout appBarLayout, int verticalOffset) {
                if (scrollRange == -1) {
                    scrollRange = appBarLayout.getTotalScrollRange();
                }
                if (scrollRange + verticalOffset == 0) {
                    collapsingToolbar.setTitle(event.getName());
                    isShow = true;
                } else if (isShow) {
                    collapsingToolbar.setTitle(" ");
                    isShow = false;
                }
            }
        });

        // dynamic color

        int bgColor = Color.parseColor("#" + event.getBgColor());
        int fgColor = Color.parseColor("#" + event.getFgColor());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final Drawable upArrow = ContextCompat.getDrawable(EventActivity.this, R.drawable.abc_ic_ab_back_material);
            upArrow.setColorFilter(fgColor, PorterDuff.Mode.SRC_ATOP);
            getSupportActionBar().setHomeAsUpIndicator(upArrow);
        }

        collapsingToolbar.setContentScrimColor(bgColor);
        collapsingToolbar.setStatusBarScrimColor(bgColor);

        Glide.with(this).load(HttpGet.IMAGEURL + event.getImage()).into(header_image_event);

        relativeLayout.setBackgroundColor(bgColor);

        // club

        clubImageView.setColorFilter(fgColor);

        final Club club = HttpGet.clubs.get(event.getAssociation());
        if(club == null){
            HttpGet request = new HttpGet(new AsyncResponse() {

                public void processFinish(String output) {
                    if (!output.isEmpty()) {
                        try {
                            JSONObject jsonobject = new JSONObject(output);

                            final Club club = new Club(jsonobject);
                            HttpGet.clubs.put(club.getId(), club);

                            clubTextView.setText(club.getName());

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            request.execute(HttpGet.ROOTASSOCIATION + "/"+ event.getAssociation() + "?token=" + HttpGet.credentials.getSessionToken());
        }
        else
            clubTextView.setText(club.getName());

        clubTextView.setTextColor(fgColor);

        // participants

        participantsImageView.setColorFilter(fgColor);

        int nb_participants = event.getParticipants().size();
        if (nb_participants == 0)
            participantsTextView.setText("Pas encore de participants");
        else if (nb_participants == 1)
            participantsTextView.setText("1 participant");
        else
            participantsTextView.setText(Integer.toString(nb_participants) + " participants");
        participantsTextView.setTextColor(fgColor);

        // date

        dateImageView.setColorFilter(fgColor);

        SimpleDateFormat format = new SimpleDateFormat("EEEE dd/MM", Locale.FRANCE);
        SimpleDateFormat format_hours_minutes = new SimpleDateFormat("HH:mm", Locale.FRANCE);

        if (event.getDateStart().getDay() == event.getDateEnd().getDay() && event.getDateStart().getMonth() == event.getDateEnd().getMonth()) {
            String day = format.format(event.getDateStart());
            dateTextView.setText(day.replaceFirst(".", (day.charAt(0) + "").toUpperCase()) + " de " + format_hours_minutes.format(event.getDateStart()) + " à " + format_hours_minutes.format(event.getDateEnd()));
        } else {
            String start = format.format(event.getDateStart()) + " à " + format_hours_minutes.format(event.getDateStart());
            String end = format.format(event.getDateEnd()) + " à " + format_hours_minutes.format(event.getDateEnd());
            dateTextView.setText("Du " + start.replaceFirst(".", (start.charAt(0) + "").toUpperCase()) + " au " + end.replaceFirst(".", (end.charAt(0) + "").toUpperCase()));
        }

        //dateTextView.setText("" + event.getDateStart() + " au " + event.getDateEnd());
        dateTextView.setTextColor(fgColor);

        // description

        this.descriptionTextView.setText(event.getDescription());

        Linkify.addLinks(descriptionTextView, Linkify.WEB_URLS);
        Utils.convertToLinkSpan(EventActivity.this, descriptionTextView);

        // floating action menu

        for (String id : event.getParticipants()) {
            if (HttpGet.credentials.getUserID().equals(id)) {
                this.userParticipates = true;
                break;
            }
        }

        this.floatingActionMenu = (FloatingActionMenu) findViewById(R.id.fab_event);
        floatingActionMenu.setIconAnimated(false);

        if (!userParticipates) {
            floatingActionMenu.setMenuButtonColorNormal(bgColor);
            floatingActionMenu.setMenuButtonColorPressed(bgColor);
            floatingActionMenu.getMenuIconView().setColorFilter(fgColor);
        } else {
            floatingActionMenu.setMenuButtonColorNormal(0xffffffff);
            floatingActionMenu.setMenuButtonColorPressed(0xffffffff);
            floatingActionMenu.getMenuIconView().setImageDrawable(ContextCompat.getDrawable(EventActivity.this, R.drawable.ic_check_black_24dp));
            floatingActionMenu.getMenuIconView().setColorFilter(0xff4caf50);
        }

        // we can't participate in a finished event
        Date atm = Calendar.getInstance().getTime();
        if (event.getDateEnd().getTime() < atm.getTime()) {
            floatingActionMenu.setVisibility(View.GONE);
        }

        this.floatingActionButton1 = (FloatingActionButton) findViewById(R.id.fab_item_1_event);
        floatingActionButton1.setLabelColors(bgColor, bgColor, 0x99ffffff);
        floatingActionButton1.setLabelTextColor(fgColor);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final Drawable tick = ContextCompat.getDrawable(EventActivity.this, R.drawable.ic_check_black_24dp);
            tick.setColorFilter(0xff4caf50, PorterDuff.Mode.SRC_ATOP);
            floatingActionButton1.setImageDrawable(tick);
        }

        floatingActionButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!userParticipates) {
                    HttpPost request = new HttpPost(new AsyncResponse() {
                        @Override
                        public void processFinish(String output) {
                            userParticipates = true;

                            HttpGet get = new HttpGet(new AsyncResponse() {
                                @Override
                                public void processFinish(String output) {
                                    try {
                                        MainActivity.user = new User(new JSONObject(output));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            get.execute(HttpGet.ROOTUSER + "/" + HttpGet.credentials.getUserID() + "?token=" + HttpGet.credentials.getSessionToken());


                            floatingActionMenu.close(true);
                            floatingActionMenu.setMenuButtonColorNormal(0xffffffff);
                            floatingActionMenu.setMenuButtonColorPressed(0xffffffff);
                            floatingActionMenu.getMenuIconView().setImageDrawable(ContextCompat.getDrawable(EventActivity.this, R.drawable.ic_check_black_24dp));
                            floatingActionMenu.getMenuIconView().setColorFilter(0xff4caf50);

                            SharedPreferences prefs = getSharedPreferences(SigninActivity.class.getSimpleName(), SigninActivity.MODE_PRIVATE);

                            // if first time user join an event
                            if (prefs.getString("addEventToCalender", "").equals("")){
                                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(EventActivity.this);

                                // set title
                                alertDialogBuilder.setTitle("Ajout au calendrier");

                                // set dialog message
                                alertDialogBuilder
                                        .setMessage("Voulez-vous ajouter les évènements auquels vous participer dans votre calendrier ?")
                                        .setCancelable(false)
                                        .setPositiveButton(R.string.positive_button, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialogAlert, int id) {
                                                SharedPreferences.Editor prefs = getSharedPreferences(SigninActivity.class.getSimpleName(), SigninActivity.MODE_PRIVATE).edit();
                                                prefs.putString("addEventToCalender", "true");
                                                prefs.apply();

                                                addEventToCalendar();
                                            }
                                        })
                                        .setNegativeButton(R.string.negative_button, new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialogAlert, int id) {
                                                SharedPreferences.Editor prefs = getSharedPreferences(
                                                        SigninActivity.class.getSimpleName(), SigninActivity.MODE_PRIVATE).edit();
                                                prefs.putString("addEventToCalender", "false");
                                                prefs.apply();

                                                dialogAlert.cancel();
                                            }
                                        });

                                // create alert dialog
                                AlertDialog alertDialog = alertDialogBuilder.create();
                                // show it
                                alertDialog.show();
                            }
                            else if(prefs.getString("addEventToCalender", "true").equals("true")){
                                addEventToCalendar();
                            }

                            refreshEvent(output);
                        }
                    });
                    request.execute(HttpGet.ROOTEVENT + "/" + event.getId() + "/participant/" + HttpGet.credentials.getUserID() + "?token=" + HttpGet.credentials.getSessionToken());
                }
                else
                    floatingActionMenu.close(true);
            }
        });

        this.floatingActionButton2 = (FloatingActionButton) findViewById(R.id.fab_item_2_event);
        floatingActionButton2.setLabelColors(bgColor, bgColor, 0x99ffffff);
        floatingActionButton2.setLabelTextColor(fgColor);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            final Drawable close = ContextCompat.getDrawable(EventActivity.this, R.drawable.ic_close_black_24dp);
            close.setColorFilter(ContextCompat.getColor(EventActivity.this, R.color.colorAccent), PorterDuff.Mode.SRC_ATOP);
            floatingActionButton2.setImageDrawable(close);
        }

        floatingActionButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (userParticipates) {
                    HttpDelete delete = new HttpDelete(new AsyncResponse() {
                        @Override
                        public void processFinish(String output) {
                            userParticipates = false;

                            HttpGet get = new HttpGet(new AsyncResponse() {
                                @Override
                                public void processFinish(String output) {
                                    try {
                                        MainActivity.user = new User(new JSONObject(output));
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                            get.execute(HttpGet.ROOTUSER + "/" + HttpGet.credentials.getUserID() + "?token=" + HttpGet.credentials.getSessionToken());


                            floatingActionMenu.close(true);
                            floatingActionMenu.setMenuButtonColorNormal(0xffffffff);
                            floatingActionMenu.setMenuButtonColorPressed(0xffffffff);
                            floatingActionMenu.getMenuIconView().setImageDrawable(ContextCompat.getDrawable(EventActivity.this, R.drawable.ic_close_black_24dp));
                            floatingActionMenu.getMenuIconView().setColorFilter(ContextCompat.getColor(EventActivity.this, R.color.colorAccent));

                            refreshEvent(output);
                        }
                    });
                    delete.execute(HttpGet.ROOTEVENT + "/" + event.getId() + "/participant/" + HttpGet.credentials.getUserID() + "?token=" + HttpGet.credentials.getSessionToken());
                }
                else
                    floatingActionMenu.close(true);
            }
        });

        // transparent status bar

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.black_trans80));
        }
    }

    public void addEventToCalendar() {
        Calendar cal = Calendar.getInstance();
        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setType("vnd.android.cursor.item/event");

        intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.getDateStart().getTime());
        intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.getDateEnd().getTime());
        intent.putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false);
        intent.putExtra(CalendarContract.Events.TITLE, event.getName());
        intent.putExtra(CalendarContract.Events.DESCRIPTION, event.getDescription());

        startActivity(intent);
    }

    @Override
    public void finish() {
        Intent sendIntent = new Intent();
        sendIntent.putExtra("event", event);

        setResult(RESULT_OK, sendIntent);

        super.finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void refreshEvent(String output) {

        try {
            JSONObject json = new JSONObject(output);

            if(event != null) {

                event = new Event(json.getJSONObject("event"));
                int nb_participants = event.getParticipants().size();

                if (nb_participants == 0)
                    participantsTextView.setText("Pas encore de participants");
                else if (nb_participants == 1)
                    participantsTextView.setText("1 participant");
                else
                    participantsTextView.setText(Integer.toString(nb_participants) + " participants");
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
