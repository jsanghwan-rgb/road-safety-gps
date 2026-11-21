package kr.local.roadsafety;
import android.Manifest;import android.app.*;import android.content.*;import android.content.pm.PackageManager;import android.location.*;import android.os.*;
public class LocationService extends Service implements LocationListener {
 LocationManager manager;NotificationManager notificationManager;static final String CHANNEL="gps_tracking";
 @Override public void onCreate(){super.onCreate();notificationManager=getSystemService(NotificationManager.class);if(Build.VERSION.SDK_INT>=26)notificationManager.createNotificationChannel(new NotificationChannel(CHANNEL,"도로안심 GPS 위치 추적",NotificationManager.IMPORTANCE_LOW));}
 Notification notification(String detail){Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,CHANNEL):new Notification.Builder(this);return b.setContentTitle("도로안심 GPS 작동 중").setContentText(detail).setSmallIcon(android.R.drawable.ic_menu_mylocation).setOngoing(true).build();}
 @Override public int onStartCommand(Intent i,int flags,int startId){startForeground(101,notification("위치 수신 대기 중"));manager=(LocationManager)getSystemService(LOCATION_SERVICE);if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){try{manager.requestLocationUpdates(LocationManager.GPS_PROVIDER,1500,5,this);}catch(Exception e){stopSelf();}}else stopSelf();return START_STICKY;}
 @Override public void onLocationChanged(Location l){int speed=l.hasSpeed()?Math.max(0,Math.round(l.getSpeed()*3.6f)):-1;notificationManager.notify(101,notification("GPS 수신 · 속도 "+(speed<0?"측정 중":speed+"km/h")+" · 정확도 "+Math.round(l.getAccuracy())+"m"));}
 @Override public void onDestroy(){if(manager!=null)manager.removeUpdates(this);super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}
}
