package com.example.mobileapp.presentation.ui.checkout

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.mobileapp.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import java.util.Locale

class LocationPickerFragment : Fragment(R.layout.fragment_location_picker) {

    private lateinit var mapView: MapView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tvDireccion: TextView
    private lateinit var progressBar: ProgressBar
    
    private var selectedGeoPoint: GeoPoint? = null
    private var selectedAddress: String = ""

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                enableMyLocation()
            }
            else -> {
                Toast.makeText(requireContext(), "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configurar OSMDroid
        Configuration.getInstance().load(
            requireContext(),
            PreferenceManager.getDefaultSharedPreferences(requireContext())
        )
        Configuration.getInstance().userAgentValue = requireContext().packageName
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        mapView = view.findViewById(R.id.mapView)
        tvDireccion = view.findViewById(R.id.tvDireccion)
        progressBar = view.findViewById(R.id.progressBar)
        val btnConfirmar = view.findViewById<MaterialButton>(R.id.btnConfirmar)
        val btnMiUbicacion = view.findViewById<MaterialButton>(R.id.btnMiUbicacion)

        toolbar.setNavigationOnClickListener {
            parentFragmentManager.popBackStack()
        }

        btnConfirmar.setOnClickListener {
            confirmarUbicacion()
        }

        btnMiUbicacion.setOnClickListener {
            requestLocationPermission()
        }

        setupMap()
    }

    private fun setupMap() {
        // Configurar el mapa
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        mapView.controller.setZoom(15.0)
        
        // Ubicación por defecto (Lima, Perú)
        val defaultLocation = GeoPoint(-12.0464, -77.0428)
        mapView.controller.setCenter(defaultLocation)
        
        // Listener para detectar cuando el mapa se mueve
        val mapEventsReceiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                return false
            }

            override fun longPressHelper(p: GeoPoint?): Boolean {
                return false
            }
        }
        
        val mapEventsOverlay = MapEventsOverlay(mapEventsReceiver)
        mapView.overlays.add(mapEventsOverlay)
        
        // Listener para cuando el usuario termina de mover el mapa
        mapView.addOnFirstLayoutListener { _, _, _, _, _ ->
            updateCenterLocation()
        }
        
        // Actualizar ubicación cuando el mapa se mueve
        mapView.setOnTouchListener { _, _ ->
            mapView.postDelayed({
                updateCenterLocation()
            }, 500)
            false
        }
        
        // Solicitar ubicación actual
        requestLocationPermission()
    }

    private fun updateCenterLocation() {
        val center = mapView.mapCenter as GeoPoint
        selectedGeoPoint = center
        getAddressFromGeoPoint(center)
    }

    private fun requestLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                enableMyLocation()
            }
            else -> {
                locationPermissionRequest.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun enableMyLocation() {
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val currentGeoPoint = GeoPoint(it.latitude, it.longitude)
                    mapView.controller.animateTo(currentGeoPoint)
                    mapView.controller.setZoom(16.0)
                    updateCenterLocation()
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(requireContext(), "Error al obtener ubicación", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getAddressFromGeoPoint(geoPoint: GeoPoint) {
        progressBar.visibility = View.VISIBLE
        
        lifecycleScope.launch {
            try {
                val address = withContext(Dispatchers.IO) {
                    val geocoder = Geocoder(requireContext(), Locale.getDefault())
                    val addresses = geocoder.getFromLocation(geoPoint.latitude, geoPoint.longitude, 1)
                    
                    if (!addresses.isNullOrEmpty()) {
                        addresses[0].getAddressLine(0) ?: "Dirección no disponible"
                    } else {
                        "No se pudo obtener la dirección"
                    }
                }
                
                selectedAddress = address
                tvDireccion.text = address
            } catch (e: Exception) {
                tvDireccion.text = "Error al obtener dirección: ${e.message}"
            } finally {
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun confirmarUbicacion() {
        if (selectedGeoPoint == null || selectedAddress.isEmpty()) {
            Toast.makeText(requireContext(), "Por favor selecciona una ubicación", Toast.LENGTH_SHORT).show()
            return
        }

        // Pasar los datos de vuelta al CheckoutFragment
        parentFragmentManager.setFragmentResult(
            "location_result",
            Bundle().apply {
                putDouble("latitude", selectedGeoPoint!!.latitude)
                putDouble("longitude", selectedGeoPoint!!.longitude)
                putString("address", selectedAddress)
            }
        )

        parentFragmentManager.popBackStack()
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        mapView.onDetach()
    }

    companion object {
        fun newInstance(): LocationPickerFragment {
            return LocationPickerFragment()
        }
    }
}
