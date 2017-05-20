package edu.niu.cs.gunjute.chat_message;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.apache.http.params.HttpParams;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private Button mButtonSend;
    private EditText mEditTextMessage;
    private ImageView mImageView;
    private TextToSpeech t1;

    private ChatMessageAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        // setting up logo in menubar

        ActionBar eksinbar=getSupportActionBar();
        if (eksinbar != null) {
            eksinbar.setDisplayHomeAsUpEnabled(true);
            eksinbar.setHomeAsUpIndicator(R.mipmap.ic_launcher);
        }


        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        //How to hide keyboard just by one tap outside of an edittext?  ---- Not Working

       findViewById(R.id.relativeLayout).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
                return true;
            }
        });

        // Text to speech

        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.US);
                }
            }
        });

        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mButtonSend = (Button) findViewById(R.id.btn_send);
        mEditTextMessage = (EditText) findViewById(R.id.et_message);
        mImageView = (ImageView) findViewById(R.id.iv_image);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new ChatMessageAdapter(this, new ArrayList<ChatMessage>());
        mRecyclerView.setAdapter(mAdapter);

        mButtonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = mEditTextMessage.getText().toString();
                if (TextUtils.isEmpty(message)) {
                    return;
                }
                sendMessage(message.trim());
                mimicOtherMessage(message.trim());

                mEditTextMessage.setText("");
            }
        });


        mImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });
    }

    private void sendMessage(String message) {
        ChatMessage chatMessage = new ChatMessage(message, true, false);
        mAdapter.add(chatMessage);
    }

    private void mimicOtherMessage(String message) {

        String res="";
        URL url;

        if(haveNetworkConnection()) {
            HttpURLConnection urlConnection = null;
            try {
                message = message.replaceAll(" ", "_");
                url = new URL("http://192.169.174.68:8080/TARS/webapi/query/" + message);

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(5000);

              //  res=urlConnection.getResponseMessage();
               // urlConnection.setReadTimeout(10000);
                InputStream in = urlConnection.getInputStream();

                InputStreamReader isw = new InputStreamReader(in);

                int data = isw.read();
                while (data != -1) {
                    char current = (char) data;
                    res += current;
                    data = isw.read();
                    System.out.print(current);
                }

                String toSpeak = res;
                Toast.makeText(getApplicationContext(), toSpeak,Toast.LENGTH_SHORT).show();
                t1.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);

            } catch(SocketTimeoutException e)
            {
                res = "Sorry, I'm currently booting";
            }
            catch (Exception e) {
                e.printStackTrace();
                res = "Sorry, I'm currently Unavailable";
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }
            else
            {
                res = "No Internet Connection";
            }

            Log.d("", " Res Message " + res);
            ChatMessage chatMessage = new ChatMessage(new String("Genie:\n"+res), false, false);
            mAdapter.add(chatMessage);
            mRecyclerView.scrollToPosition(mAdapter.getItemCount() - 1);

    }

    private void sendMessage() {

        if(haveNetworkConnection()) {
            ChatMessage chatMessage = new ChatMessage(null, true, true);
            mAdapter.add(chatMessage);
        }
    }

    private void mimicOtherMessage() {
        ChatMessage chatMessage = new ChatMessage(null, false, true);
        mAdapter.add(chatMessage);

        mRecyclerView.scrollToPosition(mAdapter.getItemCount() - 1);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_exit) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            //return true;
        } else if (id == R.id.action_single_view) {
            Intent intent = new Intent(this, SingleViewActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

// Check for Internet Connection
private boolean haveNetworkConnection() {
    boolean haveConnectedWifi = false;
    boolean haveConnectedMobile = false;

    ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo[] netInfo = cm.getAllNetworkInfo();
    for (NetworkInfo ni : netInfo) {
        if (ni.getTypeName().equalsIgnoreCase("WIFI"))
            if (ni.isConnected())
                haveConnectedWifi = true;
        if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
            if (ni.isConnected())
                haveConnectedMobile = true;
    }
    return haveConnectedWifi || haveConnectedMobile;
}

// Text To Speech onPause Method
    public void onPause(){
        if(t1 !=null){
            t1.stop();
            t1.shutdown();
        }
        super.onPause();
    }

}
