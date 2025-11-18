package com.example.final_mobile;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.final_mobile.models.Branch;
import com.example.final_mobile.services.BranchService;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.util.ArrayList;
import java.util.List;

public class MapsFragment extends Fragment {

    private static final String TAG = "MapsFragment";
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

    private MapView mapView;
    private IMapController mapController;
    private MyLocationNewOverlay myLocationOverlay;
    private FusedLocationProviderClient fusedLocationClient;
    private BranchService branchService;
    
    private TextView tvFragmentLabel;
    private ProgressBar progressBar;
    private TextView tvError;
    private CardView nearestBranchCard;
    private TextView tvNearestName;
    private TextView tvNearestAddress;
    private TextView tvNearestDistance;
    private MaterialButton btnNavigate;
    private FloatingActionButton fabMyLocation;
    
    private Location userLocation;
    private List<Branch> branches = new ArrayList<>();
    private Branch nearestBranch;
    private List<Marker> branchMarkers = new ArrayList<>();
    private Marker userMarker;
    private Polyline routePolyline;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Configure OSMDroid
        Configuration.getInstance().load(getContext(), 
            getContext().getSharedPreferences("osmdroid", 0));
        Configuration.getInstance().setUserAgentValue("BankingApp/1.0");
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_maps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        branchService = new BranchService(getContext());
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());
        
        initViews(view);
        setupMap();
        checkLocationPermission();
    }

    private void initViews(View view) {
        tvFragmentLabel = view.findViewById(R.id.tv_fragment_label);
        progressBar = view.findViewById(R.id.progress_bar);
        tvError = view.findViewById(R.id.tv_error);
        nearestBranchCard = view.findViewById(R.id.nearest_branch_card);
        tvNearestName = view.findViewById(R.id.tv_nearest_name);
        tvNearestAddress = view.findViewById(R.id.tv_nearest_address);
        tvNearestDistance = view.findViewById(R.id.tv_nearest_distance);
        btnNavigate = view.findViewById(R.id.btn_navigate);
        fabMyLocation = view.findViewById(R.id.fab_my_location);
        mapView = view.findViewById(R.id.map);
        
        tvFragmentLabel.setText("Chi nhánh ngân hàng");
        
        btnNavigate.setOnClickListener(v -> {
            if (nearestBranch != null) {
                navigateToBranch(nearestBranch);
            }
        });
        
        fabMyLocation.setOnClickListener(v -> {
            if (userLocation != null) {
                moveCameraToLocation(new GeoPoint(userLocation.getLatitude(), userLocation.getLongitude()));
            } else {
                getCurrentLocation();
            }
        });
    }

    private void setupMap() {
        if (mapView == null) return;
        
        // Set tile source
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        
        // Enable zoom controls
        mapView.setMultiTouchControls(true);
        mapView.getZoomController().setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.SHOW_AND_FADEOUT);
        
        // Get map controller
        mapController = mapView.getController();
        mapController.setZoom(12.0);
        
        // Setup my location overlay
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            setupMyLocationOverlay();
        }
        
        // Load branches
        loadBranches();
    }

    private void setupMyLocationOverlay() {
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(getContext()), mapView);
        myLocationOverlay.enableMyLocation();
        mapView.getOverlays().add(myLocationOverlay);
    }

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(requireActivity(),
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        } else {
            getCurrentLocation();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                setupMyLocationOverlay();
                getCurrentLocation();
            } else {
                // Permission denied
                Toast.makeText(getContext(), "Cần quyền truy cập vị trí để tìm chi nhánh gần nhất", 
                        Toast.LENGTH_LONG).show();
                // Load branches without location
                loadBranches();
            }
        }
    }

    private void getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        try {
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(requireActivity(), location -> {
                        if (location != null) {
                            userLocation = location;
                            GeoPoint userGeoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                            
                            // Add or update user marker
                            if (userMarker != null) {
                                mapView.getOverlays().remove(userMarker);
                            }
                            userMarker = new Marker(mapView);
                            userMarker.setPosition(userGeoPoint);
                            userMarker.setTitle("Vị trí của bạn");
                            if (ContextCompat.getDrawable(getContext(), android.R.drawable.ic_menu_mylocation) != null) {
                                userMarker.setIcon(ContextCompat.getDrawable(getContext(), android.R.drawable.ic_menu_mylocation));
                            }
                            userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                            mapView.getOverlays().add(userMarker);
                            
                            // Move camera to user location
                            moveCameraToLocation(userGeoPoint);
                            
                            // Get nearest branch
                            getNearestBranch(location.getLatitude(), location.getLongitude());
                        } else {
                            Log.w(TAG, "Location is null");
                            // Load branches without location
                            loadBranches();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Error getting location", e);
                        Toast.makeText(getContext(), "Không thể lấy vị trí hiện tại", Toast.LENGTH_SHORT).show();
                        // Load branches without location
                        loadBranches();
                    });
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception getting location", e);
        }
    }

    private void loadBranches() {
        showLoading(true);
        branchService.getBranches(new BranchService.BranchCallback() {
            @Override
            public void onSuccess(List<Branch> branchList) {
                branches = branchList;
                displayBranchesOnMap();
                showLoading(false);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading branches: " + error);
                showError("Lỗi tải danh sách chi nhánh: " + error);
                showLoading(false);
            }
        });
    }

    private void getNearestBranch(double latitude, double longitude) {
        branchService.getNearestBranch(latitude, longitude, new BranchService.NearestBranchCallback() {
            @Override
            public void onSuccess(Branch nearest, List<Branch> allBranches) {
                nearestBranch = nearest;
                branches = allBranches;
                
                // Update map with branches (including distances)
                displayBranchesOnMap();
                
                // Show nearest branch card
                showNearestBranchCard(nearest);
                
                // Draw route to nearest branch
                if (userLocation != null) {
                    drawRouteToNearestBranch();
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error getting nearest branch: " + error);
                // Still show branches without nearest info
                displayBranchesOnMap();
            }
        });
    }

    private void displayBranchesOnMap() {
        if (mapView == null) return;
        
        // Clear existing markers
        for (Marker marker : branchMarkers) {
            mapView.getOverlays().remove(marker);
        }
        branchMarkers.clear();
        
        // Add markers for each branch
        for (Branch branch : branches) {
            GeoPoint branchLocation = new GeoPoint(branch.getLatitude(), branch.getLongitude());
            
            String title = branch.getName();
            String snippet = branch.getAddress();
            if (branch.getDistanceText() != null && !branch.getDistanceText().isEmpty()) {
                snippet += "\nKhoảng cách: " + branch.getDistanceText();
            }
            
            Marker marker = new Marker(mapView);
            marker.setPosition(branchLocation);
            marker.setTitle(title);
            marker.setSnippet(snippet);
            
            // Use different icon for nearest branch
            // Note: OSMDroid markers use default icon, you can customize with custom drawable
            if (branch.equals(nearestBranch)) {
                // Green marker for nearest - use default but can be customized
                marker.setIcon(ContextCompat.getDrawable(getContext(), android.R.drawable.ic_menu_mylocation));
            } else {
                // Default marker for others
                // OSMDroid will use default marker icon
            }
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            
            mapView.getOverlays().add(marker);
            branchMarkers.add(marker);
        }
        
        // Adjust camera to show all branches
        if (!branches.isEmpty()) {
            if (userLocation != null) {
                // Show user location and branches
                GeoPoint userGeoPoint = new GeoPoint(userLocation.getLatitude(), userLocation.getLongitude());
                mapController.setCenter(userGeoPoint);
                mapController.setZoom(12.0);
            } else {
                // Show first branch
                Branch firstBranch = branches.get(0);
                GeoPoint firstLocation = new GeoPoint(firstBranch.getLatitude(), firstBranch.getLongitude());
                mapController.setCenter(firstLocation);
                mapController.setZoom(10.0);
            }
        }
        
        mapView.invalidate();
    }

    private void showNearestBranchCard(Branch branch) {
        if (branch == null) {
            nearestBranchCard.setVisibility(View.GONE);
            return;
        }
        
        tvNearestName.setText(branch.getName());
        tvNearestAddress.setText(branch.getAddress());
        if (branch.getDistanceText() != null && !branch.getDistanceText().isEmpty()) {
            tvNearestDistance.setText("Khoảng cách: " + branch.getDistanceText());
        } else {
            tvNearestDistance.setText("");
        }
        
        nearestBranchCard.setVisibility(View.VISIBLE);
    }

    private void drawRouteToNearestBranch() {
        if (mapView == null || userLocation == null || nearestBranch == null) {
            return;
        }
        
        // Clear existing route
        if (routePolyline != null) {
            mapView.getOverlays().remove(routePolyline);
        }
        
        // Create route polyline (simple straight line for now)
        // In production, you would use a routing service for actual route
        GeoPoint userGeoPoint = new GeoPoint(userLocation.getLatitude(), userLocation.getLongitude());
        GeoPoint branchGeoPoint = new GeoPoint(nearestBranch.getLatitude(), nearestBranch.getLongitude());
        
        routePolyline = new Polyline();
        routePolyline.addPoint(userGeoPoint);
        routePolyline.addPoint(branchGeoPoint);
        routePolyline.setColor(0xFF2196F3); // Blue color
        routePolyline.setWidth(8.0f);
        
        mapView.getOverlays().add(routePolyline);
        
        // Move camera to show route
        mapController.setCenter(userGeoPoint);
        mapController.setZoom(13.0);
        
        mapView.invalidate();
    }

    private void navigateToBranch(Branch branch) {
        if (branch == null) return;
        
        // Create intent to open navigation
        // Try Google Maps first, fallback to web browser
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + branch.getLatitude() + "," + branch.getLongitude());
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        
        if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
            startActivity(mapIntent);
        } else {
            // Fallback to web browser with OpenStreetMap
            Uri webUri = Uri.parse("https://www.openstreetmap.org/directions?to=" 
                    + branch.getLatitude() + "," + branch.getLongitude());
            Intent webIntent = new Intent(Intent.ACTION_VIEW, webUri);
            startActivity(webIntent);
        }
    }

    private void moveCameraToLocation(GeoPoint location) {
        if (mapController != null) {
            mapController.animateTo(location);
            mapController.setZoom(15.0);
            if (mapView != null) {
                mapView.invalidate();
            }
        }
    }

    private void showLoading(boolean show) {
        if (progressBar != null) {
            progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showError(String error) {
        if (tvError != null) {
            tvError.setText(error);
            tvError.setVisibility(View.VISIBLE);
        }
        Toast.makeText(getContext(), error, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clean up
        if (routePolyline != null && mapView != null) {
            mapView.getOverlays().remove(routePolyline);
        }
        for (Marker marker : branchMarkers) {
            if (mapView != null) {
                mapView.getOverlays().remove(marker);
            }
        }
        if (userMarker != null && mapView != null) {
            mapView.getOverlays().remove(userMarker);
        }
        if (myLocationOverlay != null && mapView != null) {
            mapView.getOverlays().remove(myLocationOverlay);
        }
    }
}
