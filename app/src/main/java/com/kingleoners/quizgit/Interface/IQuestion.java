package com.kingleoners.quizgit.Interface;

import com.kingleoners.quizgit.Model.CurrentQuestion;

public interface IQuestion {
    CurrentQuestion getSelectedAnswer(); //Get selected answer from user select
    void showCorrectAnswer(); //Bold correct answer text
    void disableAnswer(); //Disable all check box
    void resetQuestion(); //Reset all function on question
}
