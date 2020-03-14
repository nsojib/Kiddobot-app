package nsojib.com.animatedkiddobot;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import org.json.JSONObject;
import java.util.ArrayList;

public class KiddoBot extends AppCompatActivity {


    WebServer mService;
    boolean mBound = false;
    Handler handler=new Handler();

    final static String TAG=KiddoBot.class.getSimpleName();

    Intent intent_web;
    private SpeechRecognizer speech = null;
    private Intent recognizerIntent;
    boolean performingSpeechSetup = true;

    String ip="service not connected";
    View main;
    GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

//        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        int UI_OPTIONS = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;

        getWindow().getDecorView().setSystemUiVisibility(UI_OPTIONS);

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.dimAmount=0.0f;  //0->no dim, 1->dim
        getWindow().setAttributes(lp);


        requestRecordAudioPermission();
        speech_init();

        main=new  RobotView(this);
        setContentView(main);

        // Bind to LocalService
        intent_web = new Intent(this, WebServer.class);
//        startService(intent);
        bindService(intent_web, mConnection, Context.BIND_AUTO_CREATE);
        Log.i(TAG, "service bind req");

        gestureDetector = new GestureDetector(this, new GestureListener());
    }

    int ct=0;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
//        if (event.getAction() == MotionEvent.ACTION_UP) {
//            Log.i(TAG, "ontouch");
//            ++ct;
//            if(ct==5){
//                Toast.makeText(KiddoBot.this, "IP: "+ip, Toast.LENGTH_LONG).show();
//                ct=0;
//            }
////            Toast.makeText(this, "onTouchEvent", Toast.LENGTH_LONG).show();
//
//            return true;
//        } else {
//            return false;
//        }
    }

    private class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }

        // event when double tap occurs
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            float x = e.getX();
            float y = e.getY();

            Log.d("Double Tap", "Tapped at: (" + x + "," + y + ")");
            Toast.makeText(KiddoBot.this, "IP: "+ip, Toast.LENGTH_LONG).show();
            return true;
        }

    }




    void dostop() {
        System.out.println("dostop request");
        speech.stopListening();
    }
    void dostart() {
        System.out.println("dostart request");


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    speech.startListening(recognizerIntent);
                }catch (Exception ex) {
//            et.setText("Can't start recog");
                    ex.printStackTrace();
                }
            }
        });

    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection mConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,    IBinder service) {
            Log.i(TAG, "onServiceConnected");

            // We've bound to LocalService, cast the IBinder and get LocalService instance
            WebServer.LocalBinder binder = (WebServer.LocalBinder) service;
            mService = binder.getService(KiddoBot.this);
            mBound = true;
            addMsg("Service Connected.");

            ip=mService.getLocalIpAddress();
            Toast.makeText(KiddoBot.this, "IP: "+ip, Toast.LENGTH_LONG).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            Log.i(TAG, "onServiceDisconnected");
            mBound = false;
        }
    };


    public void addMsg(String msg) {
        Log.i(TAG, "addMsg="+msg);
    }

    public void on_speak_finished() {
        lip_aniimation=0;
    }
    public void on_speak_start() {
        lip_aniimation=1;
    }


    static int lip_aniimation=0;  //1, 0, -1
    public static class RobotView extends  View {
        private Context context;
        Paint paint;
        int eye_x ;
        int eye_y=300;  //half.
        int eye_dist=500;
        private int screenW, screenH;
        int count=0;

        public RobotView(Context context) {
            super(context);
            this.context=context;

            paint = new Paint();
            paint.setColor(Color.argb(0xff, 0, 0x00, 0xff));
            paint.setStrokeWidth(10);
            setBackgroundColor(0xFF000000);
        }

        @Override
        public void onSizeChanged (int w, int h, int oldw, int oldh) {
            super.onSizeChanged(w, h, oldw, oldh);
            screenW = w;
            screenH = h;

            eye_x=screenW-220;

            eye_y=screenH/2-250;
            eye_dist=500;

        }

        public void draw_eyes(Canvas canvas) {
            paint.setColor(Color.argb(0xff, 0xff, 0xff, 0xff));
            canvas.drawCircle(eye_x,eye_y,150, paint);
            canvas.drawCircle(eye_x,eye_y+eye_dist,150, paint);

            paint.setColor(Color.argb(0xff, 0, 0x00, 0xff));
            canvas.drawCircle(eye_x,eye_y,70, paint);
            canvas.drawCircle(eye_x,eye_y+eye_dist,70, paint);
        }

        int lipx=60;
        int lipd=100;
        int lipl=260;
        int lipw=60;



        void draw_lips_open(Canvas canvas) {
//            lipx=60;
//            lipd=100;

            if(lipx>60) { //minx
                lipx-=4;
            }
            if(lipd<100) {  //max dist
                lipd+=4;
            }
        }
        int ofs=0;
        void draw_lips(Canvas canvas){
            paint.setColor(Color.argb(0xff, 0, 0x00, 0xff));
            canvas.drawRect(new Rect(  lipx+ofs,screenH/2-lipl/2,  lipx+lipw+ofs ,screenH/2+lipl/2), paint);  //lip
            canvas.drawRect(new Rect(lipx+lipd-ofs,screenH/2-lipl/2, lipx+lipd+lipw-ofs ,screenH/2+lipl/2), paint);    //lip2
        }


        void draw_lips_close(Canvas canvas) {
//            lipx=75;
//            lipd=70;
            if(lipx<80) {
                lipx+=4;
            }
            if(lipd>60) {
                lipd-=4;
            }
        }


        boolean dir=true;
        @Override
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            draw_eyes(canvas);

            System.out.println("lip animation="+lip_aniimation);
            int spd=4;
            if(lip_aniimation>0) {
                ofs+=spd;
                if(ofs>30) {
                    lip_aniimation=-1;
                }
            }else if(lip_aniimation<0) {
                ofs-=spd;
                if(ofs<=-10) {
                    lip_aniimation=+1;
                }
            }else if(lip_aniimation==0){
                lipx=60;
                lipd=100;
                lipl=260;
                lipw=60;
                ofs=0;
            }


            draw_lips(canvas);

            if(dir) {
                ++count;
            }else {
                --count;
            }

            if(count<11) {
                paint.setColor(Color.argb(0xff, 0x00, 0x00, 0x00));

                int leftx=count*25;
                canvas.drawRect(new Rect(screenW-200-leftx,0, screenW ,screenH ), paint);
//                dir=false;

//                draw_lips(canvas);
            }else if(count<22) {
                paint.setColor(Color.argb(0xff, 0x00, 0x00, 0x00));

                int leftx= (22-count)*25;
                canvas.drawRect(new Rect(screenW-200-leftx,0, screenW ,screenH ), paint);

//                draw_lips_close(canvas);
            }  else if(count>200){
                count=0;
            }else {
//                draw_lips_open(canvas);
            }

            invalidate();
        }



    }


    private void requestRecordAudioPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String requiredPermission = Manifest.permission.RECORD_AUDIO;

            // If the user previously denied this permission then show a message explaining why
            // this permission is needed
            if (checkCallingOrSelfPermission(requiredPermission) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(new String[]{requiredPermission}, 101);
            }
        }
    }
    void speech_init() {
        speech = SpeechRecognizer.createSpeechRecognizer(this);
        speech.setRecognitionListener(new listener());
        recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,"bn-BD");
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,this.getPackageName());
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        recognizerIntent.putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", new String[]{"bn-BD"});
        recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD");
    }
    class listener implements RecognitionListener
    {
        public void onReadyForSpeech(Bundle params)
        {
            Log.d(TAG, "onReadyForSpeech");
            performingSpeechSetup = false;
        }
        public void onBeginningOfSpeech()
        {
            Log.d(TAG, "onBeginningOfSpeech");
        }
        public void onRmsChanged(float rmsdB)
        {
            Log.d(TAG, "onRmsChanged");
        }
        public void onBufferReceived(byte[] buffer)
        {
            Log.d(TAG, "onBufferReceived");
        }
        public void onEndOfSpeech()
        {
            Log.d(TAG, "onEndofSpeech");
        }
        public void onError(int error)
        {
            Log.d(TAG,  "error " +  error);
            if (performingSpeechSetup && error == SpeechRecognizer.ERROR_NO_MATCH) return;

            doSend("_recog_error_", null);
        }
        public void onResults(Bundle results)
        {
            String str = new String();
            Log.d(TAG, "onResults " + results);
            String resp="_none_";
            try {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String text = "";
                for (String result : matches)
                    text += result + "::";

                Log.i(TAG, text);
                String q=matches.get(0);
                doSend(q, matches);
            }catch(Exception ex) {
                ex.printStackTrace();
            }

        }
        public void onPartialResults(Bundle partialResults)
        {
            Log.d(TAG, "onPartialResults");
        }
        public void onEvent(int eventType, Bundle params)
        {
            Log.d(TAG, "onEvent " + eventType);
        }
    }

    private void doSend(String txt, ArrayList<String> recoglist) {
            JSONObject jsn = new JSONObject();
           try {
               jsn.put("data", txt);
               jsn.put("datas", recoglist);
               mService.send_json(jsn);
           }catch(Exception ex) {

           }
    }



    @Override
    protected void onStop() {
        Log.i(TAG, "onServiceStop");
//        try {
//            unbindService(mConnection);
//        }catch (Exception ex) {}
//
//        mBound = false;

        super.onStop();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mConnection  != null) {
            unbindService(mConnection);
        }
    }

}


