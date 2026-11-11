package com.example.mentalcounseling;



		//AI①・AI②の分析結果を保持するクラス


public class AiAnalysisResult {
	
	//  AIが作成した分析・相談回答
	private final String summary;
	
	// AIが判定した危険度　
	private final String dangerLevel;
	
	//分析を担当したAIの名前
	private final String provider;
	
	//3つの情報を受け取って保存する
	public AiAnalysisResult(
			String summary,
			String dangerLevel,
			String provider) {
		
		this.summary = summary ;
		this.dangerLevel = dangerLevel;
		this.provider = provider;
		
	}
			
		//分析結果を取得する　
	public String getSummary() {
		return 	summary;
	}
	
	//  危険度を取得する
		public String getDangerLevel() {
			return dangerLevel;
		}
	
	// 　使用したAIの名前を取得する
		public String getProvider() {
			return provider;
			
		}
	
}
