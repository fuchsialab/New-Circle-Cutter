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
import android.view.Display;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdView;
import com.facebook.ads.AudienceNetworkAds;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdListener;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.isseiaoki.simplecropview.util.Utils;

import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class ResultActivity extends AppCompatActivity {
  private static final String TAG = ResultActivity.class.getSimpleName();
  private static InterstitialAd interstitialAd;
  private ImageView mImageView;
  private ExecutorService mExecutor;

  FirebaseAuth mAuth;
  DatabaseReference mDatabase;
  private String bannerid;
  RelativeLayout layout;
  private AdView adView;


  private String interstitialId;


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
    AudienceNetworkAds.initialize(this);

    Ads();


      // apply custom font
    FontUtils.setFont((ViewGroup) findViewById(R.id.layout_root));

    initToolbar();

    Toast.makeText(ResultActivity.this,"Image Saved Successfully",Toast.LENGTH_SHORT).show();
    mImageView = (ImageView) findViewById(R.id.result_image);
    mExecutor = Executors.newSingleThreadExecutor();

    final Uri uri = getIntent().getData();
    mExecutor.submit(new LoadScaledImageTask(this, uri, mImageView, calcImageSize()));
  }

  @Override protected void onDestroy() {
    mExecutor.shutdown();
    if (adView != null) {
      adView.destroy();
    }else if(interstitialAd != null) {
      interstitialAd.destroy();
    }
    super.onDestroy();
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

  private void Ads() {

    DatabaseReference rootref = FirebaseDatabase.getInstance().getReference().child("FbAds");
    rootref.addListenerForSingleValueEvent(new ValueEventListener() {


      @Override
      public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        bannerid = String.valueOf(Objects.requireNonNull(dataSnapshot.child("banner").getValue()).toString());
        layout = findViewById(R.id.adView);
        adView = new com.facebook.ads.AdView(ResultActivity.this, bannerid, com.facebook.ads.AdSize.BANNER_HEIGHT_50);
        layout.addView(adView);
        adView.loadAd();

        interstitialId = String.valueOf(Objects.requireNonNull(dataSnapshot.child("Interstitial").getValue()).toString());
        interstitialAd = new com.facebook.ads.InterstitialAd(ResultActivity.this, interstitialId);

        interstitialAd.loadAd();

        InterstitialAdListener interstitialAdListener = new InterstitialAdListener() {
          @Override
          public void onInterstitialDisplayed(Ad ad) {
            interstitialAd.loadAd();
          }

          @Override
          public void onInterstitialDismissed(Ad ad) {

          }

          @Override
          public void onError(Ad ad, AdError adError) {
            interstitialAd.loadAd();
          }

          @Override
          public void onAdLoaded(Ad ad) {

            interstitialAd.show();
          }

          @Override
          public void onAdClicked(Ad ad) {

          }

          @Override
          public void onLoggingImpression(Ad ad) {

          }
        };

        interstitialAd.loadAd(
                interstitialAd.buildLoadAdConfig()
                        .withAdListener(interstitialAdListener)
                        .build());

      }


      @Override
      public void onCancelled(@NonNull DatabaseError databaseError) {

      }
    });

  }

}
