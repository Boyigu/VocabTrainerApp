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

        TextView subtitle = normalText("先用 100 個測試單字驗證流程。正式版會以每 500 個單字為一區，共 20 區，總計 10,000 個單字。");
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
                        Toast.makeText(this, "目前雛形版只有第 1 區放入 100 個測試單字", Toast.LENGTH_SHORT).show();
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
            return "1–500（測試 100 題）";
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
        int answeredQuestionNumber = quizIndex + 1;

        if (correct) {
            handleCorrectAnswer(word);
        } else {
            handleWrongAnswer(word);
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
        root.addView(wordText);

        TextView answerInfo = normalText("詞性：" + word.partOfSpeech
                + "\n你的答案：" + selectedChinese
                + "\n正確答案：" + word.chinese);
        answerInfo.setGravity(Gravity.CENTER);
        answerInfo.setPadding(0, dp(14), 0, dp(14));
        root.addView(answerInfo);

        TextView exampleTitle = sectionText("單字延伸例句");
        exampleTitle.setPadding(0, dp(14), 0, dp(8));
        root.addView(exampleTitle);

        TextView exampleEnglish = normalText(word.exampleEnglish);
        exampleEnglish.setTextSize(18);
        exampleEnglish.setTypeface(Typeface.DEFAULT_BOLD);
        exampleEnglish.setGravity(Gravity.CENTER);
        exampleEnglish.setPadding(dp(8), dp(8), dp(8), dp(8));
        root.addView(exampleEnglish);

        TextView exampleChinese = normalText(word.exampleChinese);
        exampleChinese.setTextSize(17);
        exampleChinese.setGravity(Gravity.CENTER);
        exampleChinese.setPadding(dp(8), dp(4), dp(8), dp(18));
        root.addView(exampleChinese);

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
                new Word("w001", 1, "abandon", "放棄", POS_V, "I will not abandon my dream.", "我不會放棄我的夢想。"),
                new Word("w002", 1, "accept", "接受", POS_V, "She decided to accept the challenge.", "她決定接受這個挑戰。"),
                new Word("w003", 1, "achieve", "達成", POS_V, "You can achieve your goal with practice.", "透過練習，你可以達成目標。"),
                new Word("w004", 1, "admire", "欽佩", POS_V, "I admire his courage.", "我欽佩他的勇氣。"),
                new Word("w005", 1, "announce", "宣布", POS_V, "The teacher will announce the result tomorrow.", "老師明天會宣布結果。"),
                new Word("w006", 1, "borrow", "借入", POS_V, "May I borrow your pencil?", "我可以借你的鉛筆嗎？"),
                new Word("w007", 1, "compare", "比較", POS_V, "Do not compare yourself with others.", "不要拿自己和別人比較。"),
                new Word("w008", 1, "deliver", "遞送", POS_V, "The company will deliver the package today.", "公司今天會遞送包裹。"),
                new Word("w009", 1, "encourage", "鼓勵", POS_V, "My friends encourage me to keep going.", "我的朋友鼓勵我繼續前進。"),
                new Word("w010", 1, "improve", "改善", POS_V, "Reading every day can improve your English.", "每天閱讀可以改善你的英文。"),
                new Word("w011", 1, "ancient", "古老的", POS_ADJ, "We visited an ancient temple.", "我們參觀了一座古老的寺廟。"),
                new Word("w012", 1, "brave", "勇敢的", POS_ADJ, "The brave boy helped the old man.", "那位勇敢的男孩幫助了老人。"),
                new Word("w013", 1, "calm", "冷靜的", POS_ADJ, "Stay calm when you face a problem.", "面對問題時要保持冷靜。"),
                new Word("w014", 1, "careful", "小心的", POS_ADJ, "Be careful when you cross the street.", "過馬路時要小心。"),
                new Word("w015", 1, "creative", "有創意的", POS_ADJ, "She has many creative ideas.", "她有很多有創意的點子。"),
                new Word("w016", 1, "dangerous", "危險的", POS_ADJ, "It is dangerous to drive too fast.", "開太快是危險的。"),
                new Word("w017", 1, "familiar", "熟悉的", POS_ADJ, "This song sounds familiar.", "這首歌聽起來很熟悉。"),
                new Word("w018", 1, "honest", "誠實的", POS_ADJ, "An honest person tells the truth.", "誠實的人會說實話。"),
                new Word("w019", 1, "modern", "現代的", POS_ADJ, "This is a modern library.", "這是一座現代的圖書館。"),
                new Word("w020", 1, "patient", "有耐心的", POS_ADJ, "A patient teacher helps students slowly.", "有耐心的老師會慢慢幫助學生。"),
                new Word("w021", 1, "ability", "能力", POS_N, "She has the ability to solve hard problems.", "她有解決難題的能力。"),
                new Word("w022", 1, "advice", "建議", POS_N, "Thank you for your useful advice.", "謝謝你有用的建議。"),
                new Word("w023", 1, "airport", "機場", POS_N, "We arrived at the airport early.", "我們很早就到達機場。"),
                new Word("w024", 1, "article", "文章", POS_N, "I read an interesting article.", "我讀了一篇有趣的文章。"),
                new Word("w025", 1, "balance", "平衡", POS_N, "Good balance is important in life.", "良好的平衡在生活中很重要。"),
                new Word("w026", 1, "culture", "文化", POS_N, "Food is part of every culture.", "食物是每種文化的一部分。"),
                new Word("w027", 1, "decision", "決定", POS_N, "Making a decision can be difficult.", "做決定可能很困難。"),
                new Word("w028", 1, "energy", "能量", POS_N, "Breakfast gives you energy.", "早餐會給你能量。"),
                new Word("w029", 1, "failure", "失敗", POS_N, "Failure can teach us important lessons.", "失敗可以教會我們重要的課題。"),
                new Word("w030", 1, "journey", "旅程", POS_N, "Learning English is a long journey.", "學英文是一段漫長的旅程。"),
                new Word("w031", 1, "quickly", "快速地", POS_ADV, "He quickly finished his homework.", "他快速地完成了作業。"),
                new Word("w032", 1, "slowly", "慢慢地", POS_ADV, "Please speak slowly.", "請慢慢地說。"),
                new Word("w033", 1, "clearly", "清楚地", POS_ADV, "She explained the rule clearly.", "她清楚地解釋了規則。"),
                new Word("w034", 1, "quietly", "安靜地", POS_ADV, "The baby slept quietly.", "寶寶安靜地睡著了。"),
                new Word("w035", 1, "suddenly", "突然地", POS_ADV, "It suddenly started to rain.", "天突然開始下雨。"),
                new Word("w036", 1, "usually", "通常", POS_ADV, "I usually study after dinner.", "我通常晚餐後讀書。"),
                new Word("w037", 1, "rarely", "很少地", POS_ADV, "He rarely eats fast food.", "他很少吃速食。"),
                new Word("w038", 1, "probably", "可能地", POS_ADV, "She will probably come later.", "她可能晚一點會來。"),
                new Word("w039", 1, "especially", "特別地", POS_ADV, "I like fruit, especially apples.", "我喜歡水果，特別是蘋果。"),
                new Word("w040", 1, "nearly", "幾乎", POS_ADV, "I nearly missed the bus.", "我幾乎錯過公車。"),
                new Word("w041", 1, "protect", "保護", POS_V, "We should protect the environment.", "我們應該保護環境。"),
                new Word("w042", 1, "refuse", "拒絕", POS_V, "He refused to give up.", "他拒絕放棄。"),
                new Word("w043", 1, "support", "支持", POS_V, "My family supports my plan.", "我的家人支持我的計畫。"),
                new Word("w044", 1, "develop", "發展", POS_V, "The city continues to develop.", "這座城市持續發展。"),
                new Word("w045", 1, "explain", "解釋", POS_V, "Can you explain this sentence?", "你可以解釋這個句子嗎？"),
                new Word("w046", 1, "bright", "明亮的", POS_ADJ, "The room is bright and clean.", "這個房間明亮又乾淨。"),
                new Word("w047", 1, "empty", "空的", POS_ADJ, "The box is empty.", "這個盒子是空的。"),
                new Word("w048", 1, "famous", "有名的", POS_ADJ, "This city is famous for its night market.", "這座城市以夜市聞名。"),
                new Word("w049", 1, "healthy", "健康的", POS_ADJ, "Healthy food helps your body.", "健康的食物有助於你的身體。"),
                new Word("w050", 1, "useful", "有用的", POS_ADJ, "This dictionary is very useful.", "這本字典非常有用。"),
                new Word("w051", 1, "arrive", "到達", POS_V, "We will arrive at school before eight.", "我們會在八點前到達學校。"),
                new Word("w052", 1, "believe", "相信", POS_V, "I believe you can do it.", "我相信你可以做到。"),
                new Word("w053", 1, "collect", "收集", POS_V, "He likes to collect old coins.", "他喜歡收集舊硬幣。"),
                new Word("w054", 1, "create", "創造", POS_V, "Artists create beautiful things.", "藝術家創造美麗的事物。"),
                new Word("w055", 1, "decide", "決定", POS_V, "We must decide before lunch.", "我們必須在午餐前決定。"),
                new Word("w056", 1, "discover", "發現", POS_V, "Scientists discover new facts.", "科學家發現新的事實。"),
                new Word("w057", 1, "invite", "邀請", POS_V, "I will invite her to my party.", "我會邀請她參加我的派對。"),
                new Word("w058", 1, "prepare", "準備", POS_V, "Please prepare for the test.", "請為考試做準備。"),
                new Word("w059", 1, "remember", "記得", POS_V, "Remember to bring your book.", "記得帶你的書。"),
                new Word("w060", 1, "suggest", "建議", POS_V, "I suggest taking a short break.", "我建議短暫休息一下。"),
                new Word("w061", 1, "active", "活躍的", POS_ADJ, "She is active in class.", "她在課堂上很活躍。"),
                new Word("w062", 1, "basic", "基本的", POS_ADJ, "This lesson teaches basic grammar.", "這堂課教基本文法。"),
                new Word("w063", 1, "common", "常見的", POS_ADJ, "This is a common mistake.", "這是一個常見的錯誤。"),
                new Word("w064", 1, "difficult", "困難的", POS_ADJ, "The question is difficult.", "這個問題很困難。"),
                new Word("w065", 1, "excellent", "優秀的", POS_ADJ, "You did an excellent job.", "你做得非常優秀。"),
                new Word("w066", 1, "fresh", "新鮮的", POS_ADJ, "We bought fresh fruit.", "我們買了新鮮水果。"),
                new Word("w067", 1, "gentle", "溫柔的", POS_ADJ, "She has a gentle voice.", "她有溫柔的聲音。"),
                new Word("w068", 1, "local", "當地的", POS_ADJ, "We tried local food.", "我們嘗試了當地食物。"),
                new Word("w069", 1, "serious", "嚴肅的", POS_ADJ, "This is a serious problem.", "這是一個嚴肅的問題。"),
                new Word("w070", 1, "simple", "簡單的", POS_ADJ, "The answer is simple.", "答案很簡單。"),
                new Word("w071", 1, "attention", "注意力", POS_N, "Please pay attention to the teacher.", "請注意老師說話。"),
                new Word("w072", 1, "business", "商業", POS_N, "Her father started a small business.", "她父親創立了一間小公司。"),
                new Word("w073", 1, "community", "社區", POS_N, "Our community is friendly.", "我們的社區很友善。"),
                new Word("w074", 1, "direction", "方向", POS_N, "Can you tell me the direction to the station?", "你可以告訴我去車站的方向嗎？"),
                new Word("w075", 1, "experience", "經驗", POS_N, "Travel gives us new experience.", "旅行給我們新的經驗。"),
                new Word("w076", 1, "habit", "習慣", POS_N, "Reading is a good habit.", "閱讀是一個好習慣。"),
                new Word("w077", 1, "knowledge", "知識", POS_N, "Books can increase your knowledge.", "書籍可以增加你的知識。"),
                new Word("w078", 1, "language", "語言", POS_N, "English is an international language.", "英文是一種國際語言。"),
                new Word("w079", 1, "memory", "記憶", POS_N, "Sleep can improve your memory.", "睡眠可以改善你的記憶力。"),
                new Word("w080", 1, "purpose", "目的", POS_N, "What is the purpose of this meeting?", "這場會議的目的是什麼？"),
                new Word("w081", 1, "actually", "其實", POS_ADV, "Actually, I agree with you.", "其實，我同意你。"),
                new Word("w082", 1, "almost", "幾乎", POS_ADV, "The work is almost finished.", "工作幾乎完成了。"),
                new Word("w083", 1, "always", "總是", POS_ADV, "She always arrives on time.", "她總是準時到達。"),
                new Word("w084", 1, "carefully", "小心地", POS_ADV, "Read the question carefully.", "小心地閱讀題目。"),
                new Word("w085", 1, "finally", "最後", POS_ADV, "We finally found the answer.", "我們最後找到了答案。"),
                new Word("w086", 1, "immediately", "立刻", POS_ADV, "Call me immediately if you need help.", "如果你需要幫助，立刻打給我。"),
                new Word("w087", 1, "mostly", "大多數地", POS_ADV, "The class is mostly quiet.", "這個班級大多數時候很安靜。"),
                new Word("w088", 1, "recently", "最近", POS_ADV, "I recently started learning Japanese.", "我最近開始學日文。"),
                new Word("w089", 1, "simply", "簡單地", POS_ADV, "He simply said no.", "他只是簡單地說不。"),
                new Word("w090", 1, "together", "一起", POS_ADV, "We studied together after school.", "我們放學後一起讀書。"),
                new Word("w091", 1, "avoid", "避免", POS_V, "Try to avoid making the same mistake.", "試著避免犯同樣的錯。"),
                new Word("w092", 1, "choose", "選擇", POS_V, "Choose the best answer.", "選擇最佳答案。"),
                new Word("w093", 1, "complete", "完成", POS_V, "Please complete the form.", "請完成這份表格。"),
                new Word("w094", 1, "increase", "增加", POS_V, "Exercise can increase your energy.", "運動可以增加你的能量。"),
                new Word("w095", 1, "reduce", "減少", POS_V, "We should reduce plastic waste.", "我們應該減少塑膠垃圾。"),
                new Word("w096", 1, "central", "中央的", POS_ADJ, "The hotel is in the central area.", "飯店在中央區域。"),
                new Word("w097", 1, "direct", "直接的", POS_ADJ, "Please give me a direct answer.", "請給我一個直接的答案。"),
                new Word("w098", 1, "normal", "正常的", POS_ADJ, "It is normal to feel nervous.", "感到緊張是正常的。"),
                new Word("w099", 1, "private", "私人的", POS_ADJ, "This is a private message.", "這是一則私人訊息。"),
                new Word("w100", 1, "valuable", "有價值的", POS_ADJ, "Your time is valuable.", "你的時間很有價值。")
        );
    }
    private static class Word {
        final String id;
        final int block;
        final String english;
        final String chinese;
        final String partOfSpeech;
        final String exampleEnglish;
        final String exampleChinese;

        Word(String id, int block, String english, String chinese, String partOfSpeech,
             String exampleEnglish, String exampleChinese) {
            this.id = id;
            this.block = block;
            this.english = english;
            this.chinese = chinese;
            this.partOfSpeech = partOfSpeech;
            this.exampleEnglish = exampleEnglish;
            this.exampleChinese = exampleChinese;
        }
    }
}
