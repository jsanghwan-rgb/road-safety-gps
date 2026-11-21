package kr.local.roadsafety;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.*;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.util.Locale;

public class MainActivity extends Activity {
 private WebView map;
 private TextView speedText,status,thresholdText;
 private SpeedGauge speedGauge;
 private UpdateManager updateManager;
 private Switch voiceSwitch,vibrationSwitch;
 private boolean follow=true,mapReady=false;
 private int alertSpeed=80;
 private double lastLat=0,lastLon=0,lastAccuracy=-1;
 private final BroadcastReceiver receiver=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){
  double lat=i.getDoubleExtra("lat",0),lon=i.getDoubleExtra("lon",0),kmh=i.getDoubleExtra("speed",-1);
  float accuracy=i.getFloatExtra("accuracy",-1);
  lastLat=lat;lastLon=lon;lastAccuracy=accuracy;
  speedText.setText(kmh<0?"--":String.valueOf(Math.round(kmh)));
  speedGauge.setSpeed(kmh);
  status.setText("GPS 수신 · 정확도 "+(accuracy<0?"확인 중":Math.round(accuracy)+"m")+" · 사용자 경고 "+alertSpeed+"km/h");
  updateMap();
 }};
 @Override public void onCreate(Bundle state){super.onCreate(state);
  alertSpeed=getSharedPreferences("settings",MODE_PRIVATE).getInt("threshold",80);
  getWindow().setStatusBarColor(Color.rgb(12,28,46));getWindow().setNavigationBarColor(Color.rgb(12,28,46));
  updateManager=new UpdateManager(this);
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setBackgroundColor(Color.rgb(239,246,253));
  TextView title=new TextView(this);title.setText("도로안심 GPS");title.setTextSize(19);title.setTextColor(Color.rgb(15,45,75));title.setPadding(22,14,16,4);root.addView(title);
  // Map and electronic instrument panel each occupy exactly half of the flexible display area.
  LinearLayout display=new LinearLayout(this);
  display.setOrientation(LinearLayout.VERTICAL);
  root.addView(display,new LinearLayout.LayoutParams(-1,0,1));

  map=new WebView(this);
  map.getSettings().setJavaScriptEnabled(true);
  map.getSettings().setDomStorageEnabled(true);
  map.getSettings().setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
  map.setWebViewClient(new WebViewClient(){
   @Override public void onPageFinished(WebView v,String url){mapReady=true;updateMap();}
   @Override public void onReceivedError(WebView v,WebResourceRequest req,WebResourceError error){
    if(req.isForMainFrame())status.setText("지도 로딩 실패 · 인터넷 연결을 확인하세요");
   }
  });
  map.loadDataWithBaseURL("https://unpkg.com/",MAP_HTML,"text/html","UTF-8",null);
  display.addView(map,new LinearLayout.LayoutParams(-1,0,1));

  android.widget.FrameLayout dashboard=new android.widget.FrameLayout(this);
  dashboard.setBackgroundColor(Color.rgb(8,18,35));
  display.addView(dashboard,new LinearLayout.LayoutParams(-1,0,1));
  speedGauge=new SpeedGauge(this);
  dashboard.addView(speedGauge,new android.widget.FrameLayout.LayoutParams(-1,-1));
  LinearLayout digital=new LinearLayout(this);
  digital.setGravity(Gravity.CENTER);
  digital.setOrientation(LinearLayout.VERTICAL);
  android.widget.FrameLayout.LayoutParams digitalParams=new android.widget.FrameLayout.LayoutParams(-1,-2,Gravity.CENTER);
  digitalParams.topMargin=dp(28);
  dashboard.addView(digital,digitalParams);
  speedText=new TextView(this);
  speedText.setText("--");
  speedText.setTextSize(49);
  speedText.setGravity(Gravity.CENTER);
  speedText.setTextColor(Color.WHITE);
  speedText.setIncludeFontPadding(false);
  digital.addView(speedText);
  TextView unit=new TextView(this);
  unit.setText("km/h");
  unit.setTextSize(16);
  unit.setTextColor(Color.rgb(90,205,255));
  unit.setGravity(Gravity.CENTER);
  digital.addView(unit);
  status=new TextView(this);
  status.setText("GPS 중지 · 인터넷 연결 후 지도 표시");
  status.setTextSize(11);
  status.setGravity(Gravity.CENTER);
  status.setTextColor(Color.rgb(190,218,242));
  status.setPadding(10,2,10,8);
  dashboard.addView(status,new android.widget.FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM));
  LinearLayout controls=new LinearLayout(this);controls.setOrientation(LinearLayout.VERTICAL);controls.setPadding(8,2,8,2);
  LinearLayout first=new LinearLayout(this);button("GPS 시작",first).setOnClickListener(v->start());button("GPS 중지",first).setOnClickListener(v->{stopService(new Intent(this,LocationService.class));status.setText("GPS 중지");});button("내 위치",first).setOnClickListener(v->{follow=true;if(mapReady)map.evaluateJavascript("window.centerGPS()",null);});controls.addView(first);
  LinearLayout second=new LinearLayout(this);button("기준 −10",second).setOnClickListener(v->{alertSpeed=Math.max(30,alertSpeed-10);saveSettings();});thresholdText=new TextView(this);thresholdText.setGravity(Gravity.CENTER);thresholdText.setTextSize(14);second.addView(thresholdText,new LinearLayout.LayoutParams(0,-1,1));button("기준 +10",second).setOnClickListener(v->{alertSpeed=Math.min(150,alertSpeed+10);saveSettings();});controls.addView(second);
  LinearLayout third=new LinearLayout(this);third.setGravity(Gravity.CENTER_VERTICAL);
  voiceSwitch=new Switch(this);voiceSwitch.setText("음성 경고");voiceSwitch.setChecked(getSharedPreferences("settings",MODE_PRIVATE).getBoolean("voice",false));third.addView(voiceSwitch,new LinearLayout.LayoutParams(0,-2,1));voiceSwitch.setOnCheckedChangeListener((b,on)->saveSettings());
  vibrationSwitch=new Switch(this);vibrationSwitch.setText("진동 경고");vibrationSwitch.setChecked(getSharedPreferences("settings",MODE_PRIVATE).getBoolean("vibration",true));third.addView(vibrationSwitch,new LinearLayout.LayoutParams(0,-2,1));vibrationSwitch.setOnCheckedChangeListener((b,on)->saveSettings());controls.addView(third);
  LinearLayout updateRow=new LinearLayout(this);updateRow.setGravity(Gravity.CENTER_VERTICAL);
  TextView currentVersion=new TextView(this);currentVersion.setText("현재 버전 v"+BuildConfig.VERSION_CODE);currentVersion.setTextSize(12);
  updateRow.addView(currentVersion,new LinearLayout.LayoutParams(0,-2,1));
  Button updateButton=button("↻ 업데이트",updateRow);updateButton.setOnClickListener(v->updateManager.check());
  controls.addView(updateRow);
  TextView caution=new TextView(this);caution.setText("※ 시험판 · 실제 도로 제한속도·단속카메라·사고·공사 데이터 미연동. 설정 기준은 법정 제한속도가 아닙니다. 운전 중 조작 금지.");caution.setTextSize(11);caution.setTextColor(Color.rgb(123,54,35));caution.setPadding(12,5,12,10);controls.addView(caution);root.addView(controls);setContentView(root);saveSettings();
 }
 private int dp(float v){return Math.round(getResources().getDisplayMetrics().density*v);}
 private class SpeedGauge extends View {
  private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
  private final RectF arc=new RectF();
  private double speed=-1;
  SpeedGauge(Context context){super(context);}
  void setSpeed(double next){speed=next;invalidate();}
  @Override protected void onDraw(Canvas canvas){
   super.onDraw(canvas);
   float w=getWidth(),h=getHeight(),cx=w/2f,cy=h*.48f;
   float radius=Math.min(w*.38f,h*.48f);
   float stroke=Math.max(dp(9),Math.min(dp(20),radius*.13f));
   arc.set(cx-radius,cy-radius,cx+radius,cy+radius);
   paint.setStyle(Paint.Style.STROKE);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeWidth(stroke);
   paint.setColor(Color.rgb(37,58,84));canvas.drawArc(arc,145,250,false,paint);
   if(speed>=0){
    paint.setColor(speed>=alertSpeed?Color.rgb(255,101,68):Color.rgb(0,201,255));
    canvas.drawArc(arc,145,(float)(Math.min(speed,180)/180d*250d),false,paint);
   }
   paint.setStyle(Paint.Style.FILL);
   paint.setTextAlign(Paint.Align.CENTER);
   paint.setColor(Color.rgb(134,174,205));paint.setTextSize(Math.max(dp(9),radius*.13f));
   canvas.drawText("0",cx-radius*.83f,cy+radius*.68f,paint);
   canvas.drawText("180",cx+radius*.83f,cy+radius*.68f,paint);
   paint.setTextSize(Math.max(dp(10),radius*.15f));
   paint.setColor(Color.rgb(102,226,255));
   canvas.drawText("DIGITAL SPEED",cx,Math.max(dp(17),cy-radius*.7f),paint);
  }
 }
 private Button button(String label,LinearLayout row){Button b=new Button(this);b.setText(label);b.setTextSize(12);b.setAllCaps(false);row.addView(b,new LinearLayout.LayoutParams(0,-2,1));return b;}
 private void saveSettings(){getSharedPreferences("settings",MODE_PRIVATE).edit().putInt("threshold",alertSpeed).putBoolean("voice",voiceSwitch!=null&&voiceSwitch.isChecked()).putBoolean("vibration",vibrationSwitch==null||vibrationSwitch.isChecked()).apply();if(thresholdText!=null)thresholdText.setText(alertSpeed+" km/h");if(speedGauge!=null)speedGauge.invalidate();}
 private void updateMap(){if(mapReady&&lastLat!=0&&lastLon!=0)map.evaluateJavascript(String.format(Locale.US,"window.updateGPS(%.7f,%.7f,%.1f,%s)",lastLat,lastLon,lastAccuracy,follow?"true":"false"),null);}
 private void start(){if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,Manifest.permission.ACCESS_COARSE_LOCATION},11);return;}if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},12);try{Intent i=new Intent(this,LocationService.class);if(Build.VERSION.SDK_INT>=26)startForegroundService(i);else startService(i);status.setText("GPS 시작 · 위치 수신 대기 중");}catch(Exception e){status.setText("GPS 시작 실패 · 위치 권한 및 배터리 설정 확인");}}
 @Override public void onRequestPermissionsResult(int code,String[] permissions,int[] grants){super.onRequestPermissionsResult(code,permissions,grants);if(code==11&&grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED)start();}
 @Override protected void onStart(){super.onStart();IntentFilter f=new IntentFilter(LocationService.ACTION_LOCATION);if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(receiver,f);}
 @Override protected void onStop(){try{unregisterReceiver(receiver);}catch(Exception ignored){}super.onStop();}
 @Override protected void onDestroy(){if(map!=null)map.destroy();if(updateManager!=null)updateManager.close();super.onDestroy();}
 private static final String MAP_HTML="<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1'><link rel='stylesheet' href='https://unpkg.com/leaflet@1.9.4/dist/leaflet.css'><style>html,body,#map{height:100%;margin:0}#tip{position:absolute;z-index:999;top:8px;left:8px;background:white;padding:6px 10px;border-radius:8px;font:12px sans-serif}</style></head><body><div id='map'></div><div id='tip'>OpenStreetMap · 인터넷 필요</div><script src='https://unpkg.com/leaflet@1.9.4/dist/leaflet.js'></script><script>if(window.L){var m=L.map('map').setView([35.5384,129.3114],12),marker=null,circle=null,last=null,follow=true;L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',{maxZoom:19,attribution:'© OpenStreetMap contributors'}).addTo(m);window.updateGPS=function(lat,lon,acc,f){last=[lat,lon];if(!marker)marker=L.circleMarker(last,{radius:8,color:'#1565c0',fillColor:'#2196f3',fillOpacity:1}).addTo(m);else marker.setLatLng(last);if(acc>0){if(!circle)circle=L.circle(last,{radius:acc,color:'#2196f3',weight:1,fillOpacity:.08}).addTo(m);else{circle.setLatLng(last);circle.setRadius(acc);}}if(f)m.setView(last,Math.max(m.getZoom(),15));};window.centerGPS=function(){if(last)m.setView(last,16);};m.on('dragstart',function(){follow=false;});}else document.getElementById('tip').innerText='지도 라이브러리 로드 실패 · 인터넷 확인';</script></body></html>";
}
