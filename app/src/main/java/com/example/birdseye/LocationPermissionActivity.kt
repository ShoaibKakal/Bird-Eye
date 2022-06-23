package com.example.birdseye

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener

class LocationPermissionActivity : AppCompatActivity() {

    private lateinit var permission_btn:Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_permission)

        permission_btn = findViewById(R.id.permission_btn)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }




        permission_btn.setOnClickListener(View.OnClickListener {
            Dexter.withContext(this)
                .withPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                .withListener(object : PermissionListener {
                    override fun onPermissionGranted(permissionGrantedResponse: PermissionGrantedResponse) {
                        val intent = Intent(this@LocationPermissionActivity, MainActivity::class.java)
                        startActivity(intent)
                    }

                    override fun onPermissionDenied(permissionDeniedResponse: PermissionDeniedResponse) {
                        if (permissionDeniedResponse.isPermanentlyDenied) {
                            val builder = AlertDialog.Builder(baseContext)
                            builder.setTitle("Permission Denied")
                                .setMessage(
                                    """
                                        Permission to access device location is permanently denied.
                                        You need to go to settings to allow the permission
                                        """.trimIndent()
                                )
                                .setNegativeButton("Cancel", null)
                                .setPositiveButton(
                                    "OK"
                                ) { dialog, which ->
                                    val intent = Intent()
                                    intent.action =
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                    intent.data = Uri.fromParts("package", packageName, null)
                                }
                                .show()
                        } else {
                            Toast.makeText(
                                baseContext,
                                "Permission Denied",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissionRequest: PermissionRequest,
                        permissionToken: PermissionToken
                    ) {
                        permissionToken.continuePermissionRequest()
                    }
                })
                .check()
        })
    }
}