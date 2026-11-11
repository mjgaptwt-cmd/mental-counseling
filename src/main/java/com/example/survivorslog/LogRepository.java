package com.example.survivorslog;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

//					データーベース用のムズイ言語を一行も書かずに、データを操作が可能になるjava

//		LogRepository.java は 「その用紙をファイリングしたり、書庫から引っ張り出してきたりしてくれる専属の
//		優秀な図書館スタッフ」 のような存在


	@Repository
	public interface LogRepository extends JpaRepository<Log,	Long>	{
			
		//	ここに特別な検索方法を書くこともできるが、
		//	基本的な「保存・全件習得・削除」などは、JpaRepositoryがすべて自動で用意する

		
	
}
