package netcore;

import arc.util.Http;
import arc.util.Log;
import arc.util.serialization.Json;
import arc.util.serialization.JsonValue;
import mindustry.Vars;

public class NetCoreClient {
    private static final String SERVER_URL = "http://135.106.218";

    public interface MapListCallback {
        void run(JsonValue maps);
    }

    public static void fetchMaps(String mode, MapListCallback callback) {
        Http.get(SERVER_URL + "/maps/" + mode.toLowerCase())
            .submit(response -> {
                String resString = response.getResultAsString();
                JsonValue parsedJson = new Json().parse(resString);
                arc.Core.app.post(() -> callback.run(parsedJson));
            }, error -> {
                Log.err("NetCore: Ошибка загрузки списка карт", error);
                arc.Core.app.post(() -> Vars.ui.showErrorMessage("Ошибка сети: не удалось связаться с сервером NetCore."));
            });
    }

    public static void sendAction(int mapId, String actionType) {
        String jsonBody = "{\"action\":\"" + actionType + "\"}";
        Http.post(SERVER_URL + "/maps/" + mapId + "/action")
            .header("Content-Type", "application/json")
            .content(jsonBody)
            .submit(res -> {}, err -> Log.err("NetCore: Не удалось обновить статистику"));
    }

    public static void adminReview(String token, int mapId, String difficulty, int stars, Runnable onSuccess) {
        String jsonBody = String.format(
            "{\"token\":\"%s\",\"mapId\":%d,\"difficulty\":\"%s\",\"stars\":%d}",
            token, mapId, difficulty, stars
        );

        Http.post(SERVER_URL + "/admin/review")
            .header("Content-Type", "application/json")
            .content(jsonBody)
            .submit(response -> {
                arc.Core.app.post(() -> {
                    Vars.ui.showInfo("Данные карты успешно обновлены!");
                    if (onSuccess != null) onSuccess.run();
                });
            }, error -> {
                arc.Core.app.post(() -> Vars.ui.showErrorMessage("Ошибка модерации! Неверный токен или сервер недоступен."));
            });
    }
}
