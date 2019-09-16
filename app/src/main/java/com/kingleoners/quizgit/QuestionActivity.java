package com.kingleoners.quizgit;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.github.javiersantos.materialstyleddialogs.MaterialStyledDialog;
import com.google.gson.Gson;
import com.kingleoners.quizgit.Adapter.AnswerSheetAdapter;
import com.kingleoners.quizgit.Adapter.AnswerSheetHelperAdapter;
import com.kingleoners.quizgit.Adapter.QuestionFragmentAdapter;
import com.kingleoners.quizgit.Common.Common;
import com.kingleoners.quizgit.Common.SpaceDecoration;
import com.kingleoners.quizgit.DBHelper.DBHelper;
import com.kingleoners.quizgit.Model.CurrentQuestion;

import java.util.concurrent.TimeUnit;

public class QuestionActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final int CODE_GET_RESULT = 9999;
    int time_play = Common.TOTAL_TIME;
    boolean isAnswerModeView = false;

    TextView txt_right_answer, txt_timer, txt_wrong_answer;

    RecyclerView answer_sheet_view, answer_sheet_helper;
    AnswerSheetAdapter answerSheetAdapter;
    AnswerSheetHelperAdapter answerSheetHelperAdapter;

    ViewPager viewPager;
    TabLayout tabLayout;

    DrawerLayout drawer;

    @Override
    protected void onDestroy() {

        if (Common.countDownTimer != null)
            Common.countDownTimer.cancel();
        super.onDestroy();
    }

    BroadcastReceiver gotoQuestionNum = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().toString().equals(Common.KEY_GO_TO_QUESTION))
            {
                int question = intent.getIntExtra(Common.KEY_GO_TO_QUESTION,-1);
                if (question != -1)
                    viewPager.setCurrentItem(question); //Go to question
                drawer.closeDrawer(Gravity.LEFT); //Close menu
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_question);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(Common.selectedCategory.getName());
        setSupportActionBar(toolbar);

        final DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View hView = navigationView.getHeaderView(0);
        answer_sheet_helper = (RecyclerView)hView.findViewById(R.id.answer_sheet_helper);
        answer_sheet_helper.setHasFixedSize(true);
        answer_sheet_helper.setLayoutManager(new GridLayoutManager(this,3));
        answer_sheet_helper.addItemDecoration(new SpaceDecoration(2));

        Button btn_done = (Button)hView.findViewById(R.id.btn_done);
        btn_done.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Here we will copy code on menu_finish_game
                if (!isAnswerModeView)
                {
                    new MaterialStyledDialog.Builder(QuestionActivity.this)
                            .setTitle("Selesai ?")
                            .setIcon(R.drawable.ic_mood_white_24dp)
                            .setDescription("Kamu serius ingin mengakhirinya ?")
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
                                    finishGame();
                                    drawer.closeDrawer(Gravity.LEFT);
                                }
                            })
                            .show();
                }
                else
                    finishGame();
            }
        });

        //First, we need take question from db
        takeQuestion();

        if (Common.questionList.size() > 0) {
            //Show text view right answer and text view timer
            txt_right_answer = (TextView)findViewById(R.id.txt_question_right);
            txt_timer = (TextView)findViewById(R.id.txt_timer);

            txt_right_answer.setVisibility(View.VISIBLE);
            txt_timer.setVisibility(View.VISIBLE);

            txt_right_answer.setText(new StringBuilder(String.format("%d/%d",Common.right_answer_count,Common.questionList.size())));

            countTimer();

            //View
            answer_sheet_view = (RecyclerView) findViewById(R.id.grid_answer);
            answer_sheet_view.setHasFixedSize(true);
            if (Common.questionList.size() > 5) //If question list have size > 5, we will separate 2 rows
                answer_sheet_view.setLayoutManager(new GridLayoutManager(this, Common.questionList.size() / 2));
            answerSheetAdapter = new AnswerSheetAdapter(this, Common.answerSheetList);
            answer_sheet_view.setAdapter(answerSheetAdapter);

            answerSheetHelperAdapter = new AnswerSheetHelperAdapter(this, Common.answerSheetList);
            answer_sheet_helper.setAdapter(answerSheetHelperAdapter);

            viewPager = (ViewPager)findViewById(R.id.viewpager);
            tabLayout = (TabLayout)findViewById(R.id.sliding_tabs);

            genFragmentList();

            QuestionFragmentAdapter questionFragmentAdapter = new QuestionFragmentAdapter(getSupportFragmentManager(),
                    this,
                    Common.fragmentsList);
            viewPager.setAdapter(questionFragmentAdapter);
            viewPager.setOffscreenPageLimit(Common.questionList.size()); //Fixed viewpager size

            tabLayout.setupWithViewPager(viewPager);

            //Event
            viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {

                int SCROLLING_RIGHT = 0;
                int SCROLLING_LEFT = 1;
                int SCROLLING_UNDETERMINED = 2;

                int currentScrollDirection = 2;

                private void setScrollingDirection(float positionOffset)
                {
                    if ((1-positionOffset) >= 0.5) {
                        this.currentScrollDirection = SCROLLING_RIGHT;
                    }
                    else if ((1-positionOffset) <= 0.5) {
                        this.currentScrollDirection = SCROLLING_LEFT;
                    }
                }

                private boolean isScrollDirectionUndetermined()
                {
                    return currentScrollDirection == SCROLLING_UNDETERMINED;
                }

                private boolean isScrollingRight()
                {
                    return currentScrollDirection == SCROLLING_RIGHT;
                }

                private boolean isScrollingLeft()
                {
                    return currentScrollDirection == SCROLLING_LEFT;
                }

                @Override
                public void onPageScrolled(int i, float v, int i1) {

                    if (isScrollDirectionUndetermined())
                        setScrollingDirection(v);
                }

                @Override
                public void onPageSelected(int i) {

                    QuestionFragment questionFragment;
                    int position = 0;
                    if (i > 0) {
                        if (isScrollingRight()) {
                            //If user scroll to right, get previous fragment to calculate result
                            questionFragment = Common.fragmentsList.get(i - 1);
                            position = i - 1;
                        } else if (isScrollingLeft()) {
                            //If user scroll to left, get next fragment to calculate result
                            questionFragment = Common.fragmentsList.get(i + 1);
                            position = i + 1;
                        } else {
                            questionFragment = Common.fragmentsList.get(position);
                        }
                    } else {
                        questionFragment = Common.fragmentsList.get(0);
                        position = 0;
                    }

                    //Optimize, only question have no answer just active this code
                    if (Common.answerSheetList.get(position).getType() == Common.ANSWER_TYPE.NO_ANSWER) {
                        //If you want to show correct answer, just call function here
                        CurrentQuestion question_state = questionFragment.getSelectedAnswer();
                        Common.answerSheetList.set(position, question_state); //Set question answer for answer sheet
                        answerSheetAdapter.notifyDataSetChanged(); //Change color in answer sheet
                        answerSheetHelperAdapter.notifyDataSetChanged();

                        countCorrectAnswer();

                        txt_right_answer.setText(new StringBuilder(String.format("%d", Common.right_answer_count))
                                .append("/")
                                .append(String.format("%d", Common.questionList.size())).toString());
                        txt_wrong_answer.setText(String.valueOf(Common.wrong_answer_count));

                        if (question_state.getType() != Common.ANSWER_TYPE.NO_ANSWER) {
                            questionFragment.showCorrectAnswer();
                            questionFragment.disableAnswer();
                        }
                    }
                }

                @Override
                public void onPageScrollStateChanged(int i) {

                    if (i == viewPager.SCROLL_STATE_IDLE)
                        this.currentScrollDirection = SCROLLING_UNDETERMINED;
                }
            });
        }
    }

    private void finishGame() {

        int position = viewPager.getCurrentItem();
        QuestionFragment questionFragment = Common.fragmentsList.get(position);

        if (Common.answerSheetList.get(position).getType() == Common.ANSWER_TYPE.NO_ANSWER) {
            //If you want to show correct answer, just call function here
            CurrentQuestion question_state = questionFragment.getSelectedAnswer();
            Common.answerSheetList.set(position, question_state); //Set question answer for answer sheet
            answerSheetAdapter.notifyDataSetChanged(); //Change color in answer sheet
            answerSheetHelperAdapter.notifyDataSetChanged();

            countCorrectAnswer();

            txt_right_answer.setText(new StringBuilder(String.format("%d", Common.right_answer_count))
                    .append("/")
                    .append(String.format("%d", Common.questionList.size())).toString());
            txt_wrong_answer.setText(String.valueOf(Common.wrong_answer_count));

            if (question_state.getType() != Common.ANSWER_TYPE.NO_ANSWER) {
                questionFragment.showCorrectAnswer();
                questionFragment.disableAnswer();
            }
        }

        //We will navigate to new result action activity here
        Intent intent = new Intent(QuestionActivity.this,ResultActivity.class);
        Common.timer = Common.TOTAL_TIME - time_play;
        Common.no_answer_count = Common.questionList.size() - (Common.wrong_answer_count+Common.right_answer_count);
        Common.data_question = new StringBuilder(new Gson().toJson(Common.answerSheetList));

        startActivityForResult(intent,CODE_GET_RESULT);
    }

    private void countCorrectAnswer() {

        //Reset variable
        Common.right_answer_count = Common.wrong_answer_count = 0;
        for (CurrentQuestion item:Common.answerSheetList)
            if (item.getType() == Common.ANSWER_TYPE.RIGHT_ANSWER)
                Common.right_answer_count++;
            else if (item.getType() == Common.ANSWER_TYPE.WRONG_ANSWER)
                Common.wrong_answer_count++;
    }

    private void genFragmentList() {
        for (int i=0; i<Common.questionList.size(); i++)
        {
            Bundle bundle = new Bundle();
            bundle.putInt("index",i);
            QuestionFragment fragment = new QuestionFragment();
            fragment.setArguments(bundle);

            Common.fragmentsList.add(fragment);
        }
    }

    private void countTimer() {
        if (Common.countDownTimer == null)
        {
            Common.countDownTimer = new CountDownTimer(Common.TOTAL_TIME,1000) {
                @Override
                public void onTick(long l) {
                    txt_timer.setText(String.format("%02d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes(l),
                            TimeUnit.MILLISECONDS.toSeconds(l) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(l))));
                    time_play -= 1000;
                }

                @Override
                public void onFinish() {
                    //Finish game
                    finishGame();
                }
            }.start();
        }
        else
        {
            Common.countDownTimer.cancel();
            Common.countDownTimer = new CountDownTimer(Common.TOTAL_TIME,1000) {
                @Override                                                                    
                public void onTick(long l) {
                    txt_timer.setText(String.format("%02d:%02d",
                            TimeUnit.MILLISECONDS.toMinutes(l),
                            TimeUnit.MILLISECONDS.toSeconds(l) -
                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(l))));
                    time_play -= 1000;
                }

                @Override
                public void onFinish() {
                    //Finish game
                }
            }.start();
        }
    }

    private void takeQuestion() {
        Common.questionList = DBHelper.getInstance(this).getQuestionByCategory(Common.selectedCategory.getId());
        if (Common.questionList.size() == 0)
        {
            //If no question
            new MaterialStyledDialog.Builder(this)
                    .setTitle("Hey !")
                    .setIcon(R.drawable.ic_sentiment_very_dissatisfied_black_24dp)
                    .setDescription("Kami tidak memiliki pertanyaan untuk "+Common.selectedCategory.getName()+" kategori")
                    .setPositiveText("Ok")
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            dialog.dismiss();
                            finish();
                        }
                    }).show();
        }
        else
        {
            if (Common.answerSheetList.size() > 0)
                Common.answerSheetList.clear();
            //Gen answer sheet item from question
            //20 question =  20 answer sheet item
            //1 question = 1 answer sheet item
            for (int i=0; i<Common.questionList.size(); i++)
            {
                //Because we need take index of question in list, so we will use for i
                Common.answerSheetList.add(new CurrentQuestion(i,Common.ANSWER_TYPE.NO_ANSWER)); //Default all answer is no answer
            }
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        MenuItem item = menu.findItem(R.id.menu_wrong_answer);
        ConstraintLayout constraintLayout = (ConstraintLayout)item.getActionView();
        txt_wrong_answer = (TextView)constraintLayout.findViewById(R.id.txt_wrong_answer);
        txt_wrong_answer.setText(String.valueOf(0));
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.question, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.menu_finish_game) {

            if (!isAnswerModeView)
            {
                new MaterialStyledDialog.Builder(this)
                        .setTitle("Selesai ?")
                        .setIcon(R.drawable.ic_mood_white_24dp)
                        .setDescription("Kamu serius ingin mengakhirinya ?")
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
                                finishGame();
                            }
                        })
                        .show();
            }
            else
                finishGame();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK)
        {
            String action = data.getStringExtra("aksi");
            if (action == null || TextUtils.isEmpty(action))
            {
                int questionNum = data.getIntExtra(Common.KEY_BACK_FROM_RESULT, -1);
                viewPager.setCurrentItem(questionNum);

                isAnswerModeView = true;
                Common.countDownTimer.cancel();

                txt_wrong_answer.setVisibility(View.GONE);
                txt_right_answer.setVisibility(View.GONE);
                txt_timer.setVisibility(View.GONE);
            }
            else
            {
                if (action.equals("lihatjawabanquiz"))
                {
                    viewPager.setCurrentItem(0);

                    isAnswerModeView = true;
                    Common.countDownTimer.cancel();

                    txt_wrong_answer.setVisibility(View.GONE);
                    txt_right_answer.setVisibility(View.GONE);
                    txt_timer.setVisibility(View.GONE);

                    for (int i=0; i<Common.fragmentsList.size(); i++)
                    {
                        Common.fragmentsList.get(i).showCorrectAnswer();
                        Common.fragmentsList.get(i).disableAnswer();
                    }
                }
                else if (action.equals("lakukanlagi"))
                {
                    viewPager.setCurrentItem(0);

                    isAnswerModeView = false;
                    countTimer();

                    txt_wrong_answer.setVisibility(View.VISIBLE);
                    txt_right_answer.setVisibility(View.VISIBLE);
                    txt_timer.setVisibility(View.VISIBLE);

                    for (CurrentQuestion item : Common.answerSheetList)
                        item.setType(Common.ANSWER_TYPE.NO_ANSWER); //Reset all question
                    answerSheetAdapter.notifyDataSetChanged();
                    answerSheetHelperAdapter.notifyDataSetChanged();

                    for (int i=0; i<Common.fragmentsList.size(); i++)
                        Common.fragmentsList.get(i).resetQuestion();
                }
            }
        }
    }
}
