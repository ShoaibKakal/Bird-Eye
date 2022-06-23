package com.example.birdseye

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.birdseye.model.MapData
import com.example.birdseye.model.Result
import com.example.birdseye.network.ApiInterface
import com.example.holefinder.model.HoleData
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import io.reactivex.SingleObserver
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*


class MapsFragment : Fragment() {

    val FASTEST_INTERVAL = 5
    val DEFAULT_INTERVAL = 10
    private val DEFAULT_ZOOM = 13f


//    private lateinit var layoutMiscellaneous: LinearLayout

    //for fetching the current location of the user
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null

    //this class is responsible for the suggestions of places as the user types in search bar in maps
    private val placesClient: PlacesClient? = null

    //to store the places suggestions
    private val predictionList: List<AutocompletePrediction>? = null

    private var lastLocation: Location? = null
    private var locationCallback: LocationCallback? = null
    private lateinit var addLocationImage: ImageView

    val database = Firebase.database
    val myRef = database.getReference("holesData")
    private lateinit var mGoogleMap: GoogleMap
    private var markers = ArrayList<Marker>()
    private lateinit var drawRouteBtn: Button
    private var addedMarkers = ArrayList<LatLng>()
    private lateinit var place1: MarkerOptions
    private lateinit var place2: MarkerOptions
    private lateinit var lastMarker: Marker


    private lateinit var polylineList: ArrayList<LatLng>
    private var polylineOptions = PolylineOptions()

    private val callback = OnMapReadyCallback { googleMap ->
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
        mGoogleMap = googleMap
        getData()
        currentLocation()
        addMarkerOnClick()


    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_maps, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)

        addedMarkers = ArrayList()
//        layoutMiscellaneous = view.findViewById<LinearLayout>(R.id.layoutMiscellaneous)
        drawRouteBtn = view.findViewById(R.id.btnRoute)
        addLocationImage = view.findViewById(R.id.btn_add_location)
//        bottomSheet()

//        val bottomSheetBehavior = BottomSheetBehavior.from(layoutMiscellaneous)

//        drawRouteBtn.setOnClickListener {
//            if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
//                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED)
//            } else {
//                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)
//            }
//        }

        addLocationImage.setOnClickListener {
            addLocation()
        }


        drawRouteBtn.setOnClickListener {

            val originLocation = place2.position
            mGoogleMap.addMarker(MarkerOptions().position(originLocation))
            val destinationLocation = place1.position
            mGoogleMap.addMarker(MarkerOptions().position(destinationLocation))
            val urll = getDirectionURL(originLocation, destinationLocation, getString(R.string.google_maps_key))
            Log.d(TAG, "addMarkerOnClick: $urll")
            GetDirection(urll).execute()
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(originLocation, 14F))

        }





    }


    private fun addLocation() {
        getDeviceLocation()

        val id = Calendar.getInstance().time
        val holeData = HoleData(id.toString(), lastLocation!!.latitude, lastLocation!!.longitude)
        myRef.push().setValue(holeData)
    }

