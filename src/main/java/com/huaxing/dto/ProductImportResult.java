package com.huaxing.dto;

/** luohuai codeX generate: concise confirmation result excludes stock because imports never create inventory. */
public record ProductImportResult(int productsCreated, int skusCreated, String dealerName) { }
