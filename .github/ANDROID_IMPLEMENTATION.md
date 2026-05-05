# Android OTP Authentication Implementation Guide

## Overview
This guide provides step-by-step instructions to implement the new OTP-based authentication in your Android app.

**Status:** ✅ UI Layout Updated (`fragment_login.xml`)  
**Next Steps:** Implement Java/Kotlin logic in Fragment/Activity

---

## 1. Update Your Gradle Dependencies

Edit `app/build.gradle` and add the following dependencies:

```gradle
dependencies {
    // Existing dependencies...
    
    // Retrofit for HTTP calls
    implementation 'com.squareup.retrofit2:retrofit:2.9.0'
    implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
    
    // OkHttp for HTTP client with interceptors
    implementation 'com.squareup.okhttp3:okhttp:4.9.0'
    
    // Encrypted SharedPreferences
    implementation 'androidx.security:security-crypto:1.1.0-alpha06'
    
    // Material Components (should already exist)
    implementation 'com.google.android.material:material:1.9.0'
}
```

Then sync Gradle.

---

## 2. Create Data Model Classes (DTOs)

### `OtpRequest.java`
```java
package com.fuelqueue.model;

public class OtpRequest {
    private String phoneNumber;

    public OtpRequest() {}

    public OtpRequest(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
}
```

### `OtpResponse.java`
```java
package com.fuelqueue.model;

public class OtpResponse {
    private String message;
    private String phoneNumber;
    private String otp;  // For testing only - remove in production

    public String getMessage() {
        return message;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public String getOtp() {
        return otp;
    }
}
```

### `OtpVerifyRequest.java`
```java
package com.fuelqueue.model;

public class OtpVerifyRequest {
    private String phoneNumber;
    private String otp;
    private String name;  // Optional - for registration

    public OtpVerifyRequest() {}

    public OtpVerifyRequest(String phoneNumber, String otp, String name) {
        this.phoneNumber = phoneNumber;
        this.otp = otp;
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
```

### `LoginResponse.java`
```java
package com.fuelqueue.model;

public class LoginResponse {
    private String token;
    private Long userId;
    private String name;
    private String phoneNumber;

    public String getToken() {
        return token;
    }

    public Long getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
```

---

## 3. Create the Retrofit API Service

### `AuthService.java`
```java
package com.fuelqueue.service;

import com.fuelqueue.model.OtpRequest;
import com.fuelqueue.model.OtpResponse;
import com.fuelqueue.model.OtpVerifyRequest;
import com.fuelqueue.model.LoginResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface AuthService {
    
    @POST("auth/send-otp")
    Call<OtpResponse> sendOtp(@Body OtpRequest request);

    @POST("auth/verify-otp")
    Call<LoginResponse> verifyOtp(@Body OtpVerifyRequest request);
}
```

---

## 4. Create the Auth Interceptor

### `AuthInterceptor.java`
```java
package com.fuelqueue.util;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import java.io.IOException;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public class AuthInterceptor implements Interceptor {
    private Context context;

    public AuthInterceptor(Context context) {
        this.context = context;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request originalRequest = chain.request();
        
        // Get stored JWT token
        String token = getToken();

        Request.Builder requestBuilder = originalRequest.newBuilder();
        
        // Add Authorization header if token exists
        if (token != null && !token.isEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer " + token);
        }

        Request newRequest = requestBuilder.build();
        return chain.proceed(newRequest);
    }

    private String getToken() {
        try {
            SharedPreferences prefs = context.getSharedPreferences(
                    "fuel_queue_auth", 
                    Context.MODE_PRIVATE
            );
            return prefs.getString("auth_token", null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
```

---

## 5. Create Shared Preferences Manager

