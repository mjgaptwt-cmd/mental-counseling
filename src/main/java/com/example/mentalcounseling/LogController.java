


package com.example.mentalcounseling;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;


	//ユーザーのブラウザ（画面）とデータベース（データ保管庫）を連携する


@Controller
public class LogController  {

	
	
	//  データーベースへの保存・取得を担当とするクラス
		@Autowired
	private LogRepository  logRepository;
		
		//  AI①、AI②、AI⓷をまとめて動かるサービス
		@Autowired
		private CounselingService counselingService;
		
		//　ホームページを開いた時に実行される
			@GetMapping("/")
			public String index(Model model) {				
				
			//データーベースから記録を実行される
				List<Log> logs = logRepository.findAll();
				
			 // 新しい記録が上に表示されるように並び替える
				logs.sort(Comparator.comparing(Log::getId).reversed());  
				
			//  習得した記録をHTMLに渡す
			model.addAttribute("logs", logs);
			
			//"index"という名の画面（HTML）を表示する
			return "index";
			
		}
			
			//  登録ボタンが押されたときに実行される
			@PostMapping("/add-with-ai")
			public String addLogWithAi(
					
					// ブラウザから本文を受け取る
					@RequestParam("content") String content,
					
					// ブラウザからがんばったことを受け取る
					@RequestParam("effort") String effort) {
					
				
				
				 // ==========================================
		        // ① AI①・AI②・AI③による分析と最終回答の作成
		        // ==========================================
						
				 // CounselingServiceに処理を依頼する
		        // Geminiが失敗した場合はGroqが代わりに分析する
		        // その分析結果をCloudflareが最終回答にまとめる
				
				CounselingService.CounselingResult  result = 
						counselingService.analyzeAndCreateReport(content, effort);
					
			// AI①またはAI②の分析結果取得
				AiAnalysisResult  aiResult = result.getAiResult();
				
			// AI⓷が作成した相談者向けの最終回答を取得
				String detailedReport = result.getDetailedReport();
						
				
				// ==========================================
		        // 	データベースに保存するLogの箱を作る
		        // ==========================================
				
				
				// 	新しいLogの箱作成　
				Log	log	=	new	Log();   
				
				//  相談者が入力した内容を保存
					log.setContent(content);
					
				// 相談者ががんばったことを保存	
					log.setEffort(effort);
								
					//  記録した日時を習得して保存
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
					log.setCreatedAt(LocalDateTime.now().format(formatter));
					
				//  AI が分析して返した危険度を保存する java側で受け取る
					log.setDangerLevel(aiResult.getDangerLevel());
					
				// AIが分析した要約を保存する　java側で受け取る
					log.setAiSummary(aiResult.getSummary());
					
					
					// ==========================================
			        //	 AI③の最終回答を保存
			        // ==========================================
					log.setDetailedReport(detailedReport);
					
					 // ==========================================
			        //  データベースに登録する
			        // ==========================================
					
					
					// 5.	データーベースに保存する (INSERT)する
					logRepository.save(log);
				
				// 6.	登録が終わったら、一覧画面（トップページ）に強制移動（リダイレクト）する
					return "redirect:/";
				
			}
			
}
