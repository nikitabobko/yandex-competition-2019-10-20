package com.yandex.championship.quest;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.yandex.championship.quest_core_lib.QuestCore;

import androidx.appcompat.app.AppCompatActivity;

import static com.yandex.championship.quest_core_lib.QuestCore.TaskResult;
import static com.yandex.championship.quest_core_lib.QuestCore.TaskType;


public class MainActivity extends AppCompatActivity {
    private static final int BUTTON_TEXT_SIZE = 36;

    private final QuestCore mQuestCore = new QuestCore(this);
    private Button mBackButton;
    private TextView mTitleTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout);
        mQuestCore.resetQuest();
        mBackButton = createBackButton();
        mTitleTextView = findViewById(R.id.title_text);
        renderNextTask();
    }

    private void processTaskResult(@TaskResult int result) {
        switch (result) {
            case TaskResult.WRONG_ANSWER:
                Toast.makeText(this, getString(R.string.wrong_answer_toast_text),
                        Toast.LENGTH_SHORT).show();
                break;
            case TaskResult.NEXT_QUESTION:
                renderNextTask();
                break;
            case TaskResult.BINGO:
                renderResult();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + result);
        }
    }

    private void renderNextTask() {
        mTitleTextView.setText(mQuestCore.getTaskText());
        clearChildrenBelowChild(mTitleTextView);

        LinearLayout parentLayout = findViewById(R.id.main_layout);
        int type = mQuestCore.getTaskType();
        switch (type) {
            case TaskType.DIRECTION_TASK:
                parentLayout.addView(getDirectionTaskControlsLayout());
                break;
            case TaskType.NUMBER_TASK:
                parentLayout.addView(getNumberTaskControlsLayout());
                parentLayout.addView(mBackButton);
                break;
            case TaskType.TEXT_TASK:
                parentLayout.addView(getTextTaskControlsLayout());
                parentLayout.addView(mBackButton);
                break;
            case TaskType.DEADLOCK_TASK:
                parentLayout.addView(mBackButton);
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + mQuestCore.getTaskType());
        }

        parentLayout.invalidate();
    }

    private void renderResult() {
        mTitleTextView.setText(getString(R.string.result_text));
        clearChildrenBelowChild(mTitleTextView);

        TextView resultTextView = new TextView(this);
        resultTextView.setTextSize(60);
        resultTextView.setText(String.valueOf(mQuestCore.getResult()));
        resultTextView.setGravity(Gravity.CENTER);

        LinearLayout parentLayout = findViewById(R.id.main_layout);
        parentLayout.addView(resultTextView);
        parentLayout.invalidate();
    }

    private View getDirectionTaskControlsLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setOrientation(LinearLayout.HORIZONTAL);

        Button leftButton = new Button(this);
        leftButton.setText(getString(R.string.left_button_text));
        leftButton.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        leftButton.setTextSize(BUTTON_TEXT_SIZE);

        Button rightButton = new Button(this);
        rightButton.setText(getString(R.string.right_button_text));
        rightButton.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        rightButton.setTextSize(BUTTON_TEXT_SIZE);

        leftButton.setOnClickListener((View) ->
                processTaskResult(mQuestCore.checkDirectionAnswer(QuestCore.Direction.LEFT)));
        rightButton.setOnClickListener((View) ->
                processTaskResult(mQuestCore.checkDirectionAnswer(QuestCore.Direction.RIGHT)));

        layout.addView(leftButton);
        layout.addView(rightButton);

        return layout;
    }

    private View getNumberTaskControlsLayout() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.HORIZONTAL);

        for (int i = 0; i < 16; ++i) {
            int value = i +1;
            Button button = new Button(this);
            button.setText(String.valueOf(value));
            button.setTextSize(BUTTON_TEXT_SIZE);
            button.setGravity(Gravity.CENTER);
            button.setOnClickListener((View)->{
                processTaskResult(mQuestCore.checkNumberAnswer(value));
            });
            layout.addView(button);
        }

        HorizontalScrollView view = new HorizontalScrollView(this);
        view.addView(layout);
        return view;
    }

    private View getTextTaskControlsLayout() {
        EditText textField = new EditText(this);
        textField.setGravity(Gravity.CENTER_HORIZONTAL);

        Button okButton = new Button(this);
        okButton.setText(getString(R.string.ok_button_text));
        okButton.setGravity(Gravity.CENTER_HORIZONTAL);
        okButton.setTextSize(BUTTON_TEXT_SIZE);
        okButton.setOnClickListener((View) -> processTaskResult(
                mQuestCore.checkTextAnswer(textField.getText().toString())));
        okButton.setEnabled(false);

        textField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // no-op
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // no-op
            }

            @Override
            public void afterTextChanged(Editable s) {
                okButton.setEnabled(s.length() > 0);
            }
        });
        textField.setText("yandex");
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER_HORIZONTAL);

        layout.addView(textField);
        layout.addView(okButton);
        return layout;
    }

    private Button createBackButton() {
        Button backButton = new Button(this);
        backButton.setText(getString(R.string.back_button_text));
        backButton.setGravity(Gravity.CENTER);
        backButton.setTextSize(BUTTON_TEXT_SIZE);
        backButton.setOnClickListener((View) -> processTaskResult(mQuestCore.goBack()));
        return backButton;
    }

    private void clearChildrenBelowChild(TextView child) {
        LinearLayout parentLayout = findViewById(R.id.main_layout);
        if (parentLayout.indexOfChild(child) == parentLayout.getChildCount() - 1) {
            return;
        }
        int firstChildToRemoveIndex = parentLayout.indexOfChild(child) + 1;
        parentLayout.removeViews(firstChildToRemoveIndex,
                parentLayout.getChildCount() - firstChildToRemoveIndex);
    }
}