### `AuthPrefManager.java`
```java
package com.fuelqueue.util;

import android.content.Context;
import android.content.SharedPreferences;

public class AuthPrefManager {
    private static final String PREF_NAME = "fuel_queue_auth";
    private static final String KEY_TOKEN = "auth_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_PHONE_NUMBER = "phone_number";

    private SharedPreferences sharedPreferences;

    public AuthPrefManager(Context context) {
        this.sharedPreferences = context.getSharedPreferences(
                PREF_NAME, 
                Context.MODE_PRIVATE
        );
    }

    public void saveToken(String token) {
        sharedPreferences.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return sharedPreferences.getString(KEY_TOKEN, null);
    }

    public void saveUserId(Long userId) {
        sharedPreferences.edit().putLong(KEY_USER_ID, userId).apply();
    }

    public Long getUserId() {
        return sharedPreferences.getLong(KEY_USER_ID, -1L);
    }

    public void saveUserName(String name) {
        sharedPreferences.edit().putString(KEY_USER_NAME, name).apply();
    }

    public String getUserName() {
        return sharedPreferences.getString(KEY_USER_NAME, null);
    }

    public void savePhoneNumber(String phoneNumber) {
        sharedPreferences.edit().putString(KEY_PHONE_NUMBER, phoneNumber).apply();
    }

    public String getPhoneNumber() {
        return sharedPreferences.getString(KEY_PHONE_NUMBER, null);
    }

    public void clearAll() {
        sharedPreferences.edit().clear().apply();
    }

    public boolean isLoggedIn() {
        return getToken() != null && !getToken().isEmpty();
    }
}
```

---

## 6. Create Retrofit Client

### `RetrofitClient.java`
```java
package com.fuelqueue.util;

import android.content.Context;
import com.fuelqueue.service.AuthService;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = "http://your-server-url/api/";
    private static Retrofit retrofit;
    private static OkHttpClient okHttpClient;

    public static Retrofit getRetrofitInstance(Context context) {
        if (retrofit == null) {
            okHttpClient = new OkHttpClient.Builder()
                    .addInterceptor(new AuthInterceptor(context))
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    public static AuthService getAuthService(Context context) {
        return getRetrofitInstance(context).create(AuthService.class);
    }
}
```

---

## 7. Implement LoginFragment with OTP Flow

