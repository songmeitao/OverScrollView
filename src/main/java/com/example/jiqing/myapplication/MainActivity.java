package com.example.jiqing.myapplication;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private LinearLayout ll;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ll = (LinearLayout) findViewById(R.id.ll);

        for (int i=0;i<ll.getChildCount();i++){
            View view = ll.getChildAt(i);
            setOnListner(view,i);
        }
    }

    public void setOnListner(View v,final int position){
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,"位置："+position,Toast.LENGTH_SHORT).show();
            }
        });
    }
}
