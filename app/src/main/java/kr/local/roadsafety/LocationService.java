package kr.local.roadsafety;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.*;
import android.speech.tts.TextToSpeech;
import java.util.Locale;

public class LocationService extends Service implements LocationListener, TextToSpeech.OnInitListener {
 public static final String ACTION_LOCATION="kr.local.roadsafety.LOCATION";
 public static final String ACTION_STOP="kr.local.roadsafety.STOP";
 public static final String ACTION_SETTINGS="kr.local.roadsafety.SETTINGS";
 private static final String CHANNEL="gps_tracking";
 private LocationManager manager;
 private NotificationManager notifications;
 private TextToSpeech tts;
 private boolean speechReady=false,subscribed=false;
 private long lastAlert=0,lastGpsTime=0;
 private float lastSpeed=-1;
 @Override public void onCreate(){super.onCreate();notifications=getSystemService(NotificationManager.class);if(Build.VERSION.SDK_INT>=26)notifications.createNotificationChannel(new NotificationChannel(CHANNEL,"도로안심 GPS 추적",NotificationManager.IMPORTANCE_LOW));tts=new TextToSpeech(this,this);}
 @Override public void onInit(int result){speechReady=result==TextToSpeech.SUCCESS&&tts.setLanguage(Locale.KOREAN)>=0;if(speechReady&&Build.VERSION.SDK_INT>=21)tts.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());}
 private Notification notification(String detail){PendingIntent pi=PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);return b.setContentTitle("도로안심 GPS · 위치 추적 중").setContentText(detail).setContentIntent(pi).setSmallIcon(android.R.drawable.ic_menu_mylocation).setOngoing(true).build();}
 @Override public int onStartCommand(Intent intent,int flags,int startId){
  if(intent!=null&&ACTION_STOP.equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
  if(intent!=null&&ACTION_SETTINGS.equals(intent.getAction()))return START_STICKY;
  startForeground(101,notification("GPS 수신 대기 중"));
  if(subscribed)return START_STICKY;
  manager=(LocationManager)getSystemService(LOCATION_SERVICE);
  boolean fine=checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;
  boolean coarse=checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;
  if(!fine&&!coarse){stopSelf();return START_NOT_STICKY;}
  try{
   if(fine&&manager.isProviderEnabled(LocationManager.GPS_PROVIDER)){manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1000,2,this);subscribed=true;}
   if(manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,4000,10,this);subscribed=true;}
   if(!subscribed)notifications.notify(101,notification("휴대전화 위치 서비스를 켜 주세요"));
  }catch(SecurityException|IllegalArgumentException e){notifications.notify(101,notification("GPS 권한 또는 위치 설정 확인 필요"));}
  return START_STICKY;
 }
 @Override public void onLocationChanged(Location loc){
  if(!loc.hasAccuracy()||loc.getAccuracy()>150||loc.getLatitude()==0&&loc.getLongitude()==0)return;
  long now=System.currentTimeMillis();
  // Prevent stale network positions from replacing a recent accurate GPS fix.
  if(!LocationManager.GPS_PROVIDER.equals(loc.getProvider())&&now-lastGpsTime<7000)return;
  if(LocationManager.GPS_PROVIDER.equals(loc.getProvider()))lastGpsTime=now;
  double speed=loc.hasSpeed()?Math.max(0,loc.getSpeed()*3.6):-1;
  if(speed>250)return;
  lastSpeed=(float)speed;
  Intent data=new Intent(ACTION_LOCATION).setPackage(getPackageName());
  data.putExtra("lat",loc.getLatitude());data.putExtra("lon",loc.getLongitude());data.putExtra("accuracy",loc.getAccuracy());data.putExtra("speed",speed);data.putExtra("time",loc.getTime());sendBroadcast(data);
  notifications.notify(101,notification(speed<0?"위치 수신 · 속도 측정 중":Math.round(speed)+"km/h · 정확도 "+Math.round(loc.getAccuracy())+"m"));
  int threshold=getSharedPreferences("settings",MODE_PRIVATE).getInt("threshold",80);
  boolean voice=getSharedPreferences("settings",MODE_PRIVATE).getBoolean("voice",false);
  boolean vibration=getSharedPreferences("settings",MODE_PRIVATE).getBoolean("vibration",true);
  // Only alert with sufficiently accurate speed fixes; user threshold is NOT a legal speed limit.
  if(speed>=threshold&&loc.getAccuracy()<=50&&now-lastAlert>=30000){lastAlert=now;
   if(vibration){android.os.Vibrator v=(android.os.Vibrator)getSystemService(VIBRATOR_SERVICE);if(v!=null&&v.hasVibrator())v.vibrate(VibrationEffect.createOneShot(400,VibrationEffect.DEFAULT_AMPLITUDE));}
   if(voice&&speechReady)tts.speak("설정한 속도 기준을 넘었습니다",TextToSpeech.QUEUE_FLUSH,null,"speed-alert");
  }
 }
 @Override public void onDestroy(){if(manager!=null)try{manager.removeUpdates(this);}catch(SecurityException ignored){}if(tts!=null){tts.stop();tts.shutdown();}stopForeground(true);super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}
}
