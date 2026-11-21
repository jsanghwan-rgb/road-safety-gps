package kr.local.roadsafety;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.*;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.util.Locale;

public class MainActivity extends Activity {
 private WebView map;
 private TextView speedText,status;
 private boolean follow=true,voice=false;
 private int alertSpeed=80;
 private long lastAlert=0;
 private TextToSpeech tts;
 private boolean speechReady=false;
 private final BroadcastReceiver receiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){
  double lat=i.getDoubleExtra("lat",0),lon=i.getDoubleExtra("lon",0),kmh=i.getDoubleExtra("speed",-1);
  float accuracy=i.getFloatExtra("accuracy",-1);
  speedText.setText(kmh<0?"-- km/h":Math.round(kmh)+" km/h");
  status.setText("GPS 수신 · 정확도 "+(accuracy<0?"확인 중":Math.round(accuracy)+"m")+" · 경고 기준 "+alertSpeed+"km/h (사용자 설정)");
  if(map!=null)map.evaluateJavascript("window.updateGPS("+lat+","+lon+","+accuracy+","+follow+")",null);
  if(kmh>=alertSpeed&&System.currentTimeMillis()-lastAlert>30000){lastAlert=System.currentTimeMillis();if(voice&&speechReady)tts.speak("설정한 속도 기준을 넘었습니다",TextToSpeech.QUEUE_FLUSH,null,"speed-alert");else {android.os.Vibrator v=(android.os.Vibrator)getSystemService(VIBRATOR_SERVICE);if(v!=null&&Build.VERSION.SDK_INT>=26)v.vibrate(VibrationEffect.createOneShot(350,VibrationEffect.DEFAULT_AMPLITUDE));}}
 }};
 @Override public void onCreate(Bundle state){super.onCreate(state);
  getWindow().setStatusBarColor(Color.rgb(12,28,46));getWindow().setNavigationBarColor(Color.rgb(12,28,46));
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(239,246,253));
  TextView title=new TextView(this);title.setText("도로안심 GPS");title.setTextSize(23);title.setTextColor(Color.rgb(15,45,75));title.setPadding(22,18,16,6);root.addView(title);
  speedText=new TextView(this);speedText.setText("-- km/h");speedText.setTextSize(40);speedText.setTextColor(Color.rgb(12,66,112));speedText.setPadding(22,0,20,0);root.addView(speedText);
  status=new TextView(this);status.setText("GPS 중지 · 지도는 인터넷 연결 필요");status.setTextSize(13);status.setPadding(22,0,14,12);root.addView(status);
  map=new WebView(this);map.getSettings().setJavaScriptEnabled(true);map.getSettings().setDomStorageEnabled(true);map.setWebViewClient(new WebViewClient());map.loadDataWithBaseURL("https://unpkg.com/",MAP_HTML,"text/html","UTF-8",null);root.addView(map,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.VERTICAL);controls.setPadding(10,5,10,5);
  LinearLayout first=new LinearLayout(this);Button start=button("GPS 시작",first);start.setOnClickListener(v->start());Button stop=button("중지",first);stop.setOnClickListener(v->{Intent stopIntent=new Intent(this,LocationService.class);stopIntent.setAction(LocationService.ACTION_STOP);startService(stopIntent);status.setText("GPS 중지");});Button center=button("내 위치",first);center.setOnClickListener(v->{follow=true;map.evaluateJavascript("window.centerGPS()",null);});controls.addView(first);
  LinearLayout second=new LinearLayout(this);Button minus=button("기준 −10",second);minus.setOnClickListener(v->{alertSpeed=Math.max(30,alertSpeed-10);updateThreshold();});Button plus=button("기준 +10",second);plus.setOnClickListener(v->{alertSpeed=Math.min(150,alertSpeed+10);updateThreshold();});Button speech=button("음성 OFF",second);speech.setOnClickListener(v->{voice=!voice;speech.setText(voice?"음성 ON":"음성 OFF");});controls.addView(second);
  TextView caution=new TextView(this);caution.setText("※ 지도·GPS 시험판. 실제 도로 제한속도/카메라/사고/공사 정보 미연동. 설정 속도는 법정 제한속도가 아닙니다. 운전 중 조작 금지.");caution.setTextSize(11);caution.setTextColor(Color.rgb(123,54,35));caution.setPadding(14,6,14,12);controls.addView(caution);root.addView(controls);setContentView(root);
  tts=new TextToSpeech(this,s->{speechReady=s==TextToSpeech.SUCCESS;if(speechReady)tts.setLanguage(Locale.KOREAN);});
 }
 private Button button(String text,LinearLayout row){Button b=new Button(this);b.setText(text);b.setTextSize(12);row.addView(b,new LinearLayout.LayoutParams(0,-2,1));return b;}
 private void updateThreshold(){status.setText("사용자 설정 속도 경고: "+alertSpeed+" km/h · 실제 제한속도 아님");}
 private void start(){if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},11);return;}if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},12);try{Intent i=new Intent(this,LocationService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);status.setText("GPS 시작 · 위치 수신 대기 중");}catch(Exception e){status.setText("GPS 시작 실패: "+e.getMessage());}}
 @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] grants){super.onRequestPermissionsResult(code,permissions,grants);if(code==11&&grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED)start();}
 @Override protected void onStart(){super.onStart();IntentFilter filter=new IntentFilter(LocationService.ACTION_LOCATION);if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,filter,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(receiver,filter);}
 @Override protected void onStop(){try{unregisterReceiver(receiver);}catch(Exception ignored){}super.onStop();}
 @Override protected void onDestroy(){if(map!=null)map.destroy();if(tts!=null){tts.stop();tts.shutdown();}super.onDestroy();}
 private static final String MAP_HTML="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'><link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'><style>html,body,#map{height:100%;margin:0}#tip{position:absolute;z-index:999;top:8px;left:8px;background:white;padding:5px 9px;border-radius:8px;font:12px sans-serif}</style></head><body><div id='map'></div><div id='tip'>OpenStreetMap · 인터넷 필요</div><script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script><script>var m=L.map('map').setView([35.5384,129.3114],12),marker=null,circle=null,last=null;L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© OpenStreetMap contributors'}).addTo(m);window.updateGPS=function(lat,lon,acc,follow){last=[lat,lon];if(!marker){marker=L.circleMarker(last,{radius:8,color:'#1565c0',fillColor:'#2196f3',fillOpacity:1}).addTo(m);}else marker.setLatLng(last);if(acc>0){if(!circle)circle=L.circle(last,{radius:acc,color:'#2196f3',weight:1,fillOpacity:.08}).addTo(m);else{circle.setLatLng(last);circle.setRadius(acc);}}if(follow)m.setView(last,Math.max(m.getZoom(),15));};window.centerGPS=function(){if(last)m.setView(last,16);};m.on('dragstart',function(){});</script></body></html>";
}
