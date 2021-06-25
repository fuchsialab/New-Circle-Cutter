package com.fuchsialab.circlecutter;

import android.content.Intent;
import android.os.Bundle;

import android.os.Handler;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

public class luncher extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_luncher);
        hideNavigationbar();

        new Handler().postDelayed(new Runnable() {
            @Override public void run() {
                Intent i = new Intent(luncher.this, MainActivity.class); startActivity(i);
                finish(); } }, 500);



    }

    private void hideNavigationbar(){
        this.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        |View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

        );
    }

}

