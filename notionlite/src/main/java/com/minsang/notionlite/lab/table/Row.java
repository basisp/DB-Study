package com.minsang.notionlite.lab.table;

/**
 * One logical row in our learning table.
 *
 * id: clustered primary key
 * title/content: payload columns
 */
// record는 "불변 데이터 한 묶음"을 간단히 표현할 때 유용합니다.
public record Row(long id, String title, String content) {
}
