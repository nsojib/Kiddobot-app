package nsojib.com.animatedkiddobot;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import mywebsocket.WebSocketServerSingle;
import mywebsocket.WebsocketInterface;

public class WebServer extends Service implements TTSListener{

    MyTTS tts;
    int PORT=80;
    boolean isrunning=false;
    static final String TAG=WebServer.class.getSimpleName();
    private final IBinder mBinder = new LocalBinder();

    KiddoBot leader;
    public WebServer() {
    }


    public class LocalBinder extends Binder {
        WebServer getService(KiddoBot ld) {
            Log.i(TAG, "binding");
            leader=ld;
            // Return this instance of LocalService so clients can call public methods
            return WebServer.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        this.doStart();
        return mBinder;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        this.doStart();
        //start sticky means service will be explicity started and stopped
        return START_STICKY;
    }

    String local_ip="localhost";
    WebSocketServerSingle ws;
    public void doStart() {
        Log.i(TAG, "doStart");

        if (isrunning) {
            Toast.makeText(this, "TTSService already running", Toast.LENGTH_LONG).show();
            return;
        }
        tts=new MyTTS(this, this);
        tts.speak_b("I am ready");

        try {
            new Thread() {
                public void run() {
                    try {
                        local_ip = getLocalIpAddress();
                        System.out.println("Local IP="+local_ip);
                        ws = new WebSocketServerSingle(new websocketlistener(), 8000);
                        ws.start();
                        System.out.println("websocket started");

                        tts.speak_b("I am ready");
                    }catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }.start();
        }catch(Exception ex) {
            ex.printStackTrace();
        }

    }

    class websocketlistener implements WebsocketInterface {
        @Override
        public void onopen(String remote) {
            System.out.println("onopen=" + remote);
            tts.speak_nb("connected");
        }

        public String get_help() {
            JSONObject jsr=new JSONObject();
            String data="json keys: type, data. type=tts,asr,test \nTTS Server and ASR Server over websocket.";
            return  data;
        }
        @Override
        public void onmessage(String msg) {
            System.out.println("onmessage=" + msg);
            JSONObject js=null;

            try {
                js= new JSONObject(msg);
            }catch (Exception ex) {
                String resp="parsing error";
                try {
                    JSONObject jsr = new JSONObject();
                    String data = get_help();
                    jsr.put("data", data);
                    resp=jsr.toString();
                }catch(Exception e) {

                }
                ws.send_msg(resp);
                return;
            }

            try {
                String type=null, data=null;
                if(js.has("type")) {
                    type=js.getString("type");
                }
                if(js.has("data")) {
                    data=js.getString("data");
                }

                System.out.println("type="+type+" data="+data);
                if(type==null ) {
                    data=get_help();
                }else if(data!=null && data.equals("help")) {
                    data=get_help();
                }else if(type.equals("tts")) {
                    tts.speak_nb(data);
                    leader.on_speak_start();
                    data="ToTTS="+data;
                }else if(type.equals("asr")) {
                    leader.dostart();
                    data="ToASR="+data;
                }else if(type.equals("test")) {
                    data="ToTest="+data;
                }else {
                    data="unknown type. data="+data;
                }
                JSONObject jsr=new JSONObject();
                jsr.put("data", data);
                ws.send_msg(jsr.toString() );
            }catch (Exception ex) {
                    ex.printStackTrace();
            }

//                try{
//                    boolean s=ws.send_msg("_received_"+msg);
//                    System.out.println("sent="+s);
//                }catch (Exception ex) {
//                    ex.printStackTrace();
//                }

        }

        @Override
        public void onclose() {
            System.out.println("connection closed");
            tts.speak_nb("connection lost");
        }
    }

    @Override
    public void onTTSCompleted() {
        Log.i(TAG, "tts completed");

        send_msg("tts", "tts_completed");
        if(leader!=null) {
            leader.on_speak_finished();
        }
    }

    public void send_msg(final String type, final String txt) {
        Log.i(TAG, "sending msg ="+txt);
        if(ws!=null) {
            new Thread() {
                public void run() {
                    String resp=txt;
                    try {
                        JSONObject jsr = new JSONObject();
                        jsr.put("data",txt);
                        resp=jsr.toString();
                    }catch(Exception e) {
                    }
                    ws.send_msg(resp);
                }
            }.start();

        }
    }
    public void send_json(final JSONObject jsr) {
        Log.i(TAG, "sending msg ="+jsr);
        if(ws!=null) {
            new Thread() {
                public void run() {
                    String resp=jsr.toString();
                    ws.send_msg(resp);
                }
            }.start();

        }
    }

    public String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (SocketException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "onDestroy");
        super.onDestroy();
    }
}