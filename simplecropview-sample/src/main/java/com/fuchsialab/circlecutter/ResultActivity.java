package com.fuchsialab.circlecutter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.AdapterStatus;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.isseiaoki.simplecropview.util.Utils;

import java.util.Map;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import static com.google.firebase.database.FirebaseDatabase.getInstance;

public class ResultActivity extends AppCompatActivity {

  private ImageView mImageView;
  private ExecutorService mExecutor;

  FirebaseAuth mAuth;
  DatabaseReference mDatabase;
  private String bannerid;
  private AdView mAdView;

  private String interstitialId;
  private static InterstitialAd mInterstitialAd;


  public static Intent createIntent(Activity activity, Uri uri) {
    Intent intent = new Intent(activity, ResultActivity.class);
    intent.setData(uri);
    return intent;
  }

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_result);

    mAuth=FirebaseAuth.getInstance();
    mDatabase= FirebaseDatabase.getInstance().getReference();

    bannerAds();

    MobileAds.initialize(this, new OnInitializationCompleteListener() {
      @Override
      public void onInitializationComplete(InitializationStatus initializationStatus) {
        Map<String, AdapterStatus> statusMap = initializationStatus.getAdapterStatusMap();
        for (String adapterClass : statusMap.keySet()) {
          AdapterStatus status = statusMap.get(adapterClass);
          Log.d("MyApp", String.format(
                  "Adapter name: %s, Description: %s, Latency: %d",
                  adapterClass, status.getDescription(), status.getLatency()));
        }

        // Start loading ads here...
      }
    });

      // apply custom font
    FontUtils.setFont((ViewGroup) findViewById(R.id.layout_root));

    initToolbar();


    Toast.makeText(ResultActivity.this,"Image Saved Successfully",Toast.LENGTH_SHORT).show();
    mImageView = (ImageView) findViewById(R.id.result_image);
    mExecutor = Executors.newSingleThreadExecutor();

    final Uri uri = getIntent().getData();
    mExecutor.submit(new LoadScaledImageTask(this, uri, mImageView, calcImageSize()));
  }



  @Override public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
  }

  @Override public boolean onSupportNavigateUp() {
    onBackPressed();
    return super.onSupportNavigateUp();
  }

  private void initToolbar() {
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    ActionBar actionBar = getSupportActionBar();
    FontUtils.setTitle(actionBar, "Circle Cutter");
    actionBar.setDisplayHomeAsUpEnabled(true);
    actionBar.setHomeButtonEnabled(true);
  }

  private int calcImageSize() {
    DisplayMetrics metrics = new DisplayMetrics();
    Display display = getWindowManager().getDefaultDisplay();
    display.getMetrics(metrics);
    return Math.min(Math.max(metrics.widthPixels, metrics.heightPixels), 2048);
  }

  public static class LoadScaledImageTask implements Runnable {
    private Handler mHandler = new Handler(Looper.getMainLooper());
    Context context;
    Uri uri;
    ImageView imageView;
    int width;

    public LoadScaledImageTask(Context context, Uri uri, ImageView imageView, int width) {
      this.context = context;
      this.uri = uri;
      this.imageView = imageView;
      this.width = width;
    }

    @Override public void run() {
      final int exifRotation = Utils.getExifOrientation(context, uri);
      int maxSize = Utils.getMaxSize();
      int requestSize = Math.min(width, maxSize);
      try {
        final Bitmap sampledBitmap = Utils.decodeSampledBitmapFromUri(context, uri, requestSize);
        mHandler.post(new Runnable() {
          @Override public void run() {
            imageView.setImageMatrix(Utils.getMatrixFromExifOrientation(exifRotation));
            imageView.setImageBitmap(sampledBitmap);
          }
        });
      } catch (OutOfMemoryError e) {
        e.printStackTrace();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  public void bannerAds(){
    DatabaseReference rootref= getInstance().getReference().child("AdUnits");
    rootref.addListenerForSingleValueEvent(new ValueEventListener() {


      @Override
      public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        bannerid=String.valueOf(Objects.requireNonNull(dataSnapshot.child("banner").getValue()).toString());
        interstitialId=String.valueOf(Objects.requireNonNull(dataSnapshot.child("Interstitial").getValue()).toString());

        View view= findViewById(R.id.adView);
        mAdView=new AdView(ResultActivity.this);
        ((RelativeLayout)view).addView(mAdView);
        mAdView.setAdSize(AdSize.BANNER);
        mAdView.setAdUnitId(bannerid);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        //MediationTestSuite.launch(ResultActivity.this);

        InterstitialAd.load(ResultActivity.this,interstitialId, adRequest, new InterstitialAdLoadCallback() {
          @Override
          public void onAdLoaded(@androidx.annotation.NonNull InterstitialAd interstitialAd) {

            mInterstitialAd = interstitialAd;

            mInterstitialAd.show(ResultActivity.this);

          }

          @Override
          public void onAdFailedToLoad(@androidx.annotation.NonNull LoadAdError loadAdError) {

            mInterstitialAd = null;

          }
        });


      }

      @Override
      public void onCancelled(@NonNull DatabaseError databaseError) {

      }
    });


  }


}
