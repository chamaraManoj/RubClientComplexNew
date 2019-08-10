package com.example.rubclientcomplexnew;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private  BufferManager bufferManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bufferManager = new BufferManager();

    }

    public void send(View view) {
        //Log.d("Taggg", "1");
        bufferManager.startSendRequest();

    }
}
