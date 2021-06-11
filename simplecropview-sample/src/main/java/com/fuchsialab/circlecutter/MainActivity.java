package com.fuchsialab.circlecutter;


import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.ads.Ad;
import com.facebook.ads.AdError;
import com.facebook.ads.AdOptionsView;
import com.facebook.ads.AdView;
import com.facebook.ads.InterstitialAd;
import com.facebook.ads.InterstitialAdListener;
import com.facebook.ads.MediaView;
import com.facebook.ads.NativeAd;
import com.facebook.ads.NativeAdLayout;
import com.facebook.ads.NativeAdListener;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import hotchemi.android.rate.AppRate;
import hotchemi.android.rate.OnClickButtonListener;

public class MainActivity extends AppCompatActivity implements UpdateHelper.OnUpdateCheckListener {

  NavigationView navigationView;
  ActionBarDrawerToggle toggle;
  DrawerLayout drawerLayout;
  Toolbar toolbar;

  private long backPressTime;

  FirebaseAuth mAuth;
  DatabaseReference mDatabase;

  private final String TAG = MainActivity.class.getSimpleName();
  private NativeAd nativeAd;
  private InterstitialAd interstitialAd;
  private String nativeId;
  private String interstitialId;

  private NativeAdLayout nativeAdLayout;
  private LinearLayout adView;

  Button button;
  ProgressDialog progressDialog;
  Timer timer;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mAuth=FirebaseAuth.getInstance();
    mDatabase= FirebaseDatabase.getInstance().getReference();

    Ads();

    toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    drawerLayout= findViewById(R.id.root_layout);
    navigationView= findViewById(R.id.navdrawer);
    toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.Open, R.string.Close);
    drawerLayout.addDrawerListener(toggle);
    toggle.syncState();
    toggle.getDrawerArrowDrawable().setColor(getResources().getColor(R.color.abba));


    progressDialog =new ProgressDialog(MainActivity.this);
    progressDialog.show();
    progressDialog.setContentView(R.layout.progress);
    Objects.requireNonNull(progressDialog.getWindow()).setBackgroundDrawableResource(android.R.color.transparent);

    timer=new Timer();
    timer.schedule(new TimerTask() {
      @Override
      public void run() {
        progressDialog.dismiss();

      }
    },3000);

    UpdateHelper.with(this)
            .onUpdateCheck(this)
            .check();

    AppRate.with(this)
            .setInstallDays(0)
            .setLaunchTimes(5)
            .setRemindInterval(10)
            .setShowLaterButton(true)
            .setDebug(false)
            .setOnClickButtonListener(new OnClickButtonListener() {
              @Override
              public void onClickButton(int which) {
                Log.d(MainActivity.class.getName(), Integer.toString(which));
              }
            })
            .monitor();

    AppRate.showRateDialogIfMeetsConditions(this);


    button= findViewById(R.id.rx_sample_button);

    button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {

        if (interstitialAd != null) {
          if (interstitialAd.isAdLoaded()) {
            interstitialAd.show();

          } else {
            interstitialAd.loadAd();

            Intent intent = new Intent(MainActivity.this,RxActivity.class);
            startActivity(intent);
          }

          InterstitialAdListener interstitialAdListener = new InterstitialAdListener() {
            @Override
            public void onInterstitialDisplayed(Ad ad) {
              interstitialAd.loadAd();
            }

            @Override
            public void onInterstitialDismissed(Ad ad) {
              interstitialAd.loadAd();

              Intent intent = new Intent(MainActivity.this,RxActivity.class);
              startActivity(intent);

            }

            @Override
            public void onError(Ad ad, AdError adError) {
              interstitialAd.loadAd();
            }

            @Override
            public void onAdLoaded(Ad ad) {

              Log.d(TAG, "Interstitial ad is loaded and ready to be displayed!");

            }

            @Override
            public void onAdClicked(Ad ad) {

              Log.d(TAG, "Interstitial ad clicked!");
            }

            @Override
            public void onLoggingImpression(Ad ad) {

              Log.d(TAG, "Interstitial ad impression logged!");
            }
          };

          interstitialAd.loadAd(
                  interstitialAd.buildLoadAdConfig()
                          .withAdListener(interstitialAdListener)
                          .build());

        } else {

          Intent intent = new Intent(MainActivity.this,RxActivity.class);
          startActivity(intent);
        }

      }
    });

    // apply custom font
    FontUtils.setFont(findViewById(R.id.root_layout));


    navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
      @Override
      public boolean onNavigationItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()) {
          case R.id.menuHome:

            drawerLayout.closeDrawer(GravityCompat.START);
            return true;

          case R.id.menuprivacy:
            Intent browse =new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.lkmkm)));
            startActivity(browse);
            drawerLayout.closeDrawer(GravityCompat.START);

            return true;

          case R.id.menurate:
            drawerLayout.closeDrawer(GravityCompat.START);
            final String appPackageName = getPackageName();
            try {
              startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
            } catch (ActivityNotFoundException anfe) {
              startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
            }

            return true;

          case R.id.menuwhatsapp:

            drawerLayout.closeDrawer(GravityCompat.START);
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.fuchsia.saver")));


            return true;

          case R.id.menumoreapp:
            Intent browses = new Intent(Intent.ACTION_VIEW, Uri.parse(("https://play.google.com/store/apps/collection/cluster?clp=igM4ChkKEzUzNjIwODY3OTExNjgyNTA2MTkQCBgDEhkKEzUzNjIwODY3OTExNjgyNTA2MTkQCBgDGAA%3D:S:ANO1ljJMw2s&gsr=CjuKAzgKGQoTNTM2MjA4Njc5MTE2ODI1MDYxORAIGAMSGQoTNTM2MjA4Njc5MTE2ODI1MDYxORAIGAMYAA%3D%3D:S:ANO1ljI3U6g")));
            startActivity(browses);
            drawerLayout.closeDrawer(GravityCompat.START);

            return true;

          case R.id.menushare:

            Intent sharingIntent = new Intent(Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            String shareBody = "Download Circle Cutter App and Crop your picture in circle shape .  https://play.google.com/store/apps/details?id=com.fuchsialab.circlecutter";
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Circle Cutter");
            sharingIntent.putExtra(Intent.EXTRA_TEXT, shareBody);
            startActivity(Intent.createChooser(sharingIntent, "Share via"));
            drawerLayout.closeDrawers();

            return true;

          case R.id.menuexit:

            finishAffinity();

            return true;
        }
        return false;


      }
    });

  }


  @Override
  public void onBackPressed() {


    if (backPressTime+2000>System.currentTimeMillis()){
      super.onBackPressed();
      return;
    }else {
      Toast.makeText(getBaseContext(),"Press back again to exit",Toast.LENGTH_SHORT).show();
    }

    backPressTime= System.currentTimeMillis();
  }

  @Override
  public void onUpdateCheckListener(String urlApp) {

    AlertDialog alertDialog=new AlertDialog.Builder(this,R.style.MyDialogTheme)
            .setTitle("New Version Available")
            .setMessage(" Please update for better experience")
            .setPositiveButton("UPDATE", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialogInterface, int i) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.jjjj))));

              }
            }).setNegativeButton("NOT NOW", new DialogInterface.OnClickListener() {
              @Override
              public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
              }
            }).create();
    alertDialog.show();

  }


  @Override
  public boolean onOptionsItemSelected(MenuItem item, Intent data) {
    return false;
  }


  private void Ads() {

    DatabaseReference rootref = FirebaseDatabase.getInstance().getReference().child("FbAds");
    rootref.addListenerForSingleValueEvent(new ValueEventListener() {

      @Override
      public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
        nativeId = String.valueOf(Objects.requireNonNull(dataSnapshot.child("nativeId").getValue()).toString());
        nativeAd = new NativeAd(MainActivity.this, nativeId);
        interstitialId = String.valueOf(Objects.requireNonNull(dataSnapshot.child("Interstitial").getValue()).toString());
        interstitialAd = new com.facebook.ads.InterstitialAd(MainActivity.this, interstitialId);

        loadNativeAd();
        interstitialAd.loadAd();

      }

      @Override
      public void onCancelled(@NonNull DatabaseError databaseError) {

      }
    });

  }

  @Override
  public void onDestroy() {
   if(interstitialAd != null) {
      interstitialAd.destroy();
    }
    super.onDestroy();
  }

  private void loadNativeAd() {


    NativeAdListener nativeAdListener = new NativeAdListener() {
      @Override
      public void onMediaDownloaded(Ad ad) {

      }

      @Override
      public void onError(Ad ad, AdError adError) {

      }

      @Override
      public void onAdLoaded(Ad ad) {
        // Race condition, load() called again before last ad was displayed
        if (nativeAd == null || nativeAd != ad) {
          return;
        }
        // Inflate Native Ad into Container
        inflateAd(nativeAd);
      }

      @Override
      public void onAdClicked(Ad ad) {

      }

      @Override
      public void onLoggingImpression(Ad ad) {

      }

    };

    nativeAd.loadAd(
            nativeAd.buildLoadAdConfig()
                    .withAdListener(nativeAdListener)
                    .build());
  }

  private void inflateAd(NativeAd nativeAd) {

    nativeAd.unregisterView();

    // Add the Ad view into the ad container.
    nativeAdLayout = findViewById(R.id.native_ad_container);
    LayoutInflater inflater = LayoutInflater.from(MainActivity.this);
    // Inflate the Ad view.  The layout referenced should be the one you created in the last step.
    adView = (LinearLayout) inflater.inflate(R.layout.custom_native_ad_layout, nativeAdLayout, false);
    nativeAdLayout.addView(adView);

    // Add the AdOptionsView
    LinearLayout adChoicesContainer = findViewById(R.id.ad_choices_container);
    AdOptionsView adOptionsView = new AdOptionsView(MainActivity.this, nativeAd, nativeAdLayout);
    adChoicesContainer.removeAllViews();
    adChoicesContainer.addView(adOptionsView, 0);

    // Create native UI using the ad metadata.
    MediaView nativeAdIcon = adView.findViewById(R.id.native_ad_icon);
    TextView nativeAdTitle = adView.findViewById(R.id.native_ad_title);
    MediaView nativeAdMedia = adView.findViewById(R.id.native_ad_media);
    TextView nativeAdSocialContext = adView.findViewById(R.id.native_ad_social_context);
    TextView nativeAdBody = adView.findViewById(R.id.native_ad_body);
    TextView sponsoredLabel = adView.findViewById(R.id.native_ad_sponsored_label);
    Button nativeAdCallToAction = adView.findViewById(R.id.native_ad_call_to_action);

    // Set the Text.
    nativeAdTitle.setText(nativeAd.getAdvertiserName());
    nativeAdBody.setText(nativeAd.getAdBodyText());
    nativeAdSocialContext.setText(nativeAd.getAdSocialContext());
    nativeAdCallToAction.setVisibility(nativeAd.hasCallToAction() ? View.VISIBLE : View.INVISIBLE);
    nativeAdCallToAction.setText(nativeAd.getAdCallToAction());
    sponsoredLabel.setText(nativeAd.getSponsoredTranslation());

    // Create a list of clickable views
    List<View> clickableViews = new ArrayList<>();
    clickableViews.add(nativeAdTitle);
    clickableViews.add(nativeAdCallToAction);

    // Register the Title and CTA button to listen for clicks.
    nativeAd.registerViewForInteraction(
            adView, nativeAdMedia, nativeAdIcon, clickableViews);
  }
}
