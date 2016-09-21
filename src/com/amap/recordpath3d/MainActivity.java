package com.amap.recordpath3d;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps.AMap;
import com.amap.api.maps.AMapUtils;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.LocationSource;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.database.DbAdapter;
import com.amap.record.PathRecord;
import com.example.recordpath3d.R;


/**
 * AMapV2地图中介绍如何显示一个基本地图
 */
public class MainActivity extends Activity implements LocationSource, AMapLocationListener {
	private MapView mapView;
	private AMap aMap;
	private OnLocationChangedListener mListener;
	private AMapLocationClient mlocationClient;
	private AMapLocationClientOption mLocationOption;
	private PolylineOptions mPolyoptions;
	private PathRecord record;
	private long starttime;
	private long endtime;
	private ToggleButton btn;
	private DbAdapter DbHepler;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.basicmap_activity);
	    /*
         * 设置离线地图存储目录，在下载离线地图或初始化地图设置;
         * 使用过程中可自行设置, 若自行设置了离线地图存储的路径，
         * 则需要在离线地图下载和使用地图页面都进行路径设置
         * */
	    //Demo中为了其他界面可以使用下载的离线地图，使用默认位置存储，屏蔽了自定义设置
      //  MapsInitializer.sdcardDir =OffLineMapUtils.getSdCacheDir(this);
		mapView = (MapView) findViewById(R.id.map);
		mapView.onCreate(savedInstanceState);// 此方法必须重写
	 
		init();
		initpolyline();
		
	}

	/**
	 * 初始化AMap对象
	 */
	private void init() {
		if (aMap == null) {
			aMap = mapView.getMap();
			setUpMap();
		}
		btn = (ToggleButton)findViewById(R.id.locationbtn);
		btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (btn.isChecked()) {
					Log.i("MY","isChecked");

					aMap.clear(true);
					if (record != null) {
						record = null;	
					}
					record = new PathRecord();
					starttime = System.currentTimeMillis();
					record.setDate(getcueDate(starttime));
				} else {
					endtime = System.currentTimeMillis();
					saverecord(record);
				}
			}
		});
		
	}

	protected void saverecord(PathRecord record) {
		if (record !=null && record.getPathline().size()>0) {
			DbHepler=new DbAdapter(this);
			DbHepler.open();  
			record.setDuration(String.valueOf((endtime-starttime)/1000f));
			float distance = 0;
			String pathline = "";
			for (int i = 0; i < record.getPathline().size(); i++) {
				if (i < record.getPathline().size()-1) {
					LatLng firstpoint = record.getPathline().get(i);
					LatLng secoundpoint = record.getPathline().get(i+1);
					distance = distance + AMapUtils.calculateLineDistance(firstpoint, secoundpoint);
				}
				LatLng point = record.getPathline().get(i);
				pathline = pathline + point.latitude+","+point.longitude + ";";
			}
			record.setDistance(String.valueOf(distance));
			record.setStartpoint(record.getPathline().get(0));
			record.setAveragespeed(String.valueOf(distance/(float)(endtime-starttime)));
			record.setEndpoint(record.getPathline().get(record.getPathline().size()-1));
			
			String stratpoint = record.getStartpoint().latitude+","+record.getStartpoint().longitude;
			String endpoint = record.getEndpoint().latitude+","+record.getEndpoint().longitude;
			DbHepler.createrecord(record.getDistance(), 
					record.getDuration(),
					record.getAveragespeed(), 
					pathline, stratpoint, endpoint, 
					record.getDate());
			DbHepler.close();
		} else {
			Toast.makeText(MainActivity.this, "没有记录到路径", Toast.LENGTH_SHORT).show();
		}
		
		
	}

	private void initpolyline() {
		mPolyoptions = new PolylineOptions();
		mPolyoptions.width(10f);
		mPolyoptions.color(Color.BLUE);
	}
	
	/**
	 * 设置一些amap的属性
	 */
	private void setUpMap() {
		aMap.setLocationSource(this);// 设置定位监听
		aMap.getUiSettings().setMyLocationButtonEnabled(true);// 设置默认定位按钮是否显示
		aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
		// 设置定位的类型为定位模式 ，可以由定位、跟随或地图根据面向方向旋转几种
		aMap.setMyLocationType(AMap.LOCATION_TYPE_LOCATE);
	}
	/**
	 * 方法必须重写
	 */
	@Override
	protected void onResume() {
		super.onResume();
		mapView.onResume();
	}

	/**
	 * 方法必须重写
	 */
	@Override
	protected void onPause() {
		super.onPause();
		mapView.onPause();
	}

	/**
	 * 方法必须重写
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mapView.onSaveInstanceState(outState);
	}

	/**
	 * 方法必须重写
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mapView.onDestroy();
	}

	@Override
	public void activate(OnLocationChangedListener listener) {
		mListener = listener;
		startlocation();
	}

	@Override
	public void deactivate() {
		mListener = null;
		if (mlocationClient != null) {
			mlocationClient.stopLocation();
			mlocationClient.onDestroy();
			
		}
		mlocationClient = null;	
	}

	@Override
	public void onLocationChanged(AMapLocation amapLocation) {
		if (mListener != null && amapLocation != null) {
			if (amapLocation != null
					&& amapLocation.getErrorCode() == 0) {
				mListener.onLocationChanged(amapLocation);// 显示系统小蓝点
				LatLng mylocation = new LatLng(amapLocation.getLatitude(), amapLocation.getLongitude());
				aMap.moveCamera(CameraUpdateFactory.changeLatLng(mylocation));
				if (btn.isChecked()) {
					record.addpoint(mylocation);
					mPolyoptions.add(mylocation);
					redrawline();
				}		
			} else {
				String errText = "定位失败," + amapLocation.getErrorCode()+ ": " + amapLocation.getErrorInfo();
				Log.e("AmapErr",errText);
			}
		}
	}
	
	private void startlocation(){
		if (mlocationClient == null) {
			mlocationClient = new AMapLocationClient(this);
			mLocationOption = new AMapLocationClientOption();
			//设置定位监听
			mlocationClient.setLocationListener(this);
			//设置为高精度定位模式
			mLocationOption.setLocationMode(AMapLocationMode.Hight_Accuracy);
			
			mLocationOption.setInterval(2000);
			//设置定位参数
			mlocationClient.setLocationOption(mLocationOption);
			// 此方法为每隔固定时间会发起一次定位请求，为了减少电量消耗或网络流量消耗，
			// 注意设置合适的定位时间的间隔（最小间隔支持为2000ms），并且在合适时间调用stopLocation()方法来取消定位请求
			// 在定位结束后，在合适的生命周期调用onDestroy()方法
			// 在单次定位情况下，定位无论成功与否，都无需调用stopLocation()方法移除请求，定位sdk内部会移除
			mlocationClient.startLocation();
			
		}
	}

	private void redrawline() {
		if (mPolyoptions.getPoints().size()>0) {
			aMap.clear(true);
			aMap.addPolyline(mPolyoptions);	
		}	
	}
	
	@SuppressLint("SimpleDateFormat") 
	private String getcueDate(long time){
		SimpleDateFormat formatter = new SimpleDateFormat ("yyyy-MM-dd  HH:mm:ss ");       
		Date curDate = new Date(time);  
		String date = formatter.format(curDate);
		return date;       
	}

	public void record(View view){
		Intent intent = new Intent(MainActivity.this, RecordActivity.class);
		startActivity(intent);
	}
}
