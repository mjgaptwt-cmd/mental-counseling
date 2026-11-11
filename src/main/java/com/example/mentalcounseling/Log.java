package  com.example.mentalcounseling;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

//   　　　　 データーベースに保存するデータの形（設計図）を決めるためのjava  テンプレート的なイメージ


@Entity
@Table(name = "survivor_logs")
public class Log {

	
	
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private  Long id;                            // 管理番号（自動で1,2,3…採番される）
	
	private String title;                     			  //  タイトル
	private String content;							//	本文・詳細メモ
	private String effort;								//自分が頑張ったこと
	private String createdAt;                       //記録した日付
	
	
	//    ◇　ここから下がAI分析結果を保存するための新しい項目！
	@Column(length = 1000)				//文字数の上限を1000文字に拡張
	private String aiSummary; 			//AIによる要約・分析
	private String  dangerLevel;			// 危険度　（Lv.1～Lv.5）（例: "注意", "要警戒", "極めて危険");
	
	
	// AI③による相談者向けのわかりやすくまとめた回答
	@Column(length = 10000)
	private String detailedReport;  // 詳しいアドバイス；支援情報など
	
	// -----Getter (値の取得用）---
	
	//  Getter とSetter (値の取得・設定用メソッド）
	
	
	public Long getId() {
		return id;
	}
	public String getTitle() {
		return title;
	}
	public String getContent() {
		return content;
	}
	
	public String getEffort() {
		return effort;
	}
	
	public String getCreatedAt() {
		return createdAt;
	}
	
		
	public String getAiSummary() {
		return aiSummary;
	}
	
	public String getDangerLevel() {
		return dangerLevel;
	}
	
	public String getDetailedReport() {
		return detailedReport;
	}
	
	
	//----Setter (値の設定用）---
	
	public void setId(Long id) {
		this.id = id;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public void setContent(String content) {
		this.content = content;
	}
	
	public void setEffort(String effort) {
		this.effort = effort;
	}	
	
	public void setCreatedAt(String createdAt) {
		this.createdAt = createdAt;
	}
	
	public void setAiSummary(String aiSummary) {
		this.aiSummary = aiSummary;
	}
	
	public void setDangerLevel(String dangerLevel) {
		this.dangerLevel = dangerLevel;
	}
	
	
public void setDetailedReport(String detailedReport) {
	this.detailedReport = detailedReport;
	}

}





