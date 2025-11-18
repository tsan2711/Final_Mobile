package com.example.final_mobile.services;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import com.example.final_mobile.models.Branch;

public class BranchService {
    private static final String TAG = "BranchService";
    private ApiService apiService;
    private SessionManager sessionManager;
    private Context context;

    public BranchService(Context context) {
        this.context = context;
        this.apiService = ApiService.getInstance(context);
        this.sessionManager = SessionManager.getInstance(context);
    }

    public interface BranchCallback {
        void onSuccess(List<Branch> branches);
        void onError(String error);
    }

    public interface NearestBranchCallback {
        void onSuccess(Branch nearestBranch, List<Branch> allBranches);
        void onError(String error);
    }

    // Get all branches
    public void getBranches(BranchCallback callback) {
        apiService.get(ApiConfig.GET_BRANCHES, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    if (response.getBoolean("success")) {
                        JSONArray branchesArray = response.getJSONArray("data");
                        List<Branch> branches = parseBranches(branchesArray);
                        callback.onSuccess(branches);
                    } else {
                        callback.onError(response.optString("message", "Failed to get branches"));
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing branches response", e);
                    callback.onError("Error parsing response: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error, int statusCode) {
                Log.e(TAG, "Error getting branches: " + error);
                callback.onError(error);
            }
        });
    }

    // Get nearest branch based on user location
    public void getNearestBranch(double latitude, double longitude, NearestBranchCallback callback) {
        String endpoint = ApiConfig.GET_NEAREST_BRANCH + "?latitude=" + latitude + "&longitude=" + longitude;
        
        apiService.get(endpoint, new ApiService.ApiCallback() {
            @Override
            public void onSuccess(JSONObject response) {
                try {
                    if (response.getBoolean("success")) {
                        JSONObject data = response.getJSONObject("data");
                        
                        // Parse nearest branch
                        JSONObject nearestJson = data.getJSONObject("nearest");
                        Branch nearestBranch = parseBranch(nearestJson);
                        
                        // Parse all branches with distances
                        JSONArray allBranchesArray = data.getJSONArray("allBranches");
                        List<Branch> allBranches = parseBranches(allBranchesArray);
                        
                        callback.onSuccess(nearestBranch, allBranches);
                    } else {
                        callback.onError(response.optString("message", "Failed to find nearest branch"));
                    }
                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing nearest branch response", e);
                    callback.onError("Error parsing response: " + e.getMessage());
                }
            }

            @Override
            public void onError(String error, int statusCode) {
                Log.e(TAG, "Error getting nearest branch: " + error);
                callback.onError(error);
            }
        });
    }

    // Parse single branch from JSON
    private Branch parseBranch(JSONObject branchJson) throws JSONException {
        Branch branch = new Branch();
        branch.setId(branchJson.optString("id", ""));
        branch.setName(branchJson.optString("name", ""));
        branch.setAddress(branchJson.optString("address", ""));
        branch.setPhone(branchJson.optString("phone", ""));
        branch.setLatitude(branchJson.optDouble("latitude", 0.0));
        branch.setLongitude(branchJson.optDouble("longitude", 0.0));
        branch.setOpeningHours(branchJson.optString("openingHours", ""));
        
        // Parse services array
        if (branchJson.has("services")) {
            JSONArray servicesArray = branchJson.getJSONArray("services");
            List<String> services = new ArrayList<>();
            for (int i = 0; i < servicesArray.length(); i++) {
                services.add(servicesArray.getString(i));
            }
            branch.setServices(services);
        }
        
        // Parse distance if available
        if (branchJson.has("distance")) {
            branch.setDistance(branchJson.optDouble("distance", 0.0));
        }
        if (branchJson.has("distanceText")) {
            branch.setDistanceText(branchJson.optString("distanceText", ""));
        }
        
        return branch;
    }

    // Parse branches array from JSON
    private List<Branch> parseBranches(JSONArray branchesArray) throws JSONException {
        List<Branch> branches = new ArrayList<>();
        for (int i = 0; i < branchesArray.length(); i++) {
            JSONObject branchJson = branchesArray.getJSONObject(i);
            Branch branch = parseBranch(branchJson);
            branches.add(branch);
        }
        return branches;
    }
}

