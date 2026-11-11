
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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CloudflareReportService {

    // =====================================
    // Cloudflareの接続設定
    // =====================================
	
	
	// コンソールへのログの出力
	//  このクラス空コンソールに情報を表示するための部品
	private static final Logger log = 
			LoggerFactory.getLogger(CloudflareReportService.class);

    // CloudflareのAccount ID
    @Value("${cloudflare.account.id:}")
    private String accountId;

    // CloudflareのAPIトークン
    @Value("${cloudflare.api.token:}")
    private String apiToken;

    // 使用するAIモデル
    @Value("${cloudflare.ai.model:@cf/zai-org/glm-4.7-flash}")
    private String model;

    // HTTP通信を行う部品
    private final RestTemplate restTemplate;

    // JSONを処理する部品
    private final ObjectMapper objectMapper;

    // =====================================
    // コンストラクタ
    // =====================================
    public CloudflareReportService() {

        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();

        // 接続タイムアウト：10秒
        factory.setConnectTimeout(10000);

        // 回答待ちタイムアウト：30秒
        factory.setReadTimeout(30000);

        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }

    // =====================================
    // カウンセリング回答を作成する
    // =====================================
    public String createDetailedReport(AiAnalysisResult aiResult) {
    	
    	// 処理開始をコンソールに表示
    	log.info("[Cloudflare] 詳細レポートの作成を開始しました。");
    	
         // Cloudflareの設定確認
        if (accountId == null || accountId.isBlank()
                || apiToken == null || apiToken.isBlank()) {

            throw new RuntimeException(
                    "CloudflareのAccount IDまたはAPIトークンが未設定です。");
        }

        // API 設定の確認成功
        log.info("[cloudflare] API設定を確認しました。");
      
        
        // 元のAI分析結果を確認
        if (aiResult == null
                || aiResult.getDangerLevel() == null
                || aiResult.getDangerLevel().isBlank()
                || aiResult.getSummary() == null
                || aiResult.getSummary().isBlank()) {

            throw new RuntimeException(
                    "元となるAI分析結果がありません。");
        }
        
        log.info("[Cloudflare] 元のAI分析結果を確認しました。");
        
         // =====================================
        // Cloudflare APIの接続先
        // =====================================
        String apiUrl =
                "https://api.cloudflare.com/client/v4/accounts/"
                + accountId
                + "/ai/run/"
                + model;

        // =====================================
        // AIへの指示
        // =====================================
        String systemPrompt = """
  
  あなたは、相談の分析結果を相談者向けにまとめるアシスタントです。
渡された「確定済みの危険度」と「元のAI分析結果」を、
わかりやすい詳細レポートにまとめてください。

【守ること】
・危険度や分析の意味を変更しない
・新しい診断や判断を追加しない
・JSON形式ではなく、通常の文章で書く
・全体で400〜600字程度にまとめる
・見出しは【状況の整理】【アドバイス】【今日できること】の3つにし、見出しだけで1行使う
・「**」や「#」などの装飾記号は使わない
・「このレポートは〜」のような、レポート自体の説明や締めの一文は書かない
・【今日できること】の項目を書き終えたら、そこで終了する
・漢字にふりがなつける
・AI分析と言わない

			  """;

        // =====================================
        // 元のAI分析結果をCloudflareに渡す
        // =====================================
        String userPrompt =
                "【確定済みの危険度】\n"
                + aiResult.getDangerLevel()
                + "\n\n【元のAI分析結果】\n"
                + aiResult.getSummary()
                + "\n\n上記の危険度と分析内容を維持し、"
                + "誰にでも分かりやすい詳細レポートにしてください。";

        // =====================================
        // Cloudflareに送信するJSON
        // =====================================
        Map<String, Object> requestBody = Map.of(
                "messages",
                List.of(
                        Map.of(
                                "role", "system",
                                "content", systemPrompt
                        ),
                        Map.of(
                                "role", "user",
                                "content", userPrompt
                        )
                ),
                "max_completion_tokens", 1200
        );

        // =====================================
        // HTTPヘッダーの設定
        // =====================================
        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);

        // APIトークンによる認証
        headers.setBearerAuth(apiToken);

        // HTTPリクエストを作成
        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(requestBody, headers);

        try {

        // API送信開始
        	log.info("[Cloudflare] APIへリクエストを送信します。");
        	
            // =====================================
            // Cloudflare APIへ接続
            // =====================================
            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            apiUrl,
                            request,
                            String.class
                    );
            
            log.info("[Cloudflare] HTTP応答を受信しました。 ステータス : {}",
            		response.getStatusCode().value());
            
            
            
            // =====================================
            // 回答内容を確認
            // =====================================
            if (response.getBody() == null
                    || response.getBody().isBlank()) {

                throw new RuntimeException(
                        "Cloudflareから空の回答が返されました。");
            }

            // JSONを解析
            JsonNode root =
                    objectMapper.readTree(response.getBody());
            
            log.info("[Cloudflare ] JSONの分析に成功しました。 ");

            // APIの成功状態を確認
            if (!root.path("success").asBoolean(false)) {

                throw new RuntimeException(
                        "Cloudflare APIが処理に失敗しました。");
            }

            // AIの回答本文を取得
            String report = root
                    .path("result")
                    .path("response")
                    .asText("");
      

         // 形式②:result.choices[0].message.content(OpenAI互換の形式) 未完成品を確認する箱
         // 形式①で取れなかった場合だけ、こちらを試す
            if (report.isBlank()) {
            	report = root
            			.path("result")
                        .path("choices")
                        .path(0)
                        .path("message")
                        .path("content")
                        .asText("");
            }
            	
            	// どちらでも取得できなかった場合は失敗
            	if (report.isBlank()) {

            	    // 原因調査のため、応答の先頭部分をコンソールに表示する
            	    String body = response.getBody();
            	    log.error("[Cloudflare] 回答を取得できません。応答の先頭 : {}",
            	            body.substring(0, Math.min(body.length(), 800)));

            	    throw new RuntimeException(
            	            "Cloudflareの回答を取得できませんでした。");
            	}
	
      

            	 // 装飾記号を取り除く(AIが指示を守らない場合の保険)
                // 「**」「#」「`」を削除し、行頭の「- 」は「・」に置き換える
            	  report = report
                          .replace("**", "")
                          .replace("`", "")
                          .replaceAll("(?m)^#+\\s+", "")
                          .replaceAll("(?m)^\\s*[-*]\\s+", "・");

                  // 見出しに【 】を付ける(AIが付けない場合の保険)
                  // すでに【 】が付いている場合は、二重にならないよう一度外してから付け直す
                  for (String heading : new String[] {"状況の整理", "アドバイス", "今日できること"}) {
                      report = report.replace("【" + heading + "】", heading);
                      report = report.replaceAll(
                              "(?m)^\\s*" + heading + "\\s*$",
                              "【" + heading + "】");
                  }

                  // 詳細レポートを返す
                log.info("[Cloudflare] 詳細レポートの作成に成功しました。");
                return report;
                
        } catch (HttpStatusCodeException e) {

        	log.error("[Cloudflare] HTTPエラーが発生しました。ステータス : {}",
        			e.getStatusCode().value(), e);
        	
       
        //  元エラー情報を保持して返す
        throw new RuntimeException(
        	"Cloudflare APIエラー。HTTPステータス : "
        + e.getStatusCode().value(),
        e
        );
        
        } catch (ResourceAccessException e) {
        	
         	// 詳しく原因をEclipseのエラーコンソールに表示する
        	log.error("[Cloudflare ] 通信エラーまたはタイムアウトが発生しました。" , e);
        	
        	//  通信タイムアウト・接続エラー
        	
          	
        	//  元のエラー情報を保持して返す
        	throw new RuntimeException(
        			"Cloudflareとの通信にエラー失敗しました。原因 : "
        			+ e.getMessage(),
        			e
        			);
        
        } catch (RuntimeException e) {
        	log.error("[Cloudflare] 処理中にエラーが発生しました : {}",
        			e.getMessage());
        	
        	//  プログラム内で設定したエラー　
        	
        	throw e; 
        	
        } catch (Exception e) {
        	
        	log.error("[Cloudflare] 予期しないエラーが発生しました。",  e);
        	// JSON解析などのエラ-
        	
        	//詳しい原因を表示する
        	e.printStackTrace();
        	
        	throw new RuntimeException(
        			"cloudflareの回答が処理できませんでした。原因 : "
        			
        			+ e.getMessage(),
        			e
        			);
        }
        }
    }

