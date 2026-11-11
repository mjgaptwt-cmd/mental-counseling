
package com.example.survivorslog;


import java.util.ArrayList;
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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeminiAnalysisService {
	
	// 　コンソールに処理状況を表示するためのログ機能
	private static final Logger log = 
			LoggerFactory.getLogger(GeminiAnalysisService.class);
		
	//  Gemini APIキー３個
	@Value("${gemini.api.key}")
	private String apiKey1;
	
	@Value("${gemini.api.key2}")
	private String apiKey2;
	
	
	@Value("${gemini.api.key3}")
	private String apiKey3;
	
	//  使用するGeminiのモデル名を取得
	// 現在はgemini-3.6flash
	@Value("${gemini.api.model}")
	private String model;
	
	// =====================================
    //  通信とJSON処理に使用する変数
    // =====================================

	//　Gemini　APIと通信するためのオブジェクト
	private final RestTemplate restTemplate;
	
	// JSONデータを読み書きするためのオブジェクト
	private final ObjectMapper objectMapper;
	
	// =====================================
    // 初期設定
    // =====================================
	
	//  クラスが作成されたときに実行される
	public GeminiAnalysisService() {
		
		
		// HTTP通信のタイムアウト時間を指定　
		SimpleClientHttpRequestFactory factory =
		new SimpleClientHttpRequestFactory();
		
		//  接続が確立するまでの制限時間 :10秒
		factory.setConnectTimeout(10000);
		
//		AIから返事が届くまでの制限時間 : 15秒
		factory.setReadTimeout(15000);
		
		
		
		this.restTemplate = new RestTemplate(factory);
		// JSON処理用のオブジェクトを作成
		this.objectMapper = new ObjectMapper();
	}
	
	// =====================================
    // Geminiによる相談記録の分析
    // =====================================
	
	// content : ユーザーが入力した相談内容
	// effort  : ユーザーががんばったこと　
	
	
	
	//  分析が成功するとAiAnalysisResultを探す
	public AiAnalysisResult analyzeLog(
			String content,
			String effort) {
		
		// Geminiによる分析開始をコンソールに表示
		log.info("[Gemini] 相談記録を分析開始しました。 ");
		//  APIキーを順番に試すためのリスト
		List<String> apiKeys = new ArrayList<>();
		
		
		// 1個目のAPIキーが存在する場合に増加
		if (apiKey1 != null && !apiKey1.isBlank()) {
		apiKeys.add(apiKey1);
	}
	
	//  2個目のAPIキーが存在する場合に追加
		if (apiKey2 != null && !apiKey2.isBlank()) {
			apiKeys.add(apiKey2);
		}
	

		//  3個目のAPIキーが存在する場合に追加
			if (apiKey3 != null && !apiKey3.isBlank()) {
				apiKeys.add(apiKey3);
			}
			
		
	//  APIキーが1個も登録されていない場合は処理を終了
		if (apiKeys.isEmpty()) {
			throw new RuntimeException(
					"Gemini APIキーが設定されていません。");		
		}
		
	//  使用可能なAPIキーの数をコンソールに表示
		log.info("[Gemini] 使用可能なAPIキー{}個確認しました。",
					apiKeys.size());
		
	
        // =====================================
        // Geminiに送る分析指示を作成
        // =====================================

		
	//  AIに相談記録を分籍してもらうための文章
		String prompt = """
	
	あなたは生活・仕事・学校の悩みを

					  丁寧に整理するサポートAIです。

					  入力内容を分析してください。

					  必ず以下のJSON形式だけで回答してください。

					  {

					    "dangerLevel": "注意 (Lv.1 : 軽度ストレス)",

					    "summary": "具体的な分析とアドバイス"

					  }

					  危険度は次の5段階です。

					  注意 (Lv.1 : 軽度ストレス)

					  軽微 (Lv.2 : 疲労蓄積)

					  要警戒 (Lv.3 : 心身の限界注意)

					  危険 (Lv.4 : 早期相談・休養必要)

					  極めて危険 (Lv.5 : 危険)

					  summaryには状況の整理と具体的なアドバイスを

					  記載してください。
					危険度は参考情報として扱ってください。
					
					 【文章品質】
//    自然で読みやすい日本語で回答してください。
    助詞の重複、不要な文字、脱字がないか
    確認してから回答してください。
    不自然な文章は自然な日本語に修正してください。
//    危険度は参考情報として扱ってください。
　・相談先の電話番号を書くときは、よりそいホットライン 0120-279-338(24時間)、
#いのちSOS 0120-061-338(24時間)、緊急時は119だけを使い、これ以外の番号は書かない
					
			  """;
		
		
		//　Geminに送る相談内容を作成
		String requestText = "【本文】 \n" + content
				+ "\n【がんばったこと】 \n" + effort;
		
		// ==========================================
		//  Geminiへ送信するJSONデータを作成
		//===========================================
		
		Map<String, Object> requestBody = Map.of(
				
				// AIへの分析指示
				"systemInstruction",
				Map.of(
						"parts", List.of(
								Map.of("text", prompt)
								)
				),
		
				//		AIに渡す相談内容
				"contents",
				List.of(
						Map.of(
								"parts", List.of(
										Map.of("text",requestText)	
										)
							)
				),
									
				// AIの回答に関する設定　
				"generationConfig", Map.of(
						
						// 最大回答トークン数
						"maxOutputTokens", 3000,
						
						// JSON形式で回答するように指定
						"responseMimeType", "application/json"
						)
				);
				
		//=====================================
		//Iキーを順番に試す
		//=====================================
		
		
		// 登録されているAPIキーを先頭から順番に使用
		for (int i = 0; i < apiKeys.size(); i++) {
			String keyNo = "キー" + (i + 1) + "番目(全" + apiKeys.size() + "個中)";
			try {
				
				
				// 何番目のAPIキーで分析を開始するか表示
			System.out.println("[AI① Gemini] " + keyNo + " 実行中…");
				
				//  現在のAPIキーでGeminiに分析を依頼
			String responseText = 
					sendRequest(apiKeys.get(i), requestBody);
			
			// Geminiから返されたJSONを読み取る
				JsonNode result = 
						objectMapper.readTree(responseText);
				
				
				// JSONから危険度を取得
				String dangerLevel = 
						result.path("dangerLevel").asText("");
			
			//  JSONから分析結果を取得
				String summary = 
						result.path("summary").asText("");
				
				//  危険度と分析結果が空の場合は失敗とする
				if (dangerLevel.isBlank()
						|| summary.isBlank()) {
					
					throw new RuntimeException(
							"Geminiの分析結果が空です。");				
				}
			
			//  使用を認める危険度を取得
				List<String> validLevels = List.of(
						 "注意 (Lv.1 : 軽度ストレス)",
					        "軽微 (Lv.2 : 疲労蓄積)",
					        "要警戒 (Lv.3 : 心身の限界注意)",
					        "危険 (Lv.4 : 早期相談・休養必要)",
					        "極めて危険 (Lv.5 : 危険)"
						);
						
				// 指定した5段階以外の危険度は受け付けない
				if (!validLevels.contains(dangerLevel)) {
					
					throw new RuntimeException(
							"Geminiが不正な危険度を返しました。");
				}
				
				//======================================
						
					//　分析成功
				
				//=====================================
				
				
				//  Gemini何番目APIの分析成功かをコンソールに表示
				System.out.println("[AI① Gemini] " + keyNo + " 成功");
				
					//  分析結果・危険度・使用したAI名をまとめて返す
				return new AiAnalysisResult(
						summary,
						dangerLevel,
						"Gemini 3.6 Flash"
						);
						
			
				
				//  失敗したAPIキーの番号と原因をコンソールに表示
				
				
			} catch (HttpClientErrorException.TooManyRequests e) {

				// 429(無料枠の上限)の場合。原因を1行だけ表示する
				System.out.println("[AI① Gemini] " + keyNo + " 失敗(429 無料枠の上限)");

			} catch (HttpStatusCodeException e) {

				// 429以外のHTTPエラー(401 キー不正、500 サーバー障害など)
				System.out.println("[AI① Gemini] " + keyNo + " 失敗(HTTP " + e.getStatusCode().value() + ")");
			} catch (Exception e) {

				// それ以外のエラー(応答が空、危険度が不正など)
				System.out.println("[AI① Gemini] " + keyNo + " 失敗(" + e.getMessage() + ")");
				// 詳しい原因は、必要なときにだけ見られるようにする
				log.debug("[Gemini] 詳細", e);
			}
		}
        // =====================================
        // ⑨ 3個すべてのAPIキーが失敗した場合
        // =====================================
		log.error("[Gemini] すべてのAPIキーで分析に失敗しました。");
		
		//　AI　Groqへの受け取りへの切り替えを担当する
		throw new RuntimeException(
				"GeminiのAPIキー3個すべて分析に失敗しました。"
				+ " AI②のGroqへ切替えてください");
	}
		
	// =====================================
    //  Gemini APIとの通信処理
    // =====================================

		//apikey : 今回使用するAPIキー
		// requestBody : Geminiに送る分析データ
		// 
	//  Geminiから取得した分析文章を返す
	private String sendRequest(
			String apiKey,
			Map<String, Object> requestBody) {
		
		// HTTPリクエストのヘッダーを作成
		HttpHeaders headers = new HttpHeaders();
		
		//　送信するデータはJSON形式
		headers.setContentType(MediaType.APPLICATION_JSON);
		
		//  Gemini APIキーをヘッダーに設定
		headers.set("x-goog-api-key", apiKey);
		
		//  ヘッダーと送信データをまとめる
		HttpEntity<Map<String, Object>> request = 
				new HttpEntity<>(requestBody, headers);
		
		//  Gemini　APIのURLを作成
		String url = 
				"https://generativelanguage.googleapis.com/v1beta/models/"
				+ model + ":generateContent";
		
		//  Geminiへの送信開始を表示
		log.info("[Gemini] APIへのリクエストを送信します");

		
		// Gemini APIにPOSTリクエストを送信
		ResponseEntity<String> response = 
				restTemplate.postForEntity(
						url,
						request,
						String.class
				);
		
		// Geminiから応答を受信したことを表示
		log.info("[Gemini ] HTTP応答を受信しました。　ステータス : {}",
				response.getStatusCode().value());
		
		//  Geminiから何も返ってこなかった場合は失敗
		if (response.getBody() == null
				|| response.getBody().isBlank()) {
			throw new RuntimeException(
					"Geminiから空の応答されました");
		}
		
		// =====================================
        // Geminiの応答から分析文章を取り出す
        // =====================================

		try {
			// Geminiから返されたJSONを解析
			JsonNode root = 
							objectMapper.readTree(response.getBody());
			
					//   JSONの中からAIの回答本文を取り出す
							String aiText = root
									.path("candidates")
									.path(0)
									.path("content")
									.path("parts")
									.path(0)
									.path("text")
									.asText("");
							
							// 　回答本文が空なら失敗
							if (aiText.isBlank()) {
								
								throw new RuntimeException(
										"Geminiの回答が空です。");
							}
							
							//  取得した回答本文を呼び出した元に戻す
							return aiText;
							
		} catch (Exception e) {
			
			// JSONの解析に失敗した原因をコンソールに表示
			log.error("[Gemini] 応答JSONの解析に失敗しました。 ", e);
			
			// 元のエラー情報も保持して呼び出し元に返す
			throw new RuntimeException(
					"Geminiの応答を解析できませんでした。",
						e
				
			);
		}
				
	}
	
}
	
	
	
	
	
	
	
	
	
	
	
	
	
	