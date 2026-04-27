package com.example.vocabtrainer;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int WORDS_PER_BLOCK = 20;
    private static final String DONT_KNOW_CHOICE = "窩不知道";
    private static final String PREFS_NAME = "vocab_progress";
    private static final String WRONG_IDS_KEY = "wrong_ids";

    private final Random random = new Random();
    private SharedPreferences prefs;
    private List<Word> allWords;
    private ArrayList<Word> quizWords = new ArrayList<>();
    private int quizIndex = 0;
    private int currentBlockNumber = 1;
    private boolean reviewingWrongWords = false;

    private int getTotalBlocks() {
        if (allWords == null || allWords.isEmpty()) {
            return 1;
        }
        return (allWords.size() + WORDS_PER_BLOCK - 1) / WORDS_PER_BLOCK;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        allWords = loadWordsFromAssets();
        showHome();
    }

    @Override
    public void onBackPressed() {
        showHome();
    }

    private void showHome() {
        reviewingWrongWords = false;

        LinearLayout root = verticalRoot();
        root.setPadding(dp(18), dp(22), dp(18), dp(22));

        TextView title = titleText("背單字小考");
        root.addView(title);

        TextView subtitle = normalText("目前每區 " + WORDS_PER_BLOCK + " 題，依照你目前的資料自動分區。");
        subtitle.setPadding(0, dp(8), 0, dp(14));
        root.addView(subtitle);

        Button wrongButton = mainButton("錯題複習區（目前 " + getWrongWords().size() + " 題）");
        wrongButton.setOnClickListener(v -> startWrongReview());
        root.addView(wrongButton);

        TextView blocksTitle = sectionText("選擇單字區塊");
        blocksTitle.setPadding(0, dp(18), 0, dp(8));
        root.addView(blocksTitle);

        int totalBlocks = getTotalBlocks();
        for (int row = 0; row < (totalBlocks + 1) / 2; row++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setGravity(Gravity.CENTER);

            for (int col = 0; col < 2; col++) {
                final int blockNumber = row * 2 + col + 1;
                if (blockNumber > totalBlocks) {
                    break;
                }
                Button button = smallBlockButton(blockButtonText(blockNumber));
                button.setOnClickListener(v -> startBlockQuiz(blockNumber));
                rowLayout.addView(button);
            }

            root.addView(rowLayout);
        }

        Button resetButton = secondaryButton("清除本機進度");
        resetButton.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            Toast.makeText(this, "進度已清除", Toast.LENGTH_SHORT).show();
            showHome();
        });
        root.addView(resetButton);

        setContentView(wrapScroll(root));
    }

    private String blockRangeText(int blockNumber) {
        int start = (blockNumber - 1) * WORDS_PER_BLOCK + 1;
        int end = Math.min(blockNumber * WORDS_PER_BLOCK, allWords.size());
        return start + "–" + end;
    }

    private int getBlockQuestionCount(int blockNumber) {
        int startIndex = (blockNumber - 1) * WORDS_PER_BLOCK;
        if (startIndex >= allWords.size()) {
            return 0;
        }
        int endIndex = Math.min(startIndex + WORDS_PER_BLOCK, allWords.size());
        return endIndex - startIndex;
    }

    private String blockButtonText(int blockNumber) {
        int correct = getBlockCorrect(blockNumber);
        int total = getBlockQuestionCount(blockNumber);
        String crown = (total > 0 && correct == total) ? " 👑" : "";
        return "第 " + blockNumber + " 區\n"
                + blockRangeText(blockNumber) + "\n"
                + correct + "/" + total + crown;
    }

    private void startBlockQuiz(int blockNumber) {
        reviewingWrongWords = false;
        currentBlockNumber = blockNumber;
        quizWords = new ArrayList<>();
        resetBlockProgress(blockNumber);

        int startIndex = (blockNumber - 1) * WORDS_PER_BLOCK;
        int endIndex = Math.min(startIndex + WORDS_PER_BLOCK, allWords.size());

        for (int i = startIndex; i < endIndex; i++) {
            quizWords.add(allWords.get(i));
        }

        if (quizWords.isEmpty()) {
            Toast.makeText(this, "這個區塊目前沒有單字", Toast.LENGTH_SHORT).show();
            showHome();
            return;
        }

        Collections.shuffle(quizWords, random);
        quizIndex = 0;
        showQuestion();
    }

    private void startWrongReview() {
        reviewingWrongWords = true;
        quizWords = new ArrayList<>(getWrongWords());

        if (quizWords.isEmpty()) {
            Toast.makeText(this, "目前沒有錯題，超強！", Toast.LENGTH_SHORT).show();
            showHome();
            return;
        }

        Collections.shuffle(quizWords, random);
        quizIndex = 0;
        showQuestion();
    }

    private void showQuestion() {
        if (quizIndex >= quizWords.size()) {
            showQuizFinished();
            return;
        }

        Word current = quizWords.get(quizIndex);
        LinearLayout root = verticalRoot();
        root.setPadding(dp(18), dp(18), dp(18), dp(22));

        Button backButton = secondaryButton("回首頁");
        backButton.setOnClickListener(v -> showHome());
        root.addView(backButton);

        TextView progress = normalText((reviewingWrongWords ? "錯題複習" : "第 " + currentBlockNumber + " 區測驗")
                + "  第 " + (quizIndex + 1) + " / " + quizWords.size() + " 題");
        progress.setGravity(Gravity.CENTER);
        progress.setPadding(0, dp(18), 0, dp(8));
        root.addView(progress);

        TextView pos = normalText("詞性：" + current.partOfSpeech);
        pos.setGravity(Gravity.CENTER);
        root.addView(pos);

        TextView wordText = new TextView(this);
        wordText.setText(current.english);
        wordText.setTextSize(40);
        wordText.setTextColor(Color.rgb(15, 23, 42));
        wordText.setTypeface(Typeface.DEFAULT_BOLD);
        wordText.setGravity(Gravity.CENTER);
        wordText.setPadding(0, dp(28), 0, dp(28));
        root.addView(wordText);

        List<String> choices = buildChoices(current);
        for (String choice : choices) {
            Button choiceButton = mainButton(choice);
            choiceButton.setOnClickListener(v -> handleAnswer(current, choice));
            root.addView(choiceButton);
        }

        if (reviewingWrongWords) {
            TextView note = normalText("這題需累積答對 " + getWrongRequired(current.id)
                    + " 次；目前已答對 " + getWrongCorrect(current.id)
                    + " 次。答錯會讓需要答對的次數 +1。");
            note.setPadding(0, dp(16), 0, 0);
            root.addView(note);
        }

        setContentView(wrapScroll(root));
    }

    private List<String> buildChoices(Word current) {
        ArrayList<String> choices = new ArrayList<>();
        choices.add(current.chinese);

        ArrayList<Word> samePosWords = new ArrayList<>();
        for (Word word : allWords) {
            if (!word.id.equals(current.id) && word.partOfSpeech.equals(current.partOfSpeech)) {
                samePosWords.add(word);
            }
        }

        Collections.shuffle(samePosWords, random);
        for (Word word : samePosWords) {
            if (choices.size() >= 4) {
                break;
            }
            if (!choices.contains(word.chinese)) {
                choices.add(word.chinese);
            }
        }

        while (choices.size() < 4) {
            Word fallback = allWords.get(random.nextInt(allWords.size()));
            if (!fallback.id.equals(current.id) && !choices.contains(fallback.chinese)) {
                choices.add(fallback.chinese);
            }
        }

        Collections.shuffle(choices, random);
        choices.add(DONT_KNOW_CHOICE);
        return choices;
    }

    private void handleAnswer(Word word, String selectedChinese) {
        boolean correct = word.chinese.equals(selectedChinese);
        int answeredQuestionNumber = quizIndex + 1;

        if (correct) {
            handleCorrectAnswer(word);
        } else {
            handleWrongAnswer(word);
        }

        if (!reviewingWrongWords) {
            updateBlockProgress(currentBlockNumber, correct);
        }

        quizIndex++;
        showAnswerResult(word, selectedChinese, correct, answeredQuestionNumber);
    }

    private void showAnswerResult(Word word, String selectedChinese, boolean correct, int answeredQuestionNumber) {
        LinearLayout root = verticalRoot();
        root.setPadding(dp(18), dp(22), dp(18), dp(22));

        Button homeButton = secondaryButton("回首頁");
        homeButton.setOnClickListener(v -> showHome());
        root.addView(homeButton);

        TextView progress = normalText((reviewingWrongWords ? "錯題複習" : "第 " + currentBlockNumber + " 區測驗")
                + "  第 " + answeredQuestionNumber + " / " + quizWords.size() + " 題");
        progress.setGravity(Gravity.CENTER);
        progress.setPadding(0, dp(18), 0, dp(10));
        root.addView(progress);

        TextView resultTitle = new TextView(this);
        resultTitle.setText(correct ? "答對了！你是神吧！" : "答錯了！想想爸爸媽媽對你的栽培");
        resultTitle.setTextSize(30);
        resultTitle.setTypeface(Typeface.DEFAULT_BOLD);
        resultTitle.setTextColor(correct ? Color.rgb(22, 101, 52) : Color.rgb(185, 28, 28));
        resultTitle.setGravity(Gravity.CENTER);
        resultTitle.setPadding(0, dp(18), 0, dp(18));
        root.addView(resultTitle);

        TextView wordText = titleText(word.english);
        wordText.setTextSize(38);
        root.addView(makeSection("單字", wordText));

        LinearLayout wordInfo = sectionContentLayout();
        TextView posText = normalText("詞性：" + word.partOfSpeech);
        posText.setPadding(0, 0, 0, dp(8));
        wordInfo.addView(posText);

        TextView selectedAnswer = normalText("你的答案：" + selectedChinese);
        selectedAnswer.setTextColor(correct ? Color.rgb(22, 101, 52) : Color.rgb(185, 28, 28));
        selectedAnswer.setTypeface(Typeface.DEFAULT_BOLD);
        selectedAnswer.setPadding(0, 0, 0, dp(8));
        wordInfo.addView(selectedAnswer);

        TextView correctAnswer = normalText("正確答案：" + word.chinese);
        correctAnswer.setTypeface(Typeface.DEFAULT_BOLD);
        correctAnswer.setTextColor(Color.rgb(30, 41, 59));
        wordInfo.addView(correctAnswer);

        root.addView(makeSection("作答結果", wordInfo));

        LinearLayout exampleContent = sectionContentLayout();
        TextView exampleEnglish = normalText(word.exampleEnglish);
        exampleEnglish.setTextSize(18);
        exampleEnglish.setTypeface(Typeface.DEFAULT_BOLD);
        exampleEnglish.setPadding(dp(8), dp(8), dp(8), dp(8));
        exampleContent.addView(exampleEnglish);

        TextView exampleChinese = normalText(word.exampleChinese);
        exampleChinese.setTextSize(17);
        exampleChinese.setPadding(dp(8), dp(4), dp(8), dp(8));
        exampleContent.addView(exampleChinese);
        root.addView(makeSection("例句", exampleContent));

        LinearLayout extraContent = sectionContentLayout();
        addLearningTip(extraContent, "使用情境", word.scene);
        addLearningTip(extraContent, "常見搭配", word.collocation);
        addLearningTip(extraContent, "使用提醒", word.note);

        if (extraContent.getChildCount() > 0) {
            root.addView(makeSection("延伸資訊", extraContent));
        }

        if (reviewingWrongWords) {
            TextView reviewInfo = normalText("錯題進度：目前已答對 " + getWrongCorrect(word.id)
                    + " 次，需要答對 " + getWrongRequired(word.id) + " 次才會離開錯題區。");
            reviewInfo.setPadding(0, dp(4), 0, dp(12));
            root.addView(reviewInfo);
        }

        Button nextButton = mainButton(quizIndex >= quizWords.size() ? "查看總結" : "下一題");
        nextButton.setOnClickListener(v -> showQuestion());
        root.addView(nextButton);

        setContentView(wrapScroll(root));
    }

    private void handleCorrectAnswer(Word word) {
        if (reviewingWrongWords) {
            int correctCount = getWrongCorrect(word.id) + 1;
            int requiredCount = getWrongRequired(word.id);

            if (correctCount >= requiredCount) {
                removeWrongWord(word.id);
            } else {
                prefs.edit().putInt(keyWrongCorrect(word.id), correctCount).apply();
            }
        } else {
            prefs.edit().putInt(keyNormalCorrect(word.id), getNormalCorrect(word.id) + 1).apply();
        }
    }

    private void handleWrongAnswer(Word word) {
        if (reviewingWrongWords) {
            int newRequired = getWrongRequired(word.id) + 1;
            prefs.edit().putInt(keyWrongRequired(word.id), newRequired).apply();
        } else {
            addWrongWord(word.id);
        }
    }

    private void showQuizFinished() {
        LinearLayout root = verticalRoot();
        root.setPadding(dp(18), dp(26), dp(18), dp(22));

        TextView title = titleText(reviewingWrongWords ? "錯題複習完成" : "本輪測驗完成");
        root.addView(title);

        TextView summary = normalText("目前錯題區共有 " + getWrongWords().size() + " 題。");
        summary.setGravity(Gravity.CENTER);
        summary.setPadding(0, dp(16), 0, dp(16));
        root.addView(summary);

        Button home = mainButton("回首頁");
        home.setOnClickListener(v -> showHome());
        root.addView(home);

        if (!getWrongWords().isEmpty()) {
            Button review = secondaryButton("繼續複習錯題");
            review.setOnClickListener(v -> startWrongReview());
            root.addView(review);
        }

        setContentView(wrapScroll(root));
    }

    private ArrayList<Word> getWrongWords() {
        Set<String> stored = prefs.getStringSet(WRONG_IDS_KEY, new HashSet<>());
        Set<String> wrongIds = new HashSet<>(stored);
        ArrayList<Word> result = new ArrayList<>();

        for (Word word : allWords) {
            if (wrongIds.contains(word.id)) {
                result.add(word);
            }
        }

        return result;
    }

    private void addWrongWord(String wordId) {
        HashSet<String> wrongIds = new HashSet<>(prefs.getStringSet(WRONG_IDS_KEY, new HashSet<>()));
        wrongIds.add(wordId);

        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(WRONG_IDS_KEY, wrongIds);

        if (!prefs.contains(keyWrongRequired(wordId))) {
            editor.putInt(keyWrongRequired(wordId), 5);
        }
        if (!prefs.contains(keyWrongCorrect(wordId))) {
            editor.putInt(keyWrongCorrect(wordId), 0);
        }

        editor.apply();
    }

    private void removeWrongWord(String wordId) {
        HashSet<String> wrongIds = new HashSet<>(prefs.getStringSet(WRONG_IDS_KEY, new HashSet<>()));
        wrongIds.remove(wordId);

        prefs.edit()
                .putStringSet(WRONG_IDS_KEY, wrongIds)
                .remove(keyWrongRequired(wordId))
                .remove(keyWrongCorrect(wordId))
                .apply();
    }

    private int getNormalCorrect(String wordId) {
        return prefs.getInt(keyNormalCorrect(wordId), 0);
    }

    private int getWrongCorrect(String wordId) {
        return prefs.getInt(keyWrongCorrect(wordId), 0);
    }

    private int getWrongRequired(String wordId) {
        return prefs.getInt(keyWrongRequired(wordId), 5);
    }

    private String keyNormalCorrect(String wordId) {
        return "normal_correct_" + wordId;
    }

    private String keyWrongCorrect(String wordId) {
        return "wrong_correct_" + wordId;
    }

    private String keyWrongRequired(String wordId) {
        return "wrong_required_" + wordId;
    }

    private String keyBlockCorrect(int blockNumber) {
        return "block_correct_" + blockNumber;
    }

    private String keyBlockAnswered(int blockNumber) {
        return "block_answered_" + blockNumber;
    }

    private int getBlockCorrect(int blockNumber) {
        return prefs.getInt(keyBlockCorrect(blockNumber), 0);
    }

    private int getBlockAnswered(int blockNumber) {
        return prefs.getInt(keyBlockAnswered(blockNumber), 0);
    }

    private void resetBlockProgress(int blockNumber) {
        prefs.edit()
                .putInt(keyBlockCorrect(blockNumber), 0)
                .putInt(keyBlockAnswered(blockNumber), 0)
                .apply();
    }

    private void updateBlockProgress(int blockNumber, boolean correct) {
        int total = getBlockQuestionCount(blockNumber);
        int answered = Math.min(getBlockAnswered(blockNumber) + 1, total);
        int currentCorrect = getBlockCorrect(blockNumber);
        int newCorrect = correct ? Math.min(currentCorrect + 1, total) : currentCorrect;

        prefs.edit()
                .putInt(keyBlockAnswered(blockNumber), answered)
                .putInt(keyBlockCorrect(blockNumber), newCorrect)
                .apply();
    }

    private LinearLayout verticalRoot() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        return root;
    }

    private ScrollView wrapScroll(View child) {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(false);
        scrollView.addView(child);
        return scrollView;
    }

    private TextView titleText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(28);
        view.setTextColor(Color.rgb(15, 23, 42));
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setGravity(Gravity.CENTER);
        view.setPadding(0, dp(8), 0, dp(8));
        return view;
    }

    private TextView sectionText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(20);
        view.setTextColor(Color.rgb(15, 23, 42));
        view.setTypeface(Typeface.DEFAULT_BOLD);
        view.setGravity(Gravity.CENTER);
        return view;
    }

    private TextView normalText(String text) {
        TextView view = new TextView(this);
        view.setText(text);
        view.setTextSize(16);
        view.setTextColor(Color.rgb(51, 65, 85));
        view.setGravity(Gravity.CENTER);
        return view;
    }

    private LinearLayout sectionContentLayout() {
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.setPadding(dp(10), dp(10), dp(10), dp(10));
        return content;
    }

    private LinearLayout makeSection(String title, View content) {
        LinearLayout section = new LinearLayout(this);
        section.setOrientation(LinearLayout.VERTICAL);
        section.setBackgroundColor(Color.rgb(241, 245, 249));
        section.setPadding(dp(12), dp(12), dp(12), dp(12));

        LinearLayout.LayoutParams sectionParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        sectionParams.setMargins(0, dp(10), 0, dp(10));
        section.setLayoutParams(sectionParams);

        TextView titleView = sectionText(title);
        titleView.setTextSize(18);
        titleView.setPadding(0, 0, 0, dp(6));
        section.addView(titleView);
        section.addView(content);
        return section;
    }

    private Button mainButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(17);
        button.setAllCaps(false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(58));
        params.setMargins(0, dp(6), 0, dp(6));
        button.setLayoutParams(params);
        return button;
    }

    private Button secondaryButton(String text) {
        return mainButton(text);
    }

    private Button smallBlockButton(String text) {
        Button button = new Button(this);
        button.setText(text);
        button.setTextSize(14);
        button.setAllCaps(false);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(78), 1);
        params.setMargins(dp(4), dp(4), dp(4), dp(4));
        button.setLayoutParams(params);
        return button;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private void addLearningTip(LinearLayout root, String label, String value) {
        if (value == null || value.trim().isEmpty()) {
            return;
        }

        TextView tip = normalText(label + "：\n" + value);
        tip.setGravity(Gravity.CENTER);
        tip.setPadding(dp(8), dp(6), dp(8), dp(10));
        root.addView(tip);
    }

    private List<Word> loadWordsFromAssets() {
        ArrayList<Word> words = new ArrayList<>();

        try {
            InputStream inputStream = getAssets().open("vocab_500_words.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            int readBytes = inputStream.read(buffer);
            inputStream.close();

            if (readBytes <= 0) {
                Toast.makeText(this, "單字資料讀取失敗：檔案是空的", Toast.LENGTH_LONG).show();
                return words;
            }

            String json = new String(buffer, StandardCharsets.UTF_8);
            JSONArray array = new JSONArray(json);

            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);

                String id = item.getString("id");
                int block = item.optInt("block", 1);
                String english = item.getString("english");
                String chinese = item.getString("chinese");
                String partOfSpeech = item.getString("partOfSpeech");
                String exampleEnglish = item.optString("exampleEnglish", english);
                String exampleChinese = item.optString("exampleChinese", chinese);
                String scene = item.optString("scene", "");
                String collocation = item.optString("collocation", "");
                String note = item.optString("note", "");

                words.add(new Word(id, block, english, chinese, partOfSpeech,
                        exampleEnglish, exampleChinese, scene, collocation, note));
            }
        } catch (Exception exception) {
            Toast.makeText(this, "單字資料讀取失敗：" + exception.getMessage(), Toast.LENGTH_LONG).show();
        }

        return words;
    }

    private static class Word {
        final String id;
        final int block;
        final String english;
        final String chinese;
        final String partOfSpeech;
        final String exampleEnglish;
        final String exampleChinese;
        final String scene;
        final String collocation;
        final String note;

        Word(String id, int block, String english, String chinese, String partOfSpeech,
             String exampleEnglish, String exampleChinese, String scene, String collocation, String note) {
            this.id = id;
            this.block = block;
            this.english = english;
            this.chinese = chinese;
            this.partOfSpeech = partOfSpeech;
            this.exampleEnglish = exampleEnglish;
            this.exampleChinese = exampleChinese;
            this.scene = scene;
            this.collocation = collocation;
            this.note = note;
        }
    }
}
