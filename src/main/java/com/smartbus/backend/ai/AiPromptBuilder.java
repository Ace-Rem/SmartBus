package com.smartbus.backend.ai;

import java.util.Locale;
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
                Neu CONTEXT co du lieu, bat buoc tra loi cu the (ten ben, so khach,...).
                Khong duoc tra loi cau xin loi chung chung.

                Cac cau hoi thuong gap (tra loi dua tren CONTEXT):
                - "Ben tiep theo la gi?" -> dung nextStopName
                - "Con bao nhieu khach?" -> dung totalPassengers
                - "Con bao nhieu ben?" -> dung remainingStopsCount
                - "Tom tat chuyen." / "Phan tich chuyen." -> tong hop tu CONTEXT
                - "Danh sach ben?" -> dung stopsOnRoute

                CONTEXT (lay tu database SmartBus):
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
                Goi y noi dung: ten tai xe, tuyen, thoi gian, ben hien tai, ben tiep theo, tong khach,
                khach se xuong, so ben con lai, danh sach ben, GPS neu co.
                Khong duoc tra loi cau xin loi chung chung neu CONTEXT co du lieu.

                CONTEXT (lay tu database SmartBus):
                %s
                """.formatted(formatContext(context));
    }

    /**
     * Data-driven answer from DB context when the LLM provider is unavailable.
     */
    public String buildDataDrivenAnswer(Map<String, Object> context, String question) {
        if (context == null || context.isEmpty()) {
            return "Chưa có dữ liệu chuyến để trả lời. Hãy bắt đầu chuyến trước.";
        }
        String q = sanitize(question).toLowerCase(Locale.ROOT);
        if (q.isBlank() || containsAny(q, "tom tat", "tóm tắt", "phan tich", "phân tích", "summary")) {
            return buildSummaryFromContext(context);
        }
        if (containsAny(q, "ben tiep", "bến tiếp", "next stop", "diem den ke", "điểm đến kế")) {
            return "Bến tiếp theo: " + value(context, "nextStopName", "chưa xác định")
                    + " (thứ tự " + value(context, "nextStopOrder", "—") + "). "
                    + "Khách sẽ xuống ở bến này: "
                    + value(context, "passengersAlightingAtNextStop", "0") + ".";
        }
        if (containsAny(q, "ben hien", "bến hiện", "current stop", "dang o ben", "đang ở bến")) {
            return "Bến hiện tại (gần xe nhất): " + value(context, "currentStopName", "chưa xác định")
                    + ". Khoảng cách ước lượng: "
                    + value(context, "nearestStopDistanceMeters", "—")
                    + " m. Khách sẽ xuống tại đây: "
                    + value(context, "passengersAlightingAtCurrentStop", "0") + ".";
        }
        if (containsAny(q, "bao nhieu khach", "bao nhiêu khách", "tong khach", "tổng khách",
                "hanh khach", "hành khách", "passenger")) {
            return "Tổng hành khách trên chuyến: " + value(context, "totalPassengers", "0")
                    + " (số nhóm: " + value(context, "passengerGroupCount", "0") + "). "
                    + "Chi tiết nhóm: " + value(context, "passengerGroups", "(chưa có)") + ".";
        }
        if (containsAny(q, "bao nhieu ben", "bao nhiêu bến", "con bao nhieu ben", "remaining")) {
            return "Còn " + value(context, "remainingStopsCount", "0") + " bến trên tổng "
                    + value(context, "totalStopsOnRoute", "0") + " bến của tuyến "
                    + value(context, "routeCode", "") + ".";
        }
        if (containsAny(q, "danh sach ben", "danh sách bến", "cac ben", "các bến", "tuyen", "tuyến")) {
            return "Tuyến " + value(context, "routeCode", "") + " - " + value(context, "routeName", "")
                    + ". Danh sách bến: " + value(context, "stopsOnRoute", "(chưa có)") + ".";
        }
        if (containsAny(q, "gps", "vi tri", "vị trí", "toa do", "tọa độ", "location")) {
            return "GPS hiện tại: lat=" + value(context, "currentLatitude", "—")
                    + ", lng=" + value(context, "currentLongitude", "—") + ".";
        }
        if (containsAny(q, "an toan", "an toàn", "safety", "kiem tra")) {
            return "Gợi ý an toàn: kiểm tra cửa xe, dây an toàn, tốc độ phù hợp khu dân cư; "
                    + "theo dõi thông báo bến và số khách sẽ xuống. "
                    + buildSummaryFromContext(context);
        }
        if (containsAny(q, "tac nghen", "tắc", "traffic", "ket xe", "kẹt")) {
            return "Gợi ý khi tắc đường: giữ khoảng cách, báo cáo qua điều hành nếu chậm nhiều, "
                    + "ưu tiên an toàn hành khách. Trạng thái chuyến hiện tại — "
                    + buildSummaryFromContext(context);
        }
        return buildSummaryFromContext(context)
                + "\n\n(Bạn có thể hỏi: bến tiếp theo, tổng khách, số bến còn lại, hoặc GPS.)";
    }

    private String buildSummaryFromContext(Map<String, Object> context) {
        return "Tóm tắt chuyến #" + value(context, "tripId", "?")
                + " (" + value(context, "status", "") + "): tài xế "
                + value(context, "driverName", "—")
                + ", tuyến " + value(context, "routeCode", "") + " - " + value(context, "routeName", "")
                + ", bắt đầu " + value(context, "startedAt", "—")
                + ". Bến hiện tại: " + value(context, "currentStopName", "chưa xác định")
                + "; bến tiếp theo: " + value(context, "nextStopName", "chưa xác định")
                + ". Tổng khách: " + value(context, "totalPassengers", "0")
                + "; khách xuống bến hiện tại: " + value(context, "passengersAlightingAtCurrentStop", "0")
                + "; còn " + value(context, "remainingStopsCount", "0") + "/"
                + value(context, "totalStopsOnRoute", "0") + " bến."
                + " Nhóm: " + value(context, "passengerGroups", "(chưa có)") + ".";
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

    private boolean containsAny(String haystack, String... needles) {
        for (String needle : needles) {
            if (haystack.contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private String value(Map<String, Object> context, String key, String defaultValue) {
        Object raw = context.get(key);
        if (raw == null) {
            return defaultValue;
        }
        String text = String.valueOf(raw);
        return text.isBlank() || "null".equals(text) ? defaultValue : text;
    }
}
