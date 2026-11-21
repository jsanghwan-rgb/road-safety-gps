package kr.local.roadsafety;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.widget.Toast;
import org.json.*;
import java.net.*;
import java.io.*;

/** In-app update: only GitHub Releases from this project's official repository. */
public final class UpdateManager {
 private static final String API="https://api.github.com/repos/jsanghwan-rgb/road-safety-gps/releases/latest";
 private static final String APK_TYPE="application/vnd.android.package-archive";
 private final Activity activity;
 private long downloadId=-1;
 private boolean receiverRegistered=false;
 private final BroadcastReceiver complete=new BroadcastReceiver(){@Override public void onReceive(Context c,Intent i){
  if(!DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(i.getAction()) || i.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,-1)!=downloadId)return;
  DownloadManager dm=(DownloadManager)activity.getSystemService(Context.DOWNLOAD_SERVICE);
  DownloadManager.Query q=new DownloadManager.Query().setFilterById(downloadId);
  try(android.database.Cursor cursor=dm.query(q)){
   if(cursor!=null&&cursor.moveToFirst()){
    int status=cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS));
    if(status==DownloadManager.STATUS_SUCCESSFUL){install(dm.getUriForDownloadedFile(downloadId));return;}
   }
  }catch(Exception ignored){}
  toast("업데이트 다운로드 실패 · 인터넷 연결 확인");
 }};
 public UpdateManager(Activity a){activity=a;}
 public void check(){
  toast("최신 버전 확인 중…");
  new Thread(()->{
   try{
    HttpURLConnection conn=(HttpURLConnection)new URL(API).openConnection();
    conn.setConnectTimeout(10000);conn.setReadTimeout(12000);
    conn.setRequestProperty("Accept","application/vnd.github+json");
    conn.setRequestProperty("User-Agent","RoadSafetyGPS-Android");
    int status=conn.getResponseCode();
    if(status==404){showMessage("업데이트 준비 중","아직 GitHub Releases에 배포된 정식 APK가 없습니다. 개발용 Actions APK는 자동 업데이트에 사용할 수 없습니다.");return;}
    if(status!=200)throw new IOException("GitHub HTTP "+status);
    ByteArrayOutputStream out=new ByteArrayOutputStream();
    try(InputStream in=conn.getInputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){out.write(buf,0,n);if(out.size()>500000)throw new IOException("응답 크기 초과");}}
    JSONObject json=new JSONObject(out.toString("UTF-8"));
    String tag=json.optString("tag_name","");
    int latest=versionCode(tag);
    if(latest<=0){showMessage("버전 확인 불가","배포 태그는 v6처럼 숫자 버전 코드여야 합니다.");return;}
    if(latest<=BuildConfig.VERSION_CODE){showMessage("최신 버전입니다","현재 v"+BuildConfig.VERSION_CODE+" · 배포 v"+latest);return;}
    JSONArray assets=json.optJSONArray("assets");String url=null;
    if(assets!=null)for(int j=0;j<assets.length();j++){
     JSONObject item=assets.optJSONObject(j);if(item==null)continue;
     String name=item.optString("name","");
     if(name.equals("RoadSafetyGPS-release.apk")){url=item.optString("browser_download_url");break;}
    }
    if(url==null||!url.startsWith("https://github.com/jsanghwan-rgb/road-safety-gps/releases/download/")){
     showMessage("설치 파일 없음","배포에 RoadSafetyGPS-release.apk가 없습니다.");return;
    }
    final String downloadUrl=url;
    activity.runOnUiThread(()->new AlertDialog.Builder(activity)
     .setTitle("새 버전 v"+latest+" 발견")
     .setMessage("현재 버전 v"+BuildConfig.VERSION_CODE+" → 최신 v"+latest+"\n\n새 APK를 앱에서 다운로드합니다. 설치할 때 안드로이드의 확인이 필요합니다. 기존 앱과 동일한 서명 키여야 업데이트할 수 있습니다.")
     .setNegativeButton("취소",null).setPositiveButton("업데이트",(d,w)->download(downloadUrl,latest)).show());
   }catch(Exception e){showMessage("업데이트 확인 실패","인터넷 연결 또는 GitHub 배포 상태를 확인하세요. ("+e.getClass().getSimpleName()+")");}
  }).start();
 }
 private int versionCode(String tag){try{return Integer.parseInt(tag.replaceFirst("^[vV]",""));}catch(Exception e){return -1;}}
 private void download(String url,int version){
  try{
   if(!activity.getPackageManager().canRequestPackageInstalls()){
    new AlertDialog.Builder(activity).setTitle("설치 권한 필요")
     .setMessage("이 앱에서 업데이트를 설치하려면 '이 출처 허용'을 켜야 합니다. 허용 후 업데이트 버튼을 다시 누르세요.")
     .setPositiveButton("설정 열기",(d,w)->activity.startActivity(new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,Uri.parse("package:"+activity.getPackageName()))))
     .setNegativeButton("취소",null).show();return;
   }
   DownloadManager.Request request=new DownloadManager.Request(Uri.parse(url));
   request.setTitle("도로안심 GPS v"+version+" 업데이트");request.setDescription("공식 GitHub Releases APK 다운로드");
   request.setMimeType(APK_TYPE);
   request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
   request.setDestinationInExternalFilesDir(activity,Environment.DIRECTORY_DOWNLOADS,"RoadSafetyGPS-update-v"+version+".apk");
   DownloadManager dm=(DownloadManager)activity.getSystemService(Context.DOWNLOAD_SERVICE);
   downloadId=dm.enqueue(request);
   if(!receiverRegistered){IntentFilter filter=new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);
    if(Build.VERSION.SDK_INT>=33)activity.registerReceiver(complete,filter,Context.RECEIVER_EXPORTED);
    else activity.registerReceiver(complete,filter);receiverRegistered=true;}
   toast("업데이트 다운로드 시작");
  }catch(Exception e){toast("다운로드 시작 실패: "+e.getClass().getSimpleName());}
 }
 private void install(Uri uri){
  if(uri==null){toast("다운로드한 파일을 찾지 못했습니다");return;}
  try{
   Intent intent=new Intent(Intent.ACTION_VIEW);
   intent.setDataAndType(uri,APK_TYPE);
   intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_ACTIVITY_NEW_TASK);
   activity.startActivity(intent);
  }catch(Exception e){toast("설치 화면을 열 수 없습니다. 다운로드 알림을 확인하세요.");}
 }
 private void showMessage(String title,String message){activity.runOnUiThread(()->new AlertDialog.Builder(activity).setTitle(title).setMessage(message).setPositiveButton("확인",null).show());}
 private void toast(String msg){activity.runOnUiThread(()->Toast.makeText(activity,msg,Toast.LENGTH_LONG).show());}
 public void close(){if(receiverRegistered){try{activity.unregisterReceiver(complete);}catch(Exception ignored){}receiverRegistered=false;}}
}
