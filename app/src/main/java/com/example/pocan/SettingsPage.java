package com.example.pocan;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.CheckBox;
import android.widget.EditText;

public class SettingsPage extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_page);

        CheckBox C=findViewById(R.id.box_Iw);
        C.setChecked(MainActivity.OverrideIb);

        C=findViewById(R.id.box_K);
        C.setChecked(MainActivity.OverrideK);

        EditText E=findViewById(R.id.value_override_Iw);
        E.setText(String.format("%.02f",MainActivity.OverridenIb));

        E=findViewById(R.id.value_override_K);
        E.setText(String.format("%.02f",MainActivity.OverridenK));

        E=findViewById(R.id.smooth_value);
        E.setText(String.format("%d",MainActivity.SmoothDegree));

        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
       // getSupportActionBar().setIcon(R.drawable.);

    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        onBackPressed();
        return true;
    }
    @Override
    public void onBackPressed(){
        CheckBox C=findViewById(R.id.box_Iw);
        MainActivity.OverrideIb = C.isChecked();
        C=findViewById(R.id.box_K);
        MainActivity.OverrideK=C.isChecked();
        EditText E=findViewById(R.id.value_override_Iw);
        try {
            String s=E.getText().toString();
            s = s.replace(",",".");
            MainActivity.OverridenIb = Double.parseDouble(s);
        }catch (Exception e){

        }
        E=findViewById(R.id.value_override_K);
        try {
            String s=E.getText().toString();
            s = s.replace(",",".");
            MainActivity.OverridenK = Double.parseDouble(s);
        }catch (Exception e){

        }

        E=findViewById(R.id.smooth_value);
        try {
            String s=E.getText().toString();
            s = s.replace(",",".");
            MainActivity.SmoothDegree = (int)Double.parseDouble(s);
        }catch (Exception e){

        }
        MainActivity.SettingsChanged=true;
        MainActivity.SaveSettings();
        this.finish();
    }
}