### `LoginFragment.java`
```java
package com.fuelqueue.ui.auth;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import com.fuelqueue.R;
import com.fuelqueue.model.LoginResponse;
import com.fuelqueue.model.OtpRequest;
import com.fuelqueue.model.OtpResponse;
import com.fuelqueue.model.OtpVerifyRequest;
import com.fuelqueue.service.AuthService;
import com.fuelqueue.util.AuthPrefManager;
import com.fuelqueue.util.RetrofitClient;
import com.google.android.material.button.MaterialButton;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginFragment extends Fragment {

    private EditText etPhoneNumber, etOtp;
    private MaterialButton btnSendOtp, btnVerifyOtp, btnResendOtp;
    private LinearLayout otpSection;
    private TextView tvOtpTimer;
    private ProgressBar progressBar;
    private AuthService authService;
    private AuthPrefManager prefManager;
    private String currentPhoneNumber;
    private CountDownTimer countDownTimer;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        initializeViews(view);
        setupRetrofit();
        setupListeners();
    }

    private void initializeViews(View view) {
        etPhoneNumber = view.findViewById(R.id.et_phone_number);
        etOtp = view.findViewById(R.id.et_otp);
        btnSendOtp = view.findViewById(R.id.btn_send_otp);
        btnVerifyOtp = view.findViewById(R.id.btn_verify_otp);
        btnResendOtp = view.findViewById(R.id.btn_resend_otp);
        otpSection = view.findViewById(R.id.otp_section);
        tvOtpTimer = view.findViewById(R.id.tv_otp_timer);
        progressBar = view.findViewById(R.id.progress_bar);
        
        prefManager = new AuthPrefManager(requireContext());
    }

    private void setupRetrofit() {
        authService = RetrofitClient.getAuthService(requireContext());
    }

    private void setupListeners() {
        btnSendOtp.setOnClickListener(v -> handleSendOtp());
        btnVerifyOtp.setOnClickListener(v -> handleVerifyOtp());
        btnResendOtp.setOnClickListener(v -> handleSendOtp());
    }

    private void handleSendOtp() {
        String phoneNumber = etPhoneNumber.getText().toString().trim();
        
        // Validation
        if (phoneNumber.isEmpty()) {
            Toast.makeText(getContext(), "Please enter phone number", Toast.LENGTH_SHORT).show();
            return;
        }

        if (phoneNumber.length() != 10) {
            Toast.makeText(getContext(), "Phone number must be 10 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        // Add country code (default to India +91)
        if (!phoneNumber.startsWith("+")) {
            phoneNumber = "+91" + phoneNumber;
        }

        currentPhoneNumber = phoneNumber;
        prefManager.savePhoneNumber(phoneNumber);

        // Make API call
        showProgress(true);
        OtpRequest request = new OtpRequest(phoneNumber);
        
        authService.sendOtp(request).enqueue(new Callback<OtpResponse>() {
            @Override
            public void onResponse(Call<OtpResponse> call, Response<OtpResponse> response) {
                showProgress(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    Toast.makeText(getContext(), 
                            "OTP sent to " + phoneNumber, 
                            Toast.LENGTH_SHORT).show();
                    
                    // Show OTP input section
                    showOtpSection(true);
                    etPhoneNumber.setEnabled(false);
                    btnSendOtp.setEnabled(false);
                    
                    // Start countdown timer
                    startOtpTimer();
                } else {
                    Toast.makeText(getContext(), 
                            "Failed to send OTP: " + response.message(), 
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<OtpResponse> call, Throwable t) {
                showProgress(false);
                Toast.makeText(getContext(), 
                        "Error: " + t.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleVerifyOtp() {
        String otp = etOtp.getText().toString().trim();
        
        if (otp.isEmpty()) {
            Toast.makeText(getContext(), "Please enter OTP", Toast.LENGTH_SHORT).show();
            return;
        }

        if (otp.length() != 6) {
            Toast.makeText(getContext(), "OTP must be 6 digits", Toast.LENGTH_SHORT).show();
            return;
        }

        showProgress(true);
        OtpVerifyRequest request = new OtpVerifyRequest(currentPhoneNumber, otp, null);
        
        authService.verifyOtp(request).enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                showProgress(false);
                
                if (response.isSuccessful() && response.body() != null) {
                    LoginResponse loginResponse = response.body();
                    
                    // Save auth data
                    prefManager.saveToken(loginResponse.getToken());
                    prefManager.saveUserId(loginResponse.getUserId());
                    prefManager.saveUserName(loginResponse.getName());
                    
                    Toast.makeText(getContext(), "Login successful!", Toast.LENGTH_SHORT).show();
                    
                    // Cancel timer
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                    }
                    
                    // Navigate to home screen
                    Navigation.findNavController(getView()).navigate(R.id.action_loginFragment_to_homeFragment);
                } else {
                    Toast.makeText(getContext(), 
                            "Invalid or expired OTP", 
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                showProgress(false);
                Toast.makeText(getContext(), 
                        "Error: " + t.getMessage(), 
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void startOtpTimer() {
        final long TEN_MINUTES = 10 * 60 * 1000;  // 10 minutes in ms
        
        countDownTimer = new CountDownTimer(TEN_MINUTES, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long minutes = millisUntilFinished / 60000;
                long seconds = (millisUntilFinished % 60000) / 1000;
                tvOtpTimer.setText(String.format("OTP expires in %d:%02d", minutes, seconds));
            }

            @Override
            public void onFinish() {
                tvOtpTimer.setText("OTP expired");
                resetOtpFlow();
                Toast.makeText(getContext(), "OTP expired. Please request a new one.", Toast.LENGTH_SHORT).show();
            }
        }.start();
    }

    private void resetOtpFlow() {
        showOtpSection(false);
        etPhoneNumber.setEnabled(true);
        btnSendOtp.setEnabled(true);
        etPhoneNumber.setText("");
        etOtp.setText("");
    }

    private void showOtpSection(boolean show) {
        otpSection.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showProgress(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        btnSendOtp.setEnabled(!show);
        btnVerifyOtp.setEnabled(!show);
        btnResendOtp.setEnabled(!show);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }
}
```

---

## 8. Add to Fragment Navigation

Update your `nav_graph.xml` with the home fragment destination:

```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/nav_graph"
    app:startDestination="@id/loginFragment">

    <fragment
        android:id="@+id/loginFragment"
        android:name="com.fuelqueue.ui.auth.LoginFragment"
        android:label="Login" >
        <action
            android:id="@+id/action_loginFragment_to_homeFragment"
            app:destination="@id/homeFragment"
            app:popUpTo="@id/loginFragment"
            app:popUpToInclusive="true" />
    </fragment>

    <fragment
        android:id="@+id/homeFragment"
        android:name="com.fuelqueue.ui.home.HomeFragment"
        android:label="Home" />

</navigation>
```

---

