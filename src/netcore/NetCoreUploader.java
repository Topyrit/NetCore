package netcore;

import arc.Core;
import arc.files.Fi;
import arc.util.Http;
import mindustry.Vars;

public class NetCoreUploader {
    private static final String UPLOAD_URL = "http://135.106.218";

    public static void uploadMap(Fi mapFile, String name, String author, String mode, String description, Runnable onSuccess) {
        if (!mapFile.exists()) {
            Vars.ui.showErrorMessage("Файл карты не найден.");
            return;
        }

        Vars.ui.loadAnd("Загрузка карты на сервер...", () -> {
            String boundary = "----NetCoreBoundary" + Long.toHexString(System.currentTimeMillis());
            
            StringBuilder sb = new StringBuilder();
            appendFormField(sb, boundary, "name", name);
            appendFormField(sb, boundary, "author", author);
            appendFormField(sb, boundary, "mode", mode.toLowerCase());
            appendFormField(sb, boundary, "description", description);
            
            sb.append("--").append(boundary).append("\r\n");
            sb.append("Content-Disposition: form-data; name=\"mapfile\"; filename=\"").append(mapFile.name()).append("\"\r\n");
            sb.append("Content-Type: application/octet-stream\r\n\r\n");

            byte[] headerBytes = sb.toString().getBytes();
            byte[] fileBytes = mapFile.readBytes();
            byte[] footerBytes = ("\r\n--" + boundary + "--\r\n").getBytes();

            byte[][] data = {headerBytes, fileBytes, footerBytes};
            int totalSize = 0;
            for (byte[] chunk : data) totalSize += chunk.length;

            byte[] payload = new byte[totalSize];
            int offset = 0;
            for (byte[] chunk : data) {
                System.arraycopy(chunk, 0, payload, offset, chunk.length);
                offset += chunk.length;
            }

            Http.post(UPLOAD_URL)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .content(payload)
                .submit(response -> {
                    Core.app.post(() -> {
                        Vars.ui.loadHide();
                        Vars.ui.showInfo("Карта успешно отправлена!");
                        if (onSuccess != null) onSuccess.run();
                    });
                }, error -> {
                    Core.app.post(() -> {
                        Vars.ui.loadHide();
                        Vars.ui.showErrorMessage("Ошибка загрузки на сервер.");
                    });
                });
        });
    }

    private static void appendFormField(StringBuilder sb, String boundary, String name, String value) {
        sb.append("--").append(boundary).append("\r\n");
        sb.append("Content-Disposition: form-data; name=\"").append(name).append("\"\r\n\r\n");
        sb.append(value).append("\r\n");
    }
}
