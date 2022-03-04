package com.mytaxi.demo

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.model.Place
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mytaxi.demo.Place.Companion.getPlaceNameFromPosition
import com.mytaxi.demo.viewmodels.PlaceViewModel
import io.reactivex.Flowable.just
import io.reactivex.Maybe.just
import io.reactivex.Observable.just
import io.reactivex.Single.just
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var geocoder: Geocoder
    private lateinit var googleMap: GoogleMap
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var isPermissionGranted = false
    private var isGpsOn = false
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private var wayLatitude = 0.0
    private var wayLongtitude = 0.0
    private val UPDATE_INTERVAL = (10 * 1000).toLong()
    private val FASTEST_INTERVAL: Long = 2000
    private var currentPos: LatLng? = null

    private lateinit var tashkentLat: Array<String>
    private lateinit var tashkentLng: Array<String>
    private lateinit var vertices: ArrayList<LatLng>

    private val COLOR_RED_ARGB = Color.RED
    private val POLYGON_STROKE_WIDTH_PX = 5f

    private lateinit var mPlaceViewModel: PlaceViewModel



    private lateinit var regionSupportedBottomSheetBehavior: BottomSheetBehavior<View>
    //private lateinit var regionNotSupportedDialog: RegionNotSupportedDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        locationRequest = LocationRequest.create()
        locationRequest.apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = UPDATE_INTERVAL
            fastestInterval = FASTEST_INTERVAL
        }

        // updated location will come to locationCallback
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                onLocationChanged(locationResult!!.lastLocation)
            }
        }
        geocoder = Geocoder(this, Locale.ENGLISH)

        // checking whether location access permission is granted or not
        checkPermissionGranted()


                
        checkGpsStatus()

        mPlaceViewModel = ViewModelProvider(this).get(PlaceViewModel::class.java)
        mPlaceViewModel.place.observe(this,
            androidx.lifecycle.Observer<String> {
                placeName.text = it
            })

        locationImgButt.setOnClickListener {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return@setOnClickListener
            }
            mFusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
        }
        navigateImgButt.setOnClickListener {
            dLayout.openDrawer(Gravity.LEFT)
        }

        //getting map view on the screen
//        mapFragment = supportFragmentManager.findFragmentById(com.google.android.gms.location.R.id.map) as SupportMapFragment
//        mapFragment.getMapAsync(this)

//        getDeviceLocation()
//
//        nView.setNavigationItemSelectedListener {
//            if (it.itemId == com.google.android.gms.location.R.id.trips) {
//                startActivity(Intent(this, TripHistoryActivity::class.java))
//                dLayout.closeDrawers()
//            }
           // return@setNavigationItemSelectedListener true
        }



    private fun checkGpsStatus() {
        GpsUtil(this)
            .turnGPSOn(object : GpsUtil.OnGpsStatusListener {
                override fun onGpsStatusListener(gpsStatus: Boolean) {
                    isGpsOn = gpsStatus
                }
            })
    }

    private fun checkPermissionGranted() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) !=
            PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                Constants.locationRequestCode
            )
        } else {
            isPermissionGranted = true
        }
    }

    private fun onLocationChanged(location: Location) {
        mFusedLocationProviderClient.removeLocationUpdates(locationCallback)

        wayLatitude = location.latitude
        wayLongtitude = location.longitude

        // saving device's current position
        currentPos = LatLng(wayLatitude, wayLongtitude)

        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentPos!!, 15f))
    }

    override fun onMapReady(p0: GoogleMap) {
        if (p0 != null) {

            googleMap = p0
            googleMap.moveCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(41.311081, 69.240562),
                    10f
                )
            )
            // if permission granted enabling my location
            if (isPermissionGranted) {
                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return
                }
                googleMap.isMyLocationEnabled = true
                googleMap.uiSettings.isMyLocationButtonEnabled = false
            }
            // drawing tashkent territory polygon
            //drawPolygon(vertices)

            // tracking camera in order to determine marker's position
            googleMap.setOnCameraMoveStartedListener {
                //progressBar.visibility = View.VISIBLE
            }

            googleMap.setOnCameraMoveListener {
               // progressBar.visibility = View.VISIBLE
            }

            googleMap.setOnCameraMoveCanceledListener {
               // progressBar.visibility = View.GONE
            }

            googleMap.setOnCameraIdleListener {
                // finding center position of camera because of marker is always in that point
                val latLng = googleMap.cameraPosition.target
                // Toast.makeText(activity, "working"+latLng.latitude+", "+latLng.longitude, Toast.LENGTH_SHORT).show()
                if (PointStatus.isPointInPolygon(latLng, vertices)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        // determining markers place name through it's position
                        Observable.just(latLng).map { t ->
                            Place.getPlaceNameFromPosition(
                                t,
                                geocoder
                            )
                        }
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe {
                                if (regionSupportedBottomSheetBehavior.isHideable) {
//                                    regionSupportedBottomSheetBehavior.isHideable = false
//                                    regionSupportedBottomSheetBehavior.state =
                                        BottomSheetBehavior.STATE_EXPANDED
                                }
                                mPlaceViewModel.setName(it)
                            }
                    }
                } else if (currentPos != null) {
                    regionSupportedBottomSheetBehavior.isHideable = true
                    regionSupportedBottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    mPlaceViewModel.setName("Region is not supported")
//                    regionNotSupportedDialog.show(
//                        supportFragmentManager,
//                        RegionNotSupportedDialog::class.java.name
                   // )
                }
                //progressBar.visibility = View.GONE
            }

        }
    }
}





