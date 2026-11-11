package com.example.survivorslog;

import org.springframework.stereotype.Service;

// 【既存】3つのAIの呼び出し順と切り替えの制御
@Service
public class CounselingService {

    // 【既存】Geminiの分析サービス
    private final GeminiAnalysisService geminiAnalysisService;

    // 【既存】Groqの代替分析サービス
    private final GroqFallbackService groqFallbackService;

    // 【既存】Cloudflareの詳細レポート作成サービス
    private final CloudflareReportService cloudflareReportService;

    // 【既存】3つのAIサービスを受け取る（コンストラクタ）
    public CounselingService(
            GeminiAnalysisService geminiAnalysisService,
            GroqFallbackService groqFallbackService,
            CloudflareReportService cloudflareReportService) {

        this.geminiAnalysisService = geminiAnalysisService;
        this.groqFallbackService = groqFallbackService;
        this.cloudflareReportService = cloudflareReportService;
    }

    // 【既存】Gemini → Groq → Cloudflare の連携処理
    public CounselingResult analyzeAndCreateReport(
            String content, String effort) {

        // 【追加】全体の開始をコンソールに表示する
        System.out.println("\n========== AI処理開始 ==========");

        // 【追加】tryで全体を囲む
        // 理由：最後のfinallyで「AI処理終了」を必ず表示するため
        try {

            // 【既存】基本分析の結果を入れる箱
            // 【変更】tryの中に移動しただけ（役割は同じ）
            AiAnalysisResult aiResult = null;

            // 【追加】Groqを実行したかどうかの目印（最初は false = 実行していない）
            // 理由：getAiName()は存在しないので、この目印でスキップ表示を判定する
            boolean groqAttempted = false;

            // -------------------------------------
            // 【既存】最初にGeminiで分析する
            // -------------------------------------
            try {

                // 【追加】Gemini開始の表示
                System.out.println("[AI① Gemini] 開始");

                // 【既存】Geminiで分析する
                aiResult = geminiAnalysisService.analyzeLog(
                        content, effort);

                // 【既存】結果が空なら失敗として扱う
                checkAnalysisResult(aiResult);

                // 【追加】Gemini成功の表示
                System.out.println("[AI① Gemini] 成功");

            } catch (Exception geminiError) {

                // 【追加】Gemini失敗の表示（失敗の理由も一緒に出す）
                System.out.println(
                        "[AI① Gemini] 失敗：" + geminiError.getMessage());

                // -------------------------------------
                // 【既存】Geminiが失敗した場合はGroqを使う
                // -------------------------------------
                try {

                    // 【追加】Groqを実行することを記録する
                    groqAttempted = true;

                    // 【追加】Groq開始の表示
                    System.out.println("[AI② Groq] 開始");

                    // 【既存】Groqで分析する
                    aiResult = groqFallbackService.analyzeLog(
                            content, effort);

                    // 【既存】Groqの結果も確認する
                    checkAnalysisResult(aiResult);

                    // 【追加】Groq成功の表示
                    System.out.println("[AI② Groq] 成功");

                } catch (Exception groqError) {

                    // 【追加】Groq失敗の表示
                    System.out.println(
                            "[AI② Groq] 失敗：" + groqError.getMessage());

                    // 【追加】両方失敗したので、Cloudflareはスキップと表示
                    System.out.println(
                            "[AI③ Cloudflare] スキップ（Gemini・Groqの分析失敗）");

                    // 【既存】Geminiのエラー情報もGroqのエラーに付け足す
                    groqError.addSuppressed(geminiError);

                    // 【既存】両方失敗した場合は処理を中止する
                    throw new RuntimeException(
                            "GeminiとGroqの両方で分析に失敗しました。",
                            groqError);
                }
            }

            // 【追加】Groqが実行されていなければ「スキップ」と表示
            // 理由：ここに来てGroqが未実行 = Geminiが成功したということ
            if (!groqAttempted) {
                System.out.println(
                        "[AI② Groq] スキップ（Gemini成功のため未実行）");
            }

            // -------------------------------------
            // 【既存】GeminiまたはGroqの結果をCloudflareへ渡す
            // -------------------------------------

            // 【変更】元は「String detailedReport = 〜」と1行で書いていた
            // tryの中と外の両方で使うため、先に変数だけ用意する
            String detailedReport;

            // 【追加】Cloudflare用のtry（成功・失敗の表示のため）
            try {

                // 【追加】Cloudflare開始の表示
                System.out.println("[AI③ Cloudflare] 開始");

                // 【既存】Cloudflareで詳細レポートを作成する
                detailedReport =
                        cloudflareReportService.createDetailedReport(aiResult);

                // 【追加】空のレポートは失敗として扱う
                if (detailedReport == null || detailedReport.isBlank()) {
                    throw new RuntimeException("詳細レポートが空です。");
                }

                // 【追加】Cloudflare成功の表示
                System.out.println("[AI③ Cloudflare] 成功");

            } catch (Exception cloudflareError) {

                // 【追加】Cloudflare失敗の表示
                System.out.println(
                        "[AI③ Cloudflare] 失敗：" + cloudflareError.getMessage());

                // 【追加】失敗を呼び出し元に伝える（元の原因も保持する）
                throw new RuntimeException(
                        "Cloudflareの詳細レポート作成に失敗しました。",
                        cloudflareError);
            }

            // -------------------------------------
            // 【既存】基本分析と詳細レポートをまとめて返す
            // -------------------------------------
            return new CounselingResult(aiResult, detailedReport);

        } finally {

            // 【追加】成功・失敗に関係なく、最後に必ず表示する
            System.out.println("========== AI処理終了 ==========\n");
        }
    }

    // 【既存】AIの基本分析結果が正常か確認する
    private void checkAnalysisResult(AiAnalysisResult result) {

        if (result == null
                || result.getDangerLevel() == null
                || result.getDangerLevel().isBlank()
                || result.getSummary() == null
                || result.getSummary().isBlank()) {
            // 【変更】エラー文の誤字「習得」を「取得」に直しただけ
            throw new RuntimeException(
                    "AIから正常な分析結果を取得できませんでした。");
        }
    }

    // 【既存】2種類のAI結果をまとめて返すための箱（ここは一切変更なし）
    public static class CounselingResult {

        // 【既存】GeminiまたはGroqの基本分析結果
        private final AiAnalysisResult aiResult;

        // 【既存】Cloudflareの詳細回答
        private final String detailedReport;

        public CounselingResult(
                AiAnalysisResult aiResult,
                String detailedReport) {

            this.aiResult = aiResult;
            this.detailedReport = detailedReport;
        }

        public AiAnalysisResult getAiResult() {
            return aiResult;
        }

        // 【既存】詳細回答を取り出す
        public String getDetailedReport() {
            return detailedReport;
        }
    }
}