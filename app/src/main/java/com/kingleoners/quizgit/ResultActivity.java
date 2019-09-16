package com.kingleoners.quizgit;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;
import com.kingleoners.quizgit.Adapter.ResultGridAdapter;
import com.kingleoners.quizgit.Common.Common;
import com.kingleoners.quizgit.Common.SpaceDecoration;

import java.util.concurrent.TimeUnit;

public class ResultActivity extends AppCompatActivity {

    Toolbar toolbar;
    TextView txt_timer, txt_result, txt_right_answer;
    Button btn_filter_total, btn_filter_right, btn_filter_wrong, btn_filter_no_answer;
    RecyclerView recycler_result;

    ResultGridAdapter adapter, filtered_adapter;

    BroadcastReceiver backToQuestion = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if (intent.getAction().toString().equals(Common.KEY_BACK_FROM_RESULT))
            {
                int question = intent.getIntExtra(Common.KEY_BACK_FROM_RESULT,-1);
                goBackActivityWithQuestion(question);
            }
        }
    };

    private void goBackActivityWithQuestion(int question) {

        Intent returnIntent = new Intent();
        returnIntent.putExtra(Common.KEY_BACK_FROM_RESULT,question);
        setResult(Activity.RESULT_OK,returnIntent);
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(backToQuestion, new IntentFilter(Common.KEY_BACK_FROM_RESULT));

        toolbar = (Toolbar)findViewById(R.id.toolbar);
        toolbar.setTitle("HASIL");
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        txt_result = (TextView)findViewById(R.id.txt_result);
        txt_right_answer = (TextView)findViewById(R.id.txt_right_answer);
        txt_timer = (TextView)findViewById(R.id.txt_time);

        btn_filter_no_answer = (Button)findViewById(R.id.btn_filter_no_answer);
        btn_filter_right = (Button)findViewById(R.id.btn_filter_right_answer);
        btn_filter_wrong = (Button)findViewById(R.id.btn_filter_wrong_answer);
        btn_filter_total = (Button)findViewById(R.id.btn_filter_total);

        recycler_result = (RecyclerView)findViewById(R.id.recycler_result);
        recycler_result.setHasFixedSize(true);
        recycler_result.setLayoutManager(new GridLayoutManager(this, 3));

        //Set adapter
        adapter = new ResultGridAdapter(this,Common.answerSheetList);
        recycler_result.addItemDecoration(new SpaceDecoration(4));
        recycler_result.setAdapter(adapter);

        txt_timer.setText(String.format("%02d:%02d",
                TimeUnit.MILLISECONDS.toMinutes(Common.timer),
                TimeUnit.MILLISECONDS.toSeconds(Common.timer)-
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(Common.timer))));

        txt_right_answer.setText(new StringBuilder("").append(Common.right_answer_count).append("/")
        .append(Common.questionList.size()));

        btn_filter_total.setText(new StringBuilder("").append(Common.questionList.size()));
        btn_filter_right.setText(new StringBuilder("").append(Common.right_answer_count));
        btn_filter_wrong.setText(new StringBuilder("").append(Common.wrong_answer_count));
        btn_filter_no_answer.setText(new StringBuilder("").append(Common.no_answer_count));

        //Calculate result
        int percent = (Common.right_answer_count * 100 / Common.questionList.size());
        if (percent > 80)
            txt_result.setText("Kamu Cerdas");
        else if (percent > 70)
            txt_result.setText("Kamu Pintar");
        else if (percent > 60)
            txt_result.setText("Kamu Lumayan");
        else if (percent > 50)
            txt_result.setText("Kamu Buruk");
        else
            txt_result.setText("Kamu Gagal");

        //Event filter
        btn_filter_total.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (adapter == null)
                {
                    adapter = new ResultGridAdapter(ResultActivity.this, Common.answerSheetList);
                    recycler_result.setAdapter(adapter);
                }
                else
                    recycler_result.setAdapter(adapter);
            }
        });

        btn_filter_no_answer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Common.answerSheetListFiltered.clear();
                for (int i=0; i<Common.answerSheetList.size(); i++)
                {
                    if (Common.answerSheetList.get(i).getType() == Common.ANSWER_TYPE.NO_ANSWER)
                        Common.answerSheetListFiltered.add(Common.answerSheetList.get(i));
                }

                filtered_adapter = new ResultGridAdapter(ResultActivity.this, Common.answerSheetListFiltered);
                recycler_result.setAdapter(filtered_adapter);
            }
        });

        btn_filter_wrong.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Common.answerSheetListFiltered.clear();
                for (int i=0; i<Common.answerSheetList.size(); i++)
                {
                    if (Common.answerSheetList.get(i).getType() == Common.ANSWER_TYPE.WRONG_ANSWER)
                        Common.answerSheetListFiltered.add(Common.answerSheetList.get(i));
                }

                filtered_adapter = new ResultGridAdapter(ResultActivity.this, Common.answerSheetListFiltered);
                recycler_result.setAdapter(filtered_adapter);
            }
        });

        btn_filter_right.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Common.answerSheetListFiltered.clear();
                for (int i=0; i<Common.answerSheetList.size(); i++)
                {
                    if (Common.answerSheetList.get(i).getType() == Common.ANSWER_TYPE.RIGHT_ANSWER)
                        Common.answerSheetListFiltered.add(Common.answerSheetList.get(i));
                }

                filtered_adapter = new ResultGridAdapter(ResultActivity.this, Common.answerSheetListFiltered);
                recycler_result.setAdapter(filtered_adapter);
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.result_menu,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId())
        {
            case R.id.menu_do_quiz_again:
                doQuizAgain();
                break;
            case R.id.menu_view_answer:
                viewQuizAnswer();
                break;
            case android.R.id.home:
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //Delete all activity
                startActivity(intent);
                break;
        }
        return true;
    }

    private void viewQuizAnswer() {

        Intent returnIntent = new Intent();
        returnIntent.putExtra("aksi", "lihatjawabanquiz");
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    private void doQuizAgain() {

        new MaterialStyledDialog.Builder(ResultActivity.this)
                .setTitle("Lakukan Quiz lagi ?")
                .setIcon(R.drawable.ic_mood_white_24dp)
                .setDescription("Kamu serius ingin menghapus data ini ?")
                .setNegativeText("Tidak")
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveText("Ya")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        dialog.dismiss();

                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("aksi", "lakukanlagi");
                        setResult(Activity.RESULT_OK, returnIntent);
                        finish();
                    }
                })
                .show();
    }
}
