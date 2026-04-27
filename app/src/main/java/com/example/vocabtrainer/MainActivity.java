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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int TOTAL_BLOCKS = 20;
    private static final int WORDS_PER_BLOCK = 500;
    private static final String PREFS_NAME = "vocab_progress";
    private static final String WRONG_IDS_KEY = "wrong_ids";

    private static final String POS_ADJ = "形容詞";
    private static final String POS_V = "動詞";
    private static final String POS_N = "名詞";
    private static final String POS_ADV = "副詞";

    private final Random random = new Random();
    private SharedPreferences prefs;
    private List<Word> allWords;
    private ArrayList<Word> quizWords = new ArrayList<>();
    private int quizIndex = 0;
    private int currentBlockNumber = 1;
    private boolean reviewingWrongWords = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        allWords = seedWords();
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

        TextView subtitle = normalText("先用 50 個測試單字驗證流程。正式版會以每 500 個單字為一區，共 20 區，總計 10,000 個單字。");
        subtitle.setPadding(0, dp(8), 0, dp(14));
        root.addView(subtitle);

        Button wrongButton = mainButton("錯題複習區（目前 " + getWrongWords().size() + " 題）");
        wrongButton.setOnClickListener(v -> startWrongReview());
        root.addView(wrongButton);

        TextView blocksTitle = sectionText("選擇單字區塊");
        blocksTitle.setPadding(0, dp(18), 0, dp(8));
        root.addView(blocksTitle);

        for (int row = 0; row < 10; row++) {
            LinearLayout rowLayout = new LinearLayout(this);
            rowLayout.setOrientation(LinearLayout.HORIZONTAL);
            rowLayout.setGravity(Gravity.CENTER);

            for (int col = 0; col < 2; col++) {
                final int blockNumber = row * 2 + col + 1;
                Button button = smallBlockButton("第 " + blockNumber + " 區\n" + blockRangeText(blockNumber));
                button.setOnClickListener(v -> {
                    if (blockNumber == 1) {
                        startBlockQuiz(blockNumber);
                    } else {
                        Toast.makeText(this, "目前雛形版只有第 1 區放入 50 個測試單字", Toast.LENGTH_SHORT).show();
                    }
                });
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
        int end = blockNumber * WORDS_PER_BLOCK;
        if (blockNumber == 1) {
            return "1–500（測試 50 題）";
        }
        return start + "–" + end + "（尚未開放）";
    }

    private void startBlockQuiz(int blockNumber) {
        reviewingWrongWords = false;
        currentBlockNumber = blockNumber;
        quizWords = new ArrayList<>();

        for (Word word : allWords) {
            if (word.block == blockNumber) {
                quizWords.add(word);
            }
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
        return choices;
    }

    private void handleAnswer(Word word, String selectedChinese) {
        boolean correct = word.chinese.equals(selectedChinese);

        if (correct) {
            handleCorrectAnswer(word);
        } else {
            handleWrongAnswer(word);
        }

        quizIndex++;
        showQuestion();
    }

    private void handleCorrectAnswer(Word word) {
        if (reviewingWrongWords) {
            int correctCount = getWrongCorrect(word.id) + 1;
            int requiredCount = getWrongRequired(word.id);

            if (correctCount >= requiredCount) {
                removeWrongWord(word.id);
                Toast.makeText(this, "你是神吧！這題已離開錯題區", Toast.LENGTH_SHORT).show();
            } else {
                prefs.edit().putInt(keyWrongCorrect(word.id), correctCount).apply();
                Toast.makeText(this, "你是神吧！", Toast.LENGTH_SHORT).show();
            }
        } else {
            prefs.edit().putInt(keyNormalCorrect(word.id), getNormalCorrect(word.id) + 1).apply();
            Toast.makeText(this, "你是神吧！", Toast.LENGTH_SHORT).show();
        }
    }

    private void handleWrongAnswer(Word word) {
        if (reviewingWrongWords) {
            int newRequired = getWrongRequired(word.id) + 1;
            prefs.edit().putInt(keyWrongRequired(word.id), newRequired).apply();
        } else {
            addWrongWord(word.id);
        }

        Toast.makeText(this, "想想爸爸媽媽對你的栽培", Toast.LENGTH_SHORT).show();
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
        Button button = mainButton(text);
        return button;
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

    private List<Word> seedWords() {
        return Arrays.asList(
                new Word("w001", 1, "abandon", "放棄", POS_V),
                new Word("w002", 1, "accept", "接受", POS_V),
                new Word("w003", 1, "achieve", "達成", POS_V),
                new Word("w004", 1, "admire", "欽佩", POS_V),
                new Word("w005", 1, "announce", "宣布", POS_V),
                new Word("w006", 1, "borrow", "借入", POS_V),
                new Word("w007", 1, "compare", "比較", POS_V),
                new Word("w008", 1, "deliver", "遞送", POS_V),
                new Word("w009", 1, "encourage", "鼓勵", POS_V),
                new Word("w010", 1, "improve", "改善", POS_V),
                new Word("w011", 1, "ancient", "古老的", POS_ADJ),
                new Word("w012", 1, "brave", "勇敢的", POS_ADJ),
                new Word("w013", 1, "calm", "冷靜的", POS_ADJ),
                new Word("w014", 1, "careful", "小心的", POS_ADJ),
                new Word("w015", 1, "creative", "有創意的", POS_ADJ),
                new Word("w016", 1, "dangerous", "危險的", POS_ADJ),
                new Word("w017", 1, "familiar", "熟悉的", POS_ADJ),
                new Word("w018", 1, "honest", "誠實的", POS_ADJ),
                new Word("w019", 1, "modern", "現代的", POS_ADJ),
                new Word("w020", 1, "patient", "有耐心的", POS_ADJ),
                new Word("w021", 1, "ability", "能力", POS_N),
                new Word("w022", 1, "advice", "建議", POS_N),
                new Word("w023", 1, "airport", "機場", POS_N),
                new Word("w024", 1, "article", "文章", POS_N),
                new Word("w025", 1, "balance", "平衡", POS_N),
                new Word("w026", 1, "culture", "文化", POS_N),
                new Word("w027", 1, "decision", "決定", POS_N),
                new Word("w028", 1, "energy", "能量", POS_N),
                new Word("w029", 1, "failure", "失敗", POS_N),
                new Word("w030", 1, "journey", "旅程", POS_N),
                new Word("w031", 1, "quickly", "快速地", POS_ADV),
                new Word("w032", 1, "slowly", "慢慢地", POS_ADV),
                new Word("w033", 1, "clearly", "清楚地", POS_ADV),
                new Word("w034", 1, "quietly", "安靜地", POS_ADV),
                new Word("w035", 1, "suddenly", "突然地", POS_ADV),
                new Word("w036", 1, "usually", "通常", POS_ADV),
                new Word("w037", 1, "rarely", "很少地", POS_ADV),
                new Word("w038", 1, "probably", "可能地", POS_ADV),
                new Word("w039", 1, "especially", "特別地", POS_ADV),
                new Word("w040", 1, "nearly", "幾乎", POS_ADV),
                new Word("w041", 1, "protect", "保護", POS_V),
                new Word("w042", 1, "refuse", "拒絕", POS_V),
                new Word("w043", 1, "support", "支持", POS_V),
                new Word("w044", 1, "develop", "發展", POS_V),
                new Word("w045", 1, "explain", "解釋", POS_V),
                new Word("w046", 1, "bright", "明亮的", POS_ADJ),
                new Word("w047", 1, "empty", "空的", POS_ADJ),
                new Word("w048", 1, "famous", "有名的", POS_ADJ),
                new Word("w049", 1, "healthy", "健康的", POS_ADJ),
                new Word("w050", 1, "useful", "有用的", POS_ADJ)
        );
    }

    private static class Word {
        final String id;
        final int block;
        final String english;
        final String chinese;
        final String partOfSpeech;

        Word(String id, int block, String english, String chinese, String partOfSpeech) {
            this.id = id;
            this.block = block;
            this.english = english;
            this.chinese = chinese;
            this.partOfSpeech = partOfSpeech;
        }
    }
}
