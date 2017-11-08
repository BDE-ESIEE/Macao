package fr.esiee.bde.macao.Notifications;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.view.View;

import com.alamkanak.weekview.WeekViewEvent;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import cz.msebera.android.httpclient.Header;
import fr.esiee.bde.macao.Calendar.CalendarEvent;
import fr.esiee.bde.macao.DataBaseHelper;
import fr.esiee.bde.macao.HttpUtils;
import fr.esiee.bde.macao.MainActivity;
import fr.esiee.bde.macao.R;

import static android.app.Notification.DEFAULT_ALL;
import static android.app.Notification.DEFAULT_VIBRATE;
import static fr.esiee.bde.macao.Calendar.WeekViewEvent.createWeekViewEvent;
import static nl.qbusict.cupboard.CupboardFactory.cupboard;

/**
 * Created by delevacw on 08/11/17.
 */

public class NotificationService extends Service {

    private static List<Integer> notificationId = new ArrayList<Integer>();
    private List<CalendarEvent> events = new ArrayList<CalendarEvent>();
    private DataBaseHelper dbHelper;
    private SQLiteDatabase database;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Query the database and show alarm if it applies

        // I don't want this service to stay in memory, so I stop it
        // immediately after doing what I wanted it to do.

        dbHelper =  new DataBaseHelper(this);
        database = dbHelper.getWritableDatabase();

        //getEvents();

        retrieveEvents();
        Log.e("Notification", "Start");
        for (CalendarEvent event : events){
            if(!notificationId.contains(event.getId())) {
                Log.e("Notification", event.getName());
                createNotification(event);
                /*event.setNotified(true);
                ContentValues values = new ContentValues();
                values.put("notified", true);
                cupboard().withDatabase(this.database).update(CalendarEvent.class, values, "startString = ?", event.getStartString());*/
            }
        }

        stopSelf();

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        // I want to restart this service again in one hour
        AlarmManager alarm = (AlarmManager)getSystemService(ALARM_SERVICE);
        alarm.set(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + (1000 * 10),
                PendingIntent.getService(this, 0, new Intent(this, NotificationService.class), 0)
        );
    }

    private void retrieveEvents(){
        Calendar calendar = Calendar.getInstance();
        //calendar.add(Calendar.DAY_OF_YEAR, -1);
        calendar.add(Calendar.HOUR, -1);
        Date today = calendar.getTime();
        Log.i("TIME", String.valueOf(calendar.getTime()));
        //calendar.add(Calendar.HOUR, 10);
        calendar.add(Calendar.HOUR, 1);
        //calendar.add(Calendar.MINUTE, 30);
        Log.i("TIME", String.valueOf(calendar.getTime()));
        Date tomorrow = calendar.getTime();

        String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
        SimpleDateFormat sdf = new SimpleDateFormat(ISO_FORMAT);
        String dateStart = sdf.format(today);
        String dateEnd = sdf.format(tomorrow);


        events.clear();
        //CalendarEvent calendarEvent = cupboard().withDatabase(database).query(CalendarEvent.class).get();
        Cursor cursor = cupboard().withDatabase(this.database).query(CalendarEvent.class).withSelection("startString >= ? and startString <= ? order by startString asc", dateStart, dateEnd).getCursor();
        //Cursor cursor = cupboard().withDatabase(this.database).query(CalendarEvent.class).getCursor();
        // or we can iterate all results
        Iterable<CalendarEvent> itr = cupboard().withCursor(cursor).iterate(CalendarEvent.class);
        for (CalendarEvent calendarEvent: itr) {
            // do something with book
            events.add(calendarEvent);
        }
    }

    private void createNotification(CalendarEvent event){
        final NotificationManager mNotification = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        final Intent launchNotifiactionIntent = new Intent(this, MainActivity.class);
        final PendingIntent pendingIntent = PendingIntent.getActivity(this,
                0, launchNotifiactionIntent,
                PendingIntent.FLAG_ONE_SHOT);

        Calendar calendar = Calendar.getInstance();
        String ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
        SimpleDateFormat sdf = new SimpleDateFormat(ISO_FORMAT);
        String HOUR_FORMAT = "HH:mm";
        SimpleDateFormat hdf = new SimpleDateFormat(HOUR_FORMAT);

        String startHour = event.getStartString();
        String endHour = event.getEndString();
        try {
            calendar.setTime(sdf.parse(startHour));
            calendar.add(Calendar.HOUR_OF_DAY, 1);
            startHour= hdf.format(calendar.getTime());

            calendar.setTime(sdf.parse(endHour));
            calendar.add(Calendar.HOUR_OF_DAY, 1);
            endHour = hdf.format(calendar.getTime());
        } catch (ParseException e) {
            e.printStackTrace();
        }


        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        Notification.Builder builder = new Notification.Builder(this)
                .setWhen(System.currentTimeMillis())
                .setTicker("Titre")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.mipmap.ic_launcher))
                .setContentTitle(event.getName()+" : "+event.getRooms())
                .setContentText(startHour+" - "+endHour+" "+event.getId())
                .setContentIntent(pendingIntent)
                .setDefaults(DEFAULT_ALL)
                .setOnlyAlertOnce(true);

        notificationId.add(event.getId());
        mNotification.notify(event.getId(), builder.build());
    }


    private void getEvents(){
        //SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        SharedPreferences sharedPref = getSharedPreferences("UserData", Context.MODE_PRIVATE);
        String mail = sharedPref.getString("mail", "");
        if(mail.equals("")){
            /*mListener.makeSnackBar("Veuillez vous connecter");
            loader.setVisibility(View.GONE);*/
            Log.e("Agenda", "Non connecté");
        }
        else {
            Log.i("Agenda", "Maj des events");
            RequestParams rp = new RequestParams();
            rp.add("mail", mail);

            HttpUtils.postByUrl("http://ade.wallforfry.fr/api/ade-esiee/agenda", rp, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    // If the response is JSONObject instead of expected JSONArray
                    Log.d("asd", "---------------- this is response : " + response);
                    try {
                        JSONObject serverResp = new JSONObject(response.toString());
                    } catch (JSONException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONArray timeline) {
                    // Pull out the first event on the public timeline
                    try {
                        if(!((JSONObject) timeline.get(0)).has("error")) {
                            cupboard().withDatabase(database).delete(CalendarEvent.class, null);
                            for (int i = 0; i < timeline.length(); i++) {
                                JSONObject obj = (JSONObject) timeline.get(i);
                                SimpleDateFormat dateformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.FRANCE);
                                dateformat.setTimeZone(TimeZone.getTimeZone("UTC"));
                                String start = obj.get("start").toString();
                                String end = obj.get("end").toString();

                                String title = obj.get("name") + "\n" + obj.get("rooms") + "\n" + obj.get("prof") + "\n" + obj.get("unite");
                                String name = obj.get("name").toString();
                                WeekViewEvent event = createWeekViewEvent(i, title, start, end, name);
                                CalendarEvent calendarEvent = new CalendarEvent(i, title, start, end, name);
                                calendarEvent.setRooms(obj.getString("rooms"));
                                calendarEvent.setProf(obj.getString("prof"));
                                calendarEvent.setUnite(obj.getString("unite"));
                                calendarEvent.setColor();

                                //events.add(event);
                                cupboard().withDatabase(database).put(calendarEvent);

                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    retrieveEvents();
                    Log.i("Agenda", "Agenda à jour");
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                    super.onFailure(statusCode, headers, responseString, throwable);
                    Log.d("Failed: ", ""+statusCode);
                    Log.d("Error : ", "" + throwable);
                }

            });
        }
    }
}
