package com.example.survivorslog;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GroqFallbackService {
	
	// ログ出力機能
	// Groqで発生したエラーや処理状況を記録する
	private static final Logger log = 
			LoggerFactory.getLogger(GroqFallbackService.class);

    // Groq APIキー
    // application.propertiesから読み込む
    @Value("${groq.api.key:}")
    private String apiKey;

    // Groqで使用するモデル
    @Value("${groq.api.model:openai/gpt-oss-20b}")
    private String model;

    // Groq APIの接続先
    private static final String API_URL =
            "https://api.groq.com/openai/v1/chat/completions";

    // HTTP通信
    private final RestTemplate restTemplate;

    // JSON処理
    private final ObjectMapper objectMapper;

    // コンストラクタ
    public GroqFallbackService() {

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        // 接続タイムアウト：10秒
        factory.setConnectTimeout(10000);

        // 応答待ちタイムアウト：30秒
        factory.setReadTimeout(30000);

        this.restTemplate = new RestTemplate(factory);

        this.objectMapper = new ObjectMapper();
    }

    // =====================================
    // Groqによる相談内容の分析
    // =====================================

    public AiAnalysisResult analyzeLog(
            String content,
            String effort) {
    	
    	// Groqによる分析開始をコンソールに記録
    	log.info("[Groq] 相談内容の分析を開始しました。");
    	
        // APIキーが設定されていない場合
        if (apiKey == null || apiKey.isBlank()) {

        	// APIキーがせっていされていないことを記録
        	log.error("[Groq] APIキーが設定されていません。");
        	
            throw new RuntimeException(
                    "Groq APIキーが設定されていません。");
        }

        // =====================================
        // AIへの分析指示
        // =====================================

        String prompt = """
                あなたは生活・仕事・学校の悩みを
                丁寧に整理するサポートAIです。

                入力された相談内容と、本人が頑張ったことを
                分析してください。

                必ず次のJSON形式だけで回答してください。

                {
                  "dangerLevel": "注意 (Lv.1 : 軽度ストレス)",
                  "summary": "具体的な分析とアドバイス"
                }

                危険度は次の5段階から必ず1つ選んでください。

                注意 (Lv.1 : 軽度ストレス)
                軽微 (Lv.2 : 疲労蓄積)
                要警戒 (Lv.3 : 心身の限界注意)
                危険 (Lv.4 : 早期相談・休養必要)
                極めて危険 (Lv.5 : 危険)

                summaryには次の内容を含めてください。

                ・相談内容の整理
                ・本人が置かれている状況
                ・本人が頑張ったことへの理解
                ・具体的な対処方法
                ・必要に応じた休養や相談先の提案
        		・相談先の電話番号を書くときは、よりそいホットライン 0120-279-338(24時間)、
#いのちSOS 0120-061-338(24時間)、緊急時は119だけを使い、これ以外の番号は書かない
                医学的な診断は行わないでください。
        		
        		
                自然で読みやすい日本語を使用してください。
                助詞の重複や誤字脱字がないか確認してください。
                """;

        // =====================================
        // Groqに送信する相談内容
        // =====================================

        String requestText =
                "【本文】\n"
                + (content == null ? "" : content)
                + "\n【がんばったこと】\n"
                + (effort == null ? "" : effort);

        // =====================================
        // Groqへ送信するJSONデータ
        // =====================================

        Map<String, Object> requestBody = Map.of(

                "model", model,

                "messages", List.of(

                        Map.of(
                                "role", "system",
                                "content", prompt
                        ),

                        Map.of(
                                "role", "user",
                                "content", requestText
                        )
                ),

                "max_completion_tokens", 4000,

                "response_format",
                Map.of("type", "json_object")
        );

        // =====================================
        // HTTPヘッダーの設定
        // =====================================

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.setBearerAuth(apiKey);

        // リクエストを作成
        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(requestBody, headers);

        try {

            // =====================================
            // Groq APIへ接続
            // =====================================

            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            API_URL,
                            request,
                            String.class
                    );

            // =====================================
            // Groqの回答を取得
            // =====================================

            if (response.getBody() == null
                    || response.getBody().isBlank()) {

                throw new RuntimeException(
                        "Groqから空の回答が返されました。");
            }

            JsonNode root =
                    objectMapper.readTree(response.getBody());

            String aiText = root
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText("");

            if (aiText.isBlank()) {

                throw new RuntimeException(
                        "Groqの回答内容を取得できませんでした。");
            }

            // =====================================
            // 回答JSONを解析
            // =====================================

            JsonNode result =
                    objectMapper.readTree(aiText);

            String dangerLevel =
                    result.path("dangerLevel").asText("");

            String summary =
                    result.path("summary").asText("");

            // 必須項目の確認
            if (dangerLevel.isBlank() || summary.isBlank()) {

                throw new RuntimeException(
                        "Groqの分析結果に不足があります。");
            }

            // =====================================
            // 危険度が指定の5段階か確認
            // =====================================

            List<String> validLevels = List.of(

                    "注意 (Lv.1 : 軽度ストレス)",

                    "軽微 (Lv.2 : 疲労蓄積)",

                    "要警戒 (Lv.3 : 心身の限界注意)",

                    "危険 (Lv.4 : 早期相談・休養必要)",

                    "極めて危険 (Lv.5 : 危険)"
            );

            if (!validLevels.contains(dangerLevel)) {

                throw new RuntimeException(
                        "Groqが不正な危険度を返しました。");
            }

            // =====================================
            // 分析成功
            // =====================================
            
            
            //  Groqによる分析が正常に完了したことを記録
           log.info("[Groq] 相談内容の分析が正常に完了しました。");

            return new AiAnalysisResult(
                    summary,
                    dangerLevel,
                    "Groq"
            );

        } catch (HttpClientErrorException.TooManyRequests e) {

            // 429：利用制限
            // 有料プランへの変更は行わない
        	log.error("[Groq] APIの利用制限に達しました。", e);
        	
        	//  有料プランへの切替は行わない
            throw new RuntimeException(
                    "Groqの利用制限に達しました。"
                    + "有料プランには切り替えず、"
                    + "今回は分析を中止します。",
                    e
            );
         
        } catch (HttpClientErrorException e) {

            // APIキーやリクエストなどのエラー
        	log.error(
        			"[Groq] リクエストエラーが発生しました。HTTPステータス : {}",
        			e.getStatusCode().value(),
        			e
        			);
        		
            throw new RuntimeException(
                    "Groqへのリクエストに失敗しました。"
                    + "HTTPステータス："
                    + e.getStatusCode().value(),
                    e
            );

        } catch (HttpServerErrorException e) {

            // Groq側のサーバーエラー
        	log.error(
        			"[Groq] サーバーエラーが発生しました。HTTPステータス : {}",
        			e.getStatusCode().value(),
        			e
        			
        	);
        	
           throw new RuntimeException(
                    "Groq側でサーバーエラーが発生しました。"
                    + "HTTPステータス："
                    + e.getStatusCode().value(),
                    e
            );

        } catch (ResourceAccessException e) {

            // 通信タイムアウトや接続エラーなど
        	log.error("[Groq] 通信に失敗しました。", e);
        	
            throw new RuntimeException(
                    "Groqとの通信に失敗しました。",
                    e
            );

        } catch (RuntimeException e) {

        	//  分析処理中に発生したエラー
        	log.error("[Groq] 分析処理中にエラーが発生しました。" , e);
        	
        //　元の例外をそのまま呼び出し元に返す
            throw e;

        } catch (Exception e) {

            // JSON解析などのその他の予期しないエラー
        	log.error("[Groq] 分析結果の処理に失敗しました。",e);
        	
            throw new RuntimeException(
                    "Groqの分析結果を処理できませんでした。",
                    e
            );
        }
    }
}