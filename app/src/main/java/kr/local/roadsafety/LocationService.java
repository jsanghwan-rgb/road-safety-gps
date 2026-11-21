package kr.local.roadsafety;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import java.util.Locale;

public class LocationService extends Service implements LocationListener {
 public static final String ACTION_LOCATION="kr.local.roadsafety.LOCATION";
 public static final String ACTION_STOP="kr.local.roadsafety.STOP";
 private static final String CHANNEL="gps_tracking";
 private LocationManager manager;
 private NotificationManager notifications;
 private boolean subscribed=false;
 @Override public void onCreate(){super.onCreate();notifications=getSystemService(NotificationManager.class);if(Build.VERSION.SDK_INT>=26)notifications.createNotificationChannel(new NotificationChannel(CHANNEL,"도로안심 GPS 위치 추적",NotificationManager.IMPORTANCE_LOW));}
 private Notification notification(String detail){Intent launch=new Intent(this,MainActivity.class);PendingIntent pi=PendingIntent.getActivity(this,0,launch,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);return b.setContentTitle("도로안심 GPS · 위치 추적 중").setContentText(detail).setContentIntent(pi).setSmallIcon(android.R.drawable.ic_menu_mylocation).setOngoing(true).build();}
 @Override public int onStartCommand(Intent intent,int flags,int startId){
  if(intent!=null&&ACTION_STOP.equals(intent.getAction())){stopSelf();return START_NOT_STICKY;}
  startForeground(101,notification("GPS 위치 수신 대기 중"));
  if(subscribed)return START_STICKY;
  manager=(LocationManager)getSystemService(LOCATION_SERVICE);
  boolean fine=checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;
  boolean coarse=checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION)==PackageManager.PERMISSION_GRANTED;
  if(!fine&&!coarse){stopSelf();return START_NOT_STICKY;}
  try {
   if(fine&&manager.isProviderEnabled(LocationManager.GPS_PROVIDER)){manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1500,3,this);subscribed=true;}
   if(manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)){manager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,5000,10,this);subscribed=true;}
   if(!subscribed){notifications.notify(101,notification("휴대전화 위치 서비스를 켜 주세요"));}
  }catch(SecurityException|IllegalArgumentException e){notifications.notify(101,notification("위치 수신 오류: 권한 또는 GPS 확인"));}
  return START_STICKY;
 }
 @Override public void onLocationChanged(Location loc){
  double kmh=loc.hasSpeed()?Math.max(0,loc.getSpeed()*3.6):-1;
  Intent data=new Intent(ACTION_LOCATION);
  data.setPackage(getPackageName());
  data.putExtra("lat",loc.getLatitude());data.putExtra("lon",loc.getLongitude());data.putExtra("accuracy",loc.hasAccuracy()?loc.getAccuracy():-1f);
  data.putExtra("speed",kmh);data.putExtra("time",loc.getTime());sendBroadcast(data);
  String speed=kmh<0?"속도 측정 중":String.format(Locale.KOREA,"%.0f km/h",kmh);
  notifications.notify(101,notification(speed+" · 위치 정확도 "+(loc.hasAccuracy()?Math.round(loc.getAccuracy())+"m":"확인 중")));
 }
 @Override public void onDestroy(){if(manager!=null){try{manager.removeUpdates(this);}catch(SecurityException ignored){}}stopForeground(true);super.onDestroy();}
 @Override public IBinder onBind(Intent intent){return null;}
}
