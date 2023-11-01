package com.example.myapplication666;

import androidx.annotation.NonNull;
import androidx.annotation.UiThread;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.CameraUpdate;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.NaverMapSdk;
import com.naver.maps.map.OnMapReadyCallback;
import com.naver.maps.map.overlay.InfoWindow;
import com.naver.maps.map.overlay.LocationOverlay;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.PathOverlay;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
    private NaverMap mMap;
    private DatabaseReference mDatabase;
    private GpsTracker gpsTracker;
    LatLng prev_LOC = null;
    LatLng curr_LOC;
    Marker mk = new Marker();

    LocationManager locationManager;
    LocationListener locationListener;

    // 위치 저장해야해서 불러옴...
    Location location;
    Button saveLoc;
    Button myLoc;
    EditText saveName;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        
        // 데이터베이스 연결
        mDatabase = FirebaseDatabase.getInstance().getReference();
        getLocList();


        final ListView listView = findViewById(R.id.listView);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView parent, View v, int position, long id) {
                // 누르면 해당 번호를 타고 데이터를 가져옴
                
                /**
                 아쉬운게.. 데이터 고유 번호를 따다 가져오고 싶었는데 얘는 그냥 리스트에서 얻어온 순번 가지고
                 데이터를 가져오는거라서 좀 찝찝함
                 **/
                getLoc(String.valueOf(position));
            }
        }) ;

        // 원위치 버튼
        myLoc = (Button)  findViewById(R.id.myLoc);
        myLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gpsTracker = new GpsTracker(MainActivity.this);

                double latitude = gpsTracker.getLatitude();
                double longitude = gpsTracker.getLongitude();

                curr_LOC = new LatLng(latitude, longitude);
                CameraUpdate camera = CameraUpdate.scrollTo(curr_LOC);

                mMap.moveCamera(camera);

                Toast.makeText(MainActivity.this, "내 위치로 돌아왔습니다.", Toast.LENGTH_SHORT).show();
    
            }
        });

        // 위치 저장용 목록 불러오기
        saveLoc = (Button) findViewById(R.id.saveLoc);
        saveName = (EditText) findViewById(R.id.saveName);
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // 네이버 맵
        String naver_client_id = getString(R.string.NAVER_CLIENT_ID);
        NaverMapSdk.getInstance(this).setClient(
                new NaverMapSdk.NaverCloudPlatformClient(naver_client_id)
        );

        MapFragment mapFragment = (MapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        if (mapFragment == null){
            mapFragment = MapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().add(R.id.map,
                    mapFragment).commit();
        }

        mapFragment.getMapAsync(this);



        saveLoc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gpsTracker = new GpsTracker(MainActivity.this);

                double latitude = gpsTracker.getLatitude();
                double longitude = gpsTracker.getLongitude();
                String getName = saveName.getText().toString();

                if (getName.equals(null) || getName == "" || getName == null || getName == " " || getName.length() <= 0){
                    // 별의별 조건을 다 걸긴 했는데, 만약 위치명을 작성하지 않으면 적으라고 함
                    Toast.makeText(MainActivity.this, "위치 명을 작성하셔야 합니다!", Toast.LENGTH_SHORT).show();
                } else{
                    // 이후 위치명과 위도 경도를 메소드로 내보냄
                    getLocNum(getName, latitude, longitude);
                }
            }
        });
    }
    /**
     파이어베이스로 값을 전달할 때마다 무한적으로 값이 보내지는 오류가 발생함
     EX ) 분명 5번째에 값을 저장하려고 하는데, 2000번째까지 똑값은 값이 반복적으로 저장되고 있다.

     mDatabase.child("locs") 데아터를 읽어오는 메소드인 addValueEventListener 에서
     두번의 반복 실행이 되는 점을 발견했고, stackoverflow에서도 관련 오류가 발생했다는 점을 확인함.
     그래서 해결방법인 addListenerForSingleValueEvent 로 대신해서 넣어보니 무한반복이 사라졌음.
     **/

    private void getLocNum(String locName, double latitude, double longitude){
        int cnt = 0;
        mDatabase.child("locs").addListenerForSingleValueEvent(new ValueEventListener(){
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot){
                int cnt = 0;
                // 데이터를 얻어옴
                if (dataSnapshot.getValue() != null){
                    // 가져온 값은 기본적으로 arraylist, 0번째 인덱스는 null값임
                    ArrayList dataList = (ArrayList) dataSnapshot.getValue();
                    for (int i = 1; i < dataList.size(); i ++){
                        cnt += 1;
                    }
                    writeLoc(String.valueOf(cnt+1), locName, latitude, longitude);
                } else{
                    Toast.makeText(MainActivity.this, "데이터가 없으므로 0번째부터 시작합니다.", Toast.LENGTH_SHORT).show();
                    writeLoc("0", locName, latitude, longitude);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    // 순번 리스트 호출
    private void getLocList(){
        final ListView listView = findViewById(R.id.listView);
        ArrayList<HashMap<String, String>> data = new ArrayList<HashMap<String, String>>();
        mDatabase.child("locs").addListenerForSingleValueEvent(new ValueEventListener(){
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot){
                ArrayList<LocDTO> locList = new ArrayList<LocDTO>();
                // 데이터를 얻어옴
                if (dataSnapshot.getValue() != null){

                    // 가져온 값은 기본적으로 arraylist, 0번째 인덱스는 null값임
                    ArrayList dataList = (ArrayList) dataSnapshot.getValue();
                    for (int i = 0; i < dataList.size(); i ++){
                        HashMap hashMap = (HashMap) dataList.get(i);
                        HashMap<String, String> adapterMap = new HashMap<String, String>();
                        String locName = (String) hashMap.get("locName");
                        double latitude = (double) hashMap.get("latitude");
                        double longitude = (double) hashMap.get("longitude");
                        adapterMap.put("locName", "[ " + i + " ] " + locName);
                        adapterMap.put("location", "위도 : " + latitude + " 경도 : " + longitude);
                        // 어댑터용 리스트 담기
                        data.add(adapterMap);
                        // 해시맵으로 받아온걸 dto에 담아둠
                        LocDTO locDTO = new LocDTO(locName, latitude, longitude);
                        locList.add(locDTO);
                        //listView.setAdapter();
                        //Toast.makeText(MainActivity.this, "내용 : " + locDTO, Toast.LENGTH_SHORT).show();
                    }
                    SimpleAdapter adapter = new SimpleAdapter(
                            getApplicationContext(),
                            data,
                            android.R.layout.simple_list_item_2,
                            new String[]{"locName", "location"},
                            new int[]{android.R.id.text1,android.R.id.text2});
                    listView.setAdapter(adapter);
                } else{
                    Toast.makeText(MainActivity.this, "데이터가 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    // 순번 호출
    private void getLoc(String num){
        final ListView listView = findViewById(R.id.listView);
        ArrayList<HashMap<String, String>> data = new ArrayList<HashMap<String, String>>();
        mDatabase.child("locs").child(num).addListenerForSingleValueEvent(new ValueEventListener(){
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot){
                // 데이터를 얻어옴
                if (dataSnapshot.getValue() != null){
                    HashMap hashMap = (HashMap) dataSnapshot.getValue();
                    String locName = (String) hashMap.get("locName");
                    double latitude = (double) hashMap.get("latitude");
                    double longitude = (double) hashMap.get("longitude");

                    // 가져온 데이터를 가지고 맵에서 로드시킴
                    moveMap(locName, latitude, longitude);
                } else{
                    Toast.makeText(MainActivity.this, "데이터가 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, "오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void writeLoc(String getNext, String locName, double latitude, double longitude){
        LocDTO locDTO = new LocDTO(locName, latitude, longitude);
        Toast.makeText(this, "순번 전달 성공 : " + getNext, Toast.LENGTH_SHORT).show();
        Toast.makeText(this, "DB 접근 성공", Toast.LENGTH_SHORT).show();
        mDatabase.child("locs").child(getNext).setValue(locDTO)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        Toast.makeText(MainActivity.this, "DB 추가 성공", Toast.LENGTH_SHORT).show();
                        // 리스트 새로고침시켜서 목록을 갱신함
                        getLocList();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(MainActivity.this, "데이터 추가 실패", Toast.LENGTH_SHORT).show();
                    }
                });

    }

    @UiThread
    @Override
    public void onMapReady(@NonNull NaverMap naverMap){
        // 지도 객체를 여러 메소도에서 사용할 수 있도록 글로벌 객체로 할당
        mMap = naverMap;


        locationListener = new LocationListener() {
            // 위치가 변할 때마다 호출
            public void onLocationChanged(Location location) {
                updateMap(location);
            }

            // 위치서비스가 변경될 때
            public void onStatusChanged(String provider, int status, Bundle extras) {
                alertStatus(provider);
            }

            // 사용자에 의해 Provider 가 사용 가능하게 설정될 때
            public void onProviderEnabled(String provider) {
                alertProvider(provider);
            }

            // 사용자에 의해 Provider 가 사용 불가능하게 설정될 때
            public void onProviderDisabled(String provider) {
                checkProvider(provider);
            }
        };

        // 시스템 위치 서비스 관리 객체 생성
        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);

        // 정확한 위치 접근 권한이 설정되어 있지 않으면 사용자에게 권한 요구
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 100);
            return;
        }

        String locationProvider;
        // GPS 에 의한 위치 변경 요구
        locationProvider = LocationManager.GPS_PROVIDER;
        locationManager.requestLocationUpdates(locationProvider, 1, 1, locationListener);
        // 통신사 기지국에 의한 위치 변경 요구
        locationProvider = LocationManager.NETWORK_PROVIDER;
        locationManager.requestLocationUpdates(locationProvider, 1, 1, locationListener);

    }

    public void checkProvider(String provider) {
        Toast.makeText(this, provider + "에 의한 위치서비스가 꺼져 있습니다. 켜주세요...", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    public void alertProvider(String provider) {
        Toast.makeText(this, provider + "서비스가 켜졌습니다!", Toast.LENGTH_LONG).show();
    }

    public void alertStatus(String provider) {
        Toast.makeText(this, "위치서비스가 " + provider + "로 변경되었습니다!", Toast.LENGTH_LONG).show();
    }

    public void updateMap(Location location) {
        // 위도
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        // 경도
        curr_LOC = new LatLng(latitude, longitude);

        // 이전 위치가 없는 경우
        if (prev_LOC == null) {
            // 지도 크기
            CameraUpdate cameraUpdate = CameraUpdate.scrollTo(curr_LOC);
            mMap.moveCamera(cameraUpdate);
            // 위치 오버레이 표시(원)
            LocationOverlay locationOverlay = mMap.getLocationOverlay();
            locationOverlay.setVisible(true);
            locationOverlay.setPosition(curr_LOC);

            // 현재 위치를 이전 위치로 설정
            prev_LOC = curr_LOC;

            // 이전 위치가 있는 경우
        } else {
            // 경로 표시
            PathOverlay path = new PathOverlay();
            path.setCoords(Arrays.asList(
                    new LatLng(prev_LOC.latitude, prev_LOC.longitude),
                    new LatLng(curr_LOC.latitude, curr_LOC.longitude)
            ));
            path.setMap(mMap);
            path.setOutlineColor(Color.BLACK);
            path.setColor(Color.YELLOW);
            path.setWidth(30);

            // 현재 위치에 마커 표시
            mk.setVisible(false);
            mk.setPosition(curr_LOC);
            mk.setMap(mMap);
            mk.setVisible(true);

            // 현재 경로를 이전 경로로 설정
            prev_LOC = curr_LOC;
        }
    }

    public void moveMap(String locName, double latitude, double longitude){
        curr_LOC = new LatLng(latitude, longitude);
        CameraUpdate camera = CameraUpdate.scrollTo(curr_LOC);

        mMap.moveCamera(camera);

        Marker marker = new Marker();
        marker.setPosition(curr_LOC);
        marker.setMap(mMap);

        marker.setSubCaptionText(locName);
        marker.setCaptionColor(Color.RED);
        marker.setCaptionHaloColor(Color.YELLOW);
        marker.setCaptionTextSize(10);

        InfoWindow infoWindow = new InfoWindow();
        infoWindow.setAdapter(new InfoWindow.DefaultTextAdapter(this) {
            @NonNull
            @Override
            public CharSequence getText(@NonNull InfoWindow infoWindow) {
                return locName;
            }
        });
        infoWindow.open(marker);


        Toast.makeText(this, "[" + locName + "] " + "위치를 로드했습니다.", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null)
            locationManager.removeUpdates(locationListener);
    }

    // DB 이동 순서 : 버튼 > getLocNum() > writeLoc()
    // 생각했던 DB 이동 순서 : 버튼 > writeLoc() + getLocNum()(return)

}