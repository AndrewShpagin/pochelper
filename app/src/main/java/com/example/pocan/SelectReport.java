package com.example.pocan;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Timer;

public class SelectReport extends AppCompatActivity {

    public void GoHome(View view){
        ((TextView) view).setBackgroundColor(Color.parseColor("#D0D0D0"));
        Context context = getApplicationContext();
        Toast.makeText(context, ((TextView) view).getText(), Toast.LENGTH_SHORT).show();
        finish();
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_report);
        String path0="/storage/emulated/0/PocData/";
        File directory = new File(path0);
        File[] files = directory.listFiles();

        LinearLayout VL = findViewById(R.id.file_list);

        TextView T=new TextView(this);
        T.setText(R.string.auto_file);
        T.setTextSize(24.0f);
        //final SelectReport R=this;
        T.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainActivity.ReportToOpen="";
                MainActivity.LastOpenReport="";
                MainActivity.LastFile="";
                GoHome(view);
            }
        });
        VL.addView(T);
        for (int i = 0; i < files.length; i++) {
            File myfile = files[i];
            String path = myfile.toString();
            if (path.contains(".txt")) {
                TextView T1=new TextView(this);
                path = path.replace(path0,"");
                T1.setText(path);
                T1.setTextSize(20.0f);
                final String res=path.toString();
                T1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        MainActivity.ReportToOpen=res;
                        GoHome(view);
                    }
                });
                VL.addView(T1);
            }
        }
    }
}
