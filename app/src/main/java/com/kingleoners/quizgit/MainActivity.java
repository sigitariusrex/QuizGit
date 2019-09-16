package com.kingleoners.quizgit;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;

import com.kingleoners.quizgit.Adapter.CategoryAdapter;
import com.kingleoners.quizgit.Common.SpaceDecoration;
import com.kingleoners.quizgit.DBHelper.DBHelper;

public class MainActivity extends AppCompatActivity {

    Toolbar toolbar;
    RecyclerView recycler_category;

    MediaPlayer audio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        audio = MediaPlayer.create(this, R.raw.sound);
        audio.setLooping(true);
        audio.start();

        toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setTitle("QuizGit");
        setSupportActionBar(toolbar);

        recycler_category = (RecyclerView)findViewById(R.id.recycler_category);
        recycler_category.setHasFixedSize(true);
        recycler_category.setLayoutManager(new GridLayoutManager(this,2));

        //Get screen height
        //DisplayMetrics displayMetrics = new DisplayMetrics();
        //getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        //int height = displayMetrics.heightPixels / 8; //Max size of item in category
        CategoryAdapter adapter = new CategoryAdapter(MainActivity.this, DBHelper.getInstance(this).getAllCategories());
        int spaceInPixel = 4;
        recycler_category.addItemDecoration(new SpaceDecoration(spaceInPixel));
        recycler_category.setAdapter(adapter);
    }
}
