package com.huaxing.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/** luohuai codeX generate: atomic database counters allocate the final three barcode digits. */
@Mapper
public interface BarcodeSequenceMapper {
    @Insert("INSERT IGNORE INTO barcode_sequence(prefix, current_value) VALUES(#{prefix}, 0)")
    int ensureExists(String prefix);

    @Update("UPDATE barcode_sequence SET current_value = current_value + 1 WHERE prefix = #{prefix} AND current_value < 999")
    int increment(String prefix);

    @Select("SELECT current_value FROM barcode_sequence WHERE prefix = #{prefix}")
    Integer currentValue(String prefix);
}