## 9. Add Permissions to AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
```

---

## 10. Interceptor Usage in All API Calls

The `AuthInterceptor` automatically adds the JWT token to all requests. Just use the `AuthService`:

```java
// All requests automatically include Authorization header
authService.sendOtp(request).enqueue(callback);
authService.verifyOtp(request).enqueue(callback);
```

---

## 11. Update Base URL

**IMPORTANT:** Update the BASE_URL in `RetrofitClient.java` to match your backend server:

```java
private static final String BASE_URL = "http://your-server-url/api/";
// Example: http://192.168.1.100:8080/api/
// Example: https://api.fuelqueue.com/api/
```

---

## 12. Testing the Flow

### Test Scenario 1: New User Registration
1. Enter phone number: `9876543210` (or any 10-digit number)
2. Click "Send OTP" button
3. Check backend logs for generated OTP (e.g., `654321`)
4. Enter OTP in the app
5. Click "Verify OTP"
6. Should navigate to home screen if successful

### Test Scenario 2: Returning User
1. Enter same phone number: `9876543210`
2. Click "Send OTP"
3. Enter new OTP
4. Click "Verify OTP"
5. Should get JWT token and navigate to home

### Test Scenario 3: Invalid OTP
1. Enter any OTP other than the one sent
2. Click "Verify OTP"
3. Should show error message: "Invalid or expired OTP"

### Test Scenario 4: Expired OTP
1. Send OTP
2. Wait 10 minutes (or modify timer for testing)
3. Try to verify
4. Should show error: "OTP expired"

---

## 13. Checklist

- [ ] Added Gradle dependencies
- [ ] Created all DTO classes (OtpRequest, OtpResponse, OtpVerifyRequest, LoginResponse)
- [ ] Created AuthService (Retrofit interface)
- [ ] Created AuthInterceptor
- [ ] Created AuthPrefManager (SharedPreferences wrapper)
- [ ] Created RetrofitClient
- [ ] Implemented LoginFragment with OTP flow
- [ ] Updated fragment_login.xml (✅ Already done)
- [ ] Added permissions to AndroidManifest.xml
- [ ] Updated navigation graph
- [ ] Changed BASE_URL in RetrofitClient
- [ ] Tested complete OTP flow

---

## 14. Troubleshooting

### Issue: "Failed to connect to server"
**Solution:** Check if backend is running and update BASE_URL in RetrofitClient

### Issue: "OTP not received"
**Solution:** SMS gateway not integrated yet. Check backend logs for generated OTP

### Issue: "Invalid OTP"
**Solution:** Ensure you're entering exactly the OTP displayed in backend logs

### Issue: "Token not being sent with requests"
**Solution:** Verify AuthInterceptor is properly configured in OkHttpClient

### Issue: "Navigation not working"
**Solution:** Check nav_graph.xml for correct fragment names and action IDs

---

## 15. Next Steps

1. ✅ Complete OTP flow implementation
2. ⏳ Integrate SMS gateway (Twilio / AWS SNS)
3. ⏳ Add phone number validation
4. ⏳ Implement logout functionality
5. ⏳ Add biometric authentication
6. ⏳ Setup Firebase Cloud Messaging (FCM)
7. ⏳ Add rate limiting on client side
8. ⏳ Implement token refresh mechanism

---

## File Structure

Your Android project should look like:

```
app/src/main/
├── java/com/fuelqueue/
│   ├── ui/
│   │   ├── auth/
│   │   │   └── LoginFragment.java ✅ UPDATED
│   │   └── home/
│   │       └── HomeFragment.java
│   ├── service/
│   │   └── AuthService.java ✅ NEW
│   ├── model/
│   │   ├── OtpRequest.java ✅ NEW
│   │   ├── OtpResponse.java ✅ NEW
│   │   ├── OtpVerifyRequest.java ✅ NEW
│   │   └── LoginResponse.java ✅ NEW
│   ├── util/
│   │   ├── AuthInterceptor.java ✅ NEW
│   │   ├── AuthPrefManager.java ✅ NEW
│   │   └── RetrofitClient.java ✅ NEW
│   └── MainActivity.java
├── res/
│   ├── layout/
│   │   └── fragment_login.xml ✅ UPDATED
│   └── navigation/
│       └── nav_graph.xml ✅ UPDATE NEEDED
└── AndroidManifest.xml ✅ UPDATE PERMISSIONS
```

---

## Support

For more details, refer to:
- Backend Architecture: `ARCHITECTURE.md`
- Update Log: `UPDATE_LOG.md`
- Backend API Docs: See AuthController in backend

