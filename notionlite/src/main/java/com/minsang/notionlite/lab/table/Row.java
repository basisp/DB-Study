package com.minsang.notionlite.lab.table;

/**
 * One logical row in our learning table.
 *
 * id: clustered primary key
 * title/content: payload columns
 */
public record Row(long id, String title, String content) {
}
