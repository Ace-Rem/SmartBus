package com.smartbus.backend.ai;

import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class AiPromptBuilder {

    public String buildChatPrompt(Map<String, Object> context, String question) {
        String sanitizedQuestion = sanitize(question);
        return """
                Ban la tro ly ho tro tai xe SmartBus.
                Chi dung CONTEXT ben duoi de tra loi bang tieng Viet tu nhien.
                Khong tu tinh toan lai nghiep vu, khong bia so lieu, khong thay Business Logic.

                Cac cau hoi thuong gap (tra loi dua tren CONTEXT):
                - "Ben tiep theo la gi?" -> dung nextStopName
                - "Con bao nhieu khach?" -> dung totalPassengers
                - "Con bao nhieu ben?" -> dung remainingStopsCount
                - "Tom tat chuyen." / "Phan tich chuyen." -> tong hop tu CONTEXT

                CONTEXT:
                %s

                CAU HOI CUA TAI XE:
                %s
                """.formatted(formatContext(context), sanitizedQuestion);
    }

    public String buildSummaryPrompt(Map<String, Object> context) {
        return """
                Ban la tro ly ho tro tai xe SmartBus.
                Hay viet mot ban tom tat/phan tich ngan gon, de hieu bang tieng Viet ve chuyen xe.
                Chi dung CONTEXT. Khong bia them du lieu, khong de xuat thay doi he thong.
                Goi y noi dung: ten tuyen, thoi gian, ben hien tai, ben tiep theo, tong khach,
                khach se xuong, so ben con lai, GPS neu co.

                CONTEXT:
                %s
                """.formatted(formatContext(context));
    }

    private String formatContext(Map<String, Object> context) {
        if (context == null || context.isEmpty()) {
            return "(empty)";
        }
        return context.entrySet().stream()
                .map(entry -> "- " + entry.getKey() + ": " + String.valueOf(entry.getValue()))
                .collect(Collectors.joining("\n"));
    }

    private String sanitize(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\r", " ").trim();
    }
}
