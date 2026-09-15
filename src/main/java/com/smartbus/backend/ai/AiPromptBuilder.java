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
                Ban la tro ly ho tro nguoi dung SmartBus (tai xe hoac hanh khach).
                Chi dung CONTEXT ben duoi de tra loi bang tieng Viet tu nhien.
                Chi tra loi truc tiep y chinh cua CAU HOI, toi da 2-4 cau ngan.
                Khong lap lai toan bo CONTEXT, khong liet ke cac truong khong lien quan.
                Khong tu tinh toan lai nghiep vu, khong bia so lieu, khong thay Business Logic.
                Neu CONTEXT co du lieu, bat buoc tra loi cu the (ten ben, so khach, vi tri,...).
                Cac khoa bat dau bang "client." la du lieu hien tai vua chon/dang hien thi tren app.
                Khong duoc tra loi cau xin loi chung chung.

                Cac cau hoi thuong gap (tra loi dua tren CONTEXT):
                - "Ben tiep theo la gi?" -> dung nextStopName
                - "Con bao nhieu khach?" -> dung totalPassengers
                - "Con bao nhieu ben?" -> dung remainingStopsCount
                - "Tom tat chuyen." / "Phan tich chuyen." -> tong hop tu CONTEXT
                - "Danh sach ben?" -> dung stopsOnRoute
                - "Toi dang chon ben nao?" -> dung client.selectedBoardingStopName/client.selectedDestinationStopName
                - "Xe dang o dau?" -> dung vehicleLatitude/vehicleLongitude/vehicleCurrentStopName
                - "Vi tri cua toi?" -> dung client.passengerLatitude/client.passengerLongitude/client.nearbyStopName

                CONTEXT LIEN QUAN (da loc theo y dinh cau hoi):
                %s

                CAU HOI CUA NGUOI DUNG:
                %s
                """.formatted(formatContext(relevantContext(context, sanitizedQuestion)), sanitizedQuestion);
    }

    public String buildSummaryPrompt(Map<String, Object> context) {
        return """
                Ban la tro ly ho tro nguoi dung SmartBus (tai xe hoac hanh khach).
                Hay viet mot ban tom tat/phan tich ngan gon, de hieu bang tieng Viet ve chuyen xe.
                Chi dung CONTEXT. Khong bia them du lieu, khong de xuat thay doi he thong.
                Goi y noi dung: ten tai xe, tuyen, thoi gian, ben hien tai, ben tiep theo, tong khach,
                khach se xuong, so ben con lai, danh sach ben, GPS va cac ben dang chon tren app neu co.
                Khong duoc tra loi cau xin loi chung chung neu CONTEXT co du lieu.

                CONTEXT (database SmartBus + du lieu hien tai tren app):
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
        if (containsAny(q, "dang chon ben", "đang chọn bến", "ben dang chon", "bến đang chọn",
                "ben di", "bến đi", "ben xuong", "bến xuống", "boarding", "destination")) {
            return "Dữ liệu app hiện tại: bến đi đang chọn là "
                    + value(context, "client.selectedBoardingStopName", "chưa chọn")
                    + "; bến xuống đang chọn là "
                    + value(context, "client.selectedDestinationStopName", "chưa chọn")
                    + "; chuyến đang chọn: #"
                    + value(context, "client.selectedTripId", value(context, "tripId", "?"))
                    + ". Dữ liệu backend của chuyến: tuyến "
                    + value(context, "routeCode", "") + " - " + value(context, "routeName", "") + ".";
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
            String answer = "Tổng hành khách trên chuyến: " + value(context, "totalPassengers", "0")
                    + " (số nhóm: " + value(context, "passengerGroupCount", "0") + ").";
            if (containsAny(q, "chi tiet", "chi tiết", "nhom", "nhóm")) {
                answer += " Chi tiết nhóm: " + value(context, "passengerGroups", "(chưa có)") + ".";
            }
            return answer;
        }
        if (containsAny(q, "bao nhieu ben", "bao nhiêu bến", "con bao nhieu ben", "remaining")) {
            return "Còn " + value(context, "remainingStopsCount", "0") + " bến trên tổng "
                    + value(context, "totalStopsOnRoute", "0") + " bến của tuyến "
                    + value(context, "routeCode", "") + ".";
        }
        if (containsAny(q, "danh sach ben", "danh sách bến", "cac ben", "các bến")) {
            return "Tuyến " + value(context, "routeCode", "") + " - " + value(context, "routeName", "")
                    + ". Danh sách bến: " + value(context, "stopsOnRoute", "(chưa có)") + ".";
        }
        if (containsAny(q, "tuyen", "tuyến")) {
            return "Bạn đang ở tuyến " + value(context, "routeCode", "chưa xác định")
                    + " - " + value(context, "routeName", "chưa xác định") + ".";
        }
        if (containsAny(q, "xe dang o dau", "xe đang ở đâu", "xe o dau", "xe ở đâu",
                "vi tri xe", "vị trí xe", "vi tri cua xe", "vị trí của xe",
                "toa do xe", "tọa độ xe")) {
            String latitude = value(context, "vehicleLatitude", value(context, "currentLatitude", "—"));
            String longitude = value(context, "vehicleLongitude", value(context, "currentLongitude", "—"));
            String stop = value(context, "vehicleCurrentStopName", value(context, "currentStopName", "chưa xác định"));
            if ("—".equals(latitude) || "—".equals(longitude)) {
                return "Chưa nhận được vị trí mới nhất của xe. Bến hiện tại gần nhất là " + stop + ".";
            }
            return "Xe đang ở gần " + stop + ", tọa độ khoảng " + latitude + ", " + longitude + ".";
        }
        if (containsAny(q, "gps", "vi tri", "vị trí", "toa do", "tọa độ", "location")) {
            return "Vị trí của bạn trên app: lat=" + value(context, "client.passengerLatitude", value(context, "client.currentLatitude", "—"))
                    + ", lng=" + value(context, "client.passengerLongitude", value(context, "client.currentLongitude", "—"))
                    + ". Bến gần/gợi ý trên app: "
                    + value(context, "client.nearbyStopName", value(context, "currentStopName", "chưa xác định"))
                    + "; khoảng cách: "
                    + value(context, "client.nearbyStopDistanceMeters", value(context, "nearestStopDistanceMeters", "—"))
                    + " m.";
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
        return "Mình chưa xác định được đúng thông tin bạn cần hỏi từ context hiện tại. "
                + "Bạn có thể hỏi cụ thể về bến tiếp theo, tổng khách, số bến còn lại, GPS, "
                + "tuyến hoặc bến đi/bến xuống.";
    }

    private Map<String, Object> relevantContext(Map<String, Object> context, String question) {
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        if (context == null || context.isEmpty()) return result;
        String q = question.toLowerCase(Locale.ROOT);

        include(result, context, "tripId", "routeId", "status", "selectionState",
                "routeCode", "routeName", "currentTripId", "currentRouteId");
        include(result, context, "client.selectedTripId", "client.selectedRouteId",
                "client.selectionState", "client.checkInStatus");

        boolean asksPassenger = containsAny(q, "khach", "khách", "hanh khach", "hành khách",
                "check-in", "check in", "checkin", "passenger");
        boolean asksStops = containsAny(q, "danh sach ben", "danh sách bến", "cac ben", "các bến",
                "nhung ben", "những bến");
        boolean asksPosition = containsAny(q, "gps", "vi tri", "vị trí", "toa do", "tọa độ",
                "xe dang o", "xe đang ở", "location");
        boolean asksSelected = containsAny(q, "ben di", "bến đi", "ben xuong", "bến xuống",
                "dang chon", "đang chọn", "boarding", "destination");
        boolean asksCurrent = containsAny(q, "ben hien tai", "bến hiện tại", "dang o ben", "đang ở bến",
                "ben tiep theo", "bến tiếp theo", "next stop", "con bao nhieu ben", "còn bao nhiêu bến");

        if (asksPassenger) {
            include(result, context, "totalPassengers", "passengerTotal", "passengerTotalOnBoard",
                    "passengerGroupCount", "checkInCount", "passengersAlightingAtCurrentStop",
                    "passengersAlightingAtNextStop");
            if (containsAny(q, "chi tiet", "chi tiết", "nhom", "nhóm")) {
                include(result, context, "passengerGroups", "client.passengerGroups");
            }
        }
        if (asksStops) include(result, context, "stopsOnRoute", "totalStopsOnRoute");
        if (asksPosition) include(result, context, "currentLatitude", "currentLongitude",
                "vehicleLatitude", "vehicleLongitude", "vehicleCurrentStopName", "vehicleNextStopName",
                "nearestStopDistanceMeters", "client.currentLatitude", "client.currentLongitude",
                "client.passengerLatitude", "client.passengerLongitude", "client.nearbyStopName",
                "client.nearbyStopDistanceMeters");
        if (asksSelected) include(result, context, "client.selectedBoardingStopName",
                "client.selectedDestinationStopName", "client.selectedBoardingStopId",
                "client.selectedDestinationStopId", "client.boardingStopName", "client.destinationStopName");
        if (asksCurrent) include(result, context, "currentStopName", "currentStopOrder",
                "nextStopName", "nextStopOrder", "remainingStopsCount",
                "passengersAlightingAtCurrentStop", "passengersAlightingAtNextStop");
        if (containsAny(q, "thoi gian", "thời gian", "bat dau", "bắt đầu", "ket thuc", "kết thúc",
                "trang thai", "trạng thái")) {
            include(result, context, "startedAt", "endedAt", "tripStartedAt", "tripEndedAt", "status");
        }
        if (result.size() <= 7) {
            include(result, context, "currentStopName", "nextStopName", "totalPassengers");
        }
        return result;
    }

    private void include(Map<String, Object> target, Map<String, Object> source, String... keys) {
        for (String key : keys) {
            if (source.containsKey(key) && source.get(key) != null) target.put(key, source.get(key));
        }
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
                + " Dữ liệu app: bến đi đang chọn "
                + value(context, "client.selectedBoardingStopName", "chưa chọn")
                + ", bến xuống đang chọn "
                + value(context, "client.selectedDestinationStopName", "chưa chọn")
                + ", vị trí app lat="
                + value(context, "client.currentLatitude", "—")
                + ", lng="
                + value(context, "client.currentLongitude", "—")
                + ". Nhóm: " + value(context, "passengerGroups", "(chưa có)") + ".";
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
