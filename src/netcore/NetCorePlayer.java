package netcore;

import arc.Core;
import arc.Events;
import arc.files.Fi;
import arc.util.Http;
import arc.util.Log;
import arc.util.serialization.JsonValue;
import mindustry.Vars;
import mindustry.game.EventType.StateChangeEvent;
import mindustry.maps.Map;

public class NetCorePlayer {
    private static final String DOWNLOAD_URL = "http://135.106.218";
    private static Fi tempFile;
    private static boolean isListening = false;

    public static void playMap(JsonValue mapData) {
        int mapId = mapData.getInt("id");
        String fileName = mapData.getString("file_name");
        
        tempFile = Vars.dataDirectory.child("maps/netcore_temp_" + mapId + ".msav");

        Vars.ui.loadAnd("Загрузка карты...", () -> {
            Http.get(DOWNLOAD_URL + fileName)
                .submit(response -> {
                    byte[] bytes = response.getResult();
                    tempFile.writeBytes(bytes);
                    
                    Core.app.post(() -> {
                        Vars.ui.loadHide();
                        launchMap();
                    });
                }, error -> {
                    Core.app.post(() -> {
                        Vars.ui.loadHide();
                        Vars.ui.showErrorMessage("Не удалось скачать карту.");
                    });
                });
        });
    }

    private static void launchMap() {
        if (tempFile == null || !tempFile.exists()) return;

        try {
            Map map = Vars.maps.loadInternalMap(tempFile);
            if (map != null) {
                NetCoreClient.sendAction(Integer.parseInt(tempFile.nameWithoutExtension().replace("netcore_temp_", "")), "play");
                Vars.control.playMap(map, Vars.state.rules);
                setupExitListener();
            }
        } catch (Exception e) {
            Log.err("NetCore: Ошибка запуска карты", e);
            Vars.ui.showErrorMessage("Ошибка чтения файла карты.");
        }
    }

    private static void setupExitListener() {
        if (isListening) return;
        isListening = true;

        Events.on(StateChangeEvent.class, event -> {
            if (Vars.state.isMenu()) {
                if (tempFile != null && tempFile.exists()) {
                    tempFile.delete();
                    tempFile = null;
                }
            }
        });
    }
}
