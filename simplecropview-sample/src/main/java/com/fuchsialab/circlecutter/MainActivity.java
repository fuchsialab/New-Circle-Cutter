package com.fuchsialab.circlecutter;


import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;

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

  Button button;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    drawerLayout= findViewById(R.id.root_layout);
    navigationView= findViewById(R.id.navdrawer);
    toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.Open, R.string.Close);
    drawerLayout.addDrawerListener(toggle);
    toggle.syncState();
    toggle.getDrawerArrowDrawable().setColor(getResources().getColor(R.color.abba));

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

        Intent intent = new Intent(MainActivity.this,RxActivity.class);
        startActivity(intent);
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
            } catch (android.content.ActivityNotFoundException anfe) {
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

            Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
            sharingIntent.setType("text/plain");
            String shareBody = "Download Circle Cutter App and Crop your picture in circle shape .  https://play.google.com/store/apps/details?id=com.fuchsialab.circlecutter";
            sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Circle Cutter");
            sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
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


}