//    private fun bottomSheet() {
//
//        val bottomSheetBehavior = BottomSheetBehavior.from(layoutMiscellaneous)
//
//        layoutMiscellaneous.findViewById<View>(R.id.textMiscellaneous).setOnClickListener {
//            if (bottomSheetBehavior.state != BottomSheetBehavior.STATE_EXPANDED) {
//                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED)
//            } else {
//                bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED)
//            }
//        }
//    }

    private fun currentLocation() {
        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(requireContext())

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        mGoogleMap.isMyLocationEnabled = true
        mGoogleMap.uiSettings.isMyLocationButtonEnabled = true


        val mapView = MapsFragment().view

        if (mapView?.findViewById<View>("1".toInt()) != null) {
            val locationButton = (mapView.findViewById<View>("1".toInt())
                .parent as View).findViewById<View>("2".toInt())
            val layoutParams = locationButton.layoutParams as RelativeLayout.LayoutParams
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
            layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
            layoutParams.setMargins(0, 180, 40, 0)
        }

        //check if gps is enabled or not and then request user to enable it

        //check if gps is enabled or not and then request user to enable it
        val locationRequest = LocationRequest.create()
        locationRequest.interval = (1000 * DEFAULT_INTERVAL).toLong()
        locationRequest.fastestInterval =
            (1000 * FASTEST_INTERVAL).toLong()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val task = settingsClient.checkLocationSettings(builder.build())
        //if gps enabled this will call

        //if gps enabled this will call
        task.addOnSuccessListener(requireActivity(), OnSuccessListener<LocationSettingsResponse?> {
            Log.d(TAG, "onSuccess: gps is enabled")
            getDeviceLocation()
        })


        //if gps is not enabled than this will call

        //if gps is not enabled than this will call
        task.addOnFailureListener(requireActivity(), OnFailureListener { e ->
            Log.d(TAG, "onFailure: gps is not enabled")
            if (e is ResolvableApiException) {
                val resolvableApiException = e as ResolvableApiException
                try {
                    resolvableApiException.startResolutionForResult(requireActivity(), 51)
                } catch (e1: SendIntentException) {
                    e1.printStackTrace()
                }
            }
        })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 51) {
            if (resultCode == Activity.RESULT_OK) {
                //find user current location
                Toast.makeText(requireContext(), "Location eanbled", Toast.LENGTH_LONG)
                    .show()
                //  finish();
                getDeviceLocation()
            } else {
                Toast.makeText(requireContext(), "Location is not eanbled", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }

    private fun getDeviceLocation() {
        fusedLocationProviderClient!!.lastLocation.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                lastLocation = task.result
                if (lastLocation != null) {
                    mGoogleMap.moveCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                lastLocation!!.latitude,
                                lastLocation!!.longitude
                            ), DEFAULT_ZOOM
                        )
                    )

                    place1 = MarkerOptions().position(
                        LatLng(
                            lastLocation!!.latitude,
                            lastLocation!!.longitude
                        )
                    )
                    Log.d(
                        TAG,
                        "ONSUCCESS: latitude: " + lastLocation!!.getLatitude() + "  longitude: " + lastLocation!!.getLongitude()
                    )
                } else {
                    val locationRequest = LocationRequest.create()
                    locationRequest.interval =
                        (1000 * DEFAULT_INTERVAL).toLong()
                    locationRequest.fastestInterval =
                        (1000 * FASTEST_INTERVAL).toLong()
                    locationRequest.priority =
                        LocationRequest.PRIORITY_HIGH_ACCURACY
                    locationCallback = object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult) {
                            super.onLocationResult(locationResult)

                            lastLocation = locationResult.lastLocation
                            mGoogleMap.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    LatLng(
                                        lastLocation!!.getLatitude(),
                                        lastLocation!!.getLongitude()

                                    ), DEFAULT_ZOOM
                                )
                            )
                            Log.d(
                                TAG,
                                "ONSUCCESS: latitude: " + lastLocation!!.getLatitude() + "  longitude: " + lastLocation!!.getLongitude()
                            )
                            fusedLocationProviderClient!!.removeLocationUpdates(locationCallback!!)
                        }
                    }

                    fusedLocationProviderClient!!.requestLocationUpdates(
                        locationRequest,
                        locationCallback as LocationCallback,
                        Looper.myLooper()!!
                    )
                }
            } else {
                Toast.makeText(requireContext(), "unable to fetch location", Toast.LENGTH_LONG)
                    .show()
            }
        }
    }


    fun createMarker(
        latitude: Double,
        longitude: Double,
        title: String?,
//        snippet: String?,
//        iconResID: Int
    ): Marker? {

        return mGoogleMap.addMarker(
            MarkerOptions()
                .position(LatLng(latitude, longitude))
                .anchor(0.5f, 0.5f)
                .title(title)

//                .snippet(snippet)
                .icon(BitmapFromVector(requireContext(), R.drawable.ic_flag))
        )
    }

    private fun BitmapFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        // below line is use to generate a drawable.
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)

        // below line is use to set bounds to our vector drawable.
        vectorDrawable!!.setBounds(
            0,
            0,
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight
        )

        // below line is use to create a bitmap for our
        // drawable which we have added.
        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )

        // below line is use to add bitmap in our canvas.
        val canvas = Canvas(bitmap)

        // below line is use to draw our
        // vector drawable in canvas.
        vectorDrawable.draw(canvas)

        // after generating our bitmap we are returning our bitmap.
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }


    private fun getData() {

        val childEventListener = object : ChildEventListener {

            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val holeData = dataSnapshot.getValue<HoleData>()!!
                Log.d(TAG, "onChildAdded: $holeData")

                val marker = createMarker(holeData.lat, holeData.lng, holeData.id)
                markers.add(marker!!)
                // ...
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val newHole = dataSnapshot.getValue<HoleData>()!!

                Log.d(TAG, "onChildChanged: ${newHole.id}")
                markers.forEach {
                    if (newHole.id == it.title) {
                        it.remove()

                    }
                }

                val marker = createMarker(newHole.lat, newHole.lng, newHole.id)
                markers.add(marker!!)


                // ...
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                Log.d(TAG, "onChildRemoved:" + dataSnapshot.key!!)
                val removedHole = dataSnapshot.getValue<HoleData>()!!

                markers.forEach {
                    if (removedHole.id == it.title) {
                        it.remove()
                    }
                }

                // A comment has changed, use the key to determine if we are displaying this
                // comment and if so remove it.
                val commentKey = dataSnapshot.key
                Log.d(TAG, "onChildRemoved: $commentKey")

                // ...
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
                Log.d(TAG, "onChildMoved:" + dataSnapshot.key!!)

                // A comment has changed position, use the key to determine if we are
                // displaying this comment and if so move it.
                val movedComment = dataSnapshot.getValue<HoleData>()
                val commentKey = dataSnapshot.key
                Log.d(TAG, "onChildMoved: $commentKey")

                // ...
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w(TAG, "postComments:onCancelled", databaseError.toException())
                Toast.makeText(
                    context, "Failed to load comments.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        myRef.addChildEventListener(childEventListener)

    }


    fun addMarkerOnClick() {
        mGoogleMap.setOnMapClickListener {


            if (addedMarkers.size > 0) {

//                val lastMarker =  mGoogleMap.addMarker(MarkerOptions().position(addedMarkers[0]))
                Log.d(TAG, "addMarkerOnClick: ${lastMarker.remove()}")

                addedMarkers.remove(
                    LatLng(
                        lastMarker.position.latitude,
                        lastMarker.position.longitude
                    )
                )

            }

            lastMarker = mGoogleMap.addMarker(MarkerOptions().position(it))!!
            place2 = MarkerOptions().position(it)
            addedMarkers.add(it)



        }
    }



    fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0
        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat
            shift = 0
            result = 0
            do {
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng
            val latLng = LatLng((lat.toDouble() / 1E5),(lng.toDouble() / 1E5))
            poly.add(latLng)
        }
        return poly
    }


    private fun getDirectionURL(origin:LatLng, dest:LatLng, secret: String) : String{
        return "https://maps.googleapis.com/maps/api/directions/json?origin=${origin.latitude},${origin.longitude}" +
                "&destination=${dest.latitude},${dest.longitude}" +
                "&sensor=false" +
                "&mode=driving" +
                "&key=$secret"
    }

    @SuppressLint("StaticFieldLeak")
    private inner class GetDirection(val url : String) : AsyncTask<Void, Void, List<List<LatLng>>>(){
        override fun doInBackground(vararg params: Void?): List<List<LatLng>> {

            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()
            val data = response.body!!.string()

            val result = ArrayList<List<LatLng>>()
            try{
                val respObj = Gson().fromJson(data,MapData::class.java)
                val path = ArrayList<LatLng>()
                for (i in 0 until respObj.routes[0].legs[0].steps.size){
                    path.addAll(decodePolyline(respObj.routes[0].legs[0].steps[i].polyline.points))
                }
                result.add(path)
            }catch (e:Exception){
                e.printStackTrace()
            }
            return result
        }

        override fun onPostExecute(result: List<List<LatLng>>) {
            val lineoption = PolylineOptions()
            for (i in result.indices){
                lineoption.addAll(result[i])
                lineoption.width(10f)
                lineoption.color(Color.GREEN)
                lineoption.geodesic(true)
            }
            mGoogleMap.addPolyline(lineoption)
        }
    }

}