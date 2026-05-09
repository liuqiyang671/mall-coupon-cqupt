package com.mall.cqupt.merchant.admin.task;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.util.ListUtils;
import com.github.javafaker.Faker;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * File-generation integration test; writes an Excel file and is excluded from default mvn test.
 */
@Tag("integration")
public final class ExcelGenerateIT {

    private final int writeNum = 5001;
    private final Faker faker = new Faker(Locale.CHINA);
    private final String excelPath = Paths.get("").toAbsolutePath().getParent() + "/tmp";

    @Test
    @Timeout(value = 1, unit = TimeUnit.MINUTES)
    public void testExcelGenerate() {
        if (!FileUtil.exist(excelPath)) {
            FileUtil.mkdir(excelPath);
        }
        String fileName = excelPath + "/oneCouponTaskPushExcel.xlsx";
        EasyExcel.write(fileName, ExcelGenerateDemoData.class)
                .sheet("coupon-task-push-list")
                .doWrite(data());
    }

    private List<ExcelGenerateDemoData> data() {
        List<ExcelGenerateDemoData> list = ListUtils.newArrayList();
        for (int i = 0; i < writeNum; i++) {
            ExcelGenerateDemoData data = ExcelGenerateDemoData.builder()
                    .mail(faker.number().digits(10) + "@163.com")
                    .phone(faker.phoneNumber().cellPhone())
                    .userId(IdUtil.getSnowflakeNextIdStr())
                    .build();
            list.add(data);
        }
        return list;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    static class ExcelGenerateDemoData {

        @ColumnWidth(30)
        @ExcelProperty("userId")
        private String userId;

        @ColumnWidth(20)
        @ExcelProperty("phone")
        private String phone;

        @ColumnWidth(30)
        @ExcelProperty("mail")
        private String mail;
    }
}